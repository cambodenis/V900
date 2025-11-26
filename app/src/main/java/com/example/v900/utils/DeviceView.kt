package com.example.v900.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.v900.ui.theme.BorderColorOneWay
import com.example.v900.ui.theme.BorderSize

@Composable
fun VerticalDividerTop() {
    Box(
        modifier = Modifier
            .padding(start = 10.dp, end = 10.dp)
            .fillMaxHeight()  //fill the max height
            .width(1.dp)
            .drawBehind {
                drawLine(
                    brush = Brush.verticalGradient(colors = BorderColorOneWay),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = BorderSize.toPx(),
                )
            }

    )
}
@Composable
fun VerticalDividerBottom() {
    Box(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.8f))
    )
}


