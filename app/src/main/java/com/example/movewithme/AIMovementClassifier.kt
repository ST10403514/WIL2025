package com.example.movewithme

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue

class AIMovementClassifier(context: Context) {

    companion object {
        private const val TAG = "AIMovementClassifier"

        // Model parameters
        private const val WINDOW_SIZE = 50
        private const val NUM_FEATURES = 10
        private const val NUM_CLASSES = 6
        private const val CONFIDENCE_THRESHOLD = 0.2f
        private const val STABILITY_THRESHOLD = 3

        // Activity classes
        private val ACTIVITY_CLASSES = arrayOf(
            "boxing", "clapping", "running", "sitting down", "standing up", "walking"
        )
    }

    private var tflite: Interpreter? = null
    private var inputBuffer: ByteBuffer? = null
    private var outputArray: Array<FloatArray>? = null

    // Data buffers
    private val dataBuffer = Array(WINDOW_SIZE) { FloatArray(NUM_FEATURES) }
    private val recentData: Queue<FloatArray> = LinkedList()
    private var bufferIndex = 0
    private var bufferReady = false
    private var modelLoaded = false

    // Track last detected activity for stability
    private var lastDetectedActivity: String? = null
    private var stableCount = 0
    private var lastMaxConfidence = 0.0f

    init {
        try {
            Log.d(TAG, "🚀 Starting AI model initialization...")
            val modelBuffer = loadModelFile(context, "activity_classifier_compatible.tflite")
            Log.d(TAG, "✅ Model file loaded. Size: ${modelBuffer.capacity()} bytes")

            val options = Interpreter.Options()
            options.setNumThreads(2)

            tflite = Interpreter(modelBuffer, options)

            // Allocate input buffer for [1, WINDOW_SIZE, NUM_FEATURES]
            inputBuffer = ByteBuffer.allocateDirect(1 * WINDOW_SIZE * NUM_FEATURES * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            outputArray = Array(1) { FloatArray(NUM_CLASSES) }

            modelLoaded = true
            Log.d(TAG, "🎉 AI Model loaded SUCCESSFULLY!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED to load AI model: ${e.message}", e)
            modelLoaded = false
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val inputStream = context.assets.open(modelPath)
        val modelData = ByteArray(inputStream.available())
        inputStream.read(modelData)
        inputStream.close()

        val buffer = ByteBuffer.allocateDirect(modelData.size)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(modelData)
        return buffer
    }

    @Synchronized
    fun processSensorData(ax: Float, ay: Float, az: Float): String {
        if (!modelLoaded) {
            return "ai_not_loaded"
        }

        // Validate input
        if (ax.isNaN() || ay.isNaN() || az.isNaN() ||
            ax.isInfinite() || ay.isInfinite() || az.isInfinite()) {
            Log.w(TAG, "Invalid sensor data: x=$ax, y=$ay, z=$az")
            return "error"
        }

        val magnitude = kotlin.math.sqrt(ax * ax + ay * ay + az * az)
        recentData.add(floatArrayOf(ax, ay, az))
        if (recentData.size > WINDOW_SIZE) {
            recentData.poll()
        }

        val means = FloatArray(3)
        val stds = FloatArray(3)

        if (recentData.size >= WINDOW_SIZE) {
            val sums = FloatArray(3)
            val sumSquares = FloatArray(3)
            val count = recentData.size

            for (data in recentData) {
                for (i in 0..2) {
                    sums[i] += data[i]
                    sumSquares[i] += data[i] * data[i]
                }
            }

            for (i in 0..2) {
                means[i] = sums[i] / count
                stds[i] = kotlin.math.sqrt((sumSquares[i] / count) - (means[i] * means[i]))
            }
        } else {
            means[0] = ax
            means[1] = ay
            means[2] = az
            stds[0] = 0.1f
            stds[1] = 0.1f
            stds[2] = 0.1f
        }

        val features = floatArrayOf(
            ax, ay, az,
            magnitude,
            means[0], means[1], means[2],
            stds[0], stds[1], stds[2]
        )

        dataBuffer[bufferIndex] = features
        bufferIndex = (bufferIndex + 1) % WINDOW_SIZE

        if (bufferIndex == 0 && !bufferReady) {
            bufferReady = true
        }

        return if (bufferReady) classifyMovement() else "collecting_data"
    }

    private fun classifyMovement(): String {
        return try {
            val buffer = inputBuffer ?: return "error"
            buffer.rewind()

            var maxFeatureValue = 0f
            for (i in 0 until WINDOW_SIZE) {
                for (j in 0 until NUM_FEATURES) {
                    maxFeatureValue = maxOf(maxFeatureValue, kotlin.math.abs(dataBuffer[i][j]))
                }
            }
            val scale = if (maxFeatureValue > 0) 10.0f / maxFeatureValue else 1.0f

            // Fill input buffer as [1, WINDOW_SIZE, NUM_FEATURES]
            for (i in 0 until WINDOW_SIZE) {
                for (j in 0 until NUM_FEATURES) {
                    buffer.putFloat(dataBuffer[i][j] * scale)
                }
            }

            val output = outputArray ?: return "error"
            tflite?.run(buffer, output)

            val results = output[0]
            var maxIndex = 0
            var maxConfidence = results[0]

            for (i in 1 until results.size) {
                if (results[i] > maxConfidence) {
                    maxConfidence = results[i]
                    maxIndex = i
                }
            }

            Log.d(TAG, "Confidence scores: ${results.contentToString()}, Max Confidence: $maxConfidence")
            val currentActivity = ACTIVITY_CLASSES[maxIndex]
            lastMaxConfidence = maxConfidence

            if (lastDetectedActivity == null || lastDetectedActivity != currentActivity) {
                stableCount = 1
                lastDetectedActivity = currentActivity
                Log.d(TAG, "New activity detected: $currentActivity, Stable count: $stableCount")
            } else if (maxConfidence > CONFIDENCE_THRESHOLD) {
                stableCount++
                Log.d(TAG, "Stable count increased to: $stableCount for $currentActivity")
            } else {
                stableCount = 0
                Log.d(TAG, "Confidence too low, resetting stable count to: $stableCount")
            }

            if (stableCount >= STABILITY_THRESHOLD) currentActivity else "collecting_data"

        } catch (e: Exception) {
            Log.e(TAG, "Classification error: ${e.message}", e)
            "error"
        }
    }

    fun isModelLoaded(): Boolean = modelLoaded

    fun close() {
        tflite?.close()
    }

    fun getWindowSize(): Int = WINDOW_SIZE

    fun getLastMaxConfidence(): Float = lastMaxConfidence
}
