package com.example.deadmanswitch

import android.app.Application
import com.example.deadmanswitch.utils.Logging

class DeadManApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化日志系统
        Logging.initialize(this)
        Logging.i("DeadManApp", "Application created")
        
        // Notification channels are created in AppNotificationManager
    }
}