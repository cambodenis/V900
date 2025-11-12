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
    val relays: Map<String, Boolean> = emptyMap(),
    val lastSeenMillis: Long = System.currentTimeMillis()
) {
    fun copyWithTelemetry(
        tacho: Double? = this.tacho,
        speed: Double? = this.speed,
        fuel: Double? = this.fuel,
        lastSeenMillis: Long = System.currentTimeMillis()
    ) = this.copy(tacho = tacho, speed = speed, fuel = fuel, lastSeenMillis = lastSeenMillis)

    fun copyWithState(
        relays: Map<String, Boolean> = this.relays,
        lastSeenMillis: Long = System.currentTimeMillis()
    ) = this.copy(relays = relays, lastSeenMillis = lastSeenMillis)
}
