package com.example.deadmanswitch.data

import android.content.Context
import android.content.SharedPreferences
import android.telephony.SmsManager
import org.json.JSONArray
import org.json.JSONObject

data class EmergencyContact(
    val name: String,
    val phone: String,
    val enabled: Boolean = true
)

class ContactManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("deadman_prefs", 0)

    companion object {
        private const val KEY_CONTACTS = "emergency_contacts"
        private const val KEY_SMS_ENABLED = "sms_enabled"
        private const val KEY_SMS_MESSAGE = "sms_message"
        const val DEFAULT_MESSAGE = "⚠️ DeadManSwitch 警报：我已超过设定时间未使用手机，可能遇到紧急情况。请尝试联系我或查看我的状况。"
    }

    fun getAll(): List<EmergencyContact> {
        val json = prefs.getString(KEY_CONTACTS, "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<EmergencyContact>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(EmergencyContact(
                obj.getString("name"),
                obj.getString("phone"),
                obj.optBoolean("enabled", true)
            ))
        }
        return list
    }

    fun add(contact: EmergencyContact) {
        val list = getAll().toMutableList()
        list.add(contact)
        save(list)
    }

    fun remove(index: Int) {
        val list = getAll().toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            save(list)
        }
    }

    fun update(index: Int, contact: EmergencyContact) {
        val list = getAll().toMutableList()
        if (index in list.indices) {
            list[index] = contact
            save(list)
        }
    }

    fun isSmsEnabled(): Boolean = prefs.getBoolean(KEY_SMS_ENABLED, false)

    fun setSmsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMS_ENABLED, enabled).apply()
    }

    fun getSmsMessage(): String = prefs.getString(KEY_SMS_MESSAGE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE

    fun setSmsMessage(message: String) {
        prefs.edit().putString(KEY_SMS_MESSAGE, message).apply()
    }

    fun sendAlertSms(hours: Long) {
        if (!isSmsEnabled()) return
        val contacts = getAll().filter { it.enabled }
        if (contacts.isEmpty()) return

        val message = getSmsMessage() + "\n（已 ${hours} 小时无活动）"
        val smsManager = SmsManager.getDefault()

        for (contact in contacts) {
            try {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun save(list: List<EmergencyContact>) {
        val arr = JSONArray()
        for (c in list) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("enabled", c.enabled)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CONTACTS, arr.toString()).apply()
    }
}
