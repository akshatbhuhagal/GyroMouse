package com.axat.gyromouse.presentation.screens.trackpad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axat.gyromouse.data.repository.BluetoothRepository
import com.axat.gyromouse.data.repository.SettingsRepository
import com.axat.gyromouse.domain.model.AppSettings
import com.axat.gyromouse.domain.model.DeviceState
import com.axat.gyromouse.domain.model.MouseAction
import com.axat.gyromouse.domain.model.MouseButton
import com.axat.gyromouse.domain.usecase.ConnectDeviceUseCase
import com.axat.gyromouse.domain.usecase.SendMouseReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt
import javax.inject.Inject

/**
 * ViewModel for the Trackpad screen.
 * Handles gesture-to-HID-report translation with sensitivity and acceleration applied.
 */
@HiltViewModel
class TrackpadViewModel @Inject constructor(
    private val sendMouseReportUseCase: SendMouseReportUseCase,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val bluetoothRepository: BluetoothRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    /** Current device connection state */
    val deviceState: StateFlow<DeviceState> = bluetoothRepository.deviceState

    /** Current app settings (sensitivity, scroll, etc.) */
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Whether a drag is currently in progress */
    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

    /** Whether to show gesture hint overlay */
    private val _showGestureHint = MutableStateFlow(false)
    val showGestureHint: StateFlow<Boolean> = _showGestureHint.asStateFlow()

    /** Timestamp of the last sent report — for 60fps throttling */
    @Volatile
    private var lastReportTime = 0L

    companion object {
        /** Minimum interval between reports: ~16ms = 60 reports/sec */
        private const val REPORT_INTERVAL_MS = 16L
        /** Acceleration exponent for fast movements */
        private const val ACCELERATION_EXPONENT = 1.3
    }

    init {
        // Show gesture hints on first launch (if setting is enabled)
        viewModelScope.launch {
            settings.collect { s ->
                _showGestureHint.value = s.showGestureHints
            }
        }
    }

    /**
     * Handle a pointer movement event from the trackpad.
     * Applies sensitivity multiplier and optional acceleration,
     * then sends a move (or drag-move) report.
     */
    fun onMove(rawDx: Float, rawDy: Float) {
        // Throttle to max 60 reports/second
        val now = System.currentTimeMillis()
        if (now - lastReportTime < REPORT_INTERVAL_MS) return
        lastReportTime = now

        val s = settings.value

        // Apply sensitivity
        var dx = rawDx * s.sensitivity
        var dy = rawDy * s.sensitivity

        // Apply pointer acceleration if enabled
        if (s.pointerAcceleration) {
            val magnitude = sqrt(dx * dx + dy * dy)
            if (magnitude > 1f) {
                val factor = magnitude.toDouble().pow(ACCELERATION_EXPONENT - 1.0).toFloat()
                dx *= factor
                dy *= factor
            }
        }

        val action = if (_isDragging.value) {
            MouseAction.DragMove(dx.toInt(), dy.toInt())
        } else {
            MouseAction.Move(dx.toInt(), dy.toInt())
        }

        viewModelScope.launch {
            sendMouseReportUseCase(action)
        }
    }

    /** Handle a single tap — left click */
    fun onTap() {
        if (!settings.value.tapToClick) return
        viewModelScope.launch {
            sendMouseReportUseCase(MouseAction.Click(MouseButton.LEFT))
        }
    }

    /** Handle a double tap — double click */
    fun onDoubleTap() {
        if (!settings.value.tapToClick) return
        viewModelScope.launch {
            sendMouseReportUseCase(MouseAction.DoubleClick)
        }
    }

    /** Handle a two-finger tap — right click */
    fun onTwoFingerTap() {
        viewModelScope.launch {
            sendMouseReportUseCase(MouseAction.Click(MouseButton.RIGHT))
        }
    }

    /** Handle a three-finger tap — middle click */
    fun onThreeFingerTap() {
        viewModelScope.launch {
            sendMouseReportUseCase(MouseAction.Click(MouseButton.MIDDLE))
        }
    }

    /** Handle two-finger scroll */
    fun onScroll(rawDy: Float) {
        val s = settings.value
        var scroll = rawDy * s.scrollSpeed
        if (s.naturalScroll) {
            scroll = -scroll
        }

        viewModelScope.launch {
            sendMouseReportUseCase(MouseAction.Scroll(scroll.toInt()))
        }
    }

    /** Start a long-press drag */
    fun onDragStart() {
        _isDragging.value = true
        viewModelScope.launch {
            sendMouseReportUseCase(MouseAction.DragStart)
        }
    }

    /** End a drag operation */
    fun onDragEnd() {
        _isDragging.value = false
        viewModelScope.launch {
            sendMouseReportUseCase(MouseAction.DragEnd)
        }
    }

    /** Explicit left click button press */
    fun onLeftClick() {
        viewModelScope.launch {
            sendMouseReportUseCase(MouseAction.Click(MouseButton.LEFT))
        }
    }

    /** Explicit right click button press */
    fun onRightClick() {
        viewModelScope.launch {
            sendMouseReportUseCase(MouseAction.Click(MouseButton.RIGHT))
        }
    }

    /** Disconnect from the current device */
    fun disconnect() {
        viewModelScope.launch {
            connectDeviceUseCase.disconnect()
        }
    }

    /** Dismiss the gesture hint overlay */
    fun dismissGestureHint() {
        _showGestureHint.value = false
    }
}
