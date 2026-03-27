package com.example.deadmanswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.deadmanswitch.service.MonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 开机后自动启动监控服务
            val prefs = context.getSharedPreferences("deadman_prefs", 0)
            val autoStart = prefs.getBoolean("auto_start", true)
            
            if (autoStart) {
                context.startForegroundService(
                    Intent(context, MonitorService::class.java)
                )
            }
        }
    }
}