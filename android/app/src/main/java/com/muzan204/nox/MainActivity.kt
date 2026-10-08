package com.muzan204.nox

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.MotionEvent
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var face: NoxFaceView
    private lateinit var api: NoxApi
    private lateinit var memory: NoxMemory

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var player: MediaPlayer? = null
    private var downAt = 0L
    private var wakeRegistered = false
    private var destroyed = false

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!destroyed && !isFinishing && !listening) {
                startListening()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        face = findViewById(R.id.face)
        api = NoxApi(this)
        memory = NoxMemory(this)

        registerWakeReceiver()

        face.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downAt = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val held = System.currentTimeMillis() - downAt >= 800L
                    if (held) {
                        showConnectionSettings()
                    } else {
                        onFaceTapped()
                    }
                    true
                }

                else -> true
            }
        }

        face.state = "IDLE"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_AUDIO) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startListening()
            } else {
                face.state = "IDLE"
                Toast.makeText(
                    this,
                    "Microfone necessário para conversar com o NOX.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun registerWakeReceiver() {
        if (wakeRegistered) return

        try {
            registerReceiver(
                wakeReceiver,
                IntentFilter(WAKE_ACTION),
                RECEIVER_NOT_EXPORTED
            )
            wakeRegistered = true
        } catch (_: Exception) {
            wakeRegistered = false
        }
    }

    fun onFaceTapped() {
        if (listening) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO
            )
            return
        }

        startListening()
    }

    private fun startListening() {
        if (destroyed || listening) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            face.state = "ERROR"
            Toast.makeText(
                this,
                "Reconhecimento de voz não disponível neste Android.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            listening = true
            face.state = "LISTENING"

            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)

            recognizer?.setRecognitionListener(object : SimpleRecognitionListener() {
                override fun onResults(results: Bundle) {
                    listening = false

                    val text = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()

                    if (text.isBlank()) {
                        face.state = "IDLE"
                    } else {
                        send(text)
                    }
                }

                override fun onError(error: Int) {
                    listening = false
                    face.state = "IDLE"
                }

                override fun onEndOfSpeech() {
                    face.state = "THINKING"
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale("pt", "BR")
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer?.startListening(intent)
        } catch (_: Exception) {
            listening = false
            face.state = "ERROR"
        }
    }

    private fun send(text: String) {
        memory.save("user", text)
        face.state = "THINKING"

        Thread {
            val result = api.chat(text)

            if (result == null) {
                runOnUiThread {
                    face.state = "ERROR"
                    Toast.makeText(
                        this,
                        "Não consegui conectar ao NOX Core.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@Thread
            }

            memory.save("assistant", result.text)

            runOnUiThread {
                if (!destroyed) {
                    face.state = "SPEAKING"
                }
            }

            val audio = api.voice(result.text)

            if (audio != null) {
                playAudio(audio)
            } else {
                runOnUiThread {
                    if (!destroyed) {
                        face.state = "IDLE"
                    }
                }
            }
        }.start()
    }

    private fun playAudio(bytes: ByteArray) {
        runOnUiThread {
            if (destroyed) return@runOnUiThread

            try {
                player?.release()

                val file = File(cacheDir, "nox-voice.mp3")
                file.writeBytes(bytes)

                player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)

                    setOnCompletionListener {
                        release()
                        if (player === this) {
                            player = null
                        }
                        if (!destroyed) {
                            face.state = "IDLE"
                        }
                    }

                    setOnErrorListener { _, _, _ ->
                        release()
                        if (player === this) {
                            player = null
                        }
                        if (!destroyed) {
                            face.state = "ERROR"
                        }
                        true
                    }

                    prepare()
                    start()
                }
            } catch (_: Exception) {
                face.state = "ERROR"
            }
        }
    }

    private fun showConnectionSettings() {
        val input = EditText(this).apply {
            setText(api.baseUrl())
            hint = "http://192.168.1.10:8765"
            selectAll()
        }

        AlertDialog.Builder(this)
            .setTitle("NOX Core")
            .setMessage(
                "Configure o endereço do Core. No próprio Termux use 127.0.0.1; " +
                    "para outro computador use o IP dele na rede."
            )
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                api.saveBaseUrl(input.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        destroyed = true
        listening = false

        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null

        if (wakeRegistered) {
            try {
                unregisterReceiver(wakeReceiver)
            } catch (_: Exception) {
            }
            wakeRegistered = false
        }

        // Não iniciamos o WakeWordService automaticamente.
        // Isso mantém a inicialização da interface independente do microfone.
        try {
            stopService(Intent(this, WakeWordService::class.java))
        } catch (_: Exception) {
        }

        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null

        super.onDestroy()
    }

    companion object {
        private const val REQUEST_AUDIO = 10
        private const val WAKE_ACTION = "com.muzan204.nox.WAKE"
    }
}

open class SimpleRecognitionListener : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
    override fun onError(error: Int) {}
    override fun onResults(results: Bundle) {}
}
