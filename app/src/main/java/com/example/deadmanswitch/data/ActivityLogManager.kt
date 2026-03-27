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

    companion object {
        private const val KEY_LOG = "activity_log"
        private const val MAX_ENTRIES = 100
    }

    fun addEntry(type: String) {
        val entry = ActivityEntry(System.currentTimeMillis(), type)
        val log = getAll().toMutableList()
        log.add(0, entry)
        if (log.size > MAX_ENTRIES) log.subList(MAX_ENTRIES, log.size).clear()
        save(log)
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
