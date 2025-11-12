package com.example.v900.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.v900.R
import com.example.v900.data.PrefsManager

/**
 * SettingsScreen — теперь с несколькими сворачиваемыми разделами.
 * Секция "Параметры подключения" — разворачиваемая и содержит форму.
 * Другие секции показаны как заглушки и тоже разворачиваются по клику.
 *
 * Сохранение:
 * - поля формы сохраняются в SharedPreferences по кнопке Save
 * - значения полей и состояние раскрытия секций сохраняются через rememberSaveable
 * - позиция прокрутки сохраняется при навигации
 */

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    // ScrollState сохраняется между навигациями
    val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    // состояния раскрытия секций — сохраняем
    var connExpanded by rememberSaveable { mutableStateOf(true) } // параметры подключения по умолчанию открыты
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var devicesExpanded by rememberSaveable { mutableStateOf(false) }
    var aboutExpanded by rememberSaveable { mutableStateOf(false) }

    // поля формы — сохраняем промежуточные данные между навигациями
    var serverIp by rememberSaveable { mutableStateOf(prefs.getServerIp()) }
    var serverPortText by rememberSaveable { mutableStateOf(prefs.getServerPort().toString()) }
    var deviceId by rememberSaveable { mutableStateOf(prefs.getDefaultDeviceId()) }
    var deviceToken by rememberSaveable { mutableStateOf(prefs.getDefaultDeviceToken() ?: "") }

    // валидация порта
    val portError by derivedStateOf {
        val p = serverPortText.toIntOrNull()
        p == null || p <= 0 || p > 65535
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text("Настройки устройства", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            // --- Раздел: Параметры подключения (collapsible) ---
            CollapsibleSection(
                title = "Параметры подключения",
                subtitle = "IP/порт сервера, deviceId, token",
                expanded = connExpanded,
                onToggle = { connExpanded = !connExpanded },
                leadingIcon = R.drawable.menu_settings
            ) {
                // контент секции
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = { serverIp = it },
                        label = { Text("Server IP (Android main device)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = serverPortText,
                        onValueChange = { input -> serverPortText = input.filter { ch -> ch.isDigit() } },
                        label = { Text("Server Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = portError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (portError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Port must be a number 1..65535", color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = { deviceId = it },
                        label = { Text("Default Device ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deviceToken,
                        onValueChange = { deviceToken = it },
                        label = { Text("Default Device Token (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                val port = serverPortText.toIntOrNull() ?: -1
                                if (port <= 0 || port > 65535) {
                                    Toast.makeText(context, "Invalid port", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                prefs.saveServerIp(serverIp.trim())
                                prefs.saveServerPort(port)
                                prefs.saveDefaultDeviceId(deviceId.trim())
                                prefs.saveDefaultDeviceToken(deviceToken.trim().ifEmpty { "" })
                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !portError
                        ) {
                            Text("Save")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Раздел: Advanced (пример) ---
            CollapsibleSection(
                title = "Advanced",
                subtitle = "Доп. параметры и логирование",
                expanded = advancedExpanded,
                onToggle = { advancedExpanded = !advancedExpanded },
                leadingIcon = R.drawable.menu_settings
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Здесь могут быть параметры логирования, уровни логов, debug-опции.")
                    Spacer(modifier = Modifier.height(8.dp))
                    // Примеры полей/кнопок
                    Button(onClick = { /* action */ }) {
                        Text("Clear logs")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Раздел: Devices (список устройств / профилей) ---
            CollapsibleSection(
                title = "Devices",
                subtitle = "Сохранённые профили ESP32",
                expanded = devicesExpanded,
                onToggle = { devicesExpanded = !devicesExpanded },
                leadingIcon = R.drawable.menu_settings
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Здесь список сохранённых устройств (позже можно добавить Add/Edit/Delete).")
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Раздел: About ---
            CollapsibleSection(
                title = "About",
                subtitle = "Информация о приложении",
                expanded = aboutExpanded,
                onToggle = { aboutExpanded = !aboutExpanded },
                leadingIcon = R.drawable.menu_settings
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Boat Control v0.9")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Разработчик: ваша команда")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * CollapsibleSection — универсальный сворачиваемый блок.
 *
 * title: заголовок
 * subtitle: подзаголовок (короткая подсказка)
 * expanded: текущее состояние
 * onToggle: переключатель состояния
 * leadingIcon: иконка слева
 * content: composable — содержимое, показывается когда expanded == true
 */
@Composable
private fun CollapsibleSection(
    title: String,
    subtitle: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    leadingIcon: Int,
    content: @Composable (() -> Unit)
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .animateContentSize()
    ) {
        Column {
            // header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (!subtitle.isNullOrEmpty()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

            }

            // body
            if (expanded) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    content()
                }
            }
        }
    }
}
