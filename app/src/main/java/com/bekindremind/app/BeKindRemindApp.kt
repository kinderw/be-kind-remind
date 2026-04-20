package com.bekindremind.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bekindremind.app.data.AppDatabase
import com.bekindremind.app.data.TaskRepository
import com.bekindremind.app.di.AppGraph
import com.bekindremind.app.workers.EtaRefreshWorker
import java.util.concurrent.TimeUnit

class BeKindRemindApp : Application() {
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        val repository = TaskRepository(db.taskDao(), db.scheduledAlarmDao())
        appGraph = AppGraph(this, repository)
        scheduleEtaRefreshWorker()
    }

    private fun scheduleEtaRefreshWorker() {
        val request = PeriodicWorkRequestBuilder<EtaRefreshWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ETA_REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        private const val ETA_REFRESH_WORK_NAME = "eta_refresh_work"
    }
}
