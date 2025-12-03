package com.example.v900.data

import android.util.Log
import com.example.v900.network.ServerSocketManager
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * DeviceRepository — централизованное хранилище текущих состояний устройств.
 */
class DeviceRepository(
    private val server: ServerSocketManager,
    private val prefs: PrefsManager
) {
    private val TAG = "DeviceRepository"
    private val mutex = Mutex()
    private val _devices = MutableStateFlow<Map<String, DeviceState>>(emptyMap())
    val devices: StateFlow<Map<String, DeviceState>> = _devices

    suspend fun updateTelemetry(deviceId: String, payload: JsonObject) {
        mutex.withLock {
            val tacho = payload.get("tacho")?.asInt
            val speed = payload.get("speed")?.asInt
            val fuel = payload.get("fuel")?.asInt
            val fresh_water = payload.get("fresh_water")?.asInt
            val black_water = payload.get("black_water")?.asInt

            val current = _devices.value[deviceId]
            val newState = if (current == null) {
                DeviceState(
                    deviceId = deviceId,
                    tacho = tacho,
                    speed = speed,
                    fuel = fuel,
                    fresh_water = fresh_water,
                    black_water = black_water,
                    lastSeenMillis = System.currentTimeMillis()
                )
            } else {
                current.copy(
                    tacho = tacho ?: current.tacho,
                    speed = speed ?: current.speed,
                    fuel = fuel ?: current.fuel,
                    fresh_water = fresh_water ?: current.fresh_water,
                    black_water = black_water ?: current.black_water,
                    lastSeenMillis = System.currentTimeMillis()
                )
            }
            val newMap = _devices.value.toMutableMap()
            newMap[deviceId] = newState
            _devices.value = newMap
        }
    }

    suspend fun sendRelayCommand(deviceId: String, relay: String, value: Int): Boolean {
        Log.i("toggleRelay", "DevicePero.sendRelayCommand: $deviceId, $String, $value")

        val json = JsonObject().apply {
            addProperty("type", "command")
            addProperty("deviceId", deviceId)
            addProperty("command", "relay")
            val payload = JsonObject()
            payload.addProperty("relay", relay)
            payload.addProperty("value", value)
            add("payload", payload)
        }

        // Отправляем команду через server
        return server.sendCommand(deviceId, json.toString())
    }

    suspend fun toggleRelay(deviceId: String, relay: String, value: Boolean) {
        // 1. Save state locally in prefs
        prefs.saveRelayState(deviceId, relay, value)
        Log.i("toggleRelay", "DevicePero.toggleRelay: $deviceId, $String, $value")

        // 2. Send command
        sendRelayCommand(deviceId, relay, if (value) 1 else 0)

        // 3. Update local state for UI
        mutex.withLock {
            val current = _devices.value[deviceId]
            if (current != null) {
                val updatedRelays = current.relays.toMutableMap()
                updatedRelays[relay] = value
                val new = current.copyWithState(relays = updatedRelays)
                _devices.value = _devices.value.toMutableMap().also { it[deviceId] = new }
            }
        }
    }

    suspend fun updateState(deviceId: String, payload: JsonObject) {
        try {
            val relays = mutableMapOf<String, Boolean>()
            for ((k, v) in payload.entrySet()) {
                val asInt = try { v.asInt } catch (_: Exception) { null }
                if (asInt != null) relays[k] = asInt != 0
            }

            mutex.withLock {
                val current = _devices.value[deviceId]
                val new = if (current == null) {
                    DeviceState(deviceId = deviceId).copyWithState(relays = relays)
                } else {
                    current.copyWithState(relays = relays)
                }
                _devices.value = _devices.value.toMutableMap().also { it[deviceId] = new }
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateState error: ${e.message}", e)
        }
    }

    fun getSnapshot(): Map<String, DeviceState> = _devices.value
}
