package com.example.kineticpulsemobileapp;

import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private GyroManager gyroManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // No toolbar setup needed anymore!

        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new DevicesFragment(), "devices").commit();
        else
            onBackStackChanged();

        gyroManager = new GyroManager(getApplicationContext());
    }

    @Override
    public void onBackStackChanged() {
        // Just check if we should show back button functionality
        // No toolbar to update anymore
        Log.d("MainActivity", "Back stack changed. Count: " + getSupportFragmentManager().getBackStackEntryCount());
    }

    @Override
    public void onBackPressed() {
        // Handle back press normally
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Gyro will be controlled by TerminalFragment
        Log.d("MainActivity", "onResume - gyro will be controlled by TerminalFragment");
    }

    @Override
    protected void onPause() {
        // Gyro will be controlled by TerminalFragment
        Log.d("MainActivity", "onPause - gyro will be controlled by TerminalFragment");
        super.onPause();
    }

    public void startGyro() {
        if (gyroManager != null) gyroManager.start();
    }

    public void stopGyro() {
        if (gyroManager != null) gyroManager.stop();
    }

    public GyroManager getGyroManager() {
        return gyroManager;
    }
}