package com.example.deadmanswitch.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.deadmanswitch.MainActivity

class AppNotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_ALERT = "alert_channel"
        const val NOTIFICATION_ID_ALERT = 999
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "安全警报",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "安全警报通知"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                val alarmSound: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                setSound(alarmSound, null)
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as AndroidNotificationManager
            manager.createNotificationChannel(alertChannel)
        }
    }
    
    fun createAlertNotification(hours: Long): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setContentTitle("⚠️ 安全警报")
            .setContentText("已超过 ${hours} 小时未检测到活动！")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }
    
    fun sendAlert(hours: Long) {
        try {
            val notification = createAlertNotification(hours)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as AndroidNotificationManager
            manager.notify(NOTIFICATION_ID_ALERT, notification)
        } catch (e: Exception) {
            Logging.e("AppNotificationManager", "Failed to send alert notification", e)
        }
    }
    
    fun cancelAlert() {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as AndroidNotificationManager
            manager.cancel(NOTIFICATION_ID_ALERT)
        } catch (e: Exception) {
            Logging.e("AppNotificationManager", "Failed to cancel alert notification", e)
        }
    }
}
