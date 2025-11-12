package com.example.v900.navigation


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar

import androidx.compose.material3.NavigationBarItem

import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.v900.R
import com.example.v900.ui.HomeScreen
import com.example.v900.ui.screens.BilgeScreen

import com.example.v900.ui.screens.LightsScreen
import com.example.v900.ui.screens.RunScreen
import com.example.v900.ui.screens.ServicesScreen
import com.example.v900.ui.screens.SettingsScreen



@Composable
fun BottomNavBar(navController: NavHostController, items: List<BottomNavItem>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
              enabled = !selected,
                selected = selected,

                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id)  { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(painterResource(item.icon), contentDescription = item.title) },
                // label = { Text(item.title) }
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LocalContentColor.current.copy(alpha = 1f),
                    unselectedIconColor = LocalContentColor.current.copy(alpha = 0.3f),
                    disabledIconColor=LocalContentColor.current.copy(alpha = 1f),
                    indicatorColor = LocalContentColor.current.copy(alpha = 0f),// без подложки

                ),
                        // 👇 отключаем анимацию ripple (если не нужно даже свечение при клике)
                        interactionSource = remember { MutableInteractionSource() },
                alwaysShowLabel=false,

            )
        }
    }
}



data class BottomNavItem(
    val route: String,
    val icon: Int,
    val title: String
)

fun getBottomNavItems(): List<BottomNavItem> = listOf(
    BottomNavItem(NavRoutes.RUN,  icon = R.drawable.menu_run, "Run"),
    BottomNavItem(NavRoutes.BILGE,  icon = R.drawable.menu_pump, "Bilge"),
    BottomNavItem(NavRoutes.HOME,  icon = R.drawable.menu_home, "Home"),
    BottomNavItem(NavRoutes.SERVICES,  icon = R.drawable.menu_utilites, "Services"),
    BottomNavItem(NavRoutes.LIGHTS,  icon = R.drawable.menu_light, "Lights")
)

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.HOME) {
        composable(NavRoutes.RUN) { RunScreen() }
        composable(NavRoutes.BILGE) { BilgeScreen() }
        composable(NavRoutes.HOME) { HomeScreen() }
        composable(NavRoutes.SERVICES) { ServicesScreen() }
        composable(NavRoutes.LIGHTS) { LightsScreen() }
        composable(NavRoutes.SETTINGS) { SettingsScreen() }
    }
}
