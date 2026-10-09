package com.muzan204.nox

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Escuta continuamente em segundo plano pela palavra-chave "NOX".
 * Quando a ouve, se pausa automaticamente e avisa a MainActivity via
 * broadcast (ACTION_WAKE), que então abre o microfone para o comando,
 * exatamente como um toque na carinha. A MainActivity manda PAUSE/RESUME
 * para evitar dois SpeechRecognizer concorrendo pelo microfone.
 */
class WakeWordService : Service() {

    companion object {
        const val ACTION_PAUSE = "com.muzan204.nox.action.PAUSE_WAKE"
        const val ACTION_RESUME = "com.muzan204.nox.action.RESUME_WAKE"
        const val ACTION_WAKE = "com.muzan204.nox.WAKE"
        private const val CHANNEL_ID = "nox_voice"
        private const val NOTIF_ID = 42
    }

    private var recognizer: SpeechRecognizer? = null
    private val wakeWord = "nox"
    private var paused = false
    private val handler = Handler(Looper.getMainLooper())

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                paused = true
                recognizer?.cancel()
            }
            ACTION_RESUME -> {
                paused = false
                listen()
            }
        }
        return START_STICKY
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun listen() {
        if (paused || !hasMicPermission() || !SpeechRecognizer.isRecognitionAvailable(this)) return

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.lowercase(Locale("pt","BR")).orEmpty()
                if (text.contains(wakeWord)) {
                    // Pausa já aqui, antes do broadcast: evita que este serviço volte a
                    // escutar e dispute o microfone com o reconhecimento da MainActivity.
                    paused = true
                    sendBroadcast(Intent(ACTION_WAKE).setPackage(packageName))
                    return
                }
                if (!paused) listen()
            }
            override fun onError(error: Int) {
                if (paused) return
                // Timeout/silêncio são normais numa escuta contínua e podem tentar de novo
                // na hora; outros erros ganham uma pequena pausa para não bater CPU/bateria à toa.
                val delay = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 0L
                    else -> 1500L
                }
                handler.postDelayed({ if (!paused) listen() }, delay)
            }
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
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        })
    }

    override fun onDestroy() {
        recognizer?.destroy()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
