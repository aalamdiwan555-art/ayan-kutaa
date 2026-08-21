package com.autopilot.driver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                AppPrefs.init(context.applicationContext)
            } catch (exception: Exception) {
                Log.w(TAG, "Encrypted preferences unavailable during boot", exception)
                return
            }
            BotState.isRunning = false
            AppPrefs.isBotRunning = false
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
