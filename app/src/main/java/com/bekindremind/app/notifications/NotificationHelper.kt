package com.bekindremind.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.bekindremind.app.R

class NotificationHelper(private val context: Context) {
    fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_DEPARTURE,
            "Departure reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for leave-by and leave-at reminders"
        }
        manager.createNotificationChannel(channel)
    }

    fun buildDepartureNotification(title: String, message: String) =
        NotificationCompat.Builder(context, CHANNEL_DEPARTURE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

    companion object {
        const val CHANNEL_DEPARTURE = "departure_reminders"
    }
}
