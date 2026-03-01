package com.axat.gyromouse.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.axat.gyromouse.data.bluetooth.BleHidManager
import com.axat.gyromouse.data.bluetooth.HidReportBuilder
import com.axat.gyromouse.domain.model.DeviceState
import com.axat.gyromouse.domain.model.MouseAction
import com.axat.gyromouse.domain.model.MouseButton
import com.axat.gyromouse.domain.model.ScannedDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that orchestrates all BLE HID operations.
 * Bridges the domain layer with the BleHidManager (data layer).
 *
 * All Bluetooth operations are dispatched on [Dispatchers.IO].
 */
@Singleton
class BluetoothRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val bleHidManager: BleHidManager
) {

    companion object {
        private const val TAG = "BluetoothRepository"
        private const val CLICK_DELAY_MS = 50L
    }

    /** Expose the BLE HID manager's connection state */
    val deviceState: StateFlow<DeviceState> = bleHidManager.deviceState

    /** Expose advertising state */
    val isAdvertising: StateFlow<Boolean> = bleHidManager.isAdvertisingFlow

    /**
     * Start BLE advertising — makes the device discoverable as a HID mouse.
     * The PC/host connects to us from its Bluetooth settings.
     */
    suspend fun startAdvertising() = withContext(Dispatchers.IO) {
        bleHidManager.startAdvertising()
    }

    /**
     * Stop BLE advertising.
     */
    suspend fun stopAdvertising() = withContext(Dispatchers.IO) {
        bleHidManager.stopAdvertising()
    }

    /**
     * Disconnect from the current host.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        bleHidManager.disconnect()
    }

    /**
     * Tear down all BLE resources.
     */
    suspend fun close() = withContext(Dispatchers.IO) {
        bleHidManager.close()
    }

    /**
     * Get the list of already-paired Bluetooth devices.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<ScannedDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices
                .map { device ->
                    ScannedDevice(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        isPaired = true,
                        bluetoothDevice = device
                    )
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting paired devices", e)
            emptyList()
        }
    }

    /**
     * Process a mouse action and send the corresponding HID report(s).
     *
     * @param action The mouse action to execute
     * @return true if the report was sent successfully
     */
    suspend fun sendMouseAction(action: MouseAction): Boolean = withContext(Dispatchers.IO) {
        when (action) {
            is MouseAction.Move -> {
                bleHidManager.sendReport(HidReportBuilder.buildMoveReport(action.dx, action.dy))
            }
            is MouseAction.Click -> {
                // Press the button, wait briefly, then release
                bleHidManager.sendReport(HidReportBuilder.buildClickReport(action.button))
                delay(CLICK_DELAY_MS)
                bleHidManager.sendReport(HidReportBuilder.buildReleaseReport())
            }
            is MouseAction.DoubleClick -> {
                // Two click-release cycles with a short delay between them
                bleHidManager.sendReport(HidReportBuilder.buildClickReport(MouseButton.LEFT))
                delay(CLICK_DELAY_MS)
                bleHidManager.sendReport(HidReportBuilder.buildReleaseReport())
                delay(CLICK_DELAY_MS)
                bleHidManager.sendReport(HidReportBuilder.buildClickReport(MouseButton.LEFT))
                delay(CLICK_DELAY_MS)
                bleHidManager.sendReport(HidReportBuilder.buildReleaseReport())
            }
            is MouseAction.Scroll -> {
                bleHidManager.sendReport(HidReportBuilder.buildScrollReport(action.amount))
            }
            is MouseAction.DragStart -> {
                bleHidManager.sendReport(HidReportBuilder.buildClickReport(MouseButton.LEFT))
            }
            is MouseAction.DragMove -> {
                bleHidManager.sendReport(
                    HidReportBuilder.buildMoveReport(
                        action.dx,
                        action.dy,
                        buttonsPressed = setOf(MouseButton.LEFT)
                    )
                )
            }
            is MouseAction.DragEnd -> {
                bleHidManager.sendReport(HidReportBuilder.buildReleaseReport())
            }
        }
    }

    /** Check if Bluetooth hardware is available */
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    /** Check if Bluetooth is currently enabled */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /** Check if currently connected */
    fun isConnected(): Boolean = bleHidManager.isConnected()
}
