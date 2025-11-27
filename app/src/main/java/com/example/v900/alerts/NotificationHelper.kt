package com.example.v900.alerts


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.v900.R

object NotificationHelper {
    const val CHANNEL_ID = "alerts_channel"
    const val CHANNEL_NAME = "Alerts"
    const val NOTIF_ID_FOREGROUND = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts"
                importance = NotificationManager.IMPORTANCE_HIGH
                setSound(null, null) // мы сами проигрываем звук внутри сервиса
                enableVibration(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun buildForegroundNotification(
        context: Context,
        title: String,
        text: String,
        fullScreenIntent: PendingIntent
    ): Notification {
        val openAppIntent = Intent(context, fullScreenIntent::class.java) // placeholder
        // Basic notification with full-screen intent attached
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.alarm) // у тебя свой значок
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .build()
    }
}
