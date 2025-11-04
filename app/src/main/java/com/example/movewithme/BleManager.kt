package com.example.movewithme

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

private const val TAG = "BleManager"

sealed class BleStatus {
    data object Disconnected : BleStatus()
    data object Scanning : BleStatus()
    data class Connecting(val device: BluetoothDevice) : BleStatus()
    data class Connected(val device: BluetoothDevice) : BleStatus()
    data class Error(val message: String) : BleStatus()
}

class BleManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<BleStatus>(BleStatus.Disconnected)
    val connectionState: StateFlow<BleStatus> = _connectionState.asStateFlow()

    private val _notifications = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val notifications: Flow<String> = _notifications.asSharedFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false
    private var currentScanner: BluetoothLeScanner? = null
    private var currentScanCallback: ScanCallback? = null

    private val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val NUS_RX_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val NUS_TX_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    fun scanAndConnect() {
        scope.launch {
            val adapter = bluetoothAdapter
            if (adapter == null) {
                updateState(BleStatus.Error("Bluetooth not supported"))
                return@launch
            }
            if (!adapter.isEnabled) {
                updateState(BleStatus.Error("Bluetooth disabled"))
                return@launch
            }
            if (_connectionState.value is BleStatus.Connected || _connectionState.value is BleStatus.Connecting || _connectionState.value is BleStatus.Scanning) {
                return@launch
            }

            val device = withTimeoutOrNull(15000) { scanForDevice(adapter.bluetoothLeScanner) }
            if (device == null) {
                updateState(BleStatus.Error("Device not found"))
                return@launch
            }
            updateState(BleStatus.Connecting(device))
            connectToDevice(device)
        }
    }

    fun disconnect() {
        stopScan()
        mainHandler.post {
            bluetoothGatt?.let { gatt ->
                try {
                    gatt.disconnect()
                } catch (_: Throwable) {
                }
                try {
                    gatt.close()
                } catch (_: Throwable) {
                }
            }
            bluetoothGatt = null
            rxCharacteristic = null
            txCharacteristic = null
            updateState(BleStatus.Disconnected)
        }
    }

    fun sendCommand(cmd: String) {
        val gatt = bluetoothGatt
        val rx = rxCharacteristic
        if (gatt == null || rx == null) {
            Log.w(TAG, "Cannot send command, GATT not ready")
            return
        }
        val payload = (cmd + "\n").toByteArray(Charsets.UTF_8)
        mainHandler.post {
            rx.value = payload
            rx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val status = gatt.writeCharacteristic(rx, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                status == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                gatt.writeCharacteristic(rx)
            }
            Log.d(TAG, "sendCommand: '$cmd', writeCharacteristic returned $ok")
        }
    }

    fun close() {
        disconnect()
        scope.cancel()
    }

    private suspend fun scanForDevice(scanner: BluetoothLeScanner?): BluetoothDevice? {
        if (scanner == null) {
            updateState(BleStatus.Error("BLE scanner unavailable"))
            return null
        }
        updateState(BleStatus.Scanning)
        stopScan()
        return suspendCancellableCoroutine { continuation ->
            val filters = listOf(
                ScanFilter.Builder().setDeviceName("ESP32-GYRO-ON").build(),
                ScanFilter.Builder().setServiceUuid(ParcelUuid(NUS_SERVICE_UUID)).build()
            )
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    if (device != null && matchesTarget(device, result)) {
                        Log.d(TAG, "Found target device: ${device.address}")
                        stopAndResume(device)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "Scan failed: $errorCode")
                    updateState(BleStatus.Error("Scan failed: $errorCode"))
                    stopAndResume(null)
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    results.firstOrNull { it.device != null && matchesTarget(it.device, it) }?.let {
                        onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it)
                    }
                }

                private fun stopAndResume(device: BluetoothDevice?) {
                    if (!continuation.isActive) return
                    stopScanInternal(scanner, this)
                    continuation.resume(device)
                }
            }
            startScanInternal(scanner, filters, settings, callback)
            continuation.invokeOnCancellation {
                stopScanInternal(scanner, callback)
            }
        }
    }

    private fun matchesTarget(device: BluetoothDevice, result: ScanResult): Boolean {
        if (device.name == "ESP32-GYRO-ON") return true
        val uuids = result.scanRecord?.serviceUuids
        return uuids?.any { it.uuid == NUS_SERVICE_UUID } == true
    }

    private fun connectToDevice(device: BluetoothDevice) {
        mainHandler.post {
            stopScan()
            rxCharacteristic = null
            txCharacteristic = null
            bluetoothGatt?.close()
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        }
    }

    private fun startScanInternal(
        scanner: BluetoothLeScanner,
        filters: List<ScanFilter>,
        settings: ScanSettings,
        callback: ScanCallback
    ) {
        mainHandler.post {
            if (isScanning) return@post
            isScanning = true
            currentScanner = scanner
            currentScanCallback = callback
            scanner.startScan(filters, settings, callback)
        }
    }

    private fun stopScanInternal(scanner: BluetoothLeScanner, callback: ScanCallback) {
        mainHandler.post {
            if (!isScanning) return@post
            try {
                scanner.stopScan(callback)
            } catch (_: Throwable) {
            }
            isScanning = false
            currentScanner = null
            currentScanCallback = null
        }
    }

    private fun stopScan() {
        val scanner = currentScanner ?: bluetoothAdapter?.bluetoothLeScanner ?: return
        val callback = currentScanCallback ?: return
        stopScanInternal(scanner, callback)
    }

    private fun updateState(state: BleStatus) {
        _connectionState.value = state
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Connection state change error: $status")
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT, discovering services")
                    updateState(BleStatus.Connecting(gatt.device))
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT")
                    cleanupGatt()
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        updateState(BleStatus.Disconnected)
                    } else {
                        updateState(BleStatus.Error("Disconnected: $status"))
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d(TAG, "onServicesDiscovered: status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                updateState(BleStatus.Error("Service discovery failed: $status"))
                return
            }

            Log.d(TAG, "Services found: ${gatt.services.map { it.uuid }}")
            val service = gatt.getService(NUS_SERVICE_UUID)
            val tx = service?.getCharacteristic(NUS_TX_UUID)
            val rx = service?.getCharacteristic(NUS_RX_UUID)

            if (service == null) {
                Log.e(TAG, "NUS service not found. Available services: ${gatt.services.map { it.uuid }}")
                updateState(BleStatus.Error("NUS service not found"))
                gatt.disconnect()
                return
            }

            if (tx == null || rx == null) {
                Log.e(TAG, "NUS characteristics not found. Service has: ${service.characteristics.map { it.uuid }}")
                updateState(BleStatus.Error("NUS characteristics not found"))
                gatt.disconnect()
                return
            }

            Log.d(TAG, "✓ Found NUS service and characteristics")
            bluetoothGatt = gatt
            rxCharacteristic = rx
            txCharacteristic = tx

            val notificationSet = gatt.setCharacteristicNotification(tx, true)
            Log.d(TAG, "setCharacteristicNotification for TX: $notificationSet")

            if (!notificationSet) {
                Log.e(TAG, "⚠ Failed to enable local notifications for TX characteristic")
            }

            val cccd = tx.getDescriptor(CCCD_UUID)
            if (cccd != null) {
                Log.d(TAG, "Writing ENABLE_NOTIFICATION_VALUE to CCCD descriptor...")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val wrote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == android.bluetooth.BluetoothStatusCodes.SUCCESS
                } else {
                    gatt.writeDescriptor(cccd)
                }
                Log.d(TAG, "CCCD write initiated: $wrote")
                if (!wrote) {
                    Log.e(TAG, "⚠ Failed to initiate CCCD descriptor write")
                }
            } else {
                Log.e(TAG, "⚠ CCCD descriptor not found on TX characteristic")
            }

            Log.d(TAG, "Requesting MTU increase to 512 bytes...")
            val mtuRequested = gatt.requestMtu(512)
            Log.d(TAG, "MTU request initiated: $mtuRequested")

            Log.d(TAG, "✓ BLE setup complete, updating state to Connected")
            updateState(BleStatus.Connected(gatt.device))
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "onCharacteristicChanged (deprecated): UUID=${characteristic.uuid}")
            if (characteristic.uuid != NUS_TX_UUID) {
                Log.w(TAG, "Ignoring notification from unexpected characteristic: ${characteristic.uuid}")
                return
            }
            val bytes = characteristic.value ?: run {
                Log.w(TAG, "Notification has null value")
                return
            }
            val text = try {
                bytes.toString(Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode notification", e)
                return
            }
            Log.d(TAG, "✓ Notification received: '$text'")
            _notifications.tryEmit(text)
        }

        // Override for Android 13+ (API 33+)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            Log.d(TAG, "onCharacteristicChanged (new API): UUID=${characteristic.uuid}, bytes=${value.size}")
            if (characteristic.uuid != NUS_TX_UUID) {
                Log.w(TAG, "Ignoring notification from unexpected characteristic: ${characteristic.uuid}")
                return
            }
            val text = try {
                value.toString(Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode notification", e)
                return
            }
            Log.d(TAG, "✓ Notification received: '$text'")
            _notifications.tryEmit(text)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✓✓✓ MTU changed SUCCESS! New MTU: $mtu bytes (payload: ${mtu - 3} bytes)")
            } else {
                Log.e(TAG, "⚠ MTU change failed: status=$status, using default MTU=23")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.uuid == CCCD_UUID && descriptor.characteristic.uuid == NUS_TX_UUID) {
                    Log.d(TAG, "✓✓✓ CCCD write SUCCESS! Notifications are now enabled. Waiting for data...")
                } else {
                    Log.d(TAG, "Descriptor write success: ${descriptor.uuid}")
                }
            } else {
                Log.e(TAG, "⚠⚠⚠ Descriptor write FAILED: status=$status, descriptor=${descriptor.uuid}")
            }
        }
    }

    private fun cleanupGatt() {
        rxCharacteristic = null
        txCharacteristic = null
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}