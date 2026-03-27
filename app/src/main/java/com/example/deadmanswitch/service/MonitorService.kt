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
import com.example.deadmanswitch.utils.Logging

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
            try {
                checkActivity()
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            } catch (e: Exception) {
                Logging.e("MonitorService", "Error in checkRunnable", e)
                // 继续执行，避免服务停止
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Logging.logServiceLifecycle("MonitorService", "onCreate")
        
        try {
            activityDetector = ActivityDetector(this)
            notificationManager = AppNotificationManager(this)
            
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "DeadManSwitch::MonitorWakeLock"
            )
            
            Logging.d("MonitorService", "Service components initialized")
        } catch (e: Exception) {
            Logging.e("MonitorService", "Failed to initialize service components", e)
            throw e // 重新抛出，让系统知道服务启动失败
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logging.logServiceLifecycle("MonitorService", "onStartCommand")
        
        return try {
            // 必须在5秒内调用startForeground，否则会崩溃
            startForeground(
                AppNotificationManager.NOTIFICATION_ID_MONITOR,
                notificationManager.createMonitoringNotification()
            )
            isRunning = true
            Logging.d("MonitorService", "Foreground service started")
            
            // 获取唤醒锁，10分钟超时
            if (!wakeLock.isHeld) {
                wakeLock.acquire(10 * 60 * 1000L)
                Logging.d("MonitorService", "WakeLock acquired")
            }
            
            // 立即检查一次
            checkActivity()
            
            // 开始定期检查
            handler.post(checkRunnable)
            Logging.d("MonitorService", "Periodic check started")
            
            START_STICKY
        } catch (e: Exception) {
            Logging.e("MonitorService", "Failed to start foreground service", e)
            // 尝试停止服务，避免卡在错误状态
            stopSelf()
            START_NOT_STICKY
        }
    }
    
    override fun onDestroy() {
        Logging.logServiceLifecycle("MonitorService", "onDestroy")
        
        try {
            isRunning = false
            handler.removeCallbacks(checkRunnable)
            Logging.d("MonitorService", "Handler callbacks removed")
            
            if (wakeLock.isHeld) {
                wakeLock.release()
                Logging.d("MonitorService", "WakeLock released")
            }
            
            notificationManager.cancelAlert()
            Logging.d("MonitorService", "Service destroyed")
        } catch (e: Exception) {
            Logging.e("MonitorService", "Error during service destruction", e)
        }
        
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun checkActivity() {
        try {
            Logging.logMonitoringEvent("Checking user activity")
            
            // 检测当前是否有活动
            val hasActivity = activityDetector.detectActivity()
            Logging.d("MonitorService", "Activity detection result: $hasActivity")
            
            if (hasActivity) {
                // 有活动，更新最后活动时间
                activityDetector.updateLastActivity()
                notificationManager.cancelAlert()
                Logging.logMonitoringEvent("Activity detected, timer reset")
            } else {
                // 无活动，检查是否超时
                checkTimeout()
            }
        } catch (e: Exception) {
            Logging.e("MonitorService", "Error in checkActivity", e)
        }
    }
    
    private fun checkTimeout() {
        try {
            val prefs = getSharedPreferences("deadman_prefs", 0)
            val thresholdHours = prefs.getFloat("threshold_hours", 12f)
            val thresholdMs = (thresholdHours * 60 * 60 * 1000).toLong()
            
            val inactivityDuration = activityDetector.getInactivityDuration()
            Logging.d("MonitorService", "Inactivity duration: ${inactivityDuration}ms, Threshold: ${thresholdMs}ms")
            
            if (inactivityDuration >= thresholdMs) {
                val hours = inactivityDuration / (1000 * 60 * 60)
                Logging.logMonitoringEvent("Threshold exceeded", "Inactive for ${hours} hours")
                triggerAlert(hours)
            }
        } catch (e: Exception) {
            Logging.e("MonitorService", "Error in checkTimeout", e)
        }
    }
    
    private fun triggerAlert(hours: Long) {
        try {
            Logging.logMonitoringEvent("Triggering alert", "Inactive for ${hours} hours")
            
            // 发送通知警报
            notificationManager.sendAlert(hours)
            Logging.logNotificationEvent("Alert notification sent", "alert_channel")
            
            // TODO: 未来扩展功能
            // 1. 发送短信给紧急联系人
            // 2. 拨打电话
            // 3. 调用 OpenClaw API
            // 4. 播放警报音
        } catch (e: Exception) {
            Logging.e("MonitorService", "Failed to trigger alert", e)
        }
    }
}