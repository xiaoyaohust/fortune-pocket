package com.fortunepocket.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Icon
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fortunepocket.core.ui.theme.AppColors
import com.fortunepocket.feature.astrology.AstrologyScreen
import com.fortunepocket.feature.bazi.BaziScreen
import com.fortunepocket.feature.history.HistoryScreen
import com.fortunepocket.feature.home.HomeScreen
import com.fortunepocket.feature.settings.SettingsScreen
import com.fortunepocket.feature.tarot.TarotScreen

// Sub-routes used inside features (not top-level tabs)
object AppRoutes {
    const val TAROT = "tarot"
    const val ASTROLOGY = "astrology"
    const val BAZI = "bazi"
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val topLevelDestinations = TopLevelDestination.entries

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Hide bottom bar on sub-screens (tarot result, etc.)
            val isTopLevelRoute = topLevelDestinations.any { it.route == currentDestination?.route }
            val showBottomBar = isTopLevelRoute ||
                currentDestination?.route in listOf(AppRoutes.TAROT, AppRoutes.ASTROLOGY, AppRoutes.BAZI)

            NavigationBar(
                containerColor = AppColors.backgroundBase,
                contentColor = AppColors.textPrimary
            ) {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.titleResId)
                            )
                        },
                        label = { Text(stringResource(destination.titleResId)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.accentGold,
                            selectedTextColor = AppColors.accentGold,
                            unselectedIconColor = AppColors.textSecondary,
                            unselectedTextColor = AppColors.textSecondary,
                            indicatorColor = AppColors.backgroundElevated
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onNavigateToTarot = { navController.navigate(AppRoutes.TAROT) },
                    onNavigateToAstrology = { navController.navigate(AppRoutes.ASTROLOGY) },
                    onNavigateToBazi = { navController.navigate(AppRoutes.BAZI) }
                )
            }
            composable(AppRoutes.TAROT) {
                TarotScreen(onBack = { navController.popBackStack() })
            }
            composable(AppRoutes.ASTROLOGY) {
                AstrologyScreen(onBack = { navController.popBackStack() })
            }
            composable(AppRoutes.BAZI) {
                BaziScreen(onBack = { navController.popBackStack() })
            }
            composable(TopLevelDestination.HISTORY.route) {
                HistoryScreen()
            }
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen()
            }
        }
    }
}
