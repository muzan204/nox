package com.muzan204.nox

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread
import org.json.JSONObject

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var face: NoxFaceView
    private lateinit var prefs: SharedPreferences
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())

    private val serverUrl: String
        get() = prefs.getString("server_url", "http://127.0.0.1:8765")!!.trimEnd('/')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("nox", Context.MODE_PRIVATE)
        face = NoxFaceView(this)
        face.onLongPress = { showServerConfig() }
        setContentView(face)

        window.statusBarColor = Color.rgb(5, 7, 13)
        window.navigationBarColor = Color.rgb(5, 7, 13)

        tts = TextToSpeech(this, this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }

        pollState()
    }

    fun onFaceTapped() {
        startListening()
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Reconhecimento de voz indisponível.")
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10)
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { face.state = "LISTENING" }
            override fun onBeginningOfSpeech() { face.state = "LISTENING" }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { face.state = "THINKING" }
            override fun onError(error: Int) {
                face.state = "ERROR"
                toast("Não consegui ouvir. Tente novamente.")
                handler.postDelayed({ face.state = "IDLE" }, 1200)
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) sendMessage(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        face.state = "LISTENING"
        speechRecognizer!!.startListening(intent)
    }

    private fun sendMessage(message: String) {
        face.state = "THINKING"
        thread {
            try {
                val connection = URL("$serverUrl/api/chat").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

                val body = JSONObject().put("message", message).toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                if (!json.optBoolean("ok", false)) throw Exception(json.optString("error", "Erro no NOX."))

                val answer = json.optString("text", "Não recebi uma resposta.")
                runOnUiThread {
                    face.state = "SPEAKING"
                    speak(answer)
                }
            } catch (error: Exception) {
                runOnUiThread {
                    face.state = "ERROR"
                    toast("NOX: \${error.message ?: "erro de conexão"}")
                    handler.postDelayed({ face.state = "IDLE" }, 1800)
                }
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NOX_REPLY")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "BR")
            tts?.setSpeechRate(0.98f)
        }
    }

    private fun pollState() {
        thread {
            try {
                val connection = URL("$serverUrl/api/face-state").openConnection() as HttpURLConnection
                connection.connectTimeout = 2000
                connection.readTimeout = 2000
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val state = json.optString("state", "IDLE")
                runOnUiThread { if (face.state != "SPEAKING" && face.state != "LISTENING") face.state = state }
            } catch (_: Exception) {}
        }
        handler.postDelayed({ pollState() }, 2500)
    }

    private fun showServerConfig() {
        val input = android.widget.EditText(this)
        input.setText(serverUrl)
        input.hint = "http://192.168.1.10:8765"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Conectar NOX Core")
            .setMessage("Use o endereço do PC na mesma rede Wi-Fi.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val value = input.text.toString().trim().trimEnd('/')
                if (value.startsWith("http://") || value.startsWith("https://")) {
                    prefs.edit().putString("server_url", value).apply()
                    toast("Servidor salvo.")
                } else {
                    toast("Use http:// ou https://")
                }
            }
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}