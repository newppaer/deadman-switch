package com.example.deadmanswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager
import com.example.deadmanswitch.service.MonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settings = SettingsManager(context)
            if (settings.autoStart && settings.isMonitoring) {
                context.startForegroundService(
                    Intent(context, MonitorService::class.java)
                )
            }
            ActivityLogManager(context).addEntry("boot")
        }
    }
}
