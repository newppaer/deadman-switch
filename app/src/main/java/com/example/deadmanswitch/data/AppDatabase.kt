package com.example.deadmanswitch.data

import androidx.room.*

/**
 * 活动事件实体
 */
@Entity(tableName = "activity_events")
data class ActivityEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,        // "unlock", "lock", "alert", "manual_reset", "boot", "monitor_start", "monitor_stop"
    val timestamp: Long,     // 真实事件时间
    val date: String         // "2026-03-29" 方便按日查询
)

/**
 * 每日统计结果
 */
data class DailyCount(
    val date: String,
    val unlockCount: Int,
    val lockCount: Int,
    val alertCount: Int
)

@Dao
interface ActivityEventDao {
    @Insert
    suspend fun insert(event: ActivityEventEntity)

    @Insert
    suspend fun insertAll(events: List<ActivityEventEntity>)

    /**
     * 获取今日事件
     */
    @Query("SELECT * FROM activity_events WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getTodayEvents(date: String): List<ActivityEventEntity>

    /**
     * 获取今日某类型事件数量
     */
    @Query("SELECT COUNT(*) FROM activity_events WHERE date = :date AND type = :type")
    suspend fun getTodayCount(date: String, type: String): Int

    /**
     * 获取最近 N 天的每日统计
     */
    @Query("""
        SELECT 
            date,
            SUM(CASE WHEN type = 'unlock' THEN 1 ELSE 0 END) as unlockCount,
            SUM(CASE WHEN type = 'lock' THEN 1 ELSE 0 END) as lockCount,
            SUM(CASE WHEN type = 'alert' THEN 1 ELSE 0 END) as alertCount
        FROM activity_events
        WHERE date >= :startDate
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyStats(startDate: String): List<DailyCount>

    /**
     * 获取总次数
     */
    @Query("SELECT COUNT(*) FROM activity_events WHERE type = :type")
    suspend fun getTotalCount(type: String): Int

    /**
     * 清除 30 天前的数据
     */
    @Query("DELETE FROM activity_events WHERE date < :date")
    suspend fun deleteOlderThan(date: String)

    /**
     * 获取最早的日期
     */
    @Query("SELECT MIN(date) FROM activity_events")
    suspend fun getEarliestDate(): String?

    /**
     * 清除所有数据
     */
    @Query("DELETE FROM activity_events")
    suspend fun clearAll()
}

@Database(entities = [ActivityEventEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityEventDao(): ActivityEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deadman_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
