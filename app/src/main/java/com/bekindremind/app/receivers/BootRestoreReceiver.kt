package com.bekindremind.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bekindremind.app.BeKindRemindApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        val appGraph = (context.applicationContext as BeKindRemindApp).appGraph

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = appGraph.repository.getScheduledTasks()
                tasks.forEach { task ->
                    appGraph.schedulingEngine.scheduleTask(task)
                }
                Log.i(TAG, "Restored ${tasks.size} scheduled tasks after reboot/package replace")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to restore alarms", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootRestoreReceiver"
    }
}
