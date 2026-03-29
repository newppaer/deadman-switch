package com.example.deadmanswitch.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class ActivityEntry(
    val timestamp: Long,
    val type: String // "unlock", "boot", "manual_reset", "alert", "monitor_start", "monitor_stop"
)

class ActivityLogManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("deadman_prefs", 0)
    private val settings = SettingsManager(context)

    companion object {
        private const val KEY_LOG = "activity_log"
        private const val MAX_ENTRIES = 100
    }

    fun addEntry(type: String) {
        addEntry(type, System.currentTimeMillis())
    }

    fun addEntry(type: String, timestamp: Long) {
        val entry = ActivityEntry(timestamp, type)
        val log = getAll().toMutableList()
        log.add(0, entry)
        if (log.size > MAX_ENTRIES) log.subList(MAX_ENTRIES, log.size).clear()
        save(log)

        // 持久化计数
        when (type) {
            "unlock" -> settings.incrementUnlockCount()
            "lock" -> settings.incrementLockCount()
            "alert" -> settings.incrementAlertCount()
        }
    }

    fun getAll(): List<ActivityEntry> {
        val json = prefs.getString(KEY_LOG, "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<ActivityEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(ActivityEntry(obj.getLong("t"), obj.getString("s")))
        }
        return list
    }

    /**
     * 获取今日的活动记录
     */
    fun getTodayEntries(): List<ActivityEntry> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        return getAll().filter { it.timestamp >= startOfDay }
    }

    fun clear() {
        prefs.edit().remove(KEY_LOG).apply()
    }

    private fun save(log: List<ActivityEntry>) {
        val arr = JSONArray()
        for (entry in log) {
            val obj = JSONObject()
            obj.put("t", entry.timestamp)
            obj.put("s", entry.type)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_LOG, arr.toString()).apply()
    }
}
