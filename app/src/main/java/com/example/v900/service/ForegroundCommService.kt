package com.example.v900.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.v900.alerts.AlertForegroundService
import com.example.v900.alerts.AlertType
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

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private var lastAlertTime = 0L
    private val ALERT_COOLDOWN_MS = 60_000L // 1 минута

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(applicationContext)
        //acquireLocks()
        startForegroundServiceNotification()
        serviceScope.launch {
            initServer()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock =
                powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "V900:ServerWakeLock")
            wakeLock?.acquire(1 * 1 * 1L /*10 minutes*/)

            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiLock = wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                    "V900:ServerWifiLock"
                )
            } else {
                @Suppress("DEPRECATION")
                wifiLock =
                    wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "V900:ServerWifiLock")
            }
            wifiLock?.acquire()
            Log.i(TAG, "WakeLock and WifiLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring locks: ${e.message}", e)
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing locks", e)
        }
    }

    private fun startForegroundServiceNotification() {
        val channel =
            NotificationChannel(CHANNEL_ID, "Comm Service", NotificationManager.IMPORTANCE_LOW)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
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
                        val payload = if (json.has("payload")) {
                            json.getAsJsonObject("payload")
                        } else {
                            json
                        }
                        deviceRepo.updateTelemetry(id, payload)

                        // Проверка условий тревоги
                        checkForAlerts(id, payload)
                    }
                },
                onState = { id, json ->
                    Log.d(TAG, "onState: $id -> $json")
                    if (::deviceRepo.isInitialized) {
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

            Log.i(TAG, "ServerSocketManager start on port $port")
            server.start()

        } catch (ex: Exception) {
            Log.e(TAG, "initServer failed: ${ex.message}", ex)
        }
    }

    private fun checkForAlerts(deviceId: String, payload: com.google.gson.JsonObject) {
        val now = System.currentTimeMillis()
        if (now - lastAlertTime < ALERT_COOLDOWN_MS) return

        val fuel = payload.get("fuel")?.asInt
        val fresh = payload.get("fresh_water")?.asInt
        val black = payload.get("black_water")?.asInt

        var alertMessage: String? = null

        // Логика тревог
        if (fuel != null) {
            if (fuel < 80) alertMessage = "Критически низкий уровень топлива: $fuel%"
            // else if (fuel > 95) alertMessage = "Бак топлива переполнен: $fuel%"
        }

        if (fresh != null && alertMessage == null) {
            if (fresh < 10) alertMessage = "Мало пресной воды: $fresh%"
        }

        if (black != null && alertMessage == null) {
            if (black > 5) alertMessage = "Бак сточных вод полон: $black%"
        }

        if (alertMessage != null) {
            lastAlertTime = now
            Log.w(TAG, "Alert triggered: $alertMessage")
            AlertForegroundService.trigger(
                context = applicationContext,
                type = AlertType.TANK_LEVEL,
                message = alertMessage,
                metadata = "Device: $deviceId"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
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
