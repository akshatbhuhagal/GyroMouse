package com.axat.gyromouse.domain.usecase

import com.axat.gyromouse.data.repository.BluetoothRepository
import javax.inject.Inject

/**
 * Use case for managing BLE HID advertising.
 * In BLE HOGP, the phone advertises itself and the PC connects to it.
 */
class ConnectDeviceUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    /** Start advertising as a BLE HID mouse */
    suspend fun startAdvertising() {
        bluetoothRepository.startAdvertising()
    }

    /** Stop advertising */
    suspend fun stopAdvertising() {
        bluetoothRepository.stopAdvertising()
    }

    /** Disconnect from the current host */
    suspend fun disconnect() {
        bluetoothRepository.disconnect()
    }
}
