package com.muzan204.nox

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.*
import androidx.core.app.NotificationCompat
import java.util.Locale

class WakeWordService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private val wakeWord = "nox"

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("nox_voice", "NOX ativação por voz", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        startForeground(42, NotificationCompat.Builder(this, "nox_voice")
            .setContentTitle("NOX")
            .setContentText("Diga “NOX” para ativar")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true).build())
        listen()
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.lowercase(Locale("pt","BR")).orEmpty()
                if (text.contains(wakeWord)) {
                    sendBroadcast(Intent("com.muzan204.nox.WAKE").setPackage(packageName))
                }
                listen()
            }
            override fun onError(error: Int) { listen() }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(t: Int, b: Bundle?) {}
        })
        recognizer!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        })
    }

    override fun onDestroy() {
        recognizer?.destroy()
        super.onDestroy()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
