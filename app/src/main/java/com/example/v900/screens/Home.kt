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
import com.example.v900.network.RelayController
import com.example.v900.screens.utils.TankLevelWidget
import com.example.v900.ui.theme.FuelGauge
import com.example.v900.ui.theme.GrayGauge
import com.example.v900.ui.theme.WaterGauge

@Composable
fun HomeScreen(
    AppViewModel: AppViewModel = viewModel(),
    onDeviceClick: (deviceId: String) -> Unit = {}
) {
    val devices by AppViewModel.devices.collectAsState()


    // Берём первое устройство (или null, если нет данных)
    val Sernsor = devices.find { it.id == "Sensor" }
    val Relay = devices.find { it.id == "Relay" }
    val fuel = Relay?.fuel ?: 0
    val fresh = Relay?.fresh_water ?: 0
    val black = Relay?.black_water ?: 0


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
                activeColor = FuelGauge,        // примерный цвет заполнения
                inactiveColor = Color(0xFF2B2B2B),
                alarmIconColor = Color(0xFFFFC107),      // цвет иконки при тревоге

            )


            TankLevelWidget(
                modifier = Modifier.fillMaxWidth(),
                title = "Water",
                icon = R.drawable.fresh_water,
                current = fresh,
                max = 360,
                activeColor = WaterGauge,        // примерный цвет заполнения
                inactiveColor = Color(0xFF2B2B2B),
                alarmIconColor = Color(0xFFFFC107),      // цвет иконки при тревоге
            )

            //   Spacer(Modifier.height(12.dp))

            TankLevelWidget(
                modifier = Modifier.fillMaxWidth(),
                title = "WC",
                icon = R.drawable.grey_water,
                current = black,
                max = 80,
                activeColor = GrayGauge,        // примерный цвет заполнения
                inactiveColor = Color(0xFF2B2B2B),
                alarmIconColor = Color(0xFFFFC107),      // цвет иконки при тревоге
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
                            // Replace with your image id
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

            Button(onClick = {
                RelayController.toggleRelayAsync("Relay", "r1")

            }) {
                Text("Toggle R1")
            }
        }
    }



}