package com.example.deadmanswitch.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import com.example.deadmanswitch.data.ActivityLogManager
import com.example.deadmanswitch.data.EventRepository
import com.example.deadmanswitch.data.SettingsManager

/**
 * 监控调度器
 * 根据权限自动选择方案：
 * - 有 UsageStats 权限 → WorkManager (方案B)
 * - 无权限 → 轻量 ScreenEventService (方案A)
 */
object MonitorScheduler {
    private const val TAG = "MonitorScheduler"

    /**
     * 检查 UsageStats 权限
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * 初始化 UsageStatsManager（确保 App 出现在权限列表中）
     */
    fun initUsageStats(context: Context) {
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            usm?.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                System.currentTimeMillis() - 1000,
                System.currentTimeMillis()
            )
        } catch (_: Exception) {}
    }

    /**
     * 启动监控
     */
    fun start(context: Context) {
        val settings = SettingsManager(context)
        settings.isMonitoring = true
        settings.initIfNeeded()

        // WorkManager 始终运行，负责阈值检查和推送
        MonitorWorker.schedule(context)

        if (!hasUsageStatsPermission(context)) {
            // 方案A 补充: 轻量 ScreenEventService 监听屏幕事件
            val intent = Intent(context, ScreenEventService::class.java)
            context.startService(intent)
            Log.d(TAG, "Mode: WorkManager + ScreenEventService (UsageStats not granted)")
        } else {
            Log.d(TAG, "Mode: WorkManager only (UsageStats granted)")
        }

        ActivityLogManager(context).addEntry("monitor_start")
        kotlinx.coroutines.runBlocking { EventRepository(context).logEvent("monitor_start") }
    }

    /**
     * 停止监控
     */
    fun stop(context: Context) {
        val settings = SettingsManager(context)
        settings.isMonitoring = false

        // 停止两种方案
        MonitorWorker.cancel(context)
        context.stopService(Intent(context, ScreenEventService::class.java))

        ActivityLogManager(context).addEntry("monitor_stop")
        kotlinx.coroutines.runBlocking { EventRepository(context).logEvent("monitor_stop") }
        Log.d(TAG, "Monitoring stopped")
    }

    /**
     * 检查是否正在监控
     */
    fun isRunning(context: Context): Boolean {
        return SettingsManager(context).isMonitoring
    }

    /**
     * 检查电池优化白名单
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 获取当前使用的方案
     */
    fun getCurrentMode(context: Context): String {
        return if (hasUsageStatsPermission(context)) "WorkManager" else "WorkManager + Service"
    }
}
