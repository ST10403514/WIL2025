package com.example.movewithme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GyroManager(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "Gyro"
        private const val CALIBRATION_MS = 2000L
        private const val PHASE_DURATION_MS = 1500L // Increased to 1500ms to allow time for larger movements
    }

    interface MovementListener {
        fun onLeft()
        fun onRight()
        fun onMiddle() // up/forward
        fun onBack()   // backward tilt
        fun onRaw(x: Float, y: Float, z: Float) {}
    }

    interface CalibrationListener {
        fun onCalibrationStart()
        fun onCalibrationPhase(instruction: String, color: Int)
        fun onCalibrationComplete()
    }

    private val sensorManager: SensorManager? = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyro: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var started = false
    private var firstLogs = 0
    @Volatile
    private var processingEnabled = false
    private var listener: MovementListener? = null
    private var calibrationListener: CalibrationListener? = null
    private var lastEventMs = 0L
    private var lastTsNs = 0L
    private var yawAngle = 0f   // integrate z (yaw)
    private var pitchAngle = 0f // integrate x (pitch)

    private enum class Pose { LEFT, MIDDLE, RIGHT, BACK }
    private var currentPose = Pose.MIDDLE
    private var calibrating = false
    private var calibStartMs = 0L

    // Interactive calibration states
    private enum class CalibrationPhase { NONE, STEADY, LEFT, RIGHT, FORWARD, BACK, COMPLETE }
    private var calibrationPhase = CalibrationPhase.NONE
    private var phaseStartMs = 0L

    init {
        if (gyro == null) {
            Log.w(TAG, "Gyroscope not available")
        } else {
            Log.i(TAG, "Gyroscope initialized: ${gyro.name}")
        }
    }

    fun isAvailable(): Boolean {
        return gyro != null && sensorManager != null
    }

    fun start() {
        if (!isAvailable()) {
            Log.w(TAG, "start called but gyroscope unavailable")
            return
        }
        if (started) {
            Log.d(TAG, "start ignored (already started)")
            return
        }
        started = true
        firstLogs = 0
        // Use a fast but safe sampling rate (5ms = 200Hz)
        val periodUs = 5000 // 5ms intervals for fast response without requiring special permissions
        val ok = sensorManager!!.registerListener(this, gyro, periodUs, periodUs)
        Log.i(TAG, if (ok) "Gyro started" else "Failed to start Gyro listener")
    }

    fun stop() {
        if (!started || sensorManager == null) return
        sensorManager.unregisterListener(this)
        started = false
        Log.i(TAG, "Gyro stopped")
    }

    fun setMovementListener(l: MovementListener?) {
        this.listener = l
    }

    fun setCalibrationListener(l: CalibrationListener?) {
        this.calibrationListener = l
    }

    fun setProcessingEnabled(enabled: Boolean) {
        processingEnabled = enabled
        Log.i(TAG, "processingEnabled=$enabled")
        if (enabled) {
            // Start interactive calibration sequence
            startInteractiveCalibration()
        } else {
            calibrationPhase = CalibrationPhase.NONE
            calibrating = false
            currentPose = Pose.MIDDLE // Reset to middle position
        }
    }

    private fun startInteractiveCalibration() {
        calibrationPhase = CalibrationPhase.STEADY
        phaseStartMs = System.currentTimeMillis()
        calibrating = true
        yawAngle = 0f
        pitchAngle = 0f
        lastTsNs = 0L
        currentPose = Pose.MIDDLE
        calibrationListener?.apply {
            onCalibrationStart()
            onCalibrationPhase("Hold device steady", 0xFFFFFFFF.toInt()) // white
        }
        Log.i(TAG, "Starting interactive calibration - hold device steady")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        if (firstLogs < 3) {
            firstLogs++
            Log.d(TAG, String.format("event #%d x=%.4f y=%.4f z=%.4f t=%d", firstLogs, event.values[0], event.values[1], event.values[2], event.timestamp))
        }
        if (!processingEnabled || listener == null) return

        val x = event.values[0] // pitch rate
        val y = event.values[1] // roll rate (unused in detection)
        val z = event.values[2] // yaw rate
        listener?.onRaw(x, y, z)

        // Integrate angular velocity into approximate angles (radians)
        val ts = event.timestamp // ns
        if (lastTsNs == 0L) {
            lastTsNs = ts
            return
        }
        val dt = (ts - lastTsNs) / 1_000_000_000f // seconds
        lastTsNs = ts
        yawAngle += z * dt
        pitchAngle += x * dt

        // Handle interactive calibration phases
        if (calibrating && calibrationPhase != CalibrationPhase.COMPLETE) {
            val phaseElapsed = System.currentTimeMillis() - phaseStartMs
            if (phaseElapsed >= PHASE_DURATION_MS) {
                advanceCalibrationPhase()
            }
            return // Don't process movements during calibration
        }

        // Clamp to avoid runaway drift during long inactivity
        val MAX_ANGLE = Math.toRadians(90.0).toFloat()
        yawAngle = max(-MAX_ANGLE, min(MAX_ANGLE, yawAngle))
        pitchAngle = max(-MAX_ANGLE, min(MAX_ANGLE, pitchAngle))

        // Determine pose from angles - ADJUSTED for ~0.5m movement requirement
        val YAW_THRESH = Math.toRadians(25.0).toFloat()    // left/right (increased to 25 degrees for less sensitivity)
        val PITCH_THRESH = Math.toRadians(25.0).toFloat()  // forward/back (increased to 25 degrees for less sensitivity)

        var newPose = Pose.MIDDLE // Default to middle/neutral

        // Priority: check for stronger movements first
        if (abs(yawAngle) > abs(pitchAngle)) {
            // Horizontal movement is stronger
            when {
                yawAngle > YAW_THRESH -> newPose = Pose.RIGHT
                yawAngle < -YAW_THRESH -> newPose = Pose.LEFT
            }
        } else {
            // Vertical movement is stronger
            when {
                pitchAngle > PITCH_THRESH -> newPose = Pose.MIDDLE // forward tilt (jumping up/forward)
                pitchAngle < -PITCH_THRESH -> newPose = Pose.BACK // backward tilt
            }
        }

        val now = System.currentTimeMillis()
        val DEBOUNCE_MS = 500L // Increased to 500ms for better debounce with larger movements
        val MIN_MOVEMENT = Math.toRadians(15.0).toFloat() // Increased to 15 degrees to match less sensitive detection

        // Only trigger if we have significant movement and enough time has passed
        val hasSignificantMovement = abs(yawAngle) > MIN_MOVEMENT || abs(pitchAngle) > MIN_MOVEMENT

        if (newPose != currentPose && hasSignificantMovement && now - lastEventMs > DEBOUNCE_MS) {
            currentPose = newPose
            lastEventMs = now
            Log.d(TAG, "pose=$currentPose yaw=${String.format("%.1f", Math.toDegrees(yawAngle.toDouble()))}° pitch=${String.format("%.1f", Math.toDegrees(pitchAngle.toDouble()))}°")
            // Notify listener and reset baseline to make new position the reference
            when (currentPose) {
                Pose.LEFT -> listener?.onLeft()
                Pose.RIGHT -> listener?.onRight()
                Pose.MIDDLE -> listener?.onMiddle()
                Pose.BACK -> listener?.onBack()
            }
            yawAngle = 0f
            pitchAngle = 0f
            lastTsNs = 0L // reset integration baseline
        }
    }

    private fun advanceCalibrationPhase() {
        when (calibrationPhase) {
            CalibrationPhase.STEADY -> {
                calibrationPhase = CalibrationPhase.LEFT
                phaseStartMs = System.currentTimeMillis()
                yawAngle = 0f // Reset angles for next phase
                pitchAngle = 0f
                calibrationListener?.onCalibrationPhase("🔄 Tilt LEFT", 0xFFFFFF00.toInt()) // yellow
                Log.i(TAG, "Calibration: Tilt LEFT")
            }
            CalibrationPhase.LEFT -> {
                calibrationPhase = CalibrationPhase.RIGHT
                phaseStartMs = System.currentTimeMillis()
                yawAngle = 0f
                pitchAngle = 0f
                calibrationListener?.onCalibrationPhase("🔄 Tilt RIGHT", 0xFF00FF00.toInt()) // green
                Log.i(TAG, "Calibration: Tilt RIGHT")
            }
            CalibrationPhase.RIGHT -> {
                calibrationPhase = CalibrationPhase.FORWARD
                phaseStartMs = System.currentTimeMillis()
                yawAngle = 0f
                pitchAngle = 0f
                calibrationListener?.onCalibrationPhase("🔄 Tilt FORWARD", 0xFFFFFFFF.toInt()) // white
                Log.i(TAG, "Calibration: Tilt FORWARD")
            }
            CalibrationPhase.FORWARD -> {
                calibrationPhase = CalibrationPhase.BACK
                phaseStartMs = System.currentTimeMillis()
                yawAngle = 0f
                pitchAngle = 0f
                calibrationListener?.onCalibrationPhase("🔄 Tilt BACK", 0xFFFF00FF.toInt()) // pink/magenta
                Log.i(TAG, "Calibration: Tilt BACK")
            }
            CalibrationPhase.BACK -> {
                calibrationPhase = CalibrationPhase.COMPLETE
                calibrating = false
                yawAngle = 0f
                pitchAngle = 0f
                calibrationListener?.onCalibrationComplete()
                Log.i(TAG, "Calibration complete! Motion detection active.")
            }
            else -> {}
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d(TAG, "accuracy=$accuracy")
    }
}
