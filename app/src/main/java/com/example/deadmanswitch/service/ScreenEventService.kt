package com.example.deadmanswitch.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager

/**
 * 轻量屏幕事件监听服务
 * 仅在 UsageStats 权限未授权时作为备用方案
 * 无前台通知、无 WakeLock、无轮询
 */
class ScreenEventService : Service() {

    companion object {
        private const val TAG = "ScreenEventService"

        fun isRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences("deadman_prefs", 0)
            return prefs.getBoolean("screen_service_running", false)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF")
                    ActivityLogManager(context).addEntry("lock")
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "User PRESENT (unlock)")
                    val settings = SettingsManager(context)
                    settings.resetActivity()
                    ActivityLogManager(context).addEntry("unlock")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ScreenEventService started")
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(screenReceiver, filter)
            }

            val prefs = getSharedPreferences("deadman_prefs", 0)
            prefs.edit().putBoolean("screen_service_running", true).apply()

            Log.d(TAG, "Screen receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receiver", e)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ScreenEventService destroyed")
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        val prefs = getSharedPreferences("deadman_prefs", 0)
        prefs.edit().putBoolean("screen_service_running", false).apply()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
