// app/src/main/java/com/example/v900/ui/DeviceUiModel.kt
package com.example.v900.ui

import com.example.v900.data.DeviceState

data class DeviceUiModel(
    val id: String,
    val tacho: Double?,
    val speed: Double?,
    val fuel: Double?,
    val relays: Map<String, Boolean>,
    val lastSeenMillis: Long
)

fun DeviceState.toUiModel(): DeviceUiModel = DeviceUiModel(
    id = deviceId,
    tacho = tacho,
    speed = speed,
    fuel = fuel,
    relays = relays,
    lastSeenMillis = lastSeenMillis
)
