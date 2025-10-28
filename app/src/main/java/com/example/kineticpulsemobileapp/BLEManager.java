package com.example.kineticpulsemobileapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BLEManager {
    private static final String TAG = "BLEManager";

    // Nordic UART Service UUIDs
    private static final UUID NUS_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NUS_TX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"); // ESP → Phone (notifications)
    private static final UUID NUS_RX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"); // Phone → ESP (write)
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final BLECallback callback;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic txCharacteristic;
    private BluetoothGattCharacteristic rxCharacteristic;

    private boolean isScanning = false;
    private boolean isConnected = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long SCAN_PERIOD = 10000; // 10 seconds

    public interface BLECallback {
        void onDeviceFound(BluetoothDevice device, String name);
        void onConnected();
        void onDisconnected();
        void onDataReceived(String data);
        void onError(String error);
    }

    public BLEManager(Context context, BLECallback callback) {
        this.context = context;
        this.callback = callback;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            this.scanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void startScan() {
        if (!isBluetoothEnabled() || scanner == null) {
            callback.onError("Bluetooth not available or not enabled");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                callback.onError("Bluetooth scan permission not granted");
                return;
            }
        }

        if (isScanning) {
            Log.d(TAG, "Already scanning");
            return;
        }

        // Scan filters for NUS service
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(NUS_SERVICE_UUID))
                .build();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        isScanning = true;
        scanner.startScan(filters, settings, scanCallback);
        Log.d(TAG, "🔍 Started BLE scan for NUS service");

        // Stop scan after SCAN_PERIOD
        handler.postDelayed(() -> {
            if (isScanning) {
                stopScan();
                callback.onError("No devices found");
            }
        }, SCAN_PERIOD);
    }

    public void stopScan() {
        if (!isScanning || scanner == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        scanner.stopScan(scanCallback);
        isScanning = false;
        Log.d(TAG, "⏹️ Stopped BLE scan");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            if (deviceName != null && deviceName.toLowerCase().contains("cape")) {
                Log.d(TAG, "✅ Found ESP32 device: " + deviceName);
                stopScan();
                callback.onDeviceFound(device, deviceName);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            isScanning = false;
            callback.onError("Scan failed with error: " + errorCode);
            Log.e(TAG, "❌ BLE scan failed: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                callback.onError("Bluetooth connect permission not granted");
                return;
            }
        }

        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        Log.d(TAG, "🔗 Connecting to device: " + device.getAddress());
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                Log.d(TAG, "✅ Connected to GATT server");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                Log.d(TAG, "❌ Disconnected from GATT server");
                handler.post(() -> callback.onDisconnected());
                cleanup();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                android.bluetooth.BluetoothGattService nusService = gatt.getService(NUS_SERVICE_UUID);
                if (nusService != null) {
                    txCharacteristic = nusService.getCharacteristic(NUS_TX_UUID);
                    rxCharacteristic = nusService.getCharacteristic(NUS_RX_UUID);

                    if (txCharacteristic != null && rxCharacteristic != null) {
                        Log.d(TAG, "🎯 NUS service characteristics found");
                        enableNotifications(gatt, txCharacteristic);
                        handler.post(() -> callback.onConnected());
                    } else {
                        handler.post(() -> callback.onError("NUS characteristics not found"));
                    }
                } else {
                    handler.post(() -> callback.onError("NUS service not found"));
                }
            } else {
                handler.post(() -> callback.onError("Service discovery failed: " + status));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(NUS_TX_UUID)) {
                byte[] data = characteristic.getValue();
                String message = new String(data, StandardCharsets.UTF_8);
                Log.d(TAG, "📥 Received: " + message);
                handler.post(() -> callback.onDataReceived(message));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "📤 Write successful");
            } else {
                Log.e(TAG, "❌ Write failed: " + status);
            }
        }
    };

    private void enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        gatt.setCharacteristicNotification(characteristic, true);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            Log.d(TAG, "📡 Notifications enabled for TX characteristic");
        }
    }

    public void sendCommand(String command) {
        if (!isConnected || bluetoothGatt == null || rxCharacteristic == null) {
            Log.w(TAG, "Cannot send - not connected");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        byte[] data = command.getBytes(StandardCharsets.UTF_8);
        rxCharacteristic.setValue(data);
        boolean success = bluetoothGatt.writeCharacteristic(rxCharacteristic);
        Log.d(TAG, "📤 Sending command: " + command + " - " + (success ? "queued" : "failed"));
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        cleanup();
    }

    private void cleanup() {
        txCharacteristic = null;
        rxCharacteristic = null;
        isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void close() {
        stopScan();
        disconnect();
    }
}