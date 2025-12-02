package com.example.v900.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "boat_prefs_v900")

/**
 * PrefsManager — обёртка над DataStore (Preferences).
 * Все операции теперь асинхронны (suspend / Flow).
 */
class PrefsManager(private val context: Context) {

    companion object {
        val KEY_PORT = intPreferencesKey("server_port")
        val KEY_SERVER_IP = stringPreferencesKey("server_ip")
        val KEY_DEVICE_ID = stringPreferencesKey("default_device_id")
        val KEY_DEVICE_TOKEN = stringPreferencesKey("default_device_token")

        // Defaults
        const val DEFAULT_SERVER_IP = "192.168.100.5"
        const val DEFAULT_PORT = 12345
        const val DEFAULT_DEVICE_ID = "esp01"
    }

    // --- Helper to get key for dynamic items ---
    private fun tokenKey(deviceId: String) = stringPreferencesKey("token_$deviceId")
    private fun relayKey(deviceId: String, relayName: String) =
        booleanPreferencesKey("${deviceId}_relay_$relayName")

    private fun telemetryKey(deviceId: String, key: String) =
        stringPreferencesKey("${deviceId}_telemetry_$key")

    // --- Port ---
    val serverPortFlow: Flow<Int> = context.dataStore.data.map { it[KEY_PORT] ?: DEFAULT_PORT }
    suspend fun getServerPort(): Int = serverPortFlow.first()
    suspend fun saveServerPort(port: Int) {
        context.dataStore.edit { it[KEY_PORT] = port }
    }

    // --- Server IP ---
    val serverIpFlow: Flow<String> =
        context.dataStore.data.map { it[KEY_SERVER_IP] ?: DEFAULT_SERVER_IP }

    suspend fun getServerIp(): String = serverIpFlow.first()
    suspend fun saveServerIp(ip: String) {
        context.dataStore.edit { it[KEY_SERVER_IP] = ip }
    }

    // --- Device ID/Token ---
    val defaultDeviceIdFlow: Flow<String> =
        context.dataStore.data.map { it[KEY_DEVICE_ID] ?: DEFAULT_DEVICE_ID }

    suspend fun getDefaultDeviceId(): String = defaultDeviceIdFlow.first()
    suspend fun saveDefaultDeviceId(id: String) {
        context.dataStore.edit { it[KEY_DEVICE_ID] = id }
    }

    suspend fun saveDefaultDeviceToken(token: String) {
        context.dataStore.edit { it[KEY_DEVICE_TOKEN] = token }
    }

    suspend fun getDefaultDeviceToken(): String? =
        context.dataStore.data.map { it[KEY_DEVICE_TOKEN] }.first()

    // --- Relays ---
    suspend fun saveRelayState(deviceId: String, relayName: String, value: Boolean) {
        context.dataStore.edit { it[relayKey(deviceId, relayName)] = value }
    }

    // Note: getting relay state might be better observed via Flow in UI,
    // but if needed one-shot:
    suspend fun getRelayState(deviceId: String, relayName: String): Boolean {
        return context.dataStore.data.map { it[relayKey(deviceId, relayName)] ?: false }.first()
    }

    // --- Token per device ---
    suspend fun saveDeviceToken(deviceId: String, token: String) {
        context.dataStore.edit { it[tokenKey(deviceId)] = token }
    }

    suspend fun getDeviceToken(deviceId: String): String? {
        return context.dataStore.data.map { it[tokenKey(deviceId)] }.first()
    }
}
