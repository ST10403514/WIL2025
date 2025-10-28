package com.example.kineticpulsemobileapp;

import android.widget.VideoView;
import android.net.Uri;
import android.media.MediaPlayer;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.content.pm.PackageManager;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TerminalFragment extends Fragment {

    private enum SensorMode { PHONE_GYRO, ESP32_ADXL345, AUTO_DETECT }
    private enum CharacterType { GIRL, LION, BOY }

    private CharacterType selectedCharacter = CharacterType.BOY;
    private FirebaseAuth auth;
    private String deviceAddress;
    private SensorMode currentSensorMode = SensorMode.AUTO_DETECT;

    // BLE Manager replaces old SerialService
    private BLEManager bleManager;
    private boolean bleConnected = false;

    // Character animation mappings
    private Map<String, String> characterAnimations;
    private Map<String, List<String>> characterAnimationsList;

    private AIMovementClassifier aiClassifier;
    private Button btnTestAI;
    private TextView tvAIStatus;

    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private boolean gyroEnabled = false;

    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private final Runnable reconnectRunnable = () -> {
        if (!bleConnected) connectBLE();
    };

    private int jumpLeft = 0;
    private int jumpRight = 0;
    private int jumpUp = 0;
    private int jumpBack = 0;
    private String selectedMovement = null;
    private boolean isManualModeActive = false;
    private boolean isCollectingSamples = false;

    private long lastMovementTime = 0;
    private static final long MOVEMENT_COOLDOWN = 1500;

    private static final int ACHIEVEMENT_10 = 10;
    private static final int ACHIEVEMENT_20 = 20;
    private static final int ACHIEVEMENT_50 = 50;
    private static final int ACHIEVEMENT_100 = 100;
    private boolean achievement10Shown = false;
    private boolean achievement20Shown = false;
    private boolean achievement50Shown = false;
    private boolean achievement100Shown = false;

    // UI Elements
    private Button btnWhite, btnRed, btnBlue, btnGreen, btnTopaz, btnLilac;
    private Button btnRainbow, btnSeizureMode, btnLEDOff;
    private Button btnModeToggle, btnMusicPlayer, btnConnectBLE;
    private VideoView vvMovementVideo;
    private TextView tvJumpLeft, tvJumpRight, tvJumpMiddle, tvJumpBack;
    private TextView tvLastMovement, tvAchievementBadge;
    private ImageView ivCharacterPlaceholder;
    private TextView tvCharacterName, tvBLEStatus;

    private boolean autoDetectionEnabled = true;
    private final Handler autoDetectionHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoDetectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (autoDetectionEnabled && currentSensorMode == SensorMode.AUTO_DETECT && bleConnected) {
                autoDetectionHandler.postDelayed(this, 5000);
            }
        }
    };

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        auth = FirebaseAuth.getInstance();

        // Initialize BLE Manager
        bleManager = new BLEManager(requireContext(), bleCallback);

        Bundle args = getArguments();
        deviceAddress = args != null ? args.getString("device") : null;

        if (args != null && args.containsKey("character")) {
            String character = args.getString("character");
            setCharacterFromString(character);
        } else {
            loadCharacterFromPreferences();
        }

        initializeCharacterAnimations();
        requestBLEPermissions();
    }

    private void requestBLEPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            requestPermissions(permissions, 100);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }

    private final BLEManager.BLECallback bleCallback = new BLEManager.BLECallback() {
        @Override
        public void onDeviceFound(BluetoothDevice device, String name) {
            Log.d("TerminalFragment", "Found device: " + name);
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "Found: " + name, Toast.LENGTH_SHORT).show();
                    updateBLEStatus("Connecting...", "#FFA500");
                    bleManager.connectToDevice(device);
                });
            }
        }

        @Override
        public void onConnected() {
            bleConnected = true;
            Log.d("TerminalFragment", "BLE Connected");
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "Connected to ESP32", Toast.LENGTH_SHORT).show();
                    updateBLEStatus("Connected", "#4CAF50");
                    updateLastMovementText("BLE Connected", "#4CAF50");
                    if (currentSensorMode == SensorMode.AUTO_DETECT) {
                        startAutoDetection();
                    }
                    if (btnConnectBLE != null) {
                        btnConnectBLE.setText("Connected");
                        btnConnectBLE.setEnabled(false);
                    }
                });
            }
        }

        @Override
        public void onDisconnected() {
            bleConnected = false;
            Log.d("TerminalFragment", "BLE Disconnected");
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "Disconnected", Toast.LENGTH_SHORT).show();
                    updateBLEStatus("Disconnected", "#FF0000");
                    updateLastMovementText("Disconnected", "#FF0000");
                    if (btnConnectBLE != null) {
                        btnConnectBLE.setText("Reconnect");
                        btnConnectBLE.setEnabled(true);
                    }
                    // Auto-reconnect after 2 seconds
                    reconnectHandler.postDelayed(reconnectRunnable, 2000);
                });
            }
        }

        @Override
        public void onDataReceived(String data) {
            Log.d("TerminalFragment", "Data: " + data);
            parseIncomingBLEData(data);
        }

        @Override
        public void onError(String error) {
            Log.e("TerminalFragment", "BLE Error: " + error);
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "Error: " + error, Toast.LENGTH_SHORT).show();
                    updateBLEStatus("Error", "#FF0000");
                });
            }
        }
    };

    private void parseIncomingBLEData(String data) {
        try {
            // Try parsing as JSON first (ESP32 sends JSON)
            if (data.trim().startsWith("{")) {
                JSONObject json = new JSONObject(data);
                double ax = json.optDouble("ax", 0.0);
                double ay = json.optDouble("ay", 0.0);
                double az = json.optDouble("az", 0.0);

                Log.d("TerminalFragment", String.format("Accel: x=%.2f, y=%.2f, z=%.2f", ax, ay, az));

                // Process accelerometer data
                processAccelerometerData((float)ax, (float)ay, (float)az);

            } else if (data.startsWith("ACCEL:")) {
                // Legacy format support
                parseAccelerometerData(data);
            } else if (data.startsWith("MOVE:")) {
                parseMovementCommand(data);
            }
        } catch (Exception e) {
            Log.e("TerminalFragment", "Parse error: " + e.getMessage());
        }
    }

    private void processAccelerometerData(float ax, float ay, float az) {
        // Feed to AI classifier
        processWithAI(ax, ay, az);

        // Simple threshold-based movement detection
        final float THRESHOLD = 2.0f;

        if (Math.abs(ax) > THRESHOLD) {
            if (ax > THRESHOLD) handleRightMovement();
            else handleLeftMovement();
        } else if (Math.abs(ay) > THRESHOLD) {
            if (ay > THRESHOLD) handleForwardMovement();
            else handleBackMovement();
        }
    }

    private void updateBLEStatus(String status, String colorHex) {
        if (tvBLEStatus != null) {
            tvBLEStatus.setText(status);
            tvBLEStatus.setTextColor(android.graphics.Color.parseColor(colorHex));
        }
    }

    private void setCharacterFromString(String character) {
        if (character == null) {
            selectedCharacter = CharacterType.BOY;
            return;
        }

        switch (character.toLowerCase()) {
            case "girl":
                selectedCharacter = CharacterType.GIRL;
                break;
            case "lion":
                selectedCharacter = CharacterType.LION;
                break;
            case "boy":
            default:
                selectedCharacter = CharacterType.BOY;
                break;
        }

        Log.i("TerminalFragment", "Character selected: " + selectedCharacter);
    }

    private void loadCharacterFromPreferences() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("KineticPulsePrefs", Context.MODE_PRIVATE);
            String savedCharacter = prefs.getString("selected_character", "boy");
            setCharacterFromString(savedCharacter);
            Log.i("TerminalFragment", "Loaded character from preferences: " + savedCharacter);
        } catch (Exception e) {
            Log.e("TerminalFragment", "Error loading character from preferences: " + e.getMessage());
            selectedCharacter = CharacterType.BOY;
        }
    }

    private void initializeCharacterAnimations() {
        characterAnimations = new HashMap<>();
        characterAnimationsList = new HashMap<>();

        // GIRL animations
        characterAnimations.put("GIRL_LEFT", "girl_skip_left");
        characterAnimations.put("GIRL_RIGHT", "girl_skip_right");
        characterAnimations.put("GIRL_UP", "girl_jump");
        characterAnimations.put("GIRL_BACK", "girl_1foot");
        characterAnimations.put("GIRL_SPECIAL", "girl_jump_clap");
        characterAnimations.put("GIRL_DANCE", "girl_jump_clap");

        List<String> girlAnimations = new ArrayList<>();
        girlAnimations.add("girl_1foot");
        girlAnimations.add("girl_jump");
        girlAnimations.add("girl_skip_left");
        girlAnimations.add("girl_skip_right");
        girlAnimations.add("girl_jump_clap");
        characterAnimationsList.put("GIRL", girlAnimations);

        // LION animations
        characterAnimations.put("LION_LEFT", "lion_jump_left");
        characterAnimations.put("LION_RIGHT", "lion_skip_right");
        characterAnimations.put("LION_UP", "lion_jump_up");
        characterAnimations.put("LION_BACK", "lion_march");
        characterAnimations.put("LION_SPECIAL", "lion_dance");
        characterAnimations.put("LION_CLAP", "lion_clap");
        characterAnimations.put("LION_DANCE", "lion_dance");

        List<String> lionAnimations = new ArrayList<>();
        lionAnimations.add("lion_jump_up");
        lionAnimations.add("lion_clap");
        lionAnimations.add("lion_hop_right");
        lionAnimations.add("lion_march");
        lionAnimations.add("lion_jump_left");
        lionAnimations.add("lion_dance");
        lionAnimations.add("lion_skip_right");
        characterAnimationsList.put("LION", lionAnimations);

        // BOY animations
        characterAnimations.put("BOY_LEFT", "video_left");
        characterAnimations.put("BOY_RIGHT", "video_right");
        characterAnimations.put("BOY_UP", "video_up");
        characterAnimations.put("BOY_BACK", "video_jump");
        characterAnimations.put("BOY_DANCE", "video_dance");

        List<String> boyAnimations = new ArrayList<>();
        boyAnimations.add("video_left");
        boyAnimations.add("video_right");
        boyAnimations.add("video_up");
        boyAnimations.add("video_dance");
        characterAnimationsList.put("BOY", boyAnimations);
    }

    private String getCharacterAnimation(String movement) {
        String key = selectedCharacter.name() + "_" + movement.toUpperCase();
        String animation = characterAnimations.get(key);

        if (animation == null) {
            animation = characterAnimations.get("BOY_" + movement.toUpperCase());
        }

        Log.d("TerminalFragment", "Animation key: " + key + " -> " + animation);
        return animation;
    }

    @Override
    public void onDestroy() {
        if (bleManager != null) {
            bleManager.close();
        }
        reconnectHandler.removeCallbacksAndMessages(null);
        autoDetectionHandler.removeCallbacksAndMessages(null);

        if (aiClassifier != null) {
            aiClassifier.close();
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (gyroEnabled && currentSensorMode == SensorMode.PHONE_GYRO) {
            Activity a = getActivity();
            if (a instanceof MainActivity) {
                GyroManager gm = ((MainActivity) a).getGyroManager();
                if (gm != null && gm.isAvailable()) {
                    gm.setMovementListener(gyroListener);
                    gm.setProcessingEnabled(true);
                    gm.start();
                }
            }
        }

        if (currentSensorMode == SensorMode.AUTO_DETECT && !bleConnected) {
            handler.postDelayed(() -> connectBLE(), 1000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        autoDetectionHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);

        aiClassifier = new AIMovementClassifier(getActivity());

        tvAIStatus = view.findViewById(R.id.tvAIStatus);
        tvBLEStatus = view.findViewById(R.id.tvBLEStatus);
        btnConnectBLE = view.findViewById(R.id.btnConnectBLE);

        updateAIStatus();

        btnWhite = view.findViewById(R.id.btnWhite);
        btnRed = view.findViewById(R.id.btnRed);
        btnGreen = view.findViewById(R.id.btnGreen);
        btnBlue = view.findViewById(R.id.btnBlue);
        btnTopaz = view.findViewById(R.id.btnTopaz);
        btnLilac = view.findViewById(R.id.btnLilac);
        btnRainbow = view.findViewById(R.id.btnRainbow);
        btnSeizureMode = view.findViewById(R.id.btnSeizureMode);
        btnLEDOff = view.findViewById(R.id.btnLEDOff);
        btnModeToggle = view.findViewById(R.id.btnModeToggle);
        btnMusicPlayer = view.findViewById(R.id.btnMusicPlayer);
        ivCharacterPlaceholder = view.findViewById(R.id.ivCharacterPlaceholder);

        vvMovementVideo = view.findViewById(R.id.vvMovementVideo);
        tvJumpLeft = view.findViewById(R.id.tvJumpLeft);
        tvJumpRight = view.findViewById(R.id.tvJumpRight);
        tvJumpMiddle = view.findViewById(R.id.tvJumpMiddle);
        tvJumpBack = view.findViewById(R.id.tvJumpBack);
        tvLastMovement = view.findViewById(R.id.tvLastMovement);
        tvAchievementBadge = view.findViewById(R.id.tvAchievementBadge);
        tvCharacterName = view.findViewById(R.id.tvCharacterName);

        setupCharacterDisplay();

        // LED buttons - send via BLE
        btnWhite.setOnClickListener(v -> sendBLECommand("w"));
        btnRed.setOnClickListener(v -> sendBLECommand("r"));
        btnBlue.setOnClickListener(v -> sendBLECommand("b"));
        btnGreen.setOnClickListener(v -> sendBLECommand("g"));
        btnTopaz.setOnClickListener(v -> sendBLECommand("t"));
        btnLilac.setOnClickListener(v -> sendBLECommand("l"));
        btnRainbow.setOnClickListener(v -> sendBLECommand("a"));
        btnSeizureMode.setOnClickListener(v -> sendBLECommand("m"));
        btnLEDOff.setOnClickListener(v -> sendBLECommand("o"));

        btnModeToggle.setOnClickListener(v -> toggleSensorMode());
        updateModeToggleButton();

        btnMusicPlayer.setOnClickListener(v -> openMusicAppChooser());

        // BLE Connect button
        if (btnConnectBLE != null) {
            btnConnectBLE.setOnClickListener(v -> connectBLE());
        }

        Button btnJumpLeftSelect = view.findViewById(R.id.btnJumpLeftSelect);
        Button btnJumpRightSelect = view.findViewById(R.id.btnJumpRightSelect);
        Button btnJumpUpSelect = view.findViewById(R.id.btnJumpUpSelect);
        Button btnJumpFrontSelect = view.findViewById(R.id.btnJumpFrontSelect);

        btnJumpLeftSelect.setOnClickListener(v -> selectMovement("LEFT"));
        btnJumpRightSelect.setOnClickListener(v -> selectMovement("RIGHT"));
        btnJumpUpSelect.setOnClickListener(v -> selectMovement("UP"));
        btnJumpFrontSelect.setOnClickListener(v -> selectMovement("DANCE"));

        return view;
    }

    private void connectBLE() {
        if (!bleManager.isBluetoothEnabled()) {
            Toast.makeText(getContext(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Scanning for ESP32...", Toast.LENGTH_SHORT).show();
        updateBLEStatus("Scanning...", "#FFA500");
        bleManager.startScan();
    }

    private void sendBLECommand(String command) {
        if (bleConnected && bleManager != null) {
            bleManager.sendCommand(command);
            Log.d("TerminalFragment", "Sent: " + command);
        } else {
            Toast.makeText(getContext(), "Not connected to ESP32", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendIfConnected(String str) {
        sendBLECommand(str);
    }

    private void setupCharacterDisplay() {
        if (tvCharacterName != null) {
            String charName = selectedCharacter.name();
            tvCharacterName.setText(charName);
        }

        showIdleAnimation();
    }

    private void showIdleAnimation() {
        String idleAnimation = getCharacterAnimation("LEFT");
        if (idleAnimation != null) {
            setCharacterPlaceholderImage();
            Log.d("TerminalFragment", "Setting up character: " + selectedCharacter + " with idle: " + idleAnimation);
        }
    }

    private void setCharacterPlaceholderImage() {
        if (ivCharacterPlaceholder == null) return;

        int placeholderResId;
        switch (selectedCharacter) {
            case GIRL:
                placeholderResId = R.drawable.character_girl;
                break;
            case LION:
                placeholderResId = R.drawable.character_lion;
                break;
            case BOY:
            default:
                placeholderResId = R.drawable.character_boy;
                break;
        }

        try {
            ivCharacterPlaceholder.setImageResource(placeholderResId);
        } catch (Exception e) {
            Log.e("TerminalFragment", "Error setting placeholder image: " + e.getMessage());
            ivCharacterPlaceholder.setImageResource(R.drawable.character_placeholder);
        }
    }

    // ========== AI CLASSIFIER METHODS ==========

    private void selectMovement(String movement) {
        selectedMovement = movement;
        isManualModeActive = true;
        isCollectingSamples = true;

        String animationFile = getCharacterAnimation(movement);
        if (animationFile != null) {
            showMovementVideo(animationFile);
        }

        if (tvAIStatus != null) {
            tvAIStatus.setText("Selected: " + movement + " - Collecting...");
        }

        Log.i("TerminalFragment", "Manual mode: " + movement + " with " + selectedCharacter);
        startSampleCollection(movement);
    }

    private void startSampleCollection(String movement) {
        new Thread(() -> {
            if (aiClassifier == null || !aiClassifier.isModelLoaded()) {
                updateStatus("AI Model not loaded");
                isCollectingSamples = false;
                return;
            }

            float[] baseData = getTestDataForMovement(movement);
            if (baseData == null) {
                updateStatus("Invalid movement: " + movement);
                isCollectingSamples = false;
                return;
            }

            int samplesCollected = 0;
            final int TOTAL_SAMPLES = 50;

            synchronized (aiClassifier) {
                while (samplesCollected < TOTAL_SAMPLES && isCollectingSamples) {
                    float noiseX = (float) (Math.random() * 0.4 - 0.2);
                    float noiseY = (float) (Math.random() * 0.4 - 0.2);
                    float noiseZ = (float) (Math.random() * 0.4 - 0.2);

                    aiClassifier.processSensorData(
                            baseData[0] + noiseX,
                            baseData[1] + noiseY,
                            baseData[2] + noiseZ
                    );

                    samplesCollected++;

                    if (samplesCollected % 10 == 0) {
                        updateStatus(movement + ": " + samplesCollected + "/" + TOTAL_SAMPLES);
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                String detectedActivity = "collecting_data";
                float confidence = 0.0f;
                int attempts = 0;

                while ((detectedActivity.equals("collecting_data") || detectedActivity.equals("uncertain"))
                        && attempts < 5) {
                    detectedActivity = aiClassifier.processSensorData(baseData[0], baseData[1], baseData[2]);
                    confidence = aiClassifier.getLastMaxConfidence();
                    attempts++;

                    if (!detectedActivity.equals("collecting_data")) {
                        break;
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                String mappedMovement = mapAIResultToMovement(detectedActivity, baseData[0]);

                String finalStatus = String.format("%s detected as %s (%.0f%% confidence)",
                        movement, mappedMovement, confidence * 100);
                updateStatus(finalStatus);

                isCollectingSamples = false;

                executeMovementAction(movement);
            }
        }).start();
    }

    private float[] getTestDataForMovement(String movement) {
        switch (movement.toUpperCase()) {
            case "UP":
                return new float[]{0.3f, 2.5f, 9.8f};
            case "LEFT":
                return new float[]{8.0f, 1.0f, 9.5f};
            case "RIGHT":
                return new float[]{-8.0f, 1.0f, 9.5f};
            case "DANCE":
                return new float[]{0.5f, 4.0f, 4.0f};
            default:
                return null;
        }
    }

    private String mapAIResultToMovement(String aiResult, float xAccel) {
        if (aiResult == null || aiResult.isEmpty()) {
            return "NONE";
        }

        Log.d("TerminalFragment", "AI Result: " + aiResult + ", xAccel: " + xAccel);

        switch (aiResult.toLowerCase()) {
            case "walking":
            case "running":
                return "UP";
            case "boxing":
                return (xAccel > 0) ? "LEFT" : "RIGHT";
            case "clapping":
                return "DANCE";
            case "sitting down":
            case "standing up":
                return "UP";
            default:
                Log.w("TerminalFragment", "Unmapped AI result: " + aiResult);
                return "NONE";
        }
    }

    private void executeMovementAction(String movement) {
        Activity activity = getActivity();
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            switch (movement.toUpperCase()) {
                case "UP":
                    handleForwardMovement();
                    break;
                case "LEFT":
                    handleLeftMovement();
                    break;
                case "RIGHT":
                    handleRightMovement();
                    break;
                case "DANCE":
                    handleDanceMovement();
                    playSpecialAnimation();
                    sendIfConnected("a");
                    Toast.makeText(getContext(), "Dance!", Toast.LENGTH_SHORT).show();
                    break;
            }

            updateLastMovementText(movement, "#4CAF50");
        });
    }

    private void updateStatus(String message) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (tvAIStatus != null) {
                    tvAIStatus.setText(message);
                }
                Log.d("TerminalFragment", message);
            });
        }
    }

    private void handleAIDetection(String activity) {
        if (isCollectingSamples) return;

        Log.i("TerminalFragment", "AI Detected: " + activity);

        String mappedMovement = mapAIResultToMovement(activity, 0);

        if (isManualModeActive) {
            boolean shouldExecute = false;

            if (selectedMovement.equals(mappedMovement)) {
                shouldExecute = true;
            } else if (selectedMovement.equals("UP") &&
                    (mappedMovement.equals("FORWARD") || activity.toLowerCase().contains("walk") || activity.toLowerCase().contains("run"))) {
                shouldExecute = true;
            } else if ((selectedMovement.equals("LEFT") || selectedMovement.equals("RIGHT")) &&
                    activity.toLowerCase().contains("box")) {
                shouldExecute = true;
            } else if (selectedMovement.equals("DANCE") &&
                    activity.toLowerCase().contains("clap")) {
                shouldExecute = true;
            }

            if (shouldExecute) {
                executeMovementAction(selectedMovement);
            }
        }

        updateLastMovementText(mappedMovement, "#FF6B6B");
    }

    private void processWithAI(float ax, float ay, float az) {
        if (isCollectingSamples) return;

        if (aiClassifier != null && aiClassifier.isModelLoaded()) {
            if (Float.isNaN(ax) || Float.isNaN(ay) || Float.isNaN(az) ||
                    Float.isInfinite(ax) || Float.isInfinite(ay) || Float.isInfinite(az)) {
                return;
            }

            String detectedActivity = aiClassifier.processSensorData(ax, ay, az);

            if (!detectedActivity.equals("collecting_data") &&
                    !detectedActivity.equals("uncertain") &&
                    !detectedActivity.equals("error") &&
                    !detectedActivity.equals("ai_not_loaded")) {
                handleAIDetection(detectedActivity);
            }
        }
    }

    private void updateAIStatus() {
        if (aiClassifier != null && tvAIStatus != null) {
            String status = aiClassifier.isModelLoaded() ?
                    "AI Ready" : "AI Failed";
            tvAIStatus.setText(status);
        }
    }

    private void playSpecialAnimation() {
        String specialAnim = getCharacterAnimation("SPECIAL");
        if (specialAnim == null && selectedCharacter == CharacterType.LION) {
            specialAnim = getCharacterAnimation("CLAP");
        }
        if (specialAnim != null) {
            showMovementVideo(specialAnim);
        }
    }

    // ========== SENSOR MODE & UI METHODS ==========

    private void toggleSensorMode() {
        switch (currentSensorMode) {
            case AUTO_DETECT:
                currentSensorMode = SensorMode.ESP32_ADXL345;
                stopAutoDetection();
                Toast.makeText(getActivity(), "Switched to ESP32 Mode", Toast.LENGTH_SHORT).show();
                updateLastMovementText("ESP32 active", "#667eea");
                break;
            case ESP32_ADXL345:
                currentSensorMode = SensorMode.PHONE_GYRO;
                gyroEnabled = true;
                initializePhoneGyroSensors();
                Toast.makeText(getActivity(), "Switched to Phone Gyroscope", Toast.LENGTH_SHORT).show();
                updateLastMovementText("Phone gyroscope active", "#667eea");
                break;
            case PHONE_GYRO:
                currentSensorMode = SensorMode.AUTO_DETECT;
                gyroEnabled = false;
                if (sensorManager != null && phoneSensorListener != null) {
                    sensorManager.unregisterListener(phoneSensorListener);
                }
                startAutoDetection();
                Toast.makeText(getActivity(), "Switched to Auto Detect", Toast.LENGTH_SHORT).show();
                updateLastMovementText("Auto detection active", "#667eea");
                break;
        }
        updateModeToggleButton();
    }

    private void updateModeToggleButton() {
        if (btnModeToggle == null) return;

        String text;
        switch (currentSensorMode) {
            case AUTO_DETECT:
                text = "Auto Detect";
                break;
            case PHONE_GYRO:
                text = "Phone Gyro";
                break;
            case ESP32_ADXL345:
                text = "ESP32";
                break;
            default:
                text = "Unknown";
                break;
        }

        btnModeToggle.setText(text);
    }

    private void openMusicAppChooser() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_MUSIC);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "Choose Music App"));
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("audio/*");
                startActivity(Intent.createChooser(intent, "Choose Music App"));
            } catch (Exception ex) {
                Toast.makeText(getActivity(), "No music player found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startAutoDetection() {
        autoDetectionEnabled = true;
        autoDetectionHandler.removeCallbacks(autoDetectionRunnable);
        autoDetectionHandler.postDelayed(autoDetectionRunnable, 2000);
    }

    private void stopAutoDetection() {
        autoDetectionEnabled = false;
        autoDetectionHandler.removeCallbacks(autoDetectionRunnable);
    }

    // ========== LEGACY DATA PARSING (FALLBACK) ==========

    private void parseAccelerometerData(String accelMessage) {
        if (!accelMessage.startsWith("ACCEL:")) return;
        String values = accelMessage.substring(6);
        String[] parts = values.split(",");

        if (parts.length >= 3) {
            float accelX = Float.parseFloat(parts[0].trim());
            float accelY = Float.parseFloat(parts[1].trim());
            float accelZ = Float.parseFloat(parts[2].trim());

            processWithAI(accelX, accelY, accelZ);
            processAccelerometerMovement(accelX, accelY, accelZ);
        }
    }

    private void parseMovementCommand(String moveMessage) {
        if (!moveMessage.startsWith("MOVE:")) return;
        String direction = moveMessage.substring(5).trim().toUpperCase();

        switch (direction) {
            case "LEFT": handleLeftMovement(); break;
            case "RIGHT": handleRightMovement(); break;
            case "FORWARD":
            case "UP": handleForwardMovement(); break;
            case "BACK":
            case "BACKWARD": handleBackMovement(); break;
            case "DANCE": handleDanceMovement(); break;
        }
    }

    private void processAccelerometerMovement(float accelX, float accelY, float accelZ) {
        final float ACCEL_THRESHOLD = 2.0f;

        if (Math.abs(accelX) > ACCEL_THRESHOLD) {
            if (accelX > ACCEL_THRESHOLD) handleRightMovement();
            else handleLeftMovement();
        } else if (Math.abs(accelY) > ACCEL_THRESHOLD) {
            if (accelY > ACCEL_THRESHOLD) handleForwardMovement();
            else handleBackMovement();
        }
    }

    // ========== MOVEMENT HANDLERS ==========

    private void handleLeftMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) return;

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("LEFT")) return;

        lastMovementTime = currentTime;
        jumpLeft++;
        updateJumpLabels();
        updateLastMovementText("LEFT MOVEMENT", "#667eea");

        sendIfConnected("b");

        String animation = getCharacterAnimation("LEFT");
        if (animation != null) {
            showMovementVideo(animation);
        }

        checkMovementCompletion("LEFT");
        checkAchievements();
    }

    private void handleRightMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) return;

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("RIGHT")) return;

        lastMovementTime = currentTime;
        jumpRight++;
        updateJumpLabels();
        updateLastMovementText("RIGHT MOVEMENT", "#667eea");

        sendIfConnected("g");

        String animation = getCharacterAnimation("RIGHT");
        if (animation != null) {
            showMovementVideo(animation);
        }

        checkMovementCompletion("RIGHT");
        checkAchievements();
    }

    private void handleForwardMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) return;

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("UP") && !selectedMovement.equals("FORWARD")) return;

        lastMovementTime = currentTime;
        jumpUp++;
        updateJumpLabels();
        updateLastMovementText("FORWARD MOVEMENT", "#667eea");

        sendIfConnected("w");

        String animation = getCharacterAnimation("UP");
        if (animation != null) {
            showMovementVideo(animation);
        }

        checkMovementCompletion("UP");
        checkMovementCompletion("FORWARD");
        checkAchievements();
    }

    private void handleBackMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) return;

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("BACK")) return;

        lastMovementTime = currentTime;
        jumpBack++;
        updateJumpLabels();
        updateLastMovementText("BACK MOVEMENT", "#667eea");

        sendIfConnected("r");

        String animation = getCharacterAnimation("BACK");
        if (animation != null) {
            showMovementVideo(animation);
        }

        checkMovementCompletion("BACK");
        checkAchievements();
    }

    private void handleDanceMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) return;

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("DANCE")) return;

        lastMovementTime = currentTime;
        jumpBack++;
        updateJumpLabels();
        updateLastMovementText("DANCE TIME!", "#FF6B35");

        sendIfConnected("a");

        String animation = getCharacterAnimation("DANCE");
        if (animation != null) {
            showMovementVideo(animation);
        } else {
            playSpecialAnimation();
        }

        checkMovementCompletion("DANCE");
        checkAchievements();
    }

    private void checkMovementCompletion(String detectedMovement) {
        if (selectedMovement != null && selectedMovement.equals(detectedMovement)) {
            Toast.makeText(getContext(), "Perfect! Movement completed!", Toast.LENGTH_LONG).show();
            selectedMovement = null;
            isManualModeActive = false;
        }
    }

    private void checkAchievements() {
        int totalMovements = jumpLeft + jumpRight + jumpUp + jumpBack;

        if (totalMovements >= ACHIEVEMENT_100 && !achievement100Shown) {
            showAchievement("LEGEND!", "100 movements achieved!", "5 stars");
            achievement100Shown = true;
        } else if (totalMovements >= ACHIEVEMENT_50 && !achievement50Shown) {
            showAchievement("CHAMPION!", "50 movements achieved!", "4 stars");
            achievement50Shown = true;
        } else if (totalMovements >= ACHIEVEMENT_20 && !achievement20Shown) {
            showAchievement("PRO MOVER!", "20 movements achieved!", "3 stars");
            achievement20Shown = true;
        } else if (totalMovements >= ACHIEVEMENT_10 && !achievement10Shown) {
            showAchievement("RISING STAR!", "10 movements achieved!", "2 stars");
            achievement10Shown = true;
        }
    }

    private void showAchievement(String title, String message, String badge) {
        Activity a = getActivity();
        if (a != null) {
            if (tvAchievementBadge != null) {
                tvAchievementBadge.setText(badge + " " + title);
                tvAchievementBadge.setVisibility(View.VISIBLE);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(a);
            builder.setTitle(title)
                    .setMessage(message + "\n\nKeep moving!")
                    .setPositiveButton("AWESOME!", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();

            Toast.makeText(a, title, Toast.LENGTH_LONG).show();
        }
    }

    private void showMovementVideo(String videoFileName) {
        if (vvMovementVideo != null && getContext() != null) {
            int resId = getResources().getIdentifier(videoFileName, "raw", getContext().getPackageName());
            if (resId != 0) {
                if (ivCharacterPlaceholder != null) {
                    ivCharacterPlaceholder.setVisibility(View.GONE);
                }

                vvMovementVideo.setVisibility(View.VISIBLE);
                Uri videoUri = Uri.parse("android.resource://" + getContext().getPackageName() + "/" + resId);
                vvMovementVideo.setVideoURI(videoUri);

                vvMovementVideo.setOnPreparedListener(mp -> {
                    mp.setLooping(false);
                    vvMovementVideo.start();
                });

                vvMovementVideo.setOnCompletionListener(mp -> {
                    vvMovementVideo.setVisibility(View.GONE);
                    if (ivCharacterPlaceholder != null) {
                        ivCharacterPlaceholder.setVisibility(View.VISIBLE);
                    }
                });

                vvMovementVideo.setOnErrorListener((mp, what, extra) -> {
                    Log.e("TerminalFragment", "Video error: " + what + ", " + extra);
                    vvMovementVideo.setVisibility(View.GONE);
                    if (ivCharacterPlaceholder != null) {
                        ivCharacterPlaceholder.setVisibility(View.VISIBLE);
                    }
                    return true;
                });
            }
        }
    }

    // ========== PHONE GYRO SUPPORT ==========

    private final GyroManager.MovementListener gyroListener = new GyroManager.MovementListener() {
        @Override public void onLeft() { handleLeftMovement(); }
        @Override public void onRight() { handleRightMovement(); }
        @Override public void onMiddle() { handleForwardMovement(); }
        @Override public void onBack() { handleBackMovement(); }
        @Override public void onRaw(float x, float y, float z) {}
    };

    private void initializePhoneGyroSensors() {
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyroSensor != null) {
                sensorManager.registerListener(phoneSensorListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
                gyroEnabled = true;
            }
        }
    }

    private final SensorEventListener phoneSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (currentSensorMode != SensorMode.PHONE_GYRO || !isManualModeActive) return;
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                processWithAI(x, y, z);

                float threshold = 2.0f;
                if (Math.abs(x) > threshold) {
                    if (x > 0) handleLeftMovement();
                    else handleRightMovement();
                } else if (Math.abs(y) > threshold) {
                    if (y > 0) handleForwardMovement();
                    else handleBackMovement();
                }
            }
        }
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    // ========== UI HELPER METHODS ==========

    private void updateLastMovementText(String movement, String colorHex) {
        if (tvLastMovement != null) {
            tvLastMovement.setText(movement);
            tvLastMovement.setTextColor(android.graphics.Color.parseColor(colorHex));
        }
    }

    private void updateJumpLabels() {
        if (tvJumpLeft != null) tvJumpLeft.setText(String.valueOf(jumpLeft));
        if (tvJumpRight != null) tvJumpRight.setText(String.valueOf(jumpRight));
        if (tvJumpMiddle != null) tvJumpMiddle.setText(String.valueOf(jumpUp));
        if (tvJumpBack != null) tvJumpBack.setText(String.valueOf(jumpBack));
    }



    // ========== MENU ==========

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}