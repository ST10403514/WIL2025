package com.example.kineticpulsemobileapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

public class DevicesFragment extends Fragment {

    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    private ActivityResultLauncher<String> requestBluetoothPermissionLauncher;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private Button btnBluetoothSettings;
    private Button btnRefreshDevices;
    private ListView devicesListView;
    private TextView emptyTextView;
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
                        showPermissionDeniedMessage();
                    }
                });

        // Bluetooth enable launcher
        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (isBluetoothEnabled()) {
                        Log.d(TAG, "Bluetooth enabled by user");
                        checkAndRequestPermissions();
                    } else {
                        Log.d(TAG, "User did not enable Bluetooth");
                        setEmptyText("❌ Bluetooth is required\n\nTap 'Bluetooth Settings' to enable");
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_devices, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        btnBluetoothSettings = view.findViewById(R.id.btnBluetoothSettings);
        btnRefreshDevices = view.findViewById(R.id.btnRefreshDevices);
        devicesListView = view.findViewById(R.id.devicesListView); // This should work now

        // Set up list view
        devicesListView.setAdapter(listAdapter);
        devicesListView.setOnItemClickListener(this::onListItemClick);

        // Set button listeners
        btnBluetoothSettings.setOnClickListener(v -> handleBluetoothSettings());
        btnRefreshDevices.setOnClickListener(v -> handleRefreshDevices());

        // Create empty text view
        emptyTextView = new TextView(getContext());
        emptyTextView.setTextSize(18);
        emptyTextView.setTextColor(0xFF2D3748);
        emptyTextView.setPadding(50, 50, 50, 50);
        emptyTextView.setGravity(android.view.Gravity.CENTER);

        // Add the empty view to the parent layout
        ViewGroup parent = (ViewGroup) devicesListView.getParent();
        parent.addView(emptyTextView);
        devicesListView.setEmptyView(emptyTextView);

        setEmptyText("Initializing Bluetooth...");

        // Check Bluetooth state immediately
        checkBluetoothState();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkBluetoothState();
    }

    private void setEmptyText(String text) {
        if (emptyTextView != null) {
            emptyTextView.setText(text);
        }
    }

    /**
     * Check Bluetooth state and guide user through setup if needed
     */
    private void checkBluetoothState() {
        if (bluetoothAdapter == null) {
            setEmptyText("❌ Bluetooth not supported on this device");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showBluetoothEnableDialog();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog();
                return;
            }
        }

        // Everything is ready, refresh devices
        refresh();
    }

    private void showBluetoothEnableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Enable Bluetooth");
        builder.setMessage("Bluetooth is required to connect to your ESP32 device. Would you like to enable it now?");
        builder.setNegativeButton("Not Now", (dialog, which) -> {
            setEmptyText("❌ Bluetooth is disabled\n\nTap 'Bluetooth Settings' to enable");
        });
        builder.setPositiveButton("Enable", (dialog, which) -> {
            enableBluetooth();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Bluetooth Permission Needed");
        builder.setMessage("This app needs Bluetooth permission to discover and connect to nearby devices.");
        builder.setNegativeButton("Not Now", (dialog, which) -> {
            setEmptyText("🔐 Permission required\n\nTap 'Refresh List' to grant permission");
        });
        builder.setPositiveButton("Grant Permission", (dialog, which) -> {
            requestBluetoothPermission();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void showPermissionDeniedMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Permission Denied");
        builder.setMessage("Bluetooth permission is required to scan for devices. You can grant permission in Settings.");
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });
        builder.show();
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBluetoothLauncher.launch(enableBtIntent);
    }

    private void requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermission();
            } else {
                refresh();
            }
        } else {
            refresh();
        }
    }

    private boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
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
            // Use the system dialog to enable Bluetooth
            enableBluetooth();
        } else {
            // Open Bluetooth settings for pairing
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }
    }

    /**
     * Handle Refresh Devices button click
     */
    private void handleRefreshDevices() {
        Log.d(TAG, "Refresh Devices button clicked");

        if (bluetoothAdapter == null) {
            setEmptyText("❌ Bluetooth not supported on this device");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            showBluetoothEnableDialog();
            return;
        }

        // Check if we need permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog();
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
                    listItems.add(device);
                }
                Collections.sort(listItems, BluetoothUtil::compareTo);
            }
        }

        // Update empty text based on state
        if(bluetoothAdapter == null) {
            setEmptyText("❌ Bluetooth not supported on this device");
        } else if(!bluetoothAdapter.isEnabled()) {
            setEmptyText("🔵 Bluetooth is disabled\n\nTap \"Bluetooth Settings\" to enable");
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

    private boolean isRunningOnEmulator() {
        return Build.FINGERPRINT.contains("generic")
                || Build.FINGERPRINT.contains("emulator")
                || Build.FINGERPRINT.contains("sdk_gphone")
                || Build.FINGERPRINT.contains("vbox")
                || Build.MODEL.contains("Android SDK");
    }

    private void addMockDevices() {
        try {
            Log.d(TAG, "Creating mock Bluetooth devices");
            Class<?> bluetoothDeviceClass = Class.forName("android.bluetooth.BluetoothDevice");
            Constructor<?> constructor = bluetoothDeviceClass.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);

            BluetoothDevice mockESP32 = (BluetoothDevice) constructor.newInstance("00:11:22:33:44:55");
            setMockDeviceName(mockESP32, "ESP32_Gyro_Sensor");
            listItems.add(mockESP32);

            BluetoothDevice mockArduino = (BluetoothDevice) constructor.newInstance("66:77:88:99:AA:BB");
            setMockDeviceName(mockArduino, "Arduino_Nano_BLE");
            listItems.add(mockArduino);

            BluetoothDevice mockSensor = (BluetoothDevice) constructor.newInstance("12:34:56:78:90:AB");
            setMockDeviceName(mockSensor, "Kinetic_Pulse_Meter");
            listItems.add(mockSensor);

            Collections.sort(listItems, BluetoothUtil::compareTo);

        } catch (Exception e) {
            Log.e(TAG, "Error creating mock devices: " + e.getMessage(), e);
            createSimpleMockDevices();
        }
    }

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

    private void setMockDeviceName(BluetoothDevice device, String name) {
        try {
            Field nameField = BluetoothDevice.class.getDeclaredField("mName");
            nameField.setAccessible(true);
            nameField.set(device, name);
        } catch (Exception e) {
            Log.e(TAG, "Error setting mock device name: " + e.getMessage());
        }
    }

    /**
     * Handle list item click
     */
    private void onListItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < listItems.size()) {
            BluetoothDevice device = listItems.get(position);
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