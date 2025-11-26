package com.example.v900.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.v900.R
import com.example.v900.ui.AppViewModel
import com.example.v900.ui.theme.BorderColorTwoWay
import com.example.v900.ui.theme.BorderSize
import com.example.v900.utils.VerticalDividerTop
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithSections(
    AppViewModel: AppViewModel = viewModel(),
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
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.1f)
                    .drawBehind {
                        drawLine(
                            brush = Brush.horizontalGradient(colors = BorderColorTwoWay),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = BorderSize.toPx()
                        )
                    }
            ) {
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
                    VerticalDividerTop()
                    // Центральная часть — иконки
                    Row(
                        modifier = Modifier
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.navigation_light),
                            contentDescription = "Settings"
                        )
                        Icon(
                            painterResource(R.drawable.anchor_light),
                            contentDescription = "Settings"
                        )
                    }
                    VerticalDividerTop()
                    // Центральная часть — иконки
                    Row(
                        modifier = Modifier
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.nav_light), contentDescription = "Settings")
                        Icon(painterResource(R.drawable.anchor), contentDescription = "Settings")
                    }
                    VerticalDividerTop()
                    // Правая часть — кнопка настроек
                    IconButton(onClick = onSettingsClick) {
                        Icon(painterResource(R.drawable.anchor), contentDescription = "Settings")
                    }
                } //left block

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

