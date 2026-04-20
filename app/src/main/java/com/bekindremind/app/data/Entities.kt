package com.bekindremind.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trip_task")
data class TripTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: TaskMode,
    val title: String?,
    val originLat: Double?,
    val originLng: Double?,
    val originLabel: String?,
    val destinationPlaceId: String?,
    val destinationLat: Double,
    val destinationLng: Double,
    val timeUtc: Long,
    val timeZoneId: String,
    val reminderOffsetsMinCsv: String,
    val bufferMin: Int,
    val trafficModel: TrafficModel,
    val lastKnownEtaMin: Int?,
    val status: TaskStatus,
    val createdAtUtc: Long,
    val updatedAtUtc: Long
)

@Entity(
    tableName = "scheduled_alarm",
    foreignKeys = [
        ForeignKey(
            entity = TripTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class ScheduledAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val fireAtUtc: Long,
    val type: AlarmType,
    val requestCode: Int
)

enum class TaskMode { ARRIVE_BY, LEAVE_AT }

enum class TrafficModel { BEST_GUESS, OPTIMISTIC, PESSIMISTIC }

enum class TaskStatus { SCHEDULED, COMPLETED, DISMISSED, CANCELED }

enum class AlarmType { MINUS_60, MINUS_30, MINUS_15, MINUS_5, LEAVE_NOW }
