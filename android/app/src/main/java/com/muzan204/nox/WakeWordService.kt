package com.muzan204.nox

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class WakeWordService : Service() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("nox_voice", "NOX voz", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        startForeground(42, NotificationCompat.Builder(this, "nox_voice")
            .setContentTitle("NOX")
            .setContentText("Ativação por voz disponível")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true).build())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
