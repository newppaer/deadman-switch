package com.example.deadmanswitch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.deadmanswitch.utils.ActivityDetector
import com.example.deadmanswitch.utils.Logging

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                Logging.logMonitoringEvent("Screen unlocked")
                
                // 用户解锁屏幕，记录解锁时间并更新活动时间
                val prefs = context.getSharedPreferences("deadman_prefs", 0)
                val now = System.currentTimeMillis()
                
                // 记录解锁时间（用于快速检测）
                prefs.edit().putLong("last_unlock", now).apply()
                
                // 更新最后活动时间
                prefs.edit().putLong("last_activity", now).apply()
                
                Logging.d("ScreenUnlockReceiver", "Screen unlock recorded at $now")
            }
        } catch (e: Exception) {
            Logging.e("ScreenUnlockReceiver", "Error in onReceive", e)
        }
    }
}