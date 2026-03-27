package com.example.deadmanswitch.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.deadmanswitch.MainActivity
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager

class MonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var settings: SettingsManager
    private lateinit var activityLog: ActivityLogManager
    private var wakeLock: PowerManager.WakeLock? = null

    // 动态注册屏幕锁屏广播接收器（Android 10+ 不允许 Manifest 静态接收 ACTION_SCREEN_OFF）
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF detected")
                    activityLog.addEntry("lock")
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "User PRESENT detected (unlock)")
                    settings.resetActivity()
                    activityLog.addEntry("unlock")
                }
            }
        }
    }

    companion object {
        private const val TAG = "MonitorService"
        const val CHECK_INTERVAL_MS = 60_000L
        const val ALERT_COOLDOWN_MS = 30 * 60_000L
        const val WAKELOCK_TIMEOUT_MS = 24 * 60 * 60 * 1000L

        // 用于 MainActivity 检查服务是否运行
        fun isRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences("deadman_prefs", 0)
            return prefs.getBoolean(SettingsManager.KEY_MONITORING, false)
        }

        // 用于存储最后一次错误信息，供 UI 显示
        private var lastError: String? = null
        fun consumeLastError(): String? {
            val err = lastError
            lastError = null
            return err
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
        Log.d(TAG, "onCreate")
        settings = SettingsManager(this)
        activityLog = ActivityLogManager(this)

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "DeadManSwitch::MonitorWakeLock"
            )
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock init failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        try {
            startForeground(1, createStatusNotification())
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            lastError = "前台服务启动失败: ${e.message}\n${e.stackTraceToString()}"
            activityLog.addEntry("error: ${e.message ?: "unknown"}")
            stopSelf()
            return START_NOT_STICKY
        }

        settings.isMonitoring = true
        settings.initIfNeeded()

        // 动态注册屏幕锁屏/解锁广播接收器
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenReceiver, filter)
            Log.d(TAG, "Screen receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register screen receiver", e)
            // 不致命，继续运行
        }

        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(WAKELOCK_TIMEOUT_MS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock acquire failed", e)
        }

        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)

        activityLog.addEntry("monitor_start")
        Log.d(TAG, "MonitorService started successfully")
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val restartIntent = Intent(applicationContext, MonitorService::class.java)
            restartIntent.setPackage(packageName)
            startForegroundService(restartIntent)
            activityLog.addEntry("service_restart")
        } catch (e: Exception) {
            Log.e(TAG, "onTaskRemoved restart failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        settings.isMonitoring = false
        handler.removeCallbacks(checkRunnable)
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        activityLog.addEntry("monitor_stop")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createStatusNotification(): Notification {
        val elapsedMs = System.currentTimeMillis() - settings.lastActivityTime
        val remainingMs = (settings.thresholdMs - elapsedMs).coerceAtLeast(0)
        val hours = remainingMs / (1000 * 60 * 60)
        val minutes = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)
        val text = if (remainingMs > 0) "剩余 ${hours}时${minutes}分" else "⚠️ 已超过阈值"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

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
        updateStatusNotification(elapsedMs, settings.thresholdMs)
        if (elapsedMs >= settings.thresholdMs) triggerAlert(elapsedMs)
    }

    private fun updateStatusNotification(elapsedMs: Long, thresholdMs: Long) {
        val remainingMs = (thresholdMs - elapsedMs).coerceAtLeast(0)
        val hours = remainingMs / (1000 * 60 * 60)
        val minutes = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)
        val text = if (remainingMs > 0) "剩余 ${hours}时${minutes}分 后触发警报" else "⚠️ 已超过阈值！"

        try {
            val notification = NotificationCompat.Builder(this, "monitor_channel")
                .setContentTitle("🛡️ DeadManSwitch 运行中")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
                .build()
            (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(1, notification)
        } catch (_: Exception) {}
    }

    private fun triggerAlert(elapsedMs: Long) {
        val now = System.currentTimeMillis()
        if (now - settings.lastAlertTime < ALERT_COOLDOWN_MS) return
        settings.lastAlertTime = now
        val hours = elapsedMs / (1000 * 60 * 60)
        activityLog.addEntry("alert")

        try {
            val notification = NotificationCompat.Builder(this, "alert_channel")
                .setContentTitle("⚠️ 安全警报")
                .setContentText("已超过 ${hours} 小时未检测到活动！")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
                .setContentIntent(PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(false)
                .build()
            (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(999, notification)
        } catch (_: Exception) {}
    }
}
