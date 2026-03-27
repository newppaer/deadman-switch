package com.example.deadmanswitch

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class DeadManApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                "monitor_channel",
                "监控服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持后台监控运行"
            }

            val alertChannel = NotificationChannel(
                "alert_channel",
                "安全警报",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "长时间无活动提醒"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(monitorChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }
}