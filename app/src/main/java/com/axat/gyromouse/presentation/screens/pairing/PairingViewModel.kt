package com.axat.gyromouse.presentation.screens.pairing

import androidx.lifecycle.ViewModel
import com.axat.gyromouse.data.repository.BluetoothRepository
import com.axat.gyromouse.domain.model.DeviceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel for the Pairing screen.
 * Exposes the current device state and provides a cancel action.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {

    val deviceState: StateFlow<DeviceState> = bluetoothRepository.deviceState

    /** Cancel the ongoing pairing/connection attempt */
    suspend fun cancelPairing() {
        bluetoothRepository.disconnect()
    }
}
