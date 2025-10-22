package com.example.kineticpulsemobileapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

public class BluetoothUtil {

    interface PermissionGrantedCallback {
        void call();
    }

    interface BluetoothStateCallback {
        void onReady();
        void onPermissionDenied();
        void onBluetoothDisabled();
        void onInitialSetupRequired(); // New callback for initial setup
    }

    @SuppressLint("MissingPermission")
    static int compareTo(BluetoothDevice a, BluetoothDevice b) {
        boolean aValid = a.getName()!=null && !a.getName().isEmpty();
        boolean bValid = b.getName()!=null && !b.getName().isEmpty();
        if(aValid && bValid) {
            int ret = a.getName().compareTo(b.getName());
            if (ret != 0) return ret;
            return a.getAddress().compareTo(b.getAddress());
        }
        if(aValid) return -1;
        if(bValid) return +1;
        return a.getAddress().compareTo(b.getAddress());
    }

    private static void showInitialSetupDialog(Fragment fragment, BluetoothStateCallback callback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle("Bluetooth Setup Required");
        builder.setMessage("To connect to your ESP32 device, we need to:\n\n1. Enable Bluetooth\n2. Grant Bluetooth permissions\n\nThis is a one-time setup. Would you like to proceed?");
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User canceled - show appropriate message
            callback.onPermissionDenied();
        });
        builder.setPositiveButton("Setup Bluetooth", (dialog, which) -> {
            // Start the setup process
            startBluetoothSetup(fragment, callback);
        });
        builder.setCancelable(false);
        builder.show();
    }

    private static void startBluetoothSetup(Fragment fragment, BluetoothStateCallback callback) {
        Context context = fragment.getContext();
        if (context == null) return;

        // First, check if Bluetooth is enabled
        if (!isBluetoothEnabled(context)) {
            showEnableBluetoothDialog(fragment, () -> {
                // After enabling Bluetooth, check permissions
                checkPermissionsAfterBluetoothEnabled(fragment, callback);
            });
        } else {
            // Bluetooth is enabled, check permissions
            checkPermissionsStep(fragment, callback);
        }
    }

    private static void checkPermissionsAfterBluetoothEnabled(Fragment fragment, BluetoothStateCallback callback) {
        // Wait a moment for Bluetooth to enable, then check permissions
        new android.os.Handler().postDelayed(() -> {
            checkPermissionsStep(fragment, callback);
        }, 1000);
    }

    private static void checkPermissionsStep(Fragment fragment, BluetoothStateCallback callback) {
        if (!hasPermissions(fragment)) {
            showPermissionRationaleDialog(fragment, callback);
        } else {
            // Everything is ready!
            callback.onReady();
        }
    }

    private static void showPermissionRationaleDialog(Fragment fragment, BluetoothStateCallback callback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle("Bluetooth Permission Needed");
        builder.setMessage("This app needs Bluetooth permission to discover and connect to your ESP32 device for real-time motion tracking.");
        builder.setNegativeButton("Cancel", (dialog, which) -> callback.onPermissionDenied());
        builder.setPositiveButton("Grant Permission", (dialog, which) -> {
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityResultLauncher<String> launcher = createPermissionLauncher(fragment, callback);
                if (launcher != null) {
                    launcher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                }
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    // Helper method to create permission launcher if needed
    private static ActivityResultLauncher<String> createPermissionLauncher(Fragment fragment, BluetoothStateCallback callback) {
        // This would need to be implemented in your fragment
        // For now, we'll handle it in the DevicesFragment
        return null;
    }

    private static void showEnableBluetoothDialog(Fragment fragment, Runnable onEnable) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle("Enable Bluetooth");
        builder.setMessage("Bluetooth is required to connect to your ESP32 device. Please enable Bluetooth to continue.");
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Enable Bluetooth", (dialog, which) -> {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            fragment.startActivity(enableBtIntent);
            if (onEnable != null) {
                onEnable.run();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private static void showSettingsDialog(Fragment fragment) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle("Permission Required");
        builder.setMessage("Bluetooth permission is required to use this app. Please enable it in Settings.");
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Open Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", fragment.getActivity().getPackageName(), null);
            intent.setData(uri);
            fragment.startActivity(intent);
        });
        builder.setCancelable(false);
        builder.show();
    }

    /**
     * Check if Bluetooth permissions are granted
     */
    static boolean hasPermissions(Fragment fragment) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return fragment.getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if Bluetooth is enabled
     */
    static boolean isBluetoothEnabled(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        }
        return false;
    }

    /**
     * Check if device supports Bluetooth
     */
    static boolean isBluetoothSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    /**
     * Initialize Bluetooth with better user flow
     */
    public static void initializeBluetooth(Fragment fragment,
                                           ActivityResultLauncher<String> permissionLauncher,
                                           BluetoothStateCallback callback) {
        Context context = fragment.getContext();
        if (context == null) return;

        // Check if Bluetooth is supported
        if (!isBluetoothSupported(context)) {
            return;
        }

        // Check current state
        boolean hasPerms = hasPermissions(fragment);
        boolean btEnabled = isBluetoothEnabled(context);

        if (!hasPerms || !btEnabled) {
            // Show initial setup dialog
            showInitialSetupDialog(fragment, callback);
        } else {
            // Everything is ready
            callback.onReady();
        }
    }

    /**
     * Quick check if everything is ready
     */
    static boolean isReadyToConnect(Fragment fragment) {
        return hasPermissions(fragment) && isBluetoothEnabled(fragment.getContext());
    }
}