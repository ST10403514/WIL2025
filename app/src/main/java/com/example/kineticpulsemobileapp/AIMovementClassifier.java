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

    private Interpreter tflite;
    private ByteBuffer inputBuffer;
    private float[][] outputArray;

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

    public AIMovementClassifier(Context context) {
        try {
            Log.d(TAG, "🚀 Starting AI model initialization...");

            // Debug: List assets to verify file exists
            String[] files = context.getAssets().list("");
            Log.d(TAG, "📁 Assets files: " + Arrays.toString(files));

            ByteBuffer modelBuffer = loadModelFile(context, "activity_classifier_compatible.tflite");
            Log.d(TAG, "✅ Model file loaded. Size: " + modelBuffer.capacity() + " bytes");

            // Create interpreter with SIMPLE options
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2); // Use fewer threads for compatibility

            tflite = new Interpreter(modelBuffer, options);

            // Initialize buffers
            inputBuffer = ByteBuffer.allocateDirect(WINDOW_SIZE * NUM_FEATURES * 4);
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

    public String processSensorData(float ax, float ay, float az) {
        if (!modelLoaded) {
            return "ai_not_loaded";
        }

        // Simple feature calculation
        float[] features = new float[]{
                ax, ay, az,
                (float)Math.sqrt(ax*ax + ay*ay + az*az), // magnitude
                ax, ay, az, // means (placeholder)
                0.1f, 0.1f, 0.1f // stds (placeholder)
        };

        // Add to buffer
        dataBuffer[bufferIndex] = features;
        bufferIndex = (bufferIndex + 1) % WINDOW_SIZE;

        if (bufferIndex == 0 && !bufferReady) {
            bufferReady = true;
        }

        return bufferReady ? classifyMovement() : "collecting_data";
    }

    private String classifyMovement() {
        try {
            // Prepare input
            inputBuffer.rewind();
            for (int i = 0; i < WINDOW_SIZE; i++) {
                for (int j = 0; j < NUM_FEATURES; j++) {
                    inputBuffer.putFloat(dataBuffer[i][j] / 10.0f); // Simple normalization
                }
            }

            // Run inference
            tflite.run(inputBuffer, outputArray);

            // Get results
            float[] results = outputArray[0];
            int maxIndex = 0;
            float maxConfidence = results[0];

            for (int i = 1; i < results.length; i++) {
                if (results[i] > maxConfidence) {
                    maxConfidence = results[i];
                    maxIndex = i;
                }
            }

            return maxConfidence > 0.5f ? ACTIVITY_CLASSES[maxIndex] : "uncertain";

        } catch (Exception e) {
            Log.e(TAG, "Classification error: " + e.getMessage());
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
}