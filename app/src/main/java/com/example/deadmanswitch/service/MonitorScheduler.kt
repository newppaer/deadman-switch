package com.example.deadmanswitch.service

import android.content.Context
import android.util.Log
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.SettingsManager

/**
 * 监控调度器
 * 替代前台服务，使用 WorkManager 管理定时检查
 */
object MonitorScheduler {
    private const val TAG = "MonitorScheduler"

    /**
     * 启动监控
     */
    fun start(context: Context) {
        val settings = SettingsManager(context)
        settings.isMonitoring = true
        settings.initIfNeeded()

        MonitorWorker.schedule(context)
        ActivityLogManager(context).addEntry("monitor_start")
        Log.d(TAG, "Monitoring started via WorkManager")
    }

    /**
     * 停止监控
     */
    fun stop(context: Context) {
        val settings = SettingsManager(context)
        settings.isMonitoring = false

        MonitorWorker.cancel(context)
        ActivityLogManager(context).addEntry("monitor_stop")
        Log.d(TAG, "Monitoring stopped")
    }

    /**
     * 检查是否正在监控
     */
    fun isRunning(context: Context): Boolean {
        return SettingsManager(context).isMonitoring
    }
}
