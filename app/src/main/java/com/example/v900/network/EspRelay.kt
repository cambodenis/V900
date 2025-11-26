package com.example.v900.network

import android.util.Log
import com.example.v900.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object RelayController {
    private const val TAG = "RelayController"

    // общий scope для фоновых задач. В идеале использовать DI / Shared scope, но для простоты:
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    /**
     * Fire-and-forget toggle (вызвать из UI без блокирования).
     * Оптимистично переключает: читает существующее состояние и отправляет обратное значение.
     */
    fun toggleRelayAsync(deviceId: String, relayName: String) {
        scope.launch {
            val ok = toggleRelay(deviceId, relayName)
            if (!ok) {
                Log.w(TAG, "toggleRelayAsync failed for $deviceId/$relayName")
            }
        }
    }

    /**
     * Suspend-версия: читает текущее состояние (из репозитория snapshot) и отправляет противоположное.
     * Возвращает true если отправка команды прошла успешно.
     */
    suspend fun toggleRelay(deviceId: String, relayName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val repo = AppContainer.repoFlow.value
                if (repo == null) {
                    Log.w(TAG, "toggleRelay: repo is null, service not running")
                    return@withContext false
                }

                // Снимок текущего состояния устройства (если нет — считаем false)
                val deviceState = repo.getSnapshot()[deviceId]
                val current = deviceState?.relays?.get(relayName) ?: false
                val newValue = if (current) 0 else 1

                Log.i(
                    TAG,
                    "toggleRelay: device=$deviceId relay=$relayName current=$current -> new=$newValue"
                )

                // вызов репозитория отправки команды
                val success = repo.sendRelayCommand(deviceId, relayName, newValue)
                if (!success) {
                    Log.w(
                        TAG,
                        "toggleRelay: sendRelayCommand returned false for $deviceId/$relayName"
                    )
                } else {
                    Log.i(TAG, "toggleRelay: command sent ok for $deviceId/$relayName")
                }
                return@withContext success
            } catch (e: Exception) {
                Log.e(TAG, "toggleRelay exception: ${e.message}", e)
                return@withContext false
            }
        }
    }

}