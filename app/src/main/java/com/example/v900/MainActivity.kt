package com.example.v900

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.example.v900.navigation.*
import com.example.v900.service.ForegroundCommService
import com.example.v900.ui.theme.Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startCommService()  // запускаем сервис
        setContent {  Theme {
            AppMain()
        }
        }
        hideSystemUI()
    }
    private fun startCommService() {
        val serviceIntent = Intent(this, ForegroundCommService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent) // Android 8+
        } else {
            startService(serviceIntent)
        }
    }
    private fun hideSystemUI() {

        actionBar?.hide()
        //Hide the status bars
        WindowCompat.setDecorFitsSystemWindows(
            window,
            true
        ) ///If False ignor bars if true offset to bar
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

        } else {
            window.insetsController?.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMain() {
    val navController = rememberNavController()
    val items = getBottomNavItems()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boat Control") },
                actions = {
                    IconButton(onClick = {
                        if (currentRoute == NavRoutes.SETTINGS) {
                            // если уже на экране настроек — закрываем его
                            navController.popBackStack()
                        } else {
                            // иначе открываем
                            navController.navigate(NavRoutes.SETTINGS) {
                                launchSingleTop = true
                            }
                        }
                    }) {
                        Icon(painterResource(R.drawable.menu_settings), contentDescription = "Settings")
                    }


                }
            )
        },
        bottomBar = {
            BottomNavBar(navController = navController, items = items)
        }
    ) { innerPadding ->
        // Навигационный граф — размещаем внутри content области Scaffold
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(navController = navController)
        }
    }
}


