package com.axat.gyromouse.domain.usecase

import com.axat.gyromouse.data.repository.BluetoothRepository
import com.axat.gyromouse.domain.model.ScannedDevice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing device scanning.
 * With BLE HOGP, scanning is less relevant (PC connects to us),
 * but we still provide paired device listing for the UI.
 */
class ScanDevicesUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    /** Get already-paired Bluetooth devices */
    fun getPairedDevices(): List<ScannedDevice> {
        return bluetoothRepository.getPairedDevices()
    }
}
