package com.example.v900.alerts


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationHelper {
    // Изменил ID канала, чтобы сбросить настройки на устройстве
    const val CHANNEL_ID = "alerts_channel_v2"
    const val CHANNEL_NAME = "Critical Alerts"
    const val NOTIF_ID_FOREGROUND = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH // Важно для FullScreenIntent
        ).apply {
            description = "Critical alerts for V900"
            importance = NotificationManager.IMPORTANCE_HIGH
            setSound(null, null) // Звук играем сами через ToneGenerator/MediaPlayer
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC // Видно на экране блокировки
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
