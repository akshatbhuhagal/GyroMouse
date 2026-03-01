package com.axat.gyromouse.domain.model

import android.bluetooth.BluetoothDevice

/**
 * Sealed class representing the Bluetooth HID connection state.
 * Exposed via StateFlow to drive UI navigation and state display.
 */
sealed class DeviceState {
    /** No active connection and not scanning */
    data object Disconnected : DeviceState()

    /** Actively scanning for Bluetooth devices */
    data object Scanning : DeviceState()

    /** Pairing with a specific device */
    data class Pairing(val device: BluetoothDevice) : DeviceState()

    /** Attempting to establish HID connection */
    data class Connecting(val device: BluetoothDevice) : DeviceState()

    /** HID connection is active and reports can be sent */
    data class Connected(val device: BluetoothDevice) : DeviceState()

    /** An error occurred */
    data class Error(val message: String) : DeviceState()
}

/**
 * Simplified representation of a discovered Bluetooth device for the UI layer.
 */
data class ScannedDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean,
    val rssi: Int = 0,
    val bluetoothDevice: BluetoothDevice
)
