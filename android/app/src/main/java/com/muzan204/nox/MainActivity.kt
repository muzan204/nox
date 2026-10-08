package com.muzan204.nox

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.content.BroadcastReceiver
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
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var player: MediaPlayer? = null
    private var downAt = 0L
    private val wakeReceiver = object : BroadcastReceiver() { override fun onReceive(c: android.content.Context?, i: android.content.Intent?) { if (!listening) startListening() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        face = findViewById(R.id.face)
        api = NoxApi(this)
        registerReceiver(wakeReceiver, IntentFilter("com.muzan204.nox.WAKE"), RECEIVER_NOT_EXPORTED)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startService(Intent(this, WakeWordService::class.java))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10)

        face.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> { downAt = System.currentTimeMillis(); true }
                android.view.MotionEvent.ACTION_UP -> {
                    val held = System.currentTimeMillis() - downAt > 800
                    if (held) showConnectionSettings() else onFaceTapped()
                    true
                }
                else -> true
            }
        }
    }

    fun onFaceTapped() { if (!listening) startListening() }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { face.state = "ERROR"; return }
        listening = true
        face.state = "LISTENING"
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer!!.setRecognitionListener(object : SimpleRecognitionListener() {
            override fun onResults(results: Bundle) {
                listening = false
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isNotBlank()) send(text) else face.state = "IDLE"
            }
            override fun onError(error: Int) { listening = false; face.state = "IDLE" }
        })
        recognizer!!.startListening(RecognizerIntent().apply {
            action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR"))
        })
    }

    private fun send(text: String) {
        face.state = "THINKING"
        Thread {
            val result = api.chat(text)
            if (result == null) {
                runOnUiThread { face.state = "ERROR" }
                return@Thread
            }
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
                    setOnCompletionListener { release(); player = null; face.state = "IDLE" }
                    setOnErrorListener { _, _, _ -> face.state = "ERROR"; true }
                    prepare()
                    start()
                }
            } catch (_: Exception) { face.state = "ERROR" }
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
            .setPositiveButton("Salvar") { _, _ -> api.saveBaseUrl(input.text.toString()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        recognizer?.destroy()
        unregisterReceiver(wakeReceiver)
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
}
