package com.example.v900.data



/**
 * Модель состояния устройства (телеметрия + состояния реле + метаданные).
 * Поля nullable — если ещё не пришли.
 */
data class DeviceState(
    val deviceId: String,
    val tacho: Int? = null,
    val speed: Int? = null,
    val fuel: Int? = null,
    val fresh_water: Int? = null,
    val black_water: Int? = null,
    val relays: Map<String, Boolean> = emptyMap(),
    val lastSeenMillis: Long = System.currentTimeMillis()
) {

    fun copyWithState(
        relays: Map<String, Boolean> = this.relays,
        lastSeenMillis: Long = System.currentTimeMillis()
    ) = this.copy(relays = relays, lastSeenMillis = lastSeenMillis)
}
