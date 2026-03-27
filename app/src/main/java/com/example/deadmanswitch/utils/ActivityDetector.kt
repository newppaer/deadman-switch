package com.example.deadmanswitch.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi

class ActivityDetector(private val context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("deadman_prefs", 0)
    
    /**
     * 检测用户活动状态
     * @return true 表示检测到活动，false 表示无活动
     */
    fun detectActivity(): Boolean {
        // 1. 检查屏幕解锁（已有）
        // 2. 检查应用使用情况（Android 5.0+）
        // 3. 检查传感器活动（未来扩展）
        // 4. 检查网络连接变化
        
        val hasScreenUnlock = checkScreenUnlock()
        val hasAppUsage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            checkAppUsage()
        } else {
            false
        }
        
        return hasScreenUnlock || hasAppUsage
    }
    
    private fun checkScreenUnlock(): Boolean {
        // 屏幕解锁检测由 ScreenUnlockReceiver 处理
        // 这里只检查最近是否有解锁记录
        val lastUnlock = prefs.getLong("last_unlock", 0L)
        val now = System.currentTimeMillis()
        return (now - lastUnlock) < 5 * 60 * 1000 // 5分钟内
    }
    
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun checkAppUsage(): Boolean {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) 
                as UsageStatsManager
            
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 5 * 60 * 1000 // 最近5分钟
            
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            
            stats?.any { it.totalTimeInForeground > 0 } ?: false
        } catch (e: SecurityException) {
            // 没有 PACKAGE_USAGE_STATS 权限
            false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 更新最后活动时间
     */
    fun updateLastActivity() {
        prefs.edit().putLong("last_activity", System.currentTimeMillis()).apply()
    }
    
    /**
     * 获取最后活动时间
     */
    fun getLastActivity(): Long {
        return prefs.getLong("last_activity", 0L)
    }
    
    /**
     * 获取无活动时长（毫秒）
     */
    fun getInactivityDuration(): Long {
        val lastActivity = getLastActivity()
        if (lastActivity == 0L) return 0L
        return System.currentTimeMillis() - lastActivity
    }
    
    /**
     * 格式化无活动时长
     */
    fun formatInactivityDuration(): String {
        val duration = getInactivityDuration()
        if (duration == 0L) return "无记录"
        
        val hours = duration / (1000 * 60 * 60)
        val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
        
        return if (hours > 0) {
            "${hours}小时${minutes}分钟"
        } else {
            "${minutes}分钟"
        }
    }
}