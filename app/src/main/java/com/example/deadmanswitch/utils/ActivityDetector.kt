package com.example.deadmanswitch.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Build.VERSION_CODES
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
        
        return try {
            val hasScreenUnlock = checkScreenUnlock()
            val hasAppUsage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                checkAppUsage()
            } else {
                false
            }
            
            val result = hasScreenUnlock || hasAppUsage
            Logging.d("ActivityDetector", "detectActivity: screenUnlock=$hasScreenUnlock, appUsage=$hasAppUsage, result=$result")
            result
        } catch (e: Exception) {
            Logging.e("ActivityDetector", "Error in detectActivity", e)
            false // 发生异常时返回false，避免误触发警报
        }
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
            
            Logging.d("ActivityDetector", "checkAppUsage: querying usage stats from $startTime to $endTime")
            
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            
            val hasUsage = stats?.any { it.totalTimeInForeground > 0 } ?: false
            val appCount = stats?.count { it.totalTimeInForeground > 0 } ?: 0
            
            Logging.d("ActivityDetector", "checkAppUsage: found $appCount apps with usage, result=$hasUsage")
            hasUsage
        } catch (e: SecurityException) {
            // 没有 PACKAGE_USAGE_STATS 权限
            Logging.w("ActivityDetector", "Missing PACKAGE_USAGE_STATS permission")
            false
        } catch (e: Exception) {
            Logging.e("ActivityDetector", "Error checking app usage", e)
            false
        }
    }
    
    /**
     * 更新最后活动时间
     */
    fun updateLastActivity() {
        try {
            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_activity", now).apply()
            Logging.d("ActivityDetector", "updateLastActivity: updated to $now")
        } catch (e: Exception) {
            Logging.e("ActivityDetector", "Failed to update last activity", e)
        }
    }
    
    /**
     * 获取最后活动时间
     */
    fun getLastActivity(): Long {
        return try {
            val lastActivity = prefs.getLong("last_activity", 0L)
            Logging.d("ActivityDetector", "getLastActivity: $lastActivity")
            lastActivity
        } catch (e: Exception) {
            Logging.e("ActivityDetector", "Failed to get last activity", e)
            0L
        }
    }
    
    /**
     * 获取无活动时长（毫秒）
     */
    fun getInactivityDuration(): Long {
        return try {
            val lastActivity = getLastActivity()
            if (lastActivity == 0L) {
                Logging.d("ActivityDetector", "getInactivityDuration: no activity recorded")
                return 0L
            }
            val duration = System.currentTimeMillis() - lastActivity
            Logging.d("ActivityDetector", "getInactivityDuration: $duration ms")
            duration
        } catch (e: Exception) {
            Logging.e("ActivityDetector", "Failed to calculate inactivity duration", e)
            0L
        }
    }
    
    /**
     * 格式化无活动时长
     */
    fun formatInactivityDuration(): String {
        return try {
            val duration = getInactivityDuration()
            if (duration == 0L) {
                Logging.d("ActivityDetector", "formatInactivityDuration: no record")
                return "无记录"
            }
            
            val hours = duration / (1000 * 60 * 60)
            val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
            
            val result = if (hours > 0) {
                "${hours}小时${minutes}分钟"
            } else {
                "${minutes}分钟"
            }
            
            Logging.d("ActivityDetector", "formatInactivityDuration: $result")
            result
        } catch (e: Exception) {
            Logging.e("ActivityDetector", "Failed to format inactivity duration", e)
            "计算错误"
        }
    }
}