package com.example.kineticpulsemobileapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

public class DevicesFragment extends ListFragment {

    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    private ActivityResultLauncher<String> requestBluetoothPermissionLauncher;
    private Button btnBluetoothSettings;
    private Button btnRefreshDevices;
    private boolean permissionMissing;
    private static final String TAG = "DevicesFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        if(getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, listItems) {
            @NonNull
            @Override
            public View getView(int position, View view, @NonNull ViewGroup parent) {
                BluetoothDevice device = listItems.get(position);
                if (view == null)
                    view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                TextView text1 = view.findViewById(R.id.text1);
                TextView text2 = view.findViewById(R.id.text2);
                @SuppressLint("MissingPermission") String deviceName = device.getName();
                text1.setText(deviceName != null ? deviceName : "Unknown Device");
                text2.setText(device.getAddress());
                return view;
            }
        };

        // Permission launcher
        requestBluetoothPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        Log.d(TAG, "Bluetooth permission granted");
                        refresh();
                    } else {
                        Log.d(TAG, "Bluetooth permission denied");
                        refresh(); // Still refresh to update UI state
                    }
                });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);

        // ALWAYS INFLATE AND SHOW HEADER FIRST
        View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, null, false);
        getListView().addHeaderView(header, null, false);

        // Set background color for the list
        getListView().setBackgroundColor(0xFFFFE5B4);
        getListView().setDivider(null);
        getListView().setDividerHeight(0);

        // Initialize buttons from header
        btnBluetoothSettings = header.findViewById(R.id.btnBluetoothSettings);
        btnRefreshDevices = header.findViewById(R.id.btnRefreshDevices);

        // Setup button click listeners - THESE HANDLE ALL ACTIONS
        btnBluetoothSettings.setOnClickListener(v -> handleBluetoothSettings());
        btnRefreshDevices.setOnClickListener(v -> handleRefreshDevices());

        setEmptyText("Checking Bluetooth status...");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        ((TextView) getListView().getEmptyView()).setTextColor(0xFF2D3748);

        setListAdapter(listAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Just refresh the state, don't auto-request permissions
        refresh();
    }

    /**
     * Handle Bluetooth Settings button click
     */
    private void handleBluetoothSettings() {
        Log.d(TAG, "Bluetooth Settings button clicked");

        if (bluetoothAdapter == null) {
            setEmptyText("❌ Bluetooth not supported on this device");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            // Open Bluetooth settings to enable Bluetooth
            Intent enableBtIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(enableBtIntent);
        } else {
            // Bluetooth is already enabled, just open settings for pairing
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }
    }

    /**
     * Handle Refresh Devices button click - this handles permissions too
     */
    private void handleRefreshDevices() {
        Log.d(TAG, "Refresh Devices button clicked");

        if (bluetoothAdapter == null) {
            setEmptyText("❌ Bluetooth not supported on this device");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            setEmptyText("⚠️ Please enable Bluetooth first\nTap 'Bluetooth Settings'");
            return;
        }

        // Check if we need permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting Bluetooth permission...");
                setEmptyText("📱 Requesting Bluetooth permission...");
                requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                return;
            }
        }

        // We have permissions and Bluetooth is enabled - refresh devices
        refresh();
    }

    @SuppressLint("MissingPermission")
    void refresh() {
        listItems.clear();
        permissionMissing = false;

        // Check if we're running on emulator
        boolean isEmulator = isRunningOnEmulator();

        if (isEmulator) {
            Log.d(TAG, "Running on emulator - loading mock devices");
            addMockDevices();
        } else if(bluetoothAdapter != null) {
            Log.d(TAG, "Running on real device - loading real Bluetooth devices");

            // Check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionMissing = getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
            }

            if(!permissionMissing && bluetoothAdapter.isEnabled()) {
                // We have permissions and Bluetooth is enabled - load devices
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                    // Include all device types for now
                    listItems.add(device);
                }
                Collections.sort(listItems, BluetoothUtil::compareTo);
            }
        }

        // Update empty text based on state - CLEAR INSTRUCTIONS
        if(bluetoothAdapter == null) {
            setEmptyText("❌ Bluetooth not supported on this device");
        } else if(!bluetoothAdapter.isEnabled()) {
            setEmptyText("🔵 Bluetooth is disabled\n\nTap \"Bluetooth Settings\" to enable Bluetooth");
        } else if(permissionMissing) {
            setEmptyText("🔐 Bluetooth permission required\n\nTap \"Refresh List\" to grant permission");
        } else if(listItems.isEmpty()) {
            setEmptyText("📱 No paired devices found\n\n1. Tap \"Bluetooth Settings\"\n2. Pair your ESP32 device\n3. Tap \"Refresh List\"");
        } else if (isEmulator) {
            setEmptyText("🎮 Emulator Mode - Mock devices loaded\n\nTap any device to connect");
        } else {
            setEmptyText("✅ " + listItems.size() + " device(s) found\n\nTap any device to connect");
        }

        listAdapter.notifyDataSetChanged();
    }

    /**
     * Check if the app is running on an emulator
     */
    private boolean isRunningOnEmulator() {
        return Build.FINGERPRINT.contains("generic")
                || Build.FINGERPRINT.contains("emulator")
                || Build.FINGERPRINT.contains("sdk_gphone")
                || Build.FINGERPRINT.contains("vbox")
                || Build.MODEL.contains("Android SDK");
    }

    /**
     * Add mock Bluetooth devices for emulator testing
     */
    private void addMockDevices() {
        try {
            Log.d(TAG, "Creating mock Bluetooth devices");

            // Create mock Bluetooth devices using reflection
            Class<?> bluetoothDeviceClass = Class.forName("android.bluetooth.BluetoothDevice");
            Constructor<?> constructor = bluetoothDeviceClass.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);

            // Add mock ESP32 device
            BluetoothDevice mockESP32 = (BluetoothDevice) constructor.newInstance("00:11:22:33:44:55");
            setMockDeviceName(mockESP32, "ESP32_Gyro_Sensor");
            listItems.add(mockESP32);

            // Add mock Arduino device
            BluetoothDevice mockArduino = (BluetoothDevice) constructor.newInstance("66:77:88:99:AA:BB");
            setMockDeviceName(mockArduino, "Arduino_Nano_BLE");
            listItems.add(mockArduino);

            // Add another mock device
            BluetoothDevice mockSensor = (BluetoothDevice) constructor.newInstance("12:34:56:78:90:AB");
            setMockDeviceName(mockSensor, "Kinetic_Pulse_Meter");
            listItems.add(mockSensor);

            Collections.sort(listItems, BluetoothUtil::compareTo);

        } catch (Exception e) {
            Log.e(TAG, "Error creating mock devices: " + e.getMessage(), e);
            createSimpleMockDevices();
        }
    }

    /**
     * Fallback method to create mock devices without reflection
     */
    private void createSimpleMockDevices() {
        try {
            BluetoothDevice mock1 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice("00:11:22:33:44:55");
            BluetoothDevice mock2 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice("66:77:88:99:AA:BB");
            BluetoothDevice mock3 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice("12:34:56:78:90:AB");

            listItems.add(mock1);
            listItems.add(mock2);
            listItems.add(mock3);

        } catch (Exception e) {
            Log.e(TAG, "Fallback mock device creation failed: " + e.getMessage());
        }
    }

    /**
     * Set the name of a mock Bluetooth device using reflection
     */
    private void setMockDeviceName(BluetoothDevice device, String name) {
        try {
            Field nameField = BluetoothDevice.class.getDeclaredField("mName");
            nameField.setAccessible(true);
            nameField.set(device, name);
        } catch (Exception e) {
            Log.e(TAG, "Error setting mock device name: " + e.getMessage());
        }
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        if (position > 0 && position - 1 < listItems.size()) {
            BluetoothDevice device = listItems.get(position - 1);

            Bundle args = new Bundle();
            args.putString("device", device.getAddress());
            args.putBoolean("isEmulator", isRunningOnEmulator());

            Fragment fragment = new TerminalFragment();
            fragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment, fragment, "terminal")
                    .addToBackStack(null)
                    .commit();
        }
    }
}