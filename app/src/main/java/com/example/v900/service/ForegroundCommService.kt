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

/**
 * ForegroundCommService — держит ServerSocketManager в Foreground.
 * Пакет: com.example.v900.service
 *
 * Интеграция: сервис создаёт PrefsManager и ServerSocketManager, стартует последний.
 * В production — DI (Hilt) рекомендуется.
 */
class ForegroundCommService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var prefs: PrefsManager
    private lateinit var server: ServerSocketManager

    // в классе ForegroundCommService
    private lateinit var deviceRepo: DeviceRepository

    private val TAGS = "ForegroundCommService"




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


    private fun initServer() {
        Log.i(TAGS, "initServer: entering")
        try {
            deviceRepo = DeviceRepository()
            AppContainer.setRepo(deviceRepo)
            Log.i(TAGS, "DeviceRepository created and set in AppContainer")

            server = ServerSocketManager(
                port = prefs.getServerPort(),
                scope = serviceScope,
                onTelemetry = let@{ deviceId, json ->
                    try {
                        Log.i(TAGS, "onTelemetry received for $deviceId -> $json")
                        // optional: token check (json may contain token field)
                        val token = json.get("token")?.asString
                        val expected = prefs.getDeviceToken(deviceId)
                        if (expected != null && expected != token) {
                            Log.w(TAGS, "Auth failed for $deviceId (token mismatch)")
                            return@let // or simply return
                        }

                        // payload may be passed directly by ServerSocketManager as JsonObject payload
                        // if json here is payload already — pass it directly; else, if full message, extract "payload"
                        val payload = if (json.has("tacho") || json.has("speed")) {
                            // already payload object
                            json
                        } else {
                            json.getAsJsonObject("payload") ?: json
                        }

                        // call repository (suspend), run in a coroutine scope to avoid blocking
                        // deviceRepo is available as field in the service
                        serviceScope.launch {
                            try {
                                deviceRepo.updateTelemetry(deviceId, payload)
                                Log.i(TAGS, "DeviceRepository.updateTelemetry called for $deviceId")
                            } catch (e: Exception) {
                                Log.e(TAGS, "Failed to update telemetry for $deviceId: ${e.message}", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAGS, "onTelemetry processing error: ${e.message}", e)
                    }
                },

                onState = { deviceId, json ->
                    try {
                        Log.i(TAGS, "onState received for $deviceId -> $json")
                        val payload = if (json.has("r1") || json.has("r2")) json else json.getAsJsonObject("payload") ?: json
                        serviceScope.launch {
                            try {
                                deviceRepo.updateState(deviceId, payload)
                                Log.i(TAGS, "DeviceRepository.updateState called for $deviceId")
                            } catch (e: Exception) {
                                Log.e(TAGS, "Failed to update state for $deviceId: ${e.message}", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAGS, "onState processing error: ${e.message}", e)
                    }
                },

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
