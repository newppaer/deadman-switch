package com.example.deadmanswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.deadmanswitch.service.MonitorService
import com.example.deadmanswitch.utils.Logging

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                Logging.logMonitoringEvent("Device boot completed")
                
                // 开机后自动启动监控服务
                val prefs = context.getSharedPreferences("deadman_prefs", 0)
                val autoStart = prefs.getBoolean("auto_start", true)
                
                Logging.d("BootReceiver", "Auto-start setting: $autoStart")
                
                if (autoStart) {
                    Logging.logMonitoringEvent("Auto-starting monitoring service")
                    context.startForegroundService(
                        Intent(context, MonitorService::class.java)
                    )
                    Logging.d("BootReceiver", "Foreground service start requested")
                }
            }
        } catch (e: Exception) {
            Logging.e("BootReceiver", "Error in onReceive", e)
        }
    }
}