package com.example.monethra.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.monethra.ui.components.TopLevelDestination

/**
 * NavGraph wrapper delegating to [AppNavHost] in AppNavigation.kt
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Routes.SPLASH,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    onLogout: () -> Unit = {},
) {
    AppNavHost(
        navController = navController,
        startDestination = startDestination,
        onNavigateToDestination = onNavigateToDestination,
        onLogout = onLogout,
    )
}
