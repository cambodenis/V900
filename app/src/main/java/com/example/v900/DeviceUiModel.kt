// app/src/main/java/com/example/v900/ui/DeviceUiModel.kt
package com.example.v900.ui

data class DeviceUiModel(
    val id: String,
    val tacho: Double?,
    val speed: Double?,
    val fuel: Double?,
    val water: Double?,
    val relays: Map<String, Boolean>,
    val lastSeenMillis: Long
)

