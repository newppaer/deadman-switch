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
import android.os.Build.VERSION_CODES
import androidx.core.app.NotificationCompat
import com.example.deadmanswitch.MainActivity
import com.example.deadmanswitch.R

class AppNotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_MONITOR = "monitor_channel"
        const val CHANNEL_ALERT = "alert_channel"
        const val NOTIFICATION_ID_MONITOR = 1
        const val NOTIFICATION_ID_ALERT = 999
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR,
                context.getString(R.string.notification_channel_monitor),
                AndroidNotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "监控服务运行状态通知"
                setShowBadge(false)
            }
            
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                context.getString(R.string.notification_channel_alert),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "安全警报通知"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                // 使用系统默认警报音
                val alarmSound: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                setSound(alarmSound, null)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as AndroidNotificationManager
            
            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
    
    fun createMonitoringNotification(): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setContentTitle(context.getString(R.string.notification_title_monitoring))
            .setContentText(context.getString(R.string.notification_text_monitoring))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
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
            .setContentTitle(context.getString(R.string.alert_title))
            .setContentText(context.getString(R.string.alert_message).format(hours))
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
        val notification = createAlertNotification(hours)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
            as AndroidNotificationManager
        manager.notify(NOTIFICATION_ID_ALERT, notification)
    }
    
    fun cancelAlert() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
            as AndroidNotificationManager
        manager.cancel(NOTIFICATION_ID_ALERT)
    }
}