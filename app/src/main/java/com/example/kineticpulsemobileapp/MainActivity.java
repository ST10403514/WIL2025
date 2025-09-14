package com.example.kineticpulsemobileapp;

import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private GyroManager gyroManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new DevicesFragment(), "devices").commit();
        else
            onBackStackChanged();

        gyroManager = new GyroManager(getApplicationContext());
    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gyroManager != null) gyroManager.start();
    }

    @Override
    protected void onPause() {
        if (gyroManager != null) gyroManager.stop();
        super.onPause();
    }

    public void startGyro() {
        if (gyroManager != null) gyroManager.start();
    }

    public void stopGyro() {
        if (gyroManager != null) gyroManager.stop();
    }
}
