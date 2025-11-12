package com.example.v900.screens.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.v900.R
import com.example.v900.ui.theme.VerticalDivider
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithSections(
    onSettingsClick: () -> Unit = {}
) {
    val timeState = remember { mutableStateOf(getCurrentTime()) }

    // Обновляем время каждую минуту
    LaunchedEffect(Unit) {
        while (true) {
            timeState.value = getCurrentTime()
            delay(60_000L)
        }
    }

    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Левая часть — дата и время
                Text(
                    text = timeState.value,
                    style = MaterialTheme.typography.bodyLarge
                )
                VerticalDivider()
                // Центральная часть — иконки
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.menu_settings), contentDescription = "Settings")
                    Icon(painterResource(R.drawable.menu_settings), contentDescription = "Settings")
                }
                VerticalDivider()
                // Правая часть — кнопка настроек
                IconButton(onClick = onSettingsClick) {
                    Icon(painterResource(R.drawable.menu_settings), contentDescription = "Settings")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

// Функция получения текущей даты и времени
fun getCurrentTime(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm - dd.MM.yyyy ")
    return LocalDateTime.now().format(formatter)
}

