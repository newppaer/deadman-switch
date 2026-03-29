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

        // 2. 检查是否超过阈值
        val elapsedMs = System.currentTimeMillis() - settings.lastActivityTime
        val thresholdMs = settings.thresholdMs
        Log.d(TAG, "Check: elapsed=${elapsedMs / 1000}s, threshold=${thresholdMs / 1000}s")

        if (elapsedMs >= thresholdMs) {
            triggerAlert(elapsedMs, settings)
        }

        return Result.success()
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
