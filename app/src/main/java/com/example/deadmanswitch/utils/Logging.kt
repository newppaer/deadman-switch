package com.example.deadmanswitch.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志记录工具类
 * 提供统一的日志记录和崩溃报告功能
 */
object Logging {
    
    private const val TAG = "DeadManSwitch"
    private const val LOG_PREFS = "log_prefs"
    private const val KEY_LAST_CRASH = "last_crash"
    private const val MAX_LOG_SIZE = 100 * 1024 // 100KB
    
    // 日志级别
    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    /**
     * 初始化日志系统
     */
    fun initialize(context: Context) {
        // 设置未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(context, thread, throwable)
        }
        
        Log.i(TAG, "Logging system initialized")
    }
    
    /**
     * 记录日志
     */
    fun log(level: Level, tag: String = TAG, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val threadName = Thread.currentThread().name
        val fullMessage = "[$timestamp][$threadName] $message"
        
        when (level) {
            Level.VERBOSE -> Log.v(tag, fullMessage, throwable)
            Level.DEBUG -> Log.d(tag, fullMessage, throwable)
            Level.INFO -> Log.i(tag, fullMessage, throwable)
            Level.WARN -> Log.w(tag, fullMessage, throwable)
            Level.ERROR -> Log.e(tag, fullMessage, throwable)
        }
        
        // 同时写入文件（可选，根据需要开启）
        // writeToLogFile(context, level, tag, fullMessage, throwable)
    }
    
    /**
     * 快捷方法
     */
    fun v(tag: String = TAG, message: String, throwable: Throwable? = null) = 
        log(Level.VERBOSE, tag, message, throwable)
    
    fun d(tag: String = TAG, message: String, throwable: Throwable? = null) = 
        log(Level.DEBUG, tag, message, throwable)
    
    fun i(tag: String = TAG, message: String, throwable: Throwable? = null) = 
        log(Level.INFO, tag, message, throwable)
    
    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) = 
        log(Level.WARN, tag, message, throwable)
    
    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) = 
        log(Level.ERROR, tag, message, throwable)
    
    /**
     * 处理未捕获异常
     */
    private fun handleUncaughtException(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()
        
        // 记录到日志
        Log.e(TAG, "Uncaught exception in thread ${thread.name}: ${throwable.message}")
        Log.e(TAG, stackTrace)
        
        // 保存崩溃信息到 SharedPreferences
        saveCrashReport(context, stackTrace)
        
        // 重新抛出给系统默认处理器
        Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
    }
    
    /**
     * 保存崩溃报告
     */
    private fun saveCrashReport(context: Context, stackTrace: String) {
        try {
            val prefs = context.getSharedPreferences(LOG_PREFS, Context.MODE_PRIVATE)
            val timestamp = System.currentTimeMillis()
            val crashInfo = """
                |=== CRASH REPORT ===
                |Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))}
                |Device: ${Build.MANUFACTURER} ${Build.MODEL}
                |Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                |App Version: ${getAppVersion(context)}
                |
                |Stack Trace:
                |$stackTrace
                |=== END REPORT ===
            """.trimMargin()
            
            prefs.edit()
                .putString(KEY_LAST_CRASH, crashInfo)
                .putLong("${KEY_LAST_CRASH}_time", timestamp)
                .apply()
            
            Log.i(TAG, "Crash report saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report", e)
        }
    }
    
    /**
     * 获取最后崩溃报告
     */
    fun getLastCrashReport(context: Context): String? {
        val prefs = context.getSharedPreferences(LOG_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_CRASH, null)
    }
    
    /**
     * 清除崩溃报告
     */
    fun clearCrashReport(context: Context) {
        val prefs = context.getSharedPreferences(LOG_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_LAST_CRASH)
            .remove("${KEY_LAST_CRASH}_time")
            .apply()
    }
    
    /**
     * 获取应用版本信息
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * 写入日志文件（可选功能）
     */
    private fun writeToLogFile(context: Context, level: Level, tag: String, message: String, throwable: Throwable?) {
        // 注意：频繁的文件写入可能影响性能，建议仅在调试时开启
        // 或者实现日志轮转机制
    }
    
    /**
     * 记录服务生命周期
     */
    fun logServiceLifecycle(serviceName: String, action: String) {
        d("Service", "$serviceName: $action")
    }
    
    /**
     * 记录Activity生命周期
     */
    fun logActivityLifecycle(activityName: String, action: String) {
        d("Activity", "$activityName: $action")
    }
    
    /**
     * 记录监控事件
     */
    fun logMonitoringEvent(event: String, details: String? = null) {
        i("Monitoring", event + (details?.let { ": $it" } ?: ""))
    }
    
    /**
     * 记录通知事件
     */
    fun logNotificationEvent(event: String, channel: String? = null) {
        i("Notification", event + (channel?.let { " (channel: $it)" } ?: ""))
    }
}