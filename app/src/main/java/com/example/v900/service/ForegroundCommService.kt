package com.example.v900.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.v900.data.AppContainer
import com.example.v900.data.DeviceRepository
import com.example.v900.network.ServerSocketManager
import com.example.v900.data.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ForegroundCommService — держит ServerSocketManager в Foreground.
 * Пакет: com.example.v900.service
 *
 * Интеграция: сервис создаёт PrefsManager и ServerSocketManager, стартует последний.
 * В production — DI (Hilt) рекомендуется.
 */
class ForegroundCommService : Service() {
    private val TAG = "ForegroundCommService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var prefs: PrefsManager
    private lateinit var server: ServerSocketManager

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(applicationContext)
        startForegroundServiceNotification()
        initServer()


    }

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Сервер связи запущен", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
        startForeground(NOTIFICATION_ID, buildNotification("Сервер связи запущен"))
    }

    private fun buildNotification(text: String): Notification {
        val builder = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Boat Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
        return builder.build()
    }

    // в классе ForegroundCommService
    private lateinit var deviceRepo: DeviceRepository

    private val TAGS = "ForegroundCommService"

    private fun initServer() {
        Log.i(TAGS, "initServer: entering")
        try {
            deviceRepo = DeviceRepository()
            AppContainer.setRepo(deviceRepo)
            Log.i(TAGS, "DeviceRepository created and set in AppContainer")

            server = ServerSocketManager(
                port = prefs.getServerPort(),
                scope = serviceScope,
                authValidator = { deviceId, token -> true }, // временно permissive
                onTelemetry = { id, json -> Log.i(TAG, "telemetry callback $id -> $json") },
                onState = { id, json -> Log.i(TAG, "state callback $id -> $json") },
                onClientConnected = { id -> Log.i(TAG, "client connected: $id") },
                onClientDisconnected = { id -> Log.i(TAG, "client disconnected: $id") }
            )

            Log.i(TAGS, "About to start ServerSocketManager on port ${prefs.getServerPort()}")
            server.start()
            Log.i(TAGS, "ServerSocketManager.start() returned — server should be listening now")

        } catch (ex: Exception) {
            Log.e(TAGS, "initServer failed: ${ex.message}", ex)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "boat_comm_channel"
        const val NOTIFICATION_ID = 1
    }
}
