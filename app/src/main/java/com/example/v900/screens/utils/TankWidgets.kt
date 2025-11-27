package com.example.v900.screens.utils

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TankLevelWidget(
    modifier: Modifier = Modifier,
    title: String,
    icon: Int,
    current: Int,
    max: Int,
    segments: Int = 12,
    activeColor: Color,
    inactiveColor: Color = Color.DarkGray,
    alarmIconColor: Color = Color(0xFFFFC107),

) {
    val fraction = if (max <= 0) 0f else (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    val activeCount = (segments * fraction).toInt()
    val alarm: Boolean = (current.toFloat() / max.toFloat()).let { it <= 0.10f || it >= 0.95f }
    val segmentWidth = 20.dp
    val segmentHeight = 8.dp

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1C1C1C))
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Левый блок: заголовок, иконка, текст
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = Color.White, fontSize = 25.sp)

            Spacer(modifier = Modifier.height(6.dp))

            Icon(
                painter = painterResource(id = icon),
                contentDescription = title,
                tint = if (alarm) alarmIconColor else Color.White,
                modifier = Modifier.size(50.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "$current / $max L", color = Color.White, fontSize = 25.sp)
        }

        // Правый блок: вертикальная шкала, рисуем сверху->вниз, активность считаем от низа
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.height((segments * (segmentHeight.value + 2)).dp) // ориентировочная высота
        ) {
            repeat(segments) { indexFromTop ->
                val indexFromBottom = segments - 1 - indexFromTop
                val isActive = indexFromBottom < activeCount

                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .height(segmentHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isActive) activeColor else inactiveColor)
                )
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}
