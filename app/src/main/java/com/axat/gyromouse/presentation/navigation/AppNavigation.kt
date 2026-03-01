package com.axat.gyromouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.axat.gyromouse.domain.model.DeviceState
import com.axat.gyromouse.presentation.screens.home.HomeScreen
import com.axat.gyromouse.presentation.screens.pairing.PairingScreen
import com.axat.gyromouse.presentation.screens.settings.SettingsScreen
import com.axat.gyromouse.presentation.screens.trackpad.TrackpadScreen
import kotlinx.coroutines.flow.StateFlow

/**
 * Navigation route constants.
 */
object Routes {
    const val HOME = "home"
    const val PAIRING = "pairing"
    const val TRACKPAD = "trackpad"
    const val SETTINGS = "settings"
}

/**
 * Main app navigation graph.
 *
 * Observes [deviceStateFlow] and automatically:
 * - Navigates to trackpad when a device connects
 * - Navigates back to home when connection drops while on trackpad
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    deviceStateFlow: StateFlow<DeviceState>
) {
    val deviceState by deviceStateFlow.collectAsStateWithLifecycle()

    // Auto-navigate based on connection state changes
    LaunchedEffect(deviceState) {
        when (deviceState) {
            is DeviceState.Connected -> {
                // Navigate to trackpad when connected (avoid duplicate navigations)
                if (navController.currentDestination?.route != Routes.TRACKPAD) {
                    navController.navigate(Routes.TRACKPAD) {
                        // Pop the pairing screen off the stack if it's there
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            is DeviceState.Disconnected -> {
                // If we're on the trackpad screen and lost connection, go back to home
                if (navController.currentDestination?.route == Routes.TRACKPAD) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            else -> { /* No auto-navigation for other states */ }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToPairing = {
                    navController.navigate(Routes.PAIRING)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToTrackpad = {
                    navController.navigate(Routes.TRACKPAD) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.PAIRING) {
            PairingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.TRACKPAD) {
            TrackpadScreen(
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onDisconnected = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
