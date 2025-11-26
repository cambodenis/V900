package com.example.v900.screens.utils

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.v900.network.RelayController
import kotlinx.coroutines.delay

@Composable
fun TankWithAlarm(
    title: String,
    icon: Int,
    current: Int,
    max: Int,
    activeColor: Color,
    modifier: Modifier = Modifier

) {
    // thresholds
    val lowThresholdFraction = 0.10f   // 10%
    val highThresholdFraction = 0.95f  // 95%

    val fraction = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    val isLow = fraction <= lowThresholdFraction
    val isHigh = fraction >= highThresholdFraction
    val alarmActive = isLow || isHigh

    // permanent mute (пользователь нажал "Отключить звук")
    var muted by remember { mutableStateOf(false) }
    // временная отложенная глушилка: хранит время в millis до которого глушим
    var snoozeUntil by remember { mutableStateOf<Long?>(null) }
    // effectiveMuted используется в логике отправки команд
    val effectiveMuted = muted || (snoozeUntil != null)

    // Параметры пульса реле
    val relayId = "r1"
    val pulseCountPerCycle = 3
    val pulseOnMs = 120L
    val pulseOffMs = 150L
    val cycleIntervalMs = 5000L

    // ToneGenerator: создаём и корректно освобождаем
    val toneGenerator =
        remember { ToneGenerator(AudioManager.STREAM_ALARM, 100) } // 100 = громкость 0..100
    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator.release()
            } catch (_: Throwable) { /* ignore */
            }
        }
    }

    // Корутна, которая следит за snoozeUntil и очищает его по истечении времени
    LaunchedEffect(snoozeUntil) {
        val until = snoozeUntil
        if (until != null) {
            // Ждём пока время не пройдет; проверяем периодически
            while (snoozeUntil != null && snoozeUntil!! > System.currentTimeMillis()) {
                delay(1000L)
            }
            // время вышло — очищаем snooze
            snoozeUntil = null
        }
    }

// LaunchedEffect для пульсации реле и локального звука — реагирует на alarmActive и effectiveMuted
    LaunchedEffect(alarmActive, effectiveMuted) {
        if (alarmActive && !effectiveMuted) {
            // Выполняем до тех пор, пока условие истинно и не заглушено
            while (alarmActive && !effectiveMuted) {
                repeat(pulseCountPerCycle) {
                    // включаем реле (асинхронный toggle)
                    RelayController.toggleRelayAsync("Relay", relayId)
                    // локальный звук синхронно с включением реле
                    try {
                        // тип сигнала — короткий проп-тип бип; длительность в миллисекундах
                        toneGenerator.startTone(
                            android.media.ToneGenerator.TONE_PROP_BEEP,
                            pulseOnMs.toInt()
                        )
                    } catch (_: Throwable) {
                        // безопасно игнорируем любые ошибки воспроизведения
                    }
                    delay(pulseOnMs)

                    // выключаем реле
                    RelayController.toggleRelayAsync("Relay", relayId)
                    delay(pulseOffMs)
                }
                delay(cycleIntervalMs)
                // при изменении alarmActive/effectiveMuted LaunchedEffect перезапустится
            }
        }
    }
    // UI: виджет уровня
    Column(modifier = modifier) {
        TankLevelWidget(
            title = title,
            icon = icon,
            current = current,
            max = max,
            activeColor = activeColor,
            inactiveColor = Color.DarkGray,
            alarm = alarmActive,                 // ← ПЕРЕДАЁМ
            alarmIconColor = Color(0xFFFFC107)   // ← ЖЁЛТЫЙ ЦВЕТ
        )
    }

    // Диалог: показываем если тревога и не заглушено постоянно и не отложено (effectiveMuted == false)
    if (alarmActive && !effectiveMuted) {
        AlertDialog(
            onDismissRequest = { /* требуем явного действия */ },
            title = {
                Text(text = "Аварийный сигнал", color = Color.White)
            },
            text = {
                val reason = when {
                    isLow -> "Уровень ниже 10%: $current / $max L"
                    isHigh -> "Уровень выше 95%: $current / $max L"
                    else -> ""
                }
                Text(text = reason, color = Color.White)
            },
            confirmButton = {
                TextButton(onClick = {
                    // постоянная глушилка
                    muted = true
                }) {
                    Text("Отключить звук")
                }
            },
            dismissButton = {
                // добавляем кнопку "Отложить 5 минут"
                TextButton(onClick = {
                    // устанавливаем snooze на 5 минут от текущего времени
                    val fiveMinutesMs = 5 * 60 * 1000L
                    snoozeUntil = System.currentTimeMillis() + fiveMinutesMs
                }) {
                    Text("Отложить 5 минут")
                }
            },
            containerColor = Color(0xFF333333)
        )
    }
}

@Composable
fun TankLevelWidget(
    title: String,
    icon: Int,
    current: Int,
    max: Int,
    segments: Int = 12,
    activeColor: Color,
    inactiveColor: Color = Color.DarkGray,
    alarm: Boolean = false,                     // ← ДОБАВЛЕНО
    alarmIconColor: Color = Color.Yellow,       // ← ДОБАВЛЕНО
    modifier: Modifier = Modifier
) {
    val fraction = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    val activeCount = (segments * fraction).toInt()

    Row(
        modifier = modifier.padding(5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 25.sp)

            Spacer(Modifier.height(6.dp))

            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,

                // ← ВАЖНО: ВЫБИРАЕМ ЦВЕТ ПО АЛАРМУ
                tint = if (alarm) alarmIconColor else Color.White,

                modifier = Modifier.size(50.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text("$current / ${max}L", color = Color.White, fontSize = 25.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.height(130.dp)
        ) {
            repeat(segments) { indexFromTop ->
                val indexFromBottom = segments - 1 - indexFromTop
                val isActive = indexFromBottom < activeCount

                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isActive) activeColor else inactiveColor)
                )
            }
        }
    }
}

@Composable
fun TankLevelWidget1(
    title: String,
    icon: Int,
    current: Int,
    max: Int,
    segments: Int = 12,
    activeColor: Color,
    inactiveColor: Color = Color.DarkGray,
    modifier: Modifier = Modifier
) {
    val fraction = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    val activeCount = (segments * fraction).toInt()

    Row(
        modifier = modifier
            .padding(5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 25.sp)

            Spacer(Modifier.height(6.dp))

            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "$current / ${max}L",
                color = Color.White,
                fontSize = 25.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        // Колонка сегментов: рисуем сверху->вниз, но вычисляем активность относительно низа
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.height(130.dp)
        ) {
            repeat(segments) { indexFromTop ->
                val indexFromBottom = segments - 1 - indexFromTop
                val isActive = indexFromBottom < activeCount

                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) activeColor else inactiveColor
                        )
                )
            }
        }
    }
}