package com.example.deadmanswitch

import android.app.Application
import com.example.deadmanswitch.utils.NotificationManager

class DeadManApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Notification channels are now created in NotificationManager
        // No need to create them here as NotificationManager handles it
    }
}