package com.muzan204.nox

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.Window
import android.widget.EditText
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

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: android.content.Context?, i: android.content.Intent?) {
            if (!isFinishing && !listening) startListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        face = findViewById(R.id.face)
        api = NoxApi(this)
        memory = NoxMemory(this)

        // A Activity must open even when microphone permission is unavailable.
        // Wake Word is started only after the user grants microphone access.
        registerWakeReceiver()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                10
            )
        } else {
            startWakeWordSafely()
        }

        face.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    downAt = System.currentTimeMillis()
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val held = System.currentTimeMillis() - downAt > 800
                    if (held) showConnectionSettings() else onFaceTapped()
                    true
                }
                else -> true
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 10 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startWakeWordSafely()
        }
    }

    private fun registerWakeReceiver() {
        if (wakeRegistered) return
        registerReceiver(
            wakeReceiver,
            IntentFilter("com.muzan204.nox.WAKE"),
            RECEIVER_NOT_EXPORTED
        )
        wakeRegistered = true
    }

    private fun startWakeWordSafely() {
        try {
            startService(Intent(this, WakeWordService::class.java))
        } catch (_: Exception) {
            // The face must remain usable even if the background wake service
            // is unavailable on a particular Android/OEM configuration.
        }
    }

    fun onFaceTapped() {
        if (!listening) startListening()
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                10
            )
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            face.state = "ERROR"
            return
        }

        try {
            listening = true
            face.state = "LISTENING"
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)

            recognizer!!.setRecognitionListener(object : SimpleRecognitionListener() {
                override fun onResults(results: Bundle) {
                    listening = false
                    val text = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()

                    if (text.isNotBlank()) send(text)
                    else face.state = "IDLE"
                }

                override fun onError(error: Int) {
                    listening = false
                    face.state = "IDLE"
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR"))
            }

            recognizer!!.startListening(intent)
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
                runOnUiThread { face.state = "ERROR" }
                return@Thread
            }

            memory.save("assistant", result.text)
            runOnUiThread { face.state = "SPEAKING" }

            val audio = api.voice(result.text)
            if (audio != null) playAudio(audio)
            else runOnUiThread { face.state = "IDLE" }
        }.start()
    }

    private fun playAudio(bytes: ByteArray) {
        runOnUiThread {
            try {
                player?.release()

                val file = File(cacheDir, "nox-voice.mp3")
                file.writeBytes(bytes)

                player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnCompletionListener {
                        release()
                        player = null
                        face.state = "IDLE"
                    }
                    setOnErrorListener { _, _, _ ->
                        face.state = "ERROR"
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
        val input = EditText(this)
        input.setText(api.baseUrl())
        input.hint = "http://192.168.1.10:8765"

        AlertDialog.Builder(this)
            .setTitle("NOX Core")
            .setMessage("Endereço do Termux ou computador na mesma rede.")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                api.saveBaseUrl(input.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        recognizer?.destroy()

        if (wakeRegistered) {
            try {
                unregisterReceiver(wakeReceiver)
            } catch (_: Exception) {
            }
            wakeRegistered = false
        }

        stopService(Intent(this, WakeWordService::class.java))
        player?.release()
        super.onDestroy()
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
