package com.axat.gyromouse.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.axat.gyromouse.domain.model.DeviceState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE HID over GATT (HOGP) Manager.
 *
 * Advertises the Android device as a BLE HID mouse and manages the GATT server
 * that hosts the required services (HID, Device Info, Battery).
 * The PC/host pairs with this device and receives mouse reports via BLE notifications.
 *
 * No software required on the PC side — Windows 10+, macOS, Linux all support HOGP natively.
 */
@Singleton
class BleHidManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    companion object {
        private const val TAG = "BleHidManager"

        // ─── Standard BLE UUIDs ──────────────────────────────────────
        // HID Service
        val HID_SERVICE_UUID: UUID          = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
        val REPORT_MAP_UUID: UUID           = UUID.fromString("00002a4b-0000-1000-8000-00805f9b34fb")
        val HID_INFORMATION_UUID: UUID      = UUID.fromString("00002a4a-0000-1000-8000-00805f9b34fb")
        val HID_CONTROL_POINT_UUID: UUID    = UUID.fromString("00002a4c-0000-1000-8000-00805f9b34fb")
        val PROTOCOL_MODE_UUID: UUID        = UUID.fromString("00002a4e-0000-1000-8000-00805f9b34fb")
        val REPORT_UUID: UUID               = UUID.fromString("00002a4d-0000-1000-8000-00805f9b34fb")

        // Descriptors
        val CLIENT_CONFIG_UUID: UUID        = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val REPORT_REFERENCE_UUID: UUID     = UUID.fromString("00002908-0000-1000-8000-00805f9b34fb")

        // Device Information Service
        val DEVICE_INFO_SERVICE_UUID: UUID  = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        val MANUFACTURER_NAME_UUID: UUID    = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        val PNP_ID_UUID: UUID               = UUID.fromString("00002a50-0000-1000-8000-00805f9b34fb")

