package com.example.deadmanswitch.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.*
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.ContactManager
import com.example.deadmanswitch.data.SettingsManager
import com.example.deadmanswitch.push.PushManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager 定时检查 Worker
 * 使用 UsageStatsManager 检测锁屏/解锁事件
 */
class MonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MonitorWorker"
        const val WORK_NAME = "deadman_monitor"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"

        /**
         * 调度周期性检查（15 分钟一次）
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            val request = PeriodicWorkRequestBuilder<MonitorWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d(TAG, "WorkManager periodic work scheduled (15min interval)")
        }

        /**
         * 取消定时检查
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "WorkManager periodic work cancelled")
        }

        /**
         * 立即执行一次检查
         */
        fun runImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<MonitorWorker>()
                .addTag("${WORK_NAME}_immediate")
                .build()
            WorkManager.getInstance(context).enqueue(request)
            Log.d(TAG, "WorkManager immediate work enqueued")
        }
    }

    override suspend fun doWork(): Result {
        val settings = SettingsManager(applicationContext)

        if (!settings.isMonitoring) {
            Log.d(TAG, "Monitoring disabled, skipping check")
            return Result.success()
        }

        // 1. 使用 UsageStatsManager 检测屏幕事件
        detectScreenEvents(settings)

        // 2. 检查是否在暂停期间
        if (isInPausePeriod(settings)) {
            Log.d(TAG, "In pause period, skipping threshold check")
            schedulePauseEnd(settings)
            return Result.success()
        }

        // 3. 检查是否超过阈值（使用扣除暂停时段的 elapsed）
        val elapsedMs = System.currentTimeMillis() - settings.lastActivityTime
        val remainingMs = settings.remainingMs
        Log.d(TAG, "Check: elapsed=${elapsedMs / 1000}s, remaining=${remainingMs / 1000}s (pause deducted)")

        if (remainingMs <= 0) {
            triggerAlert(elapsedMs, settings)
        }

        return Result.success()
    }

    /**
     * 检查当前是否在暂停时段内
     */
    private fun isInPausePeriod(settings: SettingsManager): Boolean {
        if (!settings.pauseEnabled) return false

        val calendar = java.util.Calendar.getInstance()
        val currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                            calendar.get(java.util.Calendar.MINUTE)
        val startMinutes = settings.pauseStartHour * 60 + settings.pauseStartMinute
        val endMinutes = settings.pauseEndHour * 60 + settings.pauseEndMinute

        return if (endMinutes < startMinutes) {
            // 跨午夜 (如 22:00-06:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            // 同一天内
            currentMinutes in startMinutes until endMinutes
        }
    }

    /**
     * 调度一个一次性任务在暂停结束时唤醒
     */
    private fun schedulePauseEnd(settings: SettingsManager) {
        if (!settings.pauseEnabled) return

        val calendar = java.util.Calendar.getInstance()
        val now = calendar.timeInMillis

        // 设置暂停结束时间
        calendar.set(java.util.Calendar.HOUR_OF_DAY, settings.pauseEndHour)
        calendar.set(java.util.Calendar.MINUTE, settings.pauseEndMinute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        // 如果结束时间已过（今天），设为明天
        if (calendar.timeInMillis <= now) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        val delayMs = calendar.timeInMillis - now

        val request = OneTimeWorkRequestBuilder<MonitorWorker>()
            .addTag("${WORK_NAME}_pause_end")
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)
        Log.d(TAG, "Scheduled pause end check in ${delayMs / 1000}s")
    }

    /**
     * 使用 UsageStatsManager 检测锁屏/解锁事件
     * 收集所有事件并按真实时间记录
     */
    private fun detectScreenEvents(settings: SettingsManager) {
        try {
            val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usm == null) {
                Log.w(TAG, "UsageStatsManager not available")
                return
            }

            val prefs = applicationContext.getSharedPreferences("deadman_prefs", 0)
            val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis() - 15 * 60 * 1000)
            val now = System.currentTimeMillis()

            // 收集所有屏幕事件
            data class ScreenEvent(val time: Long, val type: String)

            val allEvents = mutableListOf<ScreenEvent>()
            val events = usm.queryEvents(lastCheckTime, now)
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.KEYGUARD_HIDDEN -> {
                        allEvents.add(ScreenEvent(event.timeStamp, "unlock"))
                    }
                    UsageEvents.Event.KEYGUARD_SHOWN -> {
                        allEvents.add(ScreenEvent(event.timeStamp, "lock"))
                    }
                }
            }

            // 按时间排序
            allEvents.sortBy { it.time }

            // 更新最后检查时间
            prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()

            if (allEvents.isEmpty()) {
                Log.d(TAG, "No screen events since $lastCheckTime")
                return
            }

            // 记录所有事件
            val activityLog = ActivityLogManager(applicationContext)
            val lastLoggedTime = prefs.getLong("last_logged_event_time", 0)

            for (screenEvent in allEvents) {
                // 跳过已记录的事件
                if (screenEvent.time <= lastLoggedTime) continue

                activityLog.addEntry(screenEvent.type, screenEvent.time)
                Log.d(TAG, "${screenEvent.type} at ${screenEvent.time}")

                // 只有最新的解锁才重置活动时间
                if (screenEvent.type == "unlock") {
                    settings.resetActivity()
                }
            }

            // 记录最新事件时间
            if (allEvents.isNotEmpty()) {
                prefs.edit().putLong("last_logged_event_time", allEvents.last().time).apply()
            }

            Log.d(TAG, "Processed ${allEvents.size} screen events")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect screen events", e)
        }
    }

    private fun triggerAlert(elapsedMs: Long, settings: SettingsManager) {
        val now = System.currentTimeMillis()
        val cooldownMs = 30 * 60 * 1000L
        if (now - settings.lastAlertTime < cooldownMs) {
            Log.d(TAG, "Alert cooldown active, skipping")
            return
        }

        settings.lastAlertTime = now
        val hours = elapsedMs / (1000 * 60 * 60)
        val activityLog = ActivityLogManager(applicationContext)
        activityLog.addEntry("alert")

        val alertMsg = "⚠️ DeadManSwitch 警报\n已超过 ${hours} 小时未检测到活动，可能遇到紧急情况。"

        // 1. 短信推送
        try {
            val contactManager = ContactManager(applicationContext)
            if (contactManager.isSmsEnabled() && contactManager.getAll().any { it.enabled }) {
                contactManager.sendAlertSms(hours)
                Log.d(TAG, "SMS sent to contacts")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS push failed", e)
        }

        // 2. 企业微信
        try {
            if (settings.wecomEnabled && settings.wecomWebhookUrl.isNotBlank()) {
                val msg = settings.wecomMessage.ifBlank { alertMsg }
                PushManager.sendWeChatWork(settings.wecomWebhookUrl, msg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "WeChatWork push failed", e)
        }

        // 3. OpenClaw API
        try {
            if (settings.openclawEnabled && settings.openclawApiUrl.isNotBlank()) {
                val msg = settings.openclawMessage.ifBlank { alertMsg }
                PushManager.sendHttpPost(
                    apiUrl = settings.openclawApiUrl,
                    token = settings.openclawToken,
                    title = "DeadManSwitch 警报",
                    content = msg
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenClaw push failed", e)
        }

        // 4. 本地通知
        try {
            createAlertChannelIfNeeded()
            val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, "alert_channel")
                .setContentTitle("⚠️ 安全警报")
                .setContentText("已超过 ${hours} 小时未检测到活动！")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
                .setAutoCancel(false)
                .build()
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(999, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Notification failed", e)
        }

        Log.d(TAG, "Alert triggered after ${hours}h")
    }

    private fun createAlertChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alert_channel",
                "安全警报",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "长时间无活动提醒"
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
