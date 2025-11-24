package com.example.v900.data



/**
 * Модель состояния устройства (телеметрия + состояния реле + метаданные).
 * Поля nullable — если ещё не пришли.
 */
data class DeviceState(
    val deviceId: String,
    val tacho: Double? = null,
    val speed: Double? = null,
    val fuel: Double? = null,
    val water: Double? = null,
    val relays: Map<String, Boolean> = emptyMap(),
    val lastSeenMillis: Long = System.currentTimeMillis()
) {

    fun copyWithState(
        relays: Map<String, Boolean> = this.relays,
        lastSeenMillis: Long = System.currentTimeMillis()
    ) = this.copy(relays = relays, lastSeenMillis = lastSeenMillis)
}
