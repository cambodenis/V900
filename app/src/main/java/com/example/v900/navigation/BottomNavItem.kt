package com.example.v900.navigation


import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.sbi.utils.MenuItemWidth
import com.example.v900.R
import com.example.v900.ui.HomeScreen
import com.example.v900.ui.screens.BilgeScreen
import com.example.v900.ui.screens.LightsScreen
import com.example.v900.ui.screens.RunScreen
import com.example.v900.ui.screens.ServicesScreen
import com.example.v900.ui.screens.SettingsScreen
import com.example.v900.ui.theme.BackgroundColor
import com.example.v900.ui.theme.BorderColorOneWay
import com.example.v900.ui.theme.BorderColorTwoWay
import com.example.v900.ui.theme.BorderSize
import com.example.v900.ui.theme.Green
import com.example.v900.ui.theme.White


@Composable
fun BottomNavBar(navController: NavHostController, items: List<BottomNavItem>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    Row(
        modifier = Modifier.padding(top = 10.dp)
            .background(BackgroundColor)
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(colors = BorderColorTwoWay),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = BorderSize.toPx()
                )
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavigationBar() {
            items.forEachIndexed { index, item ->
                val selected = currentRoute == item.route
                val last = items.lastIndex == index

                Column(
                    modifier = Modifier .weight(1f)
                        .let {
                            if (!last) {
                                return@let it
                                    .drawBehind {
                                        rotate(degrees = 180F) {
                                            drawLine(
                                                brush = Brush.verticalGradient(colors = BorderColorOneWay),
                                                start = Offset(0f, 0f),
                                                end = Offset(0f, size.height),
                                                strokeWidth = BorderSize.toPx(),
                                            )
                                        }
                                    }

                            }
                            it
                        }
                        .padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                    Row(
                        modifier = Modifier
                            .let {
                                if (selected) {
                                    return@let it
                                        .drawBehind {
                                            drawLine(
                                                color = White,
                                                start = Offset(0f, size.height),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 1.5.dp.toPx(),
                                            )
                                        }

                                }
                                it
                            }
                        ,
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            icon = { Icon(painterResource(item.icon),
                                contentDescription = item.title
                               // tint = Green
                            ) },
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
