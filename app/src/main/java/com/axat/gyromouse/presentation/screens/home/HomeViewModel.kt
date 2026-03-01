package com.axat.gyromouse.presentation.screens.home

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axat.gyromouse.data.repository.BluetoothRepository
import com.axat.gyromouse.domain.model.DeviceState
import com.axat.gyromouse.domain.model.ScannedDevice
import com.axat.gyromouse.domain.usecase.ConnectDeviceUseCase
import com.axat.gyromouse.domain.usecase.ScanDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val isAdvertising: Boolean = false,
    val pairedDevices: List<ScannedDevice> = emptyList(),
    val deviceState: DeviceState = DeviceState.Disconnected,
    val isBluetoothAvailable: Boolean = true,
    val isBluetoothEnabled: Boolean = true,
    /** Set to true when user taps Advertise but Bluetooth is turned off */
    val showBluetoothOffPrompt: Boolean = false
)

/**
 * ViewModel for the Home screen.
 * Manages BLE HID advertising and device state.
 *
 * With BLE HOGP, the flow is:
 * 1. User taps "Start Advertising"
 * 2. Phone becomes discoverable as a BLE HID mouse
 * 3. PC pairs with the phone from its Bluetooth settings
 * 4. Connection is auto-detected → navigate to Trackpad
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val scanDevicesUseCase: ScanDevicesUseCase,
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Update Bluetooth availability
        _uiState.update {
            it.copy(
                isBluetoothAvailable = bluetoothRepository.isBluetoothAvailable(),
                isBluetoothEnabled = bluetoothRepository.isBluetoothEnabled()
            )
        }

        // Observe device state for UI updates
        viewModelScope.launch {
            bluetoothRepository.deviceState.collect { state ->
                _uiState.update { it.copy(deviceState = state) }
            }
        }

        // Observe advertising state
        viewModelScope.launch {
            bluetoothRepository.isAdvertising.collect { advertising ->
                _uiState.update { it.copy(isAdvertising = advertising) }
            }
        }

        // Load paired devices
        loadPairedDevices()
    }

    /** Load already-paired Bluetooth devices */
    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        val paired = scanDevicesUseCase.getPairedDevices()
        _uiState.update { it.copy(pairedDevices = paired) }
    }

    /**
     * Start BLE advertising — makes the phone discoverable as a HID mouse.
     * The PC connects to us from its Bluetooth settings.
     */
    fun startAdvertising() {
        // Check if Bluetooth is enabled
        if (!bluetoothRepository.isBluetoothEnabled()) {
            _uiState.update {
                it.copy(
                    isBluetoothEnabled = false,
                    showBluetoothOffPrompt = true
                )
            }
            return
        }

        viewModelScope.launch {
            connectDeviceUseCase.startAdvertising()
        }
    }

    /** Stop BLE advertising */
    fun stopAdvertising() {
        viewModelScope.launch {
            connectDeviceUseCase.stopAdvertising()
        }
    }

    /** Disconnect from the current host */
    fun disconnectDevice() {
        viewModelScope.launch {
            connectDeviceUseCase.disconnect()
        }
    }

    /** Refresh Bluetooth status */
    fun refreshBluetoothStatus() {
        _uiState.update {
            it.copy(
                isBluetoothAvailable = bluetoothRepository.isBluetoothAvailable(),
                isBluetoothEnabled = bluetoothRepository.isBluetoothEnabled(),
                showBluetoothOffPrompt = false
            )
        }
        loadPairedDevices()
    }

    /** Dismiss the Bluetooth-off prompt */
    fun dismissBluetoothOffPrompt() {
        _uiState.update { it.copy(showBluetoothOffPrompt = false) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            connectDeviceUseCase.stopAdvertising()
        }
    }
}
