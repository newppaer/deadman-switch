package com.example.deadmanswitch.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("deadman_prefs", 0)

    companion object {
        const val KEY_THRESHOLD_HOURS = "threshold_hours"
        const val KEY_LAST_ACTIVITY = "last_activity"
        const val KEY_MONITORING = "is_monitoring"
        const val KEY_AUTO_START = "auto_start"
        const val KEY_LAST_ALERT_TIME = "last_alert_time"
        const val KEY_DARK_MODE = "dark_mode"
        const val DEFAULT_THRESHOLD = 12f
    }

    var thresholdHours: Float
        get() = prefs.getFloat(KEY_THRESHOLD_HOURS, DEFAULT_THRESHOLD)
        set(value) = prefs.edit().putFloat(KEY_THRESHOLD_HOURS, value).apply()

    var lastActivityTime: Long
        get() = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ACTIVITY, value).apply()

    var isMonitoring: Boolean
        get() = prefs.getBoolean(KEY_MONITORING, false)
        set(value) = prefs.edit().putBoolean(KEY_MONITORING, value).apply()

    var autoStart: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    var lastAlertTime: Long
        get() = prefs.getLong(KEY_LAST_ALERT_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ALERT_TIME, value).apply()

    var darkMode: Int
        get() = prefs.getInt(KEY_DARK_MODE, 0) // 0=跟随系统, 1=亮色, 2=暗色
        set(value) = prefs.edit().putInt(KEY_DARK_MODE, value).apply()

    val thresholdMs: Long
        get() = (thresholdHours * 60 * 60 * 1000).toLong()

    val remainingMs: Long
        get() {
            val last = lastActivityTime
            if (last == 0L) return thresholdMs
            val elapsed = System.currentTimeMillis() - last
            return (thresholdMs - elapsed).coerceAtLeast(0)
        }

    val remainingPercent: Float
        get() {
            val last = lastActivityTime
            if (last == 0L) return 0f
            val elapsed = System.currentTimeMillis() - last
            return (elapsed.toFloat() / thresholdMs).coerceIn(0f, 1f)
        }

    fun resetActivity() {
        lastActivityTime = System.currentTimeMillis()
        lastAlertTime = 0L
    }

    fun initIfNeeded() {
        if (lastActivityTime == 0L) {
            resetActivity()
        }
    }
}
