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
        const val EXTRA_METADATA = "extra_metadata"

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
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100) // Громкость 100
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

                // Создаем FullScreenIntent для Activity
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

                // Создаем уведомление с использованием NotificationHelper (ID канала v2)
                val notif = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                    .setContentTitle("ТРЕВОГА: ${type.displayName}")
                    .setContentText(message)
                    .setSmallIcon(com.example.v900.R.drawable.alarm) // Убедитесь, что ресурс существует
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setFullScreenIntent(
                        fullScreenPendingIntent,
                        true
                    ) // ВАЖНО: true = высокая срочность
                    .build()

                startForeground(NotificationHelper.NOTIF_ID_FOREGROUND, notif)

                // Запускаем пульсацию реле и звук
                scope.launch {
                    pulseRelayAndBeep(type)
                }
            }
        }

        return START_STICKY
    }


    private suspend fun pulseRelayAndBeep(type: AlertType) {
        val pulseOnMs = 100L
        val pulseOffMs = 500L
        val cycles = 5 // Повторяем 5 раз

        val repo = AppContainer.getRepo()
        // Берем ID устройства из репозитория (первое попавшееся или конкретное)
        val targetDeviceId = repo?.getSnapshot()?.keys?.firstOrNull()
        // Какое реле использовать для сирены? Например, "r1"
        val relayName = "r1" 

        repeat(cycles) {
            // ВКЛЮЧИТЬ
            if (targetDeviceId != null) {
                try {
                    repo.toggleRelay(targetDeviceId, relayName, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            delay(pulseOnMs)

            // ВЫКЛЮЧИТЬ
            if (targetDeviceId != null) {
                try {
                    repo.toggleRelay(targetDeviceId, relayName, false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            delay(pulseOffMs)
        }

        // После завершения цикла можно остановить сервис или оставить висеть уведомление
        // stopSelf() // Если нужно остановить сервис автоматически
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
    TANK_LEVEL("Уровень в баке"),
    MAINTENANCE("Обслуживание"),
    ESP_ALARM("ESP Тревога"),
    GENERAL("Общая тревога")
}
