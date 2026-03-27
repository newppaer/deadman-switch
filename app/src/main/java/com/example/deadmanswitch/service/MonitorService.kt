package com.example.deadmanswitch.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import com.example.deadmanswitch.utils.ActivityDetector
import com.example.deadmanswitch.utils.AppNotificationManager

class MonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var activityDetector: ActivityDetector
    private lateinit var notificationManager: AppNotificationManager
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
        activityDetector = ActivityDetector(this)
        notificationManager = AppNotificationManager(this)
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DeadManSwitch::MonitorWakeLock"
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            AppNotificationManager.NOTIFICATION_ID_MONITOR,
            notificationManager.createMonitoringNotification()
        )
        isRunning = true
        
        // 获取唤醒锁，10分钟超时
        wakeLock.acquire(10 * 60 * 1000L)
        
        // 立即检查一次
        checkActivity()
        
        // 开始定期检查
        handler.post(checkRunnable)
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(checkRunnable)
        if (wakeLock.isHeld) wakeLock.release()
        notificationManager.cancelAlert()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun checkActivity() {
        // 检测当前是否有活动
        val hasActivity = activityDetector.detectActivity()
        
        if (hasActivity) {
            // 有活动，更新最后活动时间
            activityDetector.updateLastActivity()
            notificationManager.cancelAlert()
        } else {
            // 无活动，检查是否超时
            checkTimeout()
        }
    }
    
    private fun checkTimeout() {
        val prefs = getSharedPreferences("deadman_prefs", 0)
        val thresholdHours = prefs.getFloat("threshold_hours", 12f)
        val thresholdMs = (thresholdHours * 60 * 60 * 1000).toLong()
        
        val inactivityDuration = activityDetector.getInactivityDuration()
        
        if (inactivityDuration >= thresholdMs) {
            val hours = inactivityDuration / (1000 * 60 * 60)
            triggerAlert(hours)
        }
    }
    
    private fun triggerAlert(hours: Long) {
        // 发送通知警报
        notificationManager.sendAlert(hours)
        
        // TODO: 未来扩展功能
        // 1. 发送短信给紧急联系人
        // 2. 拨打电话
        // 3. 调用 OpenClaw API
        // 4. 播放警报音
    }
}