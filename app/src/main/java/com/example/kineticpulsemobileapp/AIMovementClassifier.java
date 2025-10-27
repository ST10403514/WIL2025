package com.example.kineticpulsemobileapp;

import android.content.Context;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Arrays;

public class AIMovementClassifier {
    private static final String TAG = "AIMovementClassifier";

    // Model parameters
    private static final int WINDOW_SIZE = 50;
    private static final int NUM_FEATURES = 10;
    private static final int NUM_CLASSES = 6;
    private static final float CONFIDENCE_THRESHOLD = 0.2f;
    private static final int STABILITY_THRESHOLD = 3;

    private Interpreter tflite;
    private ByteBuffer inputBuffer; // Shape: [1, WINDOW_SIZE, NUM_FEATURES]
    private float[][] outputArray;  // Shape: [1, NUM_CLASSES]

    // Activity classes
    private static final String[] ACTIVITY_CLASSES = {
            "boxing", "clapping", "running", "sitting down", "standing up", "walking"
    };

    // Data buffers
    private float[][] dataBuffer = new float[WINDOW_SIZE][NUM_FEATURES];
    private Queue<float[]> recentData = new LinkedList<>();
    private int bufferIndex = 0;
    private boolean bufferReady = false;
    private boolean modelLoaded = false;

    // Track last detected activity for stability
    private String lastDetectedActivity = null;
    private int stableCount = 0;
    private float lastMaxConfidence = 0.0f;

    public AIMovementClassifier(Context context) {
        try {
            Log.d(TAG, "🚀 Starting AI model initialization...");
            ByteBuffer modelBuffer = loadModelFile(context, "activity_classifier_compatible.tflite");
            Log.d(TAG, "✅ Model file loaded. Size: " + modelBuffer.capacity() + " bytes");

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2);

            tflite = new Interpreter(modelBuffer, options);

            // Allocate input buffer for [1, WINDOW_SIZE, NUM_FEATURES]
            inputBuffer = ByteBuffer.allocateDirect(1 * WINDOW_SIZE * NUM_FEATURES * 4);
            inputBuffer.order(ByteOrder.nativeOrder());
            outputArray = new float[1][NUM_CLASSES];

            modelLoaded = true;
            Log.d(TAG, "🎉 AI Model loaded SUCCESSFULLY!");
        } catch (Exception e) {
            Log.e(TAG, "❌ FAILED to load AI model: " + e.getMessage(), e);
            modelLoaded = false;
        }
    }

    private ByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        InputStream inputStream = context.getAssets().open(modelPath);
        byte[] modelData = new byte[inputStream.available()];
        inputStream.read(modelData);
        inputStream.close();

        ByteBuffer buffer = ByteBuffer.allocateDirect(modelData.length);
        buffer.order(ByteOrder.nativeOrder());
        buffer.put(modelData);
        return buffer;
    }

    public synchronized String processSensorData(float ax, float ay, float az) { // Synchronized for thread safety
        if (!modelLoaded) {
            return "ai_not_loaded";
        }

        // Validate input
        if (Float.isNaN(ax) || Float.isNaN(ay) || Float.isNaN(az) ||
                Float.isInfinite(ax) || Float.isInfinite(ay) || Float.isInfinite(az)) {
            Log.w(TAG, "Invalid sensor data: x=" + ax + ", y=" + ay + ", z=" + az);
            return "error";
        }

        float magnitude = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        recentData.add(new float[]{ax, ay, az});
        if (recentData.size() > WINDOW_SIZE) {
            recentData.poll();
        }

        float[] means = new float[3];
        float[] stds = new float[3];
        if (recentData.size() >= WINDOW_SIZE) {
            float[] sums = new float[3];
            float[] sumSquares = new float[3];
            int count = recentData.size();

            for (float[] data : recentData) {
                for (int i = 0; i < 3; i++) {
                    sums[i] += data[i];
                    sumSquares[i] += data[i] * data[i];
                }
            }

            for (int i = 0; i < 3; i++) {
                means[i] = sums[i] / count;
                stds[i] = (float) Math.sqrt((sumSquares[i] / count) - (means[i] * means[i]));
            }
        } else {
            means = new float[]{ax, ay, az};
            stds = new float[]{0.1f, 0.1f, 0.1f};
        }

        float[] features = new float[]{
                ax, ay, az,
                magnitude,
                means[0], means[1], means[2],
                stds[0], stds[1], stds[2]
        };

        dataBuffer[bufferIndex] = features;
        bufferIndex = (bufferIndex + 1) % WINDOW_SIZE;

        if (bufferIndex == 0 && !bufferReady) {
            bufferReady = true;
        }

        return bufferReady ? classifyMovement() : "collecting_data";
    }

    private String classifyMovement() {
        try {
            inputBuffer.rewind();
            float maxFeatureValue = 0;
            for (int i = 0; i < WINDOW_SIZE; i++) {
                for (int j = 0; j < NUM_FEATURES; j++) {
                    maxFeatureValue = Math.max(maxFeatureValue, Math.abs(dataBuffer[i][j]));
                }
            }
            float scale = (maxFeatureValue > 0) ? 10.0f / maxFeatureValue : 1.0f;

            // Fill input buffer as [1, WINDOW_SIZE, NUM_FEATURES]
            for (int i = 0; i < WINDOW_SIZE; i++) {
                for (int j = 0; j < NUM_FEATURES; j++) {
                    inputBuffer.putFloat(dataBuffer[i][j] * scale);
                }
            }

            tflite.run(inputBuffer, outputArray);

            float[] results = outputArray[0];
            int maxIndex = 0;
            float maxConfidence = results[0];

            for (int i = 1; i < results.length; i++) {
                if (results[i] > maxConfidence) {
                    maxConfidence = results[i];
                    maxIndex = i;
                }
            }

            Log.d(TAG, "Confidence scores: " + Arrays.toString(results) + ", Max Confidence: " + maxConfidence);
            String currentActivity = ACTIVITY_CLASSES[maxIndex];
            lastMaxConfidence = maxConfidence;

            if (lastDetectedActivity == null || !lastDetectedActivity.equals(currentActivity)) {
                stableCount = 1;
                lastDetectedActivity = currentActivity;
                Log.d(TAG, "New activity detected: " + currentActivity + ", Stable count: " + stableCount);
            } else if (maxConfidence > CONFIDENCE_THRESHOLD) {
                stableCount++;
                Log.d(TAG, "Stable count increased to: " + stableCount + " for " + currentActivity);
            } else {
                stableCount = 0;
                Log.d(TAG, "Confidence too low, resetting stable count to: " + stableCount);
            }

            return (stableCount >= STABILITY_THRESHOLD) ? currentActivity : "collecting_data";

        } catch (Exception e) {
            Log.e(TAG, "Classification error: " + e.getMessage(), e);
            return "error";
        }
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }

    public int getWindowSize() {
        return WINDOW_SIZE;
    }

    public float getLastMaxConfidence() {
        return lastMaxConfidence;
    }
}