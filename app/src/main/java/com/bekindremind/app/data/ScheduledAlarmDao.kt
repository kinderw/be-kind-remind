package com.bekindremind.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScheduledAlarmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<ScheduledAlarmEntity>)

    @Query("SELECT * FROM scheduled_alarm WHERE taskId = :taskId ORDER BY fireAtUtc ASC")
    suspend fun listForTask(taskId: Long): List<ScheduledAlarmEntity>

    @Query("DELETE FROM scheduled_alarm WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)

    @Query("DELETE FROM scheduled_alarm WHERE taskId = :taskId AND fireAtUtc < :thresholdUtc")
    suspend fun deletePastForTask(taskId: Long, thresholdUtc: Long)
}
