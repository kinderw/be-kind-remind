package com.bekindremind.app.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bekindremind.app.notifications.NotificationHelper

class DepartureTickerService : Service() {
    override fun onCreate() {
        super.onCreate()
        val helper = NotificationHelper(this)
        helper.ensureChannel()

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_DEPARTURE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Live departure updates")
            .setContentText("Monitoring traffic for your upcoming trip")
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: implement 2-3 minute tight-window ticker loop.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
