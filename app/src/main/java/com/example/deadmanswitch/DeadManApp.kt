package com.example.deadmanswitch

import android.app.Application

class DeadManApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Notification channels are created in AppNotificationManager
    }
}