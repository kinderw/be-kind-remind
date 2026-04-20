package com.bekindremind.app.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.bekindremind.app.data.AlarmType
import com.bekindremind.app.data.ScheduledAlarmEntity
import com.bekindremind.app.data.TaskMode
import com.bekindremind.app.data.TaskRepository
import com.bekindremind.app.data.TripTaskEntity
import com.bekindremind.app.domain.EtaProvider
import com.bekindremind.app.domain.RoutePoint
import com.bekindremind.app.receivers.AlarmReceiver
import kotlin.math.abs

class SchedulingEngine(
    private val context: Context,
    private val repository: TaskRepository,
    private val etaProvider: EtaProvider
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleTask(task: TripTaskEntity): Long {
        val taskId = repository.saveTask(task)
        val reminderOffsets = parseOffsets(task.reminderOffsetsMinCsv)

        val leaveByUtc = when (task.mode) {
            TaskMode.LEAVE_AT -> task.timeUtc
            TaskMode.ARRIVE_BY -> {
                val etaMinutes = etaProvider.etaMinutes(
                    origin = RoutePoint(task.originLat ?: task.destinationLat, task.originLng ?: task.destinationLng),
                    destination = RoutePoint(task.destinationLat, task.destinationLng),
                    trafficModel = task.trafficModel
                )
                task.timeUtc - (etaMinutes + task.bufferMin) * 60_000L
            }
        }

        val rows = (reminderOffsets + 0).map { offset ->
            val type = offsetToType(offset)
            val fireAtUtc = leaveByUtc - offset * 60_000L
            val requestCode = requestCodeFor(taskId, offset)
            scheduleExactAlarm(taskId, type, requestCode, fireAtUtc)
            ScheduledAlarmEntity(
                taskId = taskId,
                fireAtUtc = fireAtUtc,
                type = type,
                requestCode = requestCode
            )
        }

        repository.replaceScheduledAlarms(taskId, rows)
        return taskId
    }

    suspend fun rescheduleIfShifted(task: TripTaskEntity, newEtaMin: Int): Boolean {
        val oldEta = task.lastKnownEtaMin ?: return false
        val oldLeaveByUtc = task.timeUtc - (oldEta + task.bufferMin) * 60_000L
        val newLeaveByUtc = task.timeUtc - (newEtaMin + task.bufferMin) * 60_000L
        val shouldShift = abs(newLeaveByUtc - oldLeaveByUtc) >= 5 * 60_000L
        if (!shouldShift) return false

        cancelFuture(task.id)
        scheduleTask(task.copy(lastKnownEtaMin = newEtaMin, updatedAtUtc = System.currentTimeMillis()))
        return true
    }

    suspend fun cancelFuture(taskId: Long) {
        val rows = repository.getScheduledAlarms(taskId)
        rows.forEach { row ->
            val pendingIntent = pendingIntent(taskId, row.type, row.requestCode)
            alarmManager.cancel(pendingIntent)
        }
        repository.replaceScheduledAlarms(taskId, emptyList())
    }

    private fun scheduleExactAlarm(taskId: Long, type: AlarmType, requestCode: Int, fireAtUtc: Long) {
        val pendingIntent = pendingIntent(taskId, type, requestCode)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAtUtc, pendingIntent)
    }

    private fun pendingIntent(taskId: Long, type: AlarmType, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(EXTRA_ALARM_TYPE, type.name)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCodeFor(taskId: Long, offsetMinutes: Int): Int {
        return (taskId.toInt() * 1000) + offsetMinutes
    }

    private fun parseOffsets(csv: String): List<Int> {
        return csv.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .sortedDescending()
    }

    private fun offsetToType(offset: Int): AlarmType {
        return when (offset) {
            60 -> AlarmType.MINUS_60
            30 -> AlarmType.MINUS_30
            15 -> AlarmType.MINUS_15
            5 -> AlarmType.MINUS_5
            else -> AlarmType.LEAVE_NOW
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
    }
}
