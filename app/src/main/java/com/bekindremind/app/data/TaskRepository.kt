package com.bekindremind.app.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val alarmDao: ScheduledAlarmDao
) {
    fun observeTasks(): Flow<List<TripTaskEntity>> = taskDao.observeAll()

    suspend fun getAllTasks(): List<TripTaskEntity> = taskDao.getAll()

    suspend fun getScheduledTasks(): List<TripTaskEntity> = taskDao.getByStatus(TaskStatus.SCHEDULED)

    suspend fun saveTask(task: TripTaskEntity): Long = taskDao.upsert(task)

    suspend fun getTask(taskId: Long): TripTaskEntity? = taskDao.getById(taskId)

    suspend fun replaceScheduledAlarms(taskId: Long, rows: List<ScheduledAlarmEntity>) {
        alarmDao.deleteForTask(taskId)
        if (rows.isNotEmpty()) {
            alarmDao.insertAll(rows)
        }
    }

    suspend fun getScheduledAlarms(taskId: Long): List<ScheduledAlarmEntity> = alarmDao.listForTask(taskId)

    suspend fun deleteTask(taskId: Long) {
        alarmDao.deleteForTask(taskId)
        taskDao.delete(taskId)
    }
}
