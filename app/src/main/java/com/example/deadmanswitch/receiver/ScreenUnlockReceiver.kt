package com.example.deadmanswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            // 用户解锁屏幕，重置活动时间
            val prefs = context.getSharedPreferences("deadman_prefs", 0)
            prefs.edit().putLong("last_activity", System.currentTimeMillis()).apply()
        }
    }
}