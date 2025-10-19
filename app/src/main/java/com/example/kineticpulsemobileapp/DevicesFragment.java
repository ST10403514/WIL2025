package com.example.kineticpulsemobileapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;

public class DevicesFragment extends ListFragment {

    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    ActivityResultLauncher<String> requestBluetoothPermissionLauncherForRefresh;
    private Button btnBluetoothSettings;
    private Button btnRefreshDevices;
    private boolean permissionMissing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false); // Disable options menu since we're using buttons now

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

        requestBluetoothPermissionLauncherForRefresh = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> BluetoothUtil.onPermissionsResult(this, granted, this::refresh));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);

        // Inflate header with buttons
        View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, null, false);
        getListView().addHeaderView(header, null, false);

        // Set background color for the list
        getListView().setBackgroundColor(0xFFFFE5B4); // Peach background
        getListView().setDivider(null);
        getListView().setDividerHeight(0);

        // Initialize buttons from header
        btnBluetoothSettings = header.findViewById(R.id.btnBluetoothSettings);
        btnRefreshDevices = header.findViewById(R.id.btnRefreshDevices);

        // Setup button click listeners
        btnBluetoothSettings.setOnClickListener(v -> openBluetoothSettings());
        btnRefreshDevices.setOnClickListener(v -> {
            if(BluetoothUtil.hasPermissions(this, requestBluetoothPermissionLauncherForRefresh))
                refresh();
        });

        // Disable Bluetooth settings button if adapter is null
        if(bluetoothAdapter == null) {
            btnBluetoothSettings.setEnabled(false);
            btnBluetoothSettings.setAlpha(0.5f);
        }

        setEmptyText("Searching for devices...");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        ((TextView) getListView().getEmptyView()).setTextColor(0xFF2D3748);

        setListAdapter(listAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void openBluetoothSettings() {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    @SuppressLint("MissingPermission")
    void refresh() {
        listItems.clear();
        if(bluetoothAdapter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionMissing = getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;

                // Update refresh button visibility based on permission
                if(btnRefreshDevices != null) {
                    btnRefreshDevices.setVisibility(permissionMissing ? View.VISIBLE : View.VISIBLE);
                }
            }

            if(!permissionMissing) {
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
                    if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                        listItems.add(device);
                Collections.sort(listItems, BluetoothUtil::compareTo);
            }
        }

        // Update empty text based on state
        if(bluetoothAdapter == null)
            setEmptyText("Bluetooth not supported on this device");
        else if(!bluetoothAdapter.isEnabled())
            setEmptyText("Please enable Bluetooth in settings");
        else if(permissionMissing)
            setEmptyText("Permission needed - tap Refresh to grant");
        else
            setEmptyText("No paired devices found\nPair devices in Bluetooth Settings");

        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        BluetoothDevice device = listItems.get(position-1);
        Bundle args = new Bundle();
        args.putString("device", device.getAddress());
        Fragment fragment = new TerminalFragment();
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment, fragment, "terminal")
                .addToBackStack(null)
                .commit();
    }
}