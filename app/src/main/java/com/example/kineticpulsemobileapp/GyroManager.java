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
    interface MovementListener {
        void onLeft();
        void onRight();
        void onMiddle(); // up/forward
        void onBack();   // backward tilt
        default void onRaw(float x, float y, float z) {}
    }
    
    interface CalibrationListener {
        void onCalibrationStart();
        void onCalibrationPhase(String instruction, int color);
        void onCalibrationComplete();
    }

    private boolean started = false;
    private int firstLogs = 0;
    private volatile boolean processingEnabled = false;
    private MovementListener listener;
    private CalibrationListener calibrationListener;
    private long lastEventMs = 0L;
    private long lastTsNs = 0L;
    private float yawAngle = 0f;   // integrate z (yaw)
    private float pitchAngle = 0f; // integrate x (pitch)

    private enum Pose { LEFT, MIDDLE, RIGHT, BACK }
    private Pose currentPose = Pose.MIDDLE;
    private boolean calibrating = false;
    private long calibStartMs = 0L;
    private static final long CALIBRATION_MS = 2000L;
    
    // Interactive calibration states
    private enum CalibrationPhase { NONE, STEADY, LEFT, RIGHT, FORWARD, BACK, COMPLETE }
    private CalibrationPhase calibrationPhase = CalibrationPhase.NONE;
    private long phaseStartMs = 0L;
    private static final long PHASE_DURATION_MS = 3000L;

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

    void setMovementListener(MovementListener l) {
        this.listener = l;
    }
    
    void setCalibrationListener(CalibrationListener l) {
        this.calibrationListener = l;
    }

    void setProcessingEnabled(boolean enabled) {
        processingEnabled = enabled;
        Log.i(TAG, "processingEnabled=" + enabled);
        if (enabled) {
            // Start interactive calibration sequence
            startInteractiveCalibration();
        } else {
            calibrationPhase = CalibrationPhase.NONE;
            calibrating = false;
            currentPose = Pose.MIDDLE; // Reset to middle position
        }
    }
    
    private void startInteractiveCalibration() {
        calibrationPhase = CalibrationPhase.STEADY;
        phaseStartMs = System.currentTimeMillis();
        calibrating = true;
        yawAngle = 0f;
        pitchAngle = 0f;
        lastTsNs = 0L;
        currentPose = Pose.MIDDLE;
        if (calibrationListener != null) {
            calibrationListener.onCalibrationStart();
            calibrationListener.onCalibrationPhase("Hold device steady", 0xFFFFFFFF); // white
        }
        Log.i(TAG, "Starting interactive calibration - hold device steady");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE) return;
        if (firstLogs < 3) {
            firstLogs++;
            Log.d(TAG, String.format("event #%d x=%.4f y=%.4f z=%.4f t=%d", firstLogs, event.values[0], event.values[1], event.values[2], event.timestamp));
        }
        if (!processingEnabled || listener == null) return;

        final float x = event.values[0]; // pitch rate
        final float y = event.values[1]; // roll rate (unused in detection)
        final float z = event.values[2]; // yaw rate
        listener.onRaw(x, y, z);

        // Integrate angular velocity into approximate angles (radians)
        long ts = event.timestamp; // ns
        if (lastTsNs == 0L) { lastTsNs = ts; return; }
        float dt = (ts - lastTsNs) / 1_000_000_000f; // seconds
        lastTsNs = ts;
        yawAngle += z * dt;
        pitchAngle += x * dt;

        // Handle interactive calibration phases
        if (calibrating && calibrationPhase != CalibrationPhase.COMPLETE) {
            long phaseElapsed = System.currentTimeMillis() - phaseStartMs;
            if (phaseElapsed >= PHASE_DURATION_MS) {
                advanceCalibrationPhase();
            }
            return; // Don't process movements during calibration
        }

        // Clamp to avoid runaway drift during long inactivity
        final float MAX_ANGLE = (float)Math.toRadians(90);
        yawAngle = Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, yawAngle));
        pitchAngle = Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, pitchAngle));

        // Determine pose from angles with MORE SENSITIVE thresholds
        final float YAW_THRESH = (float)Math.toRadians(10);    // left/right (reduced from 20 to 10 degrees)
        final float PITCH_THRESH = (float)Math.toRadians(10);  // forward/back (reduced from 20 to 10 degrees)
        
        Pose newPose = Pose.MIDDLE; // Default to middle/neutral
        
        // Priority: check for stronger movements first
        if (Math.abs(yawAngle) > Math.abs(pitchAngle)) {
            // Horizontal movement is stronger
            if (yawAngle > YAW_THRESH) {
                newPose = Pose.RIGHT;
            } else if (yawAngle < -YAW_THRESH) {
                newPose = Pose.LEFT;
            }
        } else {
            // Vertical movement is stronger
            if (pitchAngle > PITCH_THRESH) {
                newPose = Pose.MIDDLE; // forward tilt (jumping up/forward)
            } else if (pitchAngle < -PITCH_THRESH) {
                newPose = Pose.BACK; // backward tilt
            }
        }

        long now = System.currentTimeMillis();
        final long DEBOUNCE_MS = 300; // reduced debounce time for faster response
        final float MIN_MOVEMENT = (float)Math.toRadians(5); // minimum movement to trigger detection (reduced from 10 to 5 degrees)
        
        // Only trigger if we have significant movement and enough time has passed
        boolean hasSignificantMovement = Math.abs(yawAngle) > MIN_MOVEMENT || Math.abs(pitchAngle) > MIN_MOVEMENT;
        
        if (newPose != currentPose && hasSignificantMovement && now - lastEventMs > DEBOUNCE_MS) {
            currentPose = newPose;
            lastEventMs = now;
            Log.d(TAG, "pose=" + currentPose + " yaw=" + String.format("%.1f", Math.toDegrees(yawAngle)) + "° pitch=" + String.format("%.1f", Math.toDegrees(pitchAngle)) + "°");
            // Notify listener and reset baseline to make new position the reference
            switch (currentPose) {
                case LEFT: listener.onLeft(); break;
                case RIGHT: listener.onRight(); break;
                case MIDDLE: listener.onMiddle(); break;
                case BACK: listener.onBack(); break;
            }
            yawAngle = 0f;
            pitchAngle = 0f;
            lastTsNs = 0L; // reset integration baseline
        }
    }
    
    private void advanceCalibrationPhase() {
        switch (calibrationPhase) {
            case STEADY:
                calibrationPhase = CalibrationPhase.LEFT;
                phaseStartMs = System.currentTimeMillis();
                yawAngle = 0f; // Reset angles for next phase
                pitchAngle = 0f;
                if (calibrationListener != null) {
                    calibrationListener.onCalibrationPhase("🔄 Tilt LEFT", 0xFFFFFF00); // yellow
                }
                Log.i(TAG, "Calibration: Tilt LEFT");
                break;
            case LEFT:
                calibrationPhase = CalibrationPhase.RIGHT;
                phaseStartMs = System.currentTimeMillis();
                yawAngle = 0f;
                pitchAngle = 0f;
                if (calibrationListener != null) {
                    calibrationListener.onCalibrationPhase("🔄 Tilt RIGHT", 0xFF00FF00); // green
                }
                Log.i(TAG, "Calibration: Tilt RIGHT");
                break;
            case RIGHT:
                calibrationPhase = CalibrationPhase.FORWARD;
                phaseStartMs = System.currentTimeMillis();
                yawAngle = 0f;
                pitchAngle = 0f;
                if (calibrationListener != null) {
                    calibrationListener.onCalibrationPhase("🔄 Tilt FORWARD", 0xFFFFFFFF); // white
                }
                Log.i(TAG, "Calibration: Tilt FORWARD");
                break;
            case FORWARD:
                calibrationPhase = CalibrationPhase.BACK;
                phaseStartMs = System.currentTimeMillis();
                yawAngle = 0f;
                pitchAngle = 0f;
                if (calibrationListener != null) {
                    calibrationListener.onCalibrationPhase("🔄 Tilt BACK", 0xFFFF00FF); // pink/magenta
                }
                Log.i(TAG, "Calibration: Tilt BACK");
                break;
            case BACK:
                calibrationPhase = CalibrationPhase.COMPLETE;
                calibrating = false;
                yawAngle = 0f;
                pitchAngle = 0f;
                if (calibrationListener != null) {
                    calibrationListener.onCalibrationComplete();
                }
                Log.i(TAG, "Calibration complete! Motion detection active.");
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "accuracy=" + accuracy);
    }
}
