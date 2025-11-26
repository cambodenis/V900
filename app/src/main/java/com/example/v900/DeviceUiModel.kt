// app/src/main/java/com/example/v900/ui/DeviceUiModel.kt
package com.example.v900.ui

data class DeviceUiModel(
    val id: String,
    val tacho: Int?,
    val speed: Int?,
    val fuel: Int?,
    val fresh_water: Int?,
    val black_water: Int?,
    val relays: Map<String, Boolean>,
    val lastSeenMillis: Long
)

