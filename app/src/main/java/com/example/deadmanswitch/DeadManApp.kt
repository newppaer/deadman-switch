package com.example.deadmanswitch

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class DeadManApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        CrashLogger.install(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                "alert_channel",
                "安全警报",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "长时间无活动提醒"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(alertChannel)
        }
    }
}
