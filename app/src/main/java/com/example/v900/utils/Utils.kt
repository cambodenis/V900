package com.example.sbi.utils

import android.os.Build
import android.text.format.DateUtils

import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext

import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Utils {

    fun isAndroidQOrAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun permissionList(): List<String> {
        val readPermission = if (Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            listOf(readPermission)
        } else {
            listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
}

@Composable
fun ClockText() {
    val currentTimeMillis = remember {
        mutableStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(key1 = currentTimeMillis) {
        while (true) {
            delay(250)
            currentTimeMillis.value = System.currentTimeMillis()
        }
    }
    Text(
        modifier = Modifier.wrapContentHeight(Alignment.CenterVertically, true),
        style = TopBarFontStyle,
        text = DateUtils.formatDateTime(
            LocalContext.current,
            currentTimeMillis.value,
            DateUtils.FORMAT_SHOW_TIME
        ),

        )
}




