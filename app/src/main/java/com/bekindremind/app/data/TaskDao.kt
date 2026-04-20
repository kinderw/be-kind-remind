package com.bekindremind.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM trip_task ORDER BY timeUtc ASC")
    fun observeAll(): Flow<List<TripTaskEntity>>

    @Query("SELECT * FROM trip_task ORDER BY timeUtc ASC")
    suspend fun getAll(): List<TripTaskEntity>

    @Query("SELECT * FROM trip_task WHERE status = :status ORDER BY timeUtc ASC")
    suspend fun getByStatus(status: TaskStatus): List<TripTaskEntity>

    @Query("SELECT * FROM trip_task WHERE id = :id")
    suspend fun getById(id: Long): TripTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TripTaskEntity): Long

    @Update
    suspend fun update(task: TripTaskEntity)

    @Query("DELETE FROM trip_task WHERE id = :id")
    suspend fun delete(id: Long)
}
