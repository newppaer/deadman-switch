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
        const val KEY_WECOM_WEBHOOK = "wecom_webhook_url"
        const val KEY_WECOM_ENABLED = "wecom_enabled"
        const val KEY_WECOM_MESSAGE = "wecom_message"
        const val KEY_OPENCLAW_API_URL = "openclaw_api_url"
        const val KEY_OPENCLAW_TOKEN = "openclaw_token"
        const val KEY_OPENCLAW_ENABLED = "openclaw_enabled"
        const val KEY_OPENCLAW_MESSAGE = "openclaw_message"
        // 暂停计时
        const val KEY_PAUSE_ENABLED = "pause_enabled"
        const val KEY_PAUSE_START_HOUR = "pause_start_hour"
        const val KEY_PAUSE_START_MINUTE = "pause_start_minute"
        const val KEY_PAUSE_END_HOUR = "pause_end_hour"
        const val KEY_PAUSE_END_MINUTE = "pause_end_minute"
        // 持久化统计
        const val KEY_TOTAL_UNLOCK_COUNT = "total_unlock_count"
        const val KEY_TOTAL_LOCK_COUNT = "total_lock_count"
        const val KEY_TOTAL_ALERT_COUNT = "total_alert_count"
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

    // 企业微信 Webhook URL
    var wecomWebhookUrl: String
        get() = prefs.getString(KEY_WECOM_WEBHOOK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WECOM_WEBHOOK, value).apply()

    // 企业微信推送开关
    var wecomEnabled: Boolean
        get() = prefs.getBoolean(KEY_WECOM_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WECOM_ENABLED, value).apply()

    // 企业微信自定义消息
    var wecomMessage: String
        get() = prefs.getString(KEY_WECOM_MESSAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WECOM_MESSAGE, value).apply()

    // OpenClaw API URL
    var openclawApiUrl: String
        get() = prefs.getString(KEY_OPENCLAW_API_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENCLAW_API_URL, value).apply()

    // OpenClaw Token（可选）
    var openclawToken: String
        get() = prefs.getString(KEY_OPENCLAW_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENCLAW_TOKEN, value).apply()

    // OpenClaw 推送开关
    var openclawEnabled: Boolean
        get() = prefs.getBoolean(KEY_OPENCLAW_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OPENCLAW_ENABLED, value).apply()

    // OpenClaw 自定义消息
    var openclawMessage: String
        get() = prefs.getString(KEY_OPENCLAW_MESSAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENCLAW_MESSAGE, value).apply()

    // ===== 暂停计时 =====
    var pauseEnabled: Boolean
        get() = prefs.getBoolean(KEY_PAUSE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PAUSE_ENABLED, value).apply()

    var pauseStartHour: Int
        get() = prefs.getInt(KEY_PAUSE_START_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_PAUSE_START_HOUR, value).apply()

    var pauseStartMinute: Int
        get() = prefs.getInt(KEY_PAUSE_START_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_PAUSE_START_MINUTE, value).apply()

    var pauseEndHour: Int
        get() = prefs.getInt(KEY_PAUSE_END_HOUR, 6)
        set(value) = prefs.edit().putInt(KEY_PAUSE_END_HOUR, value).apply()

    var pauseEndMinute: Int
        get() = prefs.getInt(KEY_PAUSE_END_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_PAUSE_END_MINUTE, value).apply()

    // ===== 持久化统计 =====
    var totalUnlockCount: Int
        get() = prefs.getInt(KEY_TOTAL_UNLOCK_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_UNLOCK_COUNT, value).apply()

    var totalLockCount: Int
        get() = prefs.getInt(KEY_TOTAL_LOCK_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_LOCK_COUNT, value).apply()

    var totalAlertCount: Int
        get() = prefs.getInt(KEY_TOTAL_ALERT_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_ALERT_COUNT, value).apply()

    fun incrementUnlockCount() {
        totalUnlockCount++
    }

    fun incrementLockCount() {
        totalLockCount++
    }

    fun incrementAlertCount() {
        totalAlertCount++
    }

    val thresholdMs: Long
        get() = (thresholdHours * 60 * 60 * 1000).toLong()

    val remainingMs: Long
        get() {
            val last = lastActivityTime
            if (last == 0L) return thresholdMs
            val elapsed = calculateElapsed(last)
            return (thresholdMs - elapsed).coerceAtLeast(0)
        }

    val remainingPercent: Float
        get() {
            val last = lastActivityTime
            if (last == 0L) return 0f
            val elapsed = calculateElapsed(last)
            return (elapsed.toFloat() / thresholdMs).coerceIn(0f, 1f)
        }

    /**
     * 计算实际经过的时间（扣除暂停时段）
     */
    private fun calculateElapsed(startTime: Long): Long {
        val now = System.currentTimeMillis()
        var elapsed = now - startTime

        if (pauseEnabled) {
            elapsed -= getPauseOverlap(startTime, now)
        }

        return elapsed.coerceAtLeast(0)
    }

    /**
     * 获取 [startTime, endTime] 时间段内与暂停时段的重叠毫秒数
     */
    private fun getPauseOverlap(startTime: Long, endTime: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        var totalOverlap = 0L

        // 遍历从 startTime 到 endTime 之间的每一天
        calendar.timeInMillis = startTime
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        while (calendar.timeInMillis <= endTime) {
            val dayStart = calendar.timeInMillis

            // 暂停时段的起止时间（今天）
            val pauseStart = dayStart + pauseStartHour * 3600_000L + pauseStartMinute * 60_000L
            val pauseEnd = dayStart + pauseEndHour * 3600_000L + pauseEndMinute * 60_000L

            // 如果 end < start（如 22:00-06:00 跨午夜），拆成两段
            if (pauseEndHour < pauseStartHour ||
                (pauseEndHour == pauseStartHour && pauseEndMinute < pauseStartMinute)) {
                // 跨午夜: 22:00-24:00 和 00:00-06:00
                val segment1Start = pauseStart
                val segment1End = dayStart + 24 * 3600_000L
                val segment2Start = dayStart
                val segment2End = pauseEnd

                totalOverlap += getIntervalOverlap(startTime, endTime, segment1Start, segment1End)
                totalOverlap += getIntervalOverlap(startTime, endTime, segment2Start, segment2End)
            } else {
                // 同一天内
                totalOverlap += getIntervalOverlap(startTime, endTime, pauseStart, pauseEnd)
            }

            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        return totalOverlap
    }

    /**
     * 计算两个区间的重叠长度
     */
    private fun getIntervalOverlap(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Long {
        val overlapStart = maxOf(aStart, bStart)
        val overlapEnd = minOf(aEnd, bEnd)
        return if (overlapStart < overlapEnd) overlapEnd - overlapStart else 0L
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
