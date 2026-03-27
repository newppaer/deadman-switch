package com.example.deadmanswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager

// 仅通过 Manifest 静态接收 ACTION_USER_PRESENT（解锁）
// ACTION_SCREEN_OFF 在 Android 10+ 不允许静态注册，在 MonitorService 中动态注册
class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val settings = SettingsManager(context)
            settings.resetActivity()
            ActivityLogManager(context).addEntry("unlock")
        }
    }
}
