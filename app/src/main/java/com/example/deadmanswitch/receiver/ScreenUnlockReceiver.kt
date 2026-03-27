package com.example.deadmanswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val settings = SettingsManager(context)
            settings.resetActivity()
            ActivityLogManager(context).addEntry("unlock")
        }
    }
}
