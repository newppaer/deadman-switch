package com.example.deadmanswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> {
                // 解锁屏幕
                val settings = SettingsManager(context)
                settings.resetActivity()
                ActivityLogManager(context).addEntry("unlock")
            }
            Intent.ACTION_SCREEN_OFF -> {
                // 锁屏
                ActivityLogManager(context).addEntry("lock")
            }
        }
    }
}
