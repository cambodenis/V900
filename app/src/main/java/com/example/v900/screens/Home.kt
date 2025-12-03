package com.example.v900.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.v900.R
import com.example.v900.data.AppContainer
import com.example.v900.screens.utils.TankLevelWidget
import com.example.v900.ui.theme.Blue
import com.example.v900.ui.theme.FuelGauge
import com.example.v900.ui.theme.GrayGauge
import com.example.v900.ui.theme.WaterGauge

@Composable
fun HomeScreen(
    viewModel: AppViewModel = viewModel(),
) {
    val repo = AppContainer.getRepo()
    val devices by viewModel.devices.collectAsState()
    // Явно указываем тип it, чтобы компилятор мог его вывести
    val Sensors = devices.find { it: DeviceUiModel -> it.id == "Sensors" }
    val Relay = devices.find { it: DeviceUiModel -> it.id == "Relay" }

    val fuel = Relay?.fuel?.toInt() ?: 0
    val fresh = Sensors?.fresh_water?.toInt() ?: 0
    val black = Sensors?.black_water?.toInt() ?: 0

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding()
    ) {
        Column(
            modifier = Modifier
                .padding()
                .fillMaxHeight()
                .weight(15f),
            verticalArrangement = Arrangement.Center,
        ) {
            TankLevelWidget(
                modifier = Modifier.fillMaxWidth(),
                title = "Fuel",
                icon = R.drawable.fuel,
                current = fuel,
                max = 300,
                activeColor = FuelGauge,
                inactiveColor = Color(0xFF2B2B2B),
                alarmIconColor = Color(0xFFFFC107),
            )

            TankLevelWidget(
                modifier = Modifier.fillMaxWidth(),
                title = "Water",
                icon = R.drawable.fresh_water,
                current = fresh,
                max = 360,
                activeColor = WaterGauge,
                inactiveColor = Color(0xFF2B2B2B),
                alarmIconColor = Color(0xFFFFC107),
            )

            TankLevelWidget(
                modifier = Modifier.fillMaxWidth(),
                title = "WC",
                icon = R.drawable.grey_water,
                current = black,
                max = 80,
                activeColor = GrayGauge,
                inactiveColor = Color(0xFF2B2B2B),
                alarmIconColor = Color(0xFFFFC107),
            )
        }

        Column(
            modifier = Modifier
                .padding()
                .fillMaxHeight()
                .weight(70f)
        ) {
            Box(
                modifier = with(Modifier) {
                    fillMaxSize()
                        .paint(
                            painterResource(id = R.drawable.isometry_view),
                            contentScale = ContentScale.FillBounds
                        )
                })
            {
            }
        }

        Column(
            modifier = Modifier
                .padding()
                .fillMaxHeight()
                .weight(15f)
        ) {
            val r1State = Sensors?.relays?.get("r1") ?: false

            Button(onClick = { viewModel.toggleRelay("Sensors", "r1", !r1State) })
            {
                Text(if (r1State) "R1 ON" else "R1 OFF", color = Blue)
            }
        }
    }
}
