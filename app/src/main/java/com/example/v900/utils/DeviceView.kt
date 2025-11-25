package com.example.v900.utils

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sbi.utils.windowWidth
import com.example.v900.data.AppContainer
import com.example.v900.ui.theme.White
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object RelayController {
    private const val TAG = "RelayController"
    // общий scope для фоновых задач. В идеале использовать DI / Shared scope, но для простоты:
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    /**
     * Fire-and-forget toggle (вызвать из UI без блокирования).
     * Оптимистично переключает: читает существующее состояние и отправляет обратное значение.
     */
    fun toggleRelayAsync(deviceId: String, relayName: String) {
        scope.launch {
            val ok = toggleRelay(deviceId, relayName)
            if (!ok) {
                Log.w(TAG, "toggleRelayAsync failed for $deviceId/$relayName")
            }
        }
    }

    /**
     * Suspend-версия: читает текущее состояние (из репозитория snapshot) и отправляет противоположное.
     * Возвращает true если отправка команды прошла успешно.
     */
    suspend fun toggleRelay(deviceId: String, relayName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val repo = AppContainer.repoFlow.value
                if (repo == null) {
                    Log.w(TAG, "toggleRelay: repo is null, service not running")
                    return@withContext false
                }

                // Снимок текущего состояния устройства (если нет — считаем false)
                val deviceState = repo.getSnapshot()[deviceId]
                val current = deviceState?.relays?.get(relayName) ?: false
                val newValue = if (current) 0 else 1

                Log.i(TAG, "toggleRelay: device=$deviceId relay=$relayName current=$current -> new=$newValue")

                // вызов репозитория отправки команды
                val success = repo.sendRelayCommand(deviceId, relayName, newValue)
                if (!success) {
                    Log.w(TAG, "toggleRelay: sendRelayCommand returned false for $deviceId/$relayName")
                } else {
                    Log.i(TAG, "toggleRelay: command sent ok for $deviceId/$relayName")
                }
                return@withContext success
            } catch (e: Exception) {
                Log.e(TAG, "toggleRelay exception: ${e.message}", e)
                return@withContext false
            }
        }
    }

    /**
     * Explicit set: отправить конкретное значение (0/1).
     */
    fun setRelayAsync(deviceId: String, relayName: String, valueInt: Int) {
        scope.launch {
            setRelay(deviceId, relayName, valueInt)
        }
    }

    suspend fun setRelay(deviceId: String, relayName: String, valueInt: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val repo = AppContainer.repoFlow.value ?: return@withContext false
                return@withContext repo.sendRelayCommand(deviceId, relayName, valueInt)
            } catch (e: Exception) {
                Log.e(TAG, "setRelay exception: ${e.message}", e)
                return@withContext false
            }
        }
    }
}
@Composable
fun DevicesCard(
    deviceName: String,
    deviceData: Double,
    deviceDataUnits: String,


) {

    Column(
        modifier = Modifier
           // .width((windowWidth / 3))
            .fillMaxHeight()
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = deviceName,
            textAlign = TextAlign.Center,
            color = White,
            fontSize = 30.sp,
        )
       // AsyncImage( deviceIcon, Color(deviceColorIcon), 0.8f)
        Text(
            text = "${deviceData} ${deviceDataUnits}",
            textAlign = TextAlign.Center,
            color = White, fontSize = 24.sp
        )
    }

}

@Composable
fun DeviceButton(
    deviceName: String,
    deviceColorIndicator: Int,
    onClick: (String) -> Unit = {}
   // deviceIcon: String,
   // deviceColorIcon: Int,

   // deviceColorAlarm: Int,
   // deviceData: Double,
   // deviceDataUnits: String,
   // deviceDataMin: Double,
  //  deviceDataMax: Double,
  //  deviceState:Boolean
    ) {

    Column(
        modifier = Modifier
          //  .width((windowWidth / 3))
            .fillMaxHeight()
            .wrapContentHeight()
            .padding(10.dp).drawBehind {
                //need invert coordinates for rotate
                drawLine(
                    color = Color(deviceColorIndicator),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 3.dp.toPx(),
                )},
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = deviceName,
            textAlign = TextAlign.Center,
            color = White,
            fontSize = 30.sp,
            modifier = Modifier

        )
       // AsyncImage( deviceIcon, Color(deviceColorIcon), 0.8f)


    }
}


@Composable
fun DeviceWidget(
    deviceName: String,
    deviceIcon: String,
    deviceColorIcon: Int,
    deviceColorIndicator: Int,
    deviceColorAlarm: Int,
    deviceData: Double,
    deviceDataUnits: String,
    deviceDataMin: Double,
    deviceDataMax: Double,
    deviceState:Boolean
    ) {

    val backgroundColor = Brush.horizontalGradient(
        listOf(
            Color(0xFFFFFF), Color(deviceColorIndicator)
        )
    )
    val remaining = (deviceDataMax * deviceData) / 100
    Box(
        modifier = Modifier.height((400 * deviceData / 100).dp).fillMaxWidth()
            .clip(shape = RoundedCornerShape(10.dp, 10.dp, 0.dp, 0.dp))
            .background(backgroundColor)
    )
    Column(
        modifier = Modifier
            .width((windowWidth / 3))
            .fillMaxHeight()
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = deviceName,
            textAlign = TextAlign.Center,
            color = White,
            fontSize = 30.sp,
            modifier = Modifier

        )
     //   AsyncImage( deviceIcon, Color(deviceColorIcon), 0.8f)

        Text(
            text = "${deviceData} %",
            textAlign = TextAlign.Center,
            color = White,
            fontSize = 25.sp,
            modifier = Modifier

        )
        Text(
            text = "$remaining / ${deviceDataMax} ${deviceDataUnits}",
            textAlign = TextAlign.Center,
            color = White,
            fontSize = 25.sp,
            modifier = Modifier

        )
    }
}