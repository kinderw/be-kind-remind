package com.bekindremind.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bekindremind.app.BeKindRemindApp
import com.bekindremind.app.data.TaskMode

class EtaRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val appGraph = (applicationContext as BeKindRemindApp).appGraph

        return try {
            val tasks = appGraph.repository.getScheduledTasks()
            tasks.filter { it.mode == TaskMode.ARRIVE_BY }.forEach { task ->
                val etaMinutes = appGraph.etaProvider.etaMinutes(
                    origin = com.bekindremind.app.domain.RoutePoint(
                        task.originLat ?: task.destinationLat,
                        task.originLng ?: task.destinationLng
                    ),
                    destination = com.bekindremind.app.domain.RoutePoint(task.destinationLat, task.destinationLng),
                    trafficModel = task.trafficModel
                )
                appGraph.schedulingEngine.rescheduleIfShifted(task, etaMinutes)
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
