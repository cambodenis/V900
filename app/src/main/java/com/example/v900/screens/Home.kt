package com.example.v900.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    AppViewModel: AppViewModel = viewModel(),
    onDeviceClick: (deviceId: String) -> Unit = {}
) {
    val devices by AppViewModel.devices.collectAsState()

    // сохраняем позицию списка между навигациями/пересозданиями
    val listState: LazyListState = rememberSaveable(
        saver = LazyListState.Saver
    ) {
        // начальная позиция
        LazyListState(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // используем stable key = device id
        items(items = devices, key = { it.id }) { dev ->
            DeviceCard(dev = dev, onClick = onDeviceClick)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
@Composable
fun DeviceCard(dev: DeviceUiModel, onClick: (String) -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(dev.id) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Text(text = "ID: ${dev.id}", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "Tacho: ${dev.tacho?.toString() ?: "-"} rpm", color = Color.White)
            Text(text = "Speed: ${dev.speed?.toString() ?: "-"} m/s", color = Color.White)
            Text(text = "Fuel: ${dev.fuel?.toString() ?: "-"}", color = Color.White)

            val relays = if (dev.relays.isEmpty()) "-" else dev.relays.entries.joinToString { "${it.key}:${if (it.value) "ON" else "OFF"}" }
            Text(text = "Relays: $relays", color = Color.White)

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val lastSeen = try {
                sdf.format(Date(dev.lastSeenMillis))
            } catch (_: Exception) {
                "-"
            }
            Text(text = "Last seen: $lastSeen", style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
    }
}