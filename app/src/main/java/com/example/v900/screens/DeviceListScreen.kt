// app/src/main/java/com/example/v900/ui/DeviceRow.kt
package com.example.v900.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.v900.ui.DeviceUiModel

@Composable
fun DeviceRow(
    device: DeviceUiModel,
    onToggle: (deviceId: String, relay: String, value: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {

        Text(text = "ID: ${device.id}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))

        // Вместо lastTelemetry — выводим конкретные поля
        val tachoText = device.tacho?.let { "${it} rpm" } ?: "-"
        val speedText = device.speed?.let { "${it} m/s" } ?: "-"
        val fuelText = device.fuel?.let { "${it} %" } ?: "-"

        Text(
            text = "Tacho: $tachoText  •  Speed: $speedText  •  Fuel: $fuelText",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            // R1
            Text(text = "R1", modifier = Modifier.padding(end = 8.dp))
            val r1State = device.relays["r1"] ?: false
            CheckedSwitch(checked = r1State, onCheckedChange = { onToggle(device.id, "r1", it) })

            Spacer(modifier = Modifier.width(24.dp))

            // R2
            Text(text = "R2", modifier = Modifier.padding(end = 8.dp))
            val r2State = device.relays["r2"] ?: false
            CheckedSwitch(checked = r2State, onCheckedChange = { onToggle(device.id, "r2", it) })
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Last seen: ${java.time.Instant.ofEpochMilli(device.lastSeenMillis)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Stateless switch that receives the checked value from caller */
@Composable
fun CheckedSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // Используем Material3 Switch (stateless): checked управляется внешним кодом
    Switch(checked = checked, onCheckedChange = onCheckedChange)
}
