package com.muzan204.nox

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var face: NoxFaceView
    private lateinit var prefs: SharedPreferences
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private var reconnectDelayMs = 2000L
    private var reconnectScheduled = false

    private val serverUrl: String
        get() = prefs.getString("server_url", "")!!.trimEnd('/')
    private val deviceToken: String
        get() = prefs.getString("device_token", "")!!

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            startListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("nox", Context.MODE_PRIVATE)
        face = NoxFaceView(this)
        face.onLongPress = { showServerConfig() }
        setContentView(face)

        window.statusBarColor = Color.rgb(5, 7, 13)
        window.navigationBarColor = Color.rgb(5, 7, 13)
        tts = TextToSpeech(this, this)
        requestNeededPermissions()

        if (serverUrl.isBlank() || deviceToken.isBlank()) {
            face.state = "CONFUSED"
            handler.postDelayed({ if (!isFinishing) showServerConfig() }, 500)
        } else {
            connectGateway()
            pollState()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(WakeWordService.ACTION_WAKE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(wakeReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(wakeReceiver) } catch (_: Exception) {}
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed += Manifest.permission.RECORD_AUDIO
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        if (needed.isEmpty()) startWakeService()
        else ActivityCompat.requestPermissions(this, needed.toTypedArray(), 10)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startWakeService()
        }
    }

    private fun startWakeService() {
        ContextCompat.startForegroundService(this, Intent(this, WakeWordService::class.java))
    }

    private fun pauseWakeService() {
        startService(Intent(this, WakeWordService::class.java).setAction(WakeWordService.ACTION_PAUSE))
    }

    private fun resumeWakeService() {
        startService(Intent(this, WakeWordService::class.java).setAction(WakeWordService.ACTION_RESUME))
    }

    fun onFaceTapped() {
        startListening()
    }

    private fun startListening() {
        if (serverUrl.isBlank() || deviceToken.isBlank()) {
            showServerConfig()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Reconhecimento de voz indisponível.")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestNeededPermissions()
            return
        }

        pauseWakeService()
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { face.state = "LISTENING" }
            override fun onBeginningOfSpeech() { face.state = "LISTENING" }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { face.state = "THINKING" }
            override fun onError(error: Int) {
                resumeWakeService()
                face.state = "ERROR"
                toast("Não consegui ouvir. Tente novamente.")
                handler.postDelayed({ face.state = "IDLE" }, 1200)
            }
            override fun onResults(results: Bundle?) {
                resumeWakeService()
                val recognized = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!recognized.isNullOrBlank()) sendMessage(recognized) else face.state = "IDLE"
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        face.state = "LISTENING"
        speechRecognizer!!.startListening(intent)
    }

    private fun sendMessage(message: String) {
        face.state = "THINKING"
        thread {
            try {
                val connection = (URL("$serverUrl/api/chat").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 90000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("X-NOX-Token", deviceToken)
                }
                val body = JSONObject().put("message", message).toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val ok = connection.responseCode in 200..299
                val stream = if (ok) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader()?.use { it.readText() } ?: ""
                val json = JSONObject(raw)
                if (!json.optBoolean("ok", false)) {
                    throw Exception(json.optString("error", "Erro no NOX (HTTP ${connection.responseCode})."))
                }
                val answer = json.optString("text", "Não recebi uma resposta.")
                runOnUiThread {
                    face.state = "SPEAKING"
                    speak(answer)
                }
                connection.disconnect()
            } catch (error: Exception) {
                runOnUiThread {
                    face.state = "ERROR"
                    toast("NOX: ${error.message ?: "erro de conexão"}")
                    handler.postDelayed({ face.state = "IDLE" }, 1800)
                }
            }
        }
    }

    private fun connectGateway() {
        if (serverUrl.isBlank() || deviceToken.isBlank()) return
        webSocket?.cancel()
        val wsUrl = serverUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/ws"
        val request = Request.Builder().url(wsUrl).header("X-NOX-Token", deviceToken).build()
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectDelayMs = 2000L
                reconnectScheduled = false
                runOnUiThread {
                    face.statusText = "CONECTADO • recebendo monitoramento"
                    if (face.state == "ERROR") face.state = "IDLE"
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val data = JSONObject(text)
                    if (data.optString("type") == "snapshot") {
                        val state = data.optString("faceState", "IDLE")
                        val device = data.optJSONObject("device")
                        val memory = device?.optJSONObject("memory")
                        val ram = memory?.optInt("usedPercent", -1) ?: -1
                        val load = device?.optJSONArray("loadAverage")?.optDouble(0, 0.0) ?: 0.0
                        val wifi = device?.optJSONArray("network")?.length()?.let { it > 0 } ?: false
                        val status = if (ram >= 0) "ONLINE • RAM ${ram}% • CPU ${"%.1f".format(Locale.US, load)} • ${if (wifi) "Wi-Fi" else "LAN"}" else "ONLINE • servidor conectado"
                        runOnUiThread {
                            face.statusText = status
                            if (face.state != "SPEAKING" && face.state != "LISTENING") face.state = state
                        }
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread { face.statusText = "SEM CONEXÃO • tentando reconectar" }
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectScheduled || isFinishing) return
        reconnectScheduled = true
        handler.postDelayed({
            reconnectScheduled = false
            connectGateway()
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30000L)
        }, reconnectDelayMs)
    }

    private fun pollState() {
        if (serverUrl.isNotBlank() && deviceToken.isNotBlank()) {
            thread {
                try {
                    val connection = (URL("$serverUrl/api/face-state").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 2000
                        readTimeout = 2000
                        setRequestProperty("X-NOX-Token", deviceToken)
                    }
                    val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val state = json.optString("state", "IDLE")
                    runOnUiThread { if (face.state != "SPEAKING" && face.state != "LISTENING") face.state = state }
                    connection.disconnect()
                } catch (_: Exception) {}
            }
        }
        handler.postDelayed({ pollState() }, 8000)
    }

    private fun showServerConfig() {
        val urlInput = EditText(this).apply {
            setText(serverUrl)
            hint = "http://192.168.1.10:8766"
            setSingleLine(true)
        }
        val tokenInput = EditText(this).apply {
            setText(deviceToken)
            hint = "Token exibido em logs/gateway.log"
            singleLine = true
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 8, 48, 0)
            addView(urlInput)
            addView(tokenInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Parear NOX com Redmi 15C")
            .setMessage("No Redmi 15C, inicie o servidor e copie o endereço IP e o token de pareamento exibidos em ~/nox/logs/gateway.log. Os dois celulares devem estar na mesma rede Wi-Fi.")
            .setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Conectar") { _, _ ->
                val url = urlInput.text.toString().trim().trimEnd('/')
                val token = tokenInput.text.toString().trim()
                if ((url.startsWith("http://") || url.startsWith("https://")) && token.length >= 32) {
                    prefs.edit().putString("server_url", url).putString("device_token", token).apply()
                    webSocket?.cancel()
                    connectGateway()
                    pollState()
                    toast("Configuração salva. Conectando ao NOX...")
                } else {
                    toast("Informe o endereço http(s) e o token completo.")
                }
            }
            .show()
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NOX_REPLY")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "BR")
            tts?.setSpeechRate(0.98f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    runOnUiThread { if (face.state == "SPEAKING") face.state = "IDLE" }
                }
                @Deprecated("Deprecated in Java", ReplaceWith(""))
                override fun onError(utteranceId: String?) {
                    runOnUiThread { if (face.state == "SPEAKING") face.state = "IDLE" }
                }
            })
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "activity destroyed")
        httpClient.dispatcher.executorService.shutdown()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
