package com.bekindremind.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.bekindremind.app.notifications.NotificationHelper
import com.bekindremind.app.scheduling.SchedulingEngine

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(SchedulingEngine.EXTRA_TASK_ID, -1L)
        val alarmType = intent.getStringExtra(SchedulingEngine.EXTRA_ALARM_TYPE) ?: "LEAVE_NOW"

        val notificationHelper = NotificationHelper(context)
        notificationHelper.ensureChannel()

        val notification = notificationHelper.buildDepartureNotification(
            title = "Departure reminder",
            message = "Task #$taskId ($alarmType)"
        )

        NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
    }
}
