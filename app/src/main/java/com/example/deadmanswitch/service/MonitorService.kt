package com.example.deadmanswitch.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.deadmanswitch.MainActivity
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager

class MonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var settings: SettingsManager
    private lateinit var activityLog: ActivityLogManager
    private lateinit var wakeLock: PowerManager.WakeLock

    companion object {
        const val CHECK_INTERVAL_MS = 60_000L
        const val ALERT_COOLDOWN_MS = 30 * 60_000L // 30分钟内不重复报警
        const val WAKELOCK_TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24小时

        fun isRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences("deadman_prefs", 0)
            return prefs.getBoolean(SettingsManager.KEY_MONITORING, false)
        }
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkActivity()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsManager(this)
        activityLog = ActivityLogManager(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DeadManSwitch::MonitorWakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createStatusNotification())
        settings.isMonitoring = true
        settings.initIfNeeded()

        if (!wakeLock.isHeld) {
            wakeLock.acquire(WAKELOCK_TIMEOUT_MS)
        }

        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)

        activityLog.addEntry("monitor_start")
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 应用被划掉时自动重启服务
        val restartIntent = Intent(applicationContext, MonitorService::class.java)
        restartIntent.setPackage(packageName)
        startForegroundService(restartIntent)
        activityLog.addEntry("service_restart")
    }

    override fun onDestroy() {
        super.onDestroy()
        settings.isMonitoring = false
        handler.removeCallbacks(checkRunnable)
        if (wakeLock.isHeld) wakeLock.release()
        activityLog.addEntry("monitor_stop")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createStatusNotification(): Notification {
        val elapsedMs = System.currentTimeMillis() - settings.lastActivityTime
        val thresholdMs = settings.thresholdMs
        val remainingMs = (thresholdMs - elapsedMs).coerceAtLeast(0)
        val hours = remainingMs / (1000 * 60 * 60)
        val minutes = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)

        val text = if (remainingMs > 0) {
            "剩余 ${hours}时${minutes}分 | 阈值 ${settings.thresholdHours.toInt()}h"
        } else {
            "⚠️ 已超过阈值"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "monitor_channel")
            .setContentTitle("🛡️ DeadManSwitch 运行中")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun checkActivity() {
        val elapsedMs = System.currentTimeMillis() - settings.lastActivityTime
        val thresholdMs = settings.thresholdMs
        updateStatusNotification(elapsedMs, thresholdMs)

        if (elapsedMs >= thresholdMs) {
            triggerAlert(elapsedMs)
        }
    }

    private fun updateStatusNotification(elapsedMs: Long, thresholdMs: Long) {
        val remainingMs = (thresholdMs - elapsedMs).coerceAtLeast(0)
        val hours = remainingMs / (1000 * 60 * 60)
        val minutes = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)

        val text = if (remainingMs > 0) {
            "剩余 ${hours}时${minutes}分 后触发警报"
        } else {
            "⚠️ 已超过阈值！"
        }

        try {
            val notification = NotificationCompat.Builder(this, "monitor_channel")
                .setContentTitle("🛡️ DeadManSwitch 运行中")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this, 0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(1, notification)
        } catch (_: Exception) {}
    }

    private fun triggerAlert(elapsedMs: Long) {
        val now = System.currentTimeMillis()

        // 去重：30分钟内不重复报警
        if (now - settings.lastAlertTime < ALERT_COOLDOWN_MS) return
        settings.lastAlertTime = now

        val hours = elapsedMs / (1000 * 60 * 60)
        activityLog.addEntry("alert")

        // 发通知
        try {
            val alertIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 1, alertIntent, PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "alert_channel")
                .setContentTitle("⚠️ 安全警报")
                .setContentText("已超过 ${hours} 小时未检测到活动！")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .build()

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(999, notification)
        } catch (_: Exception) {}
    }
}
