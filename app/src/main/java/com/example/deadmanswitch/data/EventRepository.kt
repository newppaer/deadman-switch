package com.example.deadmanswitch.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class EventRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).activityEventDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun todayDate(): String = dateFormat.format(Date())

    fun dateOf(timestamp: Long): String = dateFormat.format(Date(timestamp))

    /**
     * 记录事件
     */
    suspend fun logEvent(type: String, timestamp: Long = System.currentTimeMillis()) {
        val event = ActivityEventEntity(
            type = type,
            timestamp = timestamp,
            date = dateOf(timestamp)
        )
        dao.insert(event)
    }

    /**
     * 获取今日事件列表
     */
    suspend fun getTodayEvents(): List<ActivityEventEntity> {
        return dao.getTodayEvents(todayDate())
    }

    /**
     * 获取今日某类型数量
     */
    suspend fun getTodayCount(type: String): Int {
        return dao.getTodayCount(todayDate(), type)
    }

    /**
     * 获取最近 N 天的每日统计
     */
    suspend fun getDailyStats(days: Int = 7): List<DailyCount> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val startDate = dateFormat.format(cal.time)
        return dao.getDailyStats(startDate)
    }

    /**
     * 获取总次数
     */
    suspend fun getTotalCount(type: String): Int {
        return dao.getTotalCount(type)
    }

    /**
     * 清理旧数据（保留 90 天）
     */
    suspend fun cleanup() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -90)
        dao.deleteOlderThan(dateFormat.format(cal.time))
    }

    /**
     * 从 SharedPreferences 迁移历史数据
     */
    suspend fun migrateFromPrefs(prefsEvents: List<ActivityEntry>) {
        if (prefsEvents.isEmpty()) return
        val entities = prefsEvents.map { entry ->
            ActivityEventEntity(
                type = entry.type,
                timestamp = entry.timestamp,
                date = dateOf(entry.timestamp)
            )
        }
        dao.insertAll(entities)
    }
}
