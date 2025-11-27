package com.example.v900.alerts

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.v900.data.AppContainer
import com.example.v900.network.RelayController.toggleRelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class AlertForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var toneGenerator: ToneGenerator? = null

    companion object {
        const val ACTION_TRIGGER = "ACTION_TRIGGER"
        const val EXTRA_ALERT_TYPE = "extra_alert_type"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_METADATA = "extra_metadata" // json or simple string

        fun trigger(
            context: Context,
            type: AlertType,
            message: String,
            metadata: String? = null
        ) {
            val intent = Intent(context, AlertForegroundService::class.java).apply {
                action = ACTION_TRIGGER
                putExtra(EXTRA_ALERT_TYPE, type.name)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_METADATA, metadata)
            }
            // Use startForegroundService for API >= 26
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 80)
    }

    @SuppressLint("FullScreenIntentPolicy")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        when (intent.action) {
            ACTION_TRIGGER -> {
                val typeName = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: "UNKNOWN"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                val metadata = intent.getStringExtra(EXTRA_METADATA)

                val type = try {
                    AlertType.valueOf(typeName)
                } catch (e: Exception) {
                    AlertType.GENERAL
                }

                // FULLSCREEN INTENT — исправлено
                val fullScreenIntent = Intent(this, AlertActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                    putExtra(AlertActivity.EXTRA_ALERT_TYPE, type.name)
                    putExtra(AlertActivity.EXTRA_MESSAGE, message)
                    putExtra(AlertActivity.EXTRA_METADATA, metadata)
                }

                val fullScreenPendingIntent = PendingIntent.getActivity(
                    this,
                    type.ordinal,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or getMutableFlag()
                )

                // foreground notification
                val notif = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                    .setContentTitle("Сигнал: ${type.displayName}")
                    .setContentText(message)
                    .setSmallIcon(com.example.v900.R.drawable.alarm)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .build()

                startForeground(NotificationHelper.NOTIF_ID_FOREGROUND, notif)

                // реле + звук
                scope.launch {
                    pulseRelayAndBeep(type)
                }
            }
        }

        return START_STICKY
    }


    private suspend fun pulseRelayAndBeep(type: AlertType) {
        // Простой пример: 2 коротких импульса + пауза
        val pulseOnMs = 120L
        val pulseOffMs = 150L
        val cycles = 2

        // Пытаемся найти устройство для сигнализации
        val repo = AppContainer.getRepo()
        // Берем первое попавшееся устройство, если нет конкретного ID в метаданных
        // В идеале ID устройства должно приходить в metadata
        val targetDeviceId = repo?.devices?.value?.keys?.firstOrNull()
        val relayName = "r1" // Предположим, что сирена на r1

        repeat(cycles) {
            if (targetDeviceId != null) {
                try {
                    toggleRelay(targetDeviceId, relayName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, pulseOnMs.toInt())
            delay(pulseOnMs)

            if (targetDeviceId != null) {
                try {
                    toggleRelay(targetDeviceId, relayName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            delay(pulseOffMs)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        toneGenerator?.release()
        toneGenerator = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getMutableFlag(): Int {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else 0
    }
}

enum class AlertType(val displayName: String) {
    TANK_LEVEL("Tank level"),
    MAINTENANCE("Maintenance"),
    ESP_ALARM("ESP Alarm"),
    GENERAL("General")
}
