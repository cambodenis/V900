package com.example.v900.data

import android.content.Context
import androidx.core.content.edit

/**
 * PrefsManager — потокобезопасная обёртка SharedPreferences.
 * Расширен для хранения параметров подключения ESP32:
 * - server IP (главный Android сервер / broker IP)
 * - server port
 * - defaultDeviceId (префикс/идентификатор устройства)
 * - defaultDeviceToken
 *
 * Использование: PrefsManager(context).getServerIp() / saveServerIp(...)
 */
class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("boat_prefs_v900", Context.MODE_PRIVATE)

    // --- существующие методы (сохраняем) ---
    fun saveDeviceToken(deviceId: String, token: String) {
        prefs.edit { putString("token_$deviceId", token) }
    }

    fun getDeviceToken(deviceId: String): String? = prefs.getString("token_$deviceId", null)

    fun saveRelayState(deviceId: String, relayName: String, value: Boolean) {
        prefs.edit { putBoolean("${deviceId}_relay_$relayName", value) }
    }

    fun getRelayState(deviceId: String, relayName: String): Boolean =
        prefs.getBoolean("${deviceId}_relay_$relayName", false)

    fun saveServerPort(port: Int) { prefs.edit { putInt(KEY_PORT, port) } }
    fun getServerPort(): Int = prefs.getInt(KEY_PORT, DEFAULT_PORT)

    fun saveLastTelemetry(deviceId: String, key: String, value: String) {
        prefs.edit { putString("${deviceId}_telemetry_$key", value) }
    }
    fun getLastTelemetry(deviceId: String, key: String): String? =
        prefs.getString("${deviceId}_telemetry_$key", null)

    // --- new: esp connection params ---
    fun saveServerIp(ip: String) { prefs.edit { putString(KEY_SERVER_IP, ip) } }
    fun getServerIp(): String = prefs.getString(KEY_SERVER_IP, DEFAULT_SERVER_IP) ?: DEFAULT_SERVER_IP

    fun saveDefaultDeviceId(deviceId: String) { prefs.edit { putString(KEY_DEVICE_ID, deviceId) } }
    fun getDefaultDeviceId(): String = prefs.getString(KEY_DEVICE_ID, DEFAULT_DEVICE_ID) ?: DEFAULT_DEVICE_ID

    fun saveDefaultDeviceToken(token: String) { prefs.edit { putString(KEY_DEVICE_TOKEN, token) } }
    fun getDefaultDeviceToken(): String? = prefs.getString(KEY_DEVICE_TOKEN, null)

    companion object {
        private const val KEY_PORT = "server_port"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_DEVICE_ID = "default_device_id"
        private const val KEY_DEVICE_TOKEN = "default_device_token"

        // sensible defaults — поменяйте при необходимости
        private const val DEFAULT_SERVER_IP = "192.168.4.100"
        private const val DEFAULT_PORT = 12345
        private const val DEFAULT_DEVICE_ID = "esp01"
    }
}
