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
import com.example.deadmanswitch.R

class MonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var wakeLock: PowerManager.WakeLock

    companion object {
        var isRunning = false
        const val CHECK_INTERVAL_MS = 60000L // 每分钟检查一次
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkActivity()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("deadman_prefs", 0)
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DeadManSwitch::MonitorWakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        isRunning = true
        wakeLock.acquire(10*60*1000L) // 10分钟超时
        handler.post(checkRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(checkRunnable)
        if (wakeLock.isHeld) wakeLock.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "monitor_channel")
            .setContentTitle("安全监控运行中")
            .setContentText("正在监听您的活动状态")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun checkActivity() {
        val lastActivity = prefs.getLong("last_activity", 0L)
        if (lastActivity == 0L) {
            // 首次运行，记录当前时间
            prefs.edit().putLong("last_activity", System.currentTimeMillis()).apply()
            return
        }

        val thresholdHours = prefs.getFloat("threshold_hours", 12f)
        val thresholdMs = (thresholdHours * 60 * 60 * 1000).toLong()
        val elapsedMs = System.currentTimeMillis() - lastActivity

        if (elapsedMs >= thresholdMs) {
            triggerAlert(elapsedMs)
        }
    }

    private fun triggerAlert(elapsedMs: Long) {
        val hours = elapsedMs / (1000 * 60 * 60)
        
        // 发出通知
        val alertIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 1, alertIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "alert_channel")
            .setContentTitle("⚠️ 安全警报")
            .setContentText("已超过 ${hours} 小时未检测到活动")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) 
            as android.app.NotificationManager
        manager.notify(999, notification)

        // TODO: 这里可以添加更多报警方式
        // - 发送短信给紧急联系人
        // - 调用 API 通知 OpenClaw
        // - 播放警报音
    }
}