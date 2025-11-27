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
import com.example.v900.data.PrefsManager
import com.example.v900.network.ServerSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ForegroundCommService : Service() {
    private val TAG = "ForegroundCommService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var prefs: PrefsManager
    private lateinit var server: ServerSocketManager
    private lateinit var deviceRepo: DeviceRepository

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(applicationContext)
        startForegroundServiceNotification()

        serviceScope.launch {
            initServer()
        }
    }

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, "Comm Service", NotificationManager.IMPORTANCE_LOW)
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

    private suspend fun initServer() {
        Log.i(TAG, "initServer: entering")
        try {
            val port = prefs.getServerPort()

            server = ServerSocketManager(
                port = port,
                scope = serviceScope,
                authValidator = { deviceId, token ->
                    val expected = prefs.getDeviceToken(deviceId)
                    if (expected != null && token != expected) {
                        Log.w(TAG, "Auth failed for $deviceId")
                        false
                    } else {
                        true
                    }
                },
                onTelemetry = { id, json ->
                    Log.d(TAG, "onTelemetry: $id -> $json")
                    if (::deviceRepo.isInitialized) {
                        // Извлекаем payload, если он есть (для ESP)
                        val payload = if (json.has("payload")) {
                            json.getAsJsonObject("payload")
                        } else {
                            json
                        }
                        deviceRepo.updateTelemetry(id, payload)
                    }
                },
                onState = { id, json ->
                    Log.d(TAG, "onState: $id -> $json")
                    if (::deviceRepo.isInitialized) {
                        // Извлекаем payload, если он есть
                        val payload = if (json.has("payload")) {
                            json.getAsJsonObject("payload")
                        } else {
                            json
                        }
                        deviceRepo.updateState(id, payload)
                    }
                },
                onClientConnected = { id -> Log.i(TAG, "client connected: $id") },
                onClientDisconnected = { id -> Log.i(TAG, "client disconnected: $id") }
            )

            deviceRepo = DeviceRepository(server, prefs)
            AppContainer.setRepo(deviceRepo)
            Log.i(TAG, "DeviceRepository created and set in AppContainer")

            Log.i(TAG, "About to start ServerSocketManager on port $port")
            server.start()
            Log.i(TAG, "ServerSocketManager.start() returned — server should be listening now")

        } catch (ex: Exception) {
            Log.e(TAG, "initServer failed: ${ex.message}", ex)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::server.isInitialized) {
                server.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "boat_comm_channel"
        const val NOTIFICATION_ID = 1
    }
}
