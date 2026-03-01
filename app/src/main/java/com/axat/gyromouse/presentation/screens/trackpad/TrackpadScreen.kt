package com.axat.gyromouse.presentation.screens.trackpad

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.axat.gyromouse.R
import com.axat.gyromouse.domain.model.DeviceState
import com.axat.gyromouse.presentation.theme.LeftClickButton
import com.axat.gyromouse.presentation.theme.RightClickButton
import com.axat.gyromouse.presentation.theme.StatusConnected
import kotlinx.coroutines.launch

/**
 * Full-screen trackpad screen with:
 * - Top bar showing connected device & controls
 * - Main trackpad area (~70% of screen)
 * - Left/Right click buttons below the trackpad
 * - Floating keyboard button
 * - Gesture hint overlay
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackpadScreen(
    onNavigateToSettings: () -> Unit,
    onDisconnected: () -> Unit,
    viewModel: TrackpadViewModel = hiltViewModel()
) {
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDragging by viewModel.isDragging.collectAsStateWithLifecycle()
    val showGestureHint by viewModel.showGestureHint.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Get connected device name
    val deviceName = when (val state = deviceState) {
        is DeviceState.Connected -> {
            try { state.device.name ?: state.device.address } catch (_: SecurityException) { "Device" }
        }
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Connection status indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (deviceState is DeviceState.Connected) StatusConnected
                                    else Color.Red
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (deviceName.isNotEmpty()) {
                                stringResource(R.string.trackpad_connected_to, deviceName)
                            } else {
                                stringResource(R.string.trackpad_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Settings button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                    // Disconnect button
                    IconButton(onClick = {
                        viewModel.disconnect()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.disconnect),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Floating keyboard button
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Keyboard input is a planned future feature"
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = stringResource(R.string.trackpad_keyboard),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Trackpad area — 70% of available height
                TrackpadCanvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f),
                    onMove = { dx, dy -> viewModel.onMove(dx, dy) },
                    onTap = { viewModel.onTap() },
                    onDoubleTap = { viewModel.onDoubleTap() },
                    onTwoFingerTap = { viewModel.onTwoFingerTap() },
                    onThreeFingerTap = { viewModel.onThreeFingerTap() },
                    onScroll = { dy -> viewModel.onScroll(dy) },
                    onDragStart = { viewModel.onDragStart() },
                    onDragEnd = { viewModel.onDragEnd() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Left and Right click buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.12f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left click button
                    Button(
                        onClick = { viewModel.onLeftClick() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LeftClickButton
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.trackpad_left_click),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Right click button
                    Button(
                        onClick = { viewModel.onRightClick() },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RightClickButton
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.trackpad_right_click),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Drag status indicator
                AnimatedVisibility(
                    visible = isDragging,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Dragging...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Remaining space
                Spacer(modifier = Modifier.weight(0.05f))
            }

            // Gesture hint overlay
            AnimatedVisibility(
                visible = showGestureHint,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GestureHintOverlay(
                    onDismiss = { viewModel.dismissGestureHint() }
                )
            }
        }
    }
}

/** Gesture hint overlay shown on first use */
@Composable
private fun GestureHintOverlay(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.trackpad_gesture_hint_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                val gestures = listOf(
                    R.string.trackpad_gesture_move,
                    R.string.trackpad_gesture_left_click,
                    R.string.trackpad_gesture_right_click,
                    R.string.trackpad_gesture_double_click,
                    R.string.trackpad_gesture_scroll,
                    R.string.trackpad_gesture_drag,
                    R.string.trackpad_gesture_middle_click
                )

                gestures.forEach { gestureRes ->
                    Text(
                        text = "• ${stringResource(gestureRes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                FilledTonalButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.trackpad_gesture_dismiss))
                }
            }
        }
    }
}
