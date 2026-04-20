package com.bekindremind.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Boot/package replaced detected. Alarm restore hook triggered.")
            // Foundation slice: receiver wiring is in place.
            // Next step: resolve repository + scheduling engine and re-schedule all future alarms.
        }
    }

    companion object {
        private const val TAG = "BootRestoreReceiver"
    }
}
