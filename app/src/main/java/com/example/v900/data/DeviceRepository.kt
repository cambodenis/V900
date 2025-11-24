package com.example.v900.data

import android.R.attr.value
import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * DeviceRepository — централизованное хранилище текущих состояний устройств.
 * - exposes StateFlow<Map<deviceId, DeviceState>>
 * - thread-safe updates
 *
 * Создаётся один экземпляр в приложении (singleton-like). Для простоты — объект с init(context) не нужен:
 * используйте конструктор в ForegroundCommService и передавайте туда ссылку, либо создайте один глобальный экземпляр.
 *
 * Здесь реализован простой класс, создайте единственный экземпляр в Service (ниже пример).
 */

class DeviceRepository() {
    private val TAG = "DeviceRepository"
    private val mutex = Mutex()
    private val _devices = MutableStateFlow<Map<String, DeviceState>>(emptyMap())
    val devices: StateFlow<Map<String, DeviceState>> = _devices
    suspend fun updateTelemetry(deviceId: String, payload: JsonObject) {

        mutex.withLock {
            
            val tacho = payload.get("tacho")?.asDouble
            val speed = payload.get("speed")?.asDouble
            val fuel = payload.get("fuel")?.asDouble
            val water = payload.get("water")?.asDouble

            val current = _devices.value[deviceId]
            val newState = if (current == null) {
                DeviceState(deviceId = deviceId, tacho = tacho, speed = speed, fuel = fuel, water = water, lastSeenMillis = System.currentTimeMillis())
            } else {
                current.copy(
                    tacho = tacho ?: current.tacho,
                    speed = speed ?: current.speed,
                    fuel = fuel ?: current.fuel,
                    water = water ?: current.fuel,
                    lastSeenMillis = System.currentTimeMillis()
                )
            }
            val newMap = _devices.value.toMutableMap()
            newMap[deviceId] = newState
            _devices.value = newMap
        }


    }
    suspend fun updateState(deviceId: String, payload: JsonObject) {
        try {
            // ожидаем payload с ключами реле, например {"r1":1,"r2":0}
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

    // optional helper to get snapshot
    fun getSnapshot(): Map<String, DeviceState> = _devices.value

}