        // Battery Service
        val BATTERY_SERVICE_UUID: UUID      = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_UUID: UUID        = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        // Report Reference descriptor values
        const val REPORT_ID: Byte = 1
        const val INPUT_REPORT: Byte = 1
    }

    // ─── State ───────────────────────────────────────────────────────
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isAdvertising = false

    /** The Input Report characteristic used to send mouse data */
    private var inputReportCharacteristic: BluetoothGattCharacteristic? = null

    /** Whether notifications are enabled by the connected host */
    private var notificationsEnabled = false

    /** Connection state exposed to the UI layer */
    private val _deviceState = MutableStateFlow<DeviceState>(DeviceState.Disconnected)
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    /** Whether advertising is active */
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertisingFlow: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    // ─── GATT Server Callback ────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: device=${device.address}, status=$status, newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Host connected: ${device.address}")
                    connectedDevice = device
                    _deviceState.value = DeviceState.Connected(device)
                    // Stop advertising once a host connects
                    stopAdvertising()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Host disconnected: ${device.address}")
                    if (connectedDevice?.address == device.address) {
                        connectedDevice = null
                        notificationsEnabled = false
                        _deviceState.value = DeviceState.Disconnected
                    }
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.d(TAG, "onCharacteristicRead: ${characteristic.uuid}")
            val value = characteristic.value ?: byteArrayOf()
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                if (offset > 0) value.copyOfRange(offset, value.size) else value
            )
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            Log.d(TAG, "onDescriptorRead: ${descriptor.uuid}")
            val value = descriptor.value ?: byteArrayOf()
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray
        ) {
            Log.d(TAG, "onDescriptorWrite: ${descriptor.uuid}, value=${value.contentToString()}")

            // Handle Client Characteristic Configuration (enable/disable notifications)
            if (descriptor.uuid == CLIENT_CONFIG_UUID) {
                if (value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    notificationsEnabled = true
                    Log.i(TAG, "Notifications ENABLED by host")
                } else if (value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    notificationsEnabled = false
                    Log.i(TAG, "Notifications DISABLED by host")
                }
                descriptor.value = value
            }

            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray
        ) {
            Log.d(TAG, "onCharacteristicWrite: ${characteristic.uuid}")
            // HID Control Point or Protocol Mode writes from the host
            characteristic.value = value
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    // ─── Advertise Callback ──────────────────────────────────────────

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "BLE advertising started successfully")
            isAdvertising = true
            _isAdvertising.value = true
            _deviceState.value = DeviceState.Scanning // Reusing Scanning state for "discoverable"
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE advertising failed: errorCode=$errorCode")
            isAdvertising = false
            _isAdvertising.value = false
            val msg = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "Already advertising"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertise data too large"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "BLE advertising not supported"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                else -> "Unknown error ($errorCode)"
            }
            _deviceState.value = DeviceState.Error("Advertising failed: $msg")
        }
    }

    // ─── Public API ──────────────────────────────────────────────────

    /**
     * Initialize the GATT server and start BLE advertising.
     * The device becomes discoverable as "Gyro Mouse" to nearby PCs.
     */
    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _deviceState.value = DeviceState.Error("Bluetooth is not available or disabled")
            return
        }

        // Set up the GATT server first
        if (gattServer == null) {
            setupGattServer()
        }

        // Start BLE advertising
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            _deviceState.value = DeviceState.Error("BLE advertising is not supported on this device")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0) // Advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(HID_SERVICE_UUID))
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(DEVICE_INFO_SERVICE_UUID))
            .build()

        Log.d(TAG, "Starting BLE advertising...")
        advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
    }

    /**
     * Stop BLE advertising.
     */
    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (isAdvertising) {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            _isAdvertising.value = false
            Log.d(TAG, "BLE advertising stopped")
        }
    }

    /**
     * Send a HID mouse report to the connected host.
     *
     * @param report 4-byte mouse report: [buttons, dx, dy, wheel]
     * @return true if the report was successfully queued for notification
     */
    @SuppressLint("MissingPermission")
    fun sendReport(report: ByteArray): Boolean {
        val device = connectedDevice ?: return false
        val server = gattServer ?: return false
        val characteristic = inputReportCharacteristic ?: return false

        if (!notificationsEnabled) {
            Log.w(TAG, "Cannot send report: notifications not enabled by host")
            return false
        }

        return try {
            characteristic.value = report
            server.notifyCharacteristicChanged(device, characteristic, false)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending HID report", e)
            false
        }
    }

    /**
     * Disconnect from the current host and stop all BLE operations.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        connectedDevice?.let { device ->
            gattServer?.cancelConnection(device)
        }
        connectedDevice = null
        notificationsEnabled = false
        _deviceState.value = DeviceState.Disconnected
    }

    /**
     * Tear down the GATT server and stop advertising.
     */
    @SuppressLint("MissingPermission")
    fun close() {
        stopAdvertising()
        disconnect()
        gattServer?.close()
        gattServer = null
        inputReportCharacteristic = null
        Log.d(TAG, "BLE HID Manager closed")
    }

    fun isConnected(): Boolean = connectedDevice != null && _deviceState.value is DeviceState.Connected

    fun getConnectedDevice(): BluetoothDevice? = connectedDevice

    // ─── GATT Server Setup ───────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun setupGattServer() {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (btManager == null) {
            Log.e(TAG, "BluetoothManager not available")
            _deviceState.value = DeviceState.Error("Bluetooth service not available")
            return
        }

        gattServer = btManager.openGattServer(context, gattServerCallback)
        if (gattServer == null) {
            Log.e(TAG, "Failed to open GATT server")
            _deviceState.value = DeviceState.Error("Failed to open GATT server")
            return
        }

        // Add services in order (some hosts expect this)
        addDeviceInfoService()
        addBatteryService()
        addHidService()

        Log.i(TAG, "GATT server initialized with HID, Device Info, and Battery services")
    }

    /**
     * HID Service (0x1812) — the core service that makes us a mouse.
     */
    @SuppressLint("MissingPermission")
    private fun addHidService() {
        val service = BluetoothGattService(HID_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // 1. HID Information (read only)
        //    bcdHID=1.1, bCountryCode=0, flags=0x02 (normally connectable)
        val hidInfo = BluetoothGattCharacteristic(
            HID_INFORMATION_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
        )
        hidInfo.value = byteArrayOf(0x11, 0x01, 0x00, 0x02) // HID 1.1, no country, normally connectable
        service.addCharacteristic(hidInfo)

        // 2. Report Map (read only) — contains the HID descriptor
        val reportMap = BluetoothGattCharacteristic(
            REPORT_MAP_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
        )
        reportMap.value = HidDescriptors.MOUSE_DESCRIPTOR
        service.addCharacteristic(reportMap)

        // 3. Protocol Mode (read + write without response)
        val protocolMode = BluetoothGattCharacteristic(
            PROTOCOL_MODE_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
        )
        protocolMode.value = byteArrayOf(0x01) // Report Protocol Mode
        service.addCharacteristic(protocolMode)

        // 4. HID Control Point (write without response)
        val controlPoint = BluetoothGattCharacteristic(
            HID_CONTROL_POINT_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
        )
        service.addCharacteristic(controlPoint)

        // 5. Input Report (notify + read) — this is where mouse data goes
        val inputReport = BluetoothGattCharacteristic(
            REPORT_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
        )
        // Client Characteristic Configuration Descriptor (for enabling notifications)
        val cccd = BluetoothGattDescriptor(
            CLIENT_CONFIG_UUID,
            BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
        )
        cccd.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        inputReport.addDescriptor(cccd)

        // Report Reference Descriptor — tells the host this is Input Report ID 1
        val reportRef = BluetoothGattDescriptor(
            REPORT_REFERENCE_UUID,
            BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED
        )
        reportRef.value = byteArrayOf(REPORT_ID, INPUT_REPORT) // Report ID=1, Type=Input
        inputReport.addDescriptor(reportRef)

        inputReport.value = byteArrayOf(0, 0, 0, 0) // Initial empty report
        service.addCharacteristic(inputReport)

        // Save reference for sending reports later
        inputReportCharacteristic = inputReport

        gattServer?.addService(service)
        Log.d(TAG, "HID Service added")
    }

    /**
     * Device Information Service (0x180A) — required by HOGP.
     */
    @SuppressLint("MissingPermission")
    private fun addDeviceInfoService() {
        val service = BluetoothGattService(DEVICE_INFO_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        // Manufacturer Name
        val manufacturerName = BluetoothGattCharacteristic(
            MANUFACTURER_NAME_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        manufacturerName.value = "GyroMouse".toByteArray()
        service.addCharacteristic(manufacturerName)

        // PnP ID
        val pnpId = BluetoothGattCharacteristic(
            PNP_ID_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        // Vendor ID Source=0x02 (USB), Vendor ID=0x0000, Product ID=0x0001, Version=0x0100
        pnpId.value = byteArrayOf(0x02, 0x00, 0x00, 0x01, 0x00, 0x00, 0x01)
        service.addCharacteristic(pnpId)

        gattServer?.addService(service)
        Log.d(TAG, "Device Information Service added")
    }

    /**
     * Battery Service (0x180F) — required by HOGP.
     */
    @SuppressLint("MissingPermission")
    private fun addBatteryService() {
        val service = BluetoothGattService(BATTERY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val batteryLevel = BluetoothGattCharacteristic(
            BATTERY_LEVEL_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        batteryLevel.value = byteArrayOf(100) // 100% battery

        val cccd = BluetoothGattDescriptor(
            CLIENT_CONFIG_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        cccd.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        batteryLevel.addDescriptor(cccd)

        service.addCharacteristic(batteryLevel)

        gattServer?.addService(service)
        Log.d(TAG, "Battery Service added")
    }
}
