package com.example.kineticpulsemobileapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

class GyroManager implements SensorEventListener {

    private static final String TAG = "Gyro";
    private final SensorManager sensorManager;
    private final Sensor gyro;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean started = false;
    private int firstLogs = 0;

    GyroManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyro = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) : null;
        if (gyro == null) {
            Log.w(TAG, "Gyroscope not available");
        } else {
            Log.i(TAG, "Gyroscope initialized: " + gyro.getName());
        }
    }

    boolean isAvailable() {
        return gyro != null && sensorManager != null;
    }

    void start() {
        if (!isAvailable()) {
            Log.w(TAG, "start called but gyroscope unavailable");
            return;
        }
        if (started) {
            Log.d(TAG, "start ignored (already started)");
            return;
        }
        started = true;
        firstLogs = 0;
        // ~60 Hz target (microseconds)
        int periodUs = 16667;
        boolean ok = sensorManager.registerListener(this, gyro, periodUs, periodUs);
        Log.i(TAG, ok ? "Gyro started" : "Failed to start Gyro listener");
    }

    void stop() {
        if (!started || sensorManager == null) return;
        sensorManager.unregisterListener(this);
        started = false;
        Log.i(TAG, "Gyro stopped");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE) return;
        if (firstLogs < 3) {
            firstLogs++;
            Log.d(TAG, String.format("event #%d x=%.4f y=%.4f z=%.4f t=%d", firstLogs, event.values[0], event.values[1], event.values[2], event.timestamp));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "accuracy=" + accuracy);
    }
}
