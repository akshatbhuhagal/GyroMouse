package com.axat.gyromouse.presentation.screens.trackpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.axat.gyromouse.R
import com.axat.gyromouse.presentation.theme.TrackpadBorder
import com.axat.gyromouse.presentation.theme.TrackpadRipple
import com.axat.gyromouse.presentation.theme.TrackpadSurface

/**
 * Custom trackpad canvas that detects gestures and forwards them to the ViewModel.
 *
 * Gesture detection:
 * - Single finger drag → cursor movement
 * - Single tap → left click
 * - Double tap → double click
 * - Two finger tap → right click
 * - Two finger vertical drag → scroll
 * - Long press → drag start, release → drag end
 * - Three finger tap → middle click
 *
 * Throttled to 60 reports/second via the ViewModel.
 */
@Composable
fun TrackpadCanvas(
    modifier: Modifier = Modifier,
    onMove: (dx: Float, dy: Float) -> Unit,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onTwoFingerTap: () -> Unit,
    onThreeFingerTap: () -> Unit,
    onScroll: (dy: Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    // Track tap positions for ripple effect
    var rippleCenter by remember { mutableStateOf<Offset?>(null) }
    var rippleAlpha by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TrackpadSurface)
            .border(1.dp, TrackpadBorder, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    var lastPosition = firstDown.position
                    var moved = false
                    var isLongPress = false
                    var pointerCount = 1
                    var lastTapTime = 0L
                    val tapThreshold = 15f // pixels — movement beyond this is a drag
                    val longPressThreshold = 400L // ms

                    // Show ripple
                    rippleCenter = firstDown.position
                    rippleAlpha = 0.4f

                    // Track all pointer events until release
                    while (true) {
                        val event = awaitPointerEvent()
                        val currentPointerCount = event.changes.size
                        if (currentPointerCount > pointerCount) {
                            pointerCount = currentPointerCount
                        }

                        when (event.type) {
                            PointerEventType.Move -> {
                                val current = event.changes.first().position
                                val dx = current.x - lastPosition.x
                                val dy = current.y - lastPosition.y
                                val totalDx = current.x - firstDown.position.x
                                val totalDy = current.y - firstDown.position.y
                                val totalDistance = kotlin.math.sqrt(
                                    totalDx * totalDx + totalDy * totalDy
                                )

                                if (totalDistance > tapThreshold) {
                                    moved = true
                                }

                                // Check for long press during movement
                                val elapsed = System.currentTimeMillis() - downTime
                                if (!isLongPress && elapsed > longPressThreshold && !moved) {
                                    isLongPress = true
                                    onDragStart()
                                }

                                if (moved) {
                                    if (currentPointerCount >= 2) {
                                        // Two or more fingers moving → scroll
                                        onScroll(-dy)
                                    } else {
                                        // Single finger → move cursor
                                        onMove(dx, dy)
                                    }
                                }

                                lastPosition = current

                                // Consume all changes
                                event.changes.forEach { it.consume() }
                            }

                            PointerEventType.Release -> {
                                val upTime = System.currentTimeMillis()
                                val duration = upTime - downTime

                                // Only register taps if no significant movement
                                if (!moved) {
                                    if (isLongPress) {
                                        // End of long-press drag
                                        onDragEnd()
                                    } else {
                                        when (pointerCount) {
                                            1 -> {
                                                // Check for double tap
                                                if (upTime - lastTapTime < 300L) {
                                                    onDoubleTap()
                                                } else {
                                                    onTap()
                                                }
                                                lastTapTime = upTime
                                            }
                                            2 -> onTwoFingerTap()
                                            3 -> onThreeFingerTap()
                                        }
                                    }
                                } else if (isLongPress) {
                                    onDragEnd()
                                }

                                // Hide ripple
                                rippleAlpha = 0f

                                // Check if all pointers are up
                                val allUp = event.changes.all { !it.pressed }
                                if (allUp) break
                            }
                        }
                    }
                }
            }
    ) {
        // Draw subtle grid pattern and ripple effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw subtle dot grid
            val dotSpacing = 40.dp.toPx()
            val dotRadius = 1.dp.toPx()
            val dotColor = Color.White.copy(alpha = 0.06f)

            var x = dotSpacing
            while (x < size.width) {
                var y = dotSpacing
                while (y < size.height) {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += dotSpacing
                }
                x += dotSpacing
            }

            // Draw tap ripple
            rippleCenter?.let { center ->
                if (rippleAlpha > 0f) {
                    drawCircle(
                        color = TrackpadRipple.copy(alpha = rippleAlpha),
                        radius = 60.dp.toPx(),
                        center = center
                    )
                }
            }
        }

        // Scroll zone hint on the right edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(4.dp)
                .fillMaxSize()
                .padding(vertical = 48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.1f))
        )

        // Center hint text
        Text(
            text = stringResource(R.string.trackpad_gesture_move),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
