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
import android.os.IBinder;
import android.os.Handler;
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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }
    private enum SensorMode { PHONE_GYRO, ESP32_ADXL345, AUTO_DETECT }

    // Character types
    private enum CharacterType { GIRL, LION, BOY }
    private CharacterType selectedCharacter = CharacterType.BOY;

    private FirebaseAuth auth;
    private String deviceAddress;
    private SerialService service;
    private SensorMode currentSensorMode = SensorMode.AUTO_DETECT;

    // Character animation mappings
    private Map<String, String> characterAnimations;
    private Map<String, List<String>> characterAnimationsList; // All available animations per character

    private AIMovementClassifier aiClassifier;
    private Button btnTestAI;
    private TextView tvAIStatus;

    private DataSyncManager dataSyncManager;

    private TextView receiveText;
    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private String newline = TextUtil.newline_crlf;

    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private boolean gyroEnabled = false;

    private static final int REQ_BT_CONNECT = 1001;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private final Runnable reconnectRunnable = () -> {
        if (connected != Connected.True) connect();
    };

    private int jumpLeft = 0;
    private int jumpRight = 0;
    private int jumpUp = 0;
    private int jumpBack = 0;
    private String selectedMovement = null;
    private boolean isManualModeActive = false;

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
    private Button btnModeToggle, btnMusicPlayer;
    private VideoView vvMovementVideo;
    private TextView tvJumpLeft, tvJumpRight, tvJumpMiddle, tvJumpBack;
    private TextView tvLastMovement, tvAchievementBadge;
    private ImageView ivCharacterPlaceholder;
    private TextView tvCharacterName;

    private static final String PREFS_NAME = "BluetoothPrefs";
    private static final String PREF_DEVICE_ADDRESS = "saved_device_address";
    private static final int MAX_CONNECT_ATTEMPTS = 3;
    private int currentConnectAttempt = 0;
    private boolean isAutoConnecting = false;

    private boolean autoDetectionEnabled = true;
    private final Handler autoDetectionHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoDetectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (autoDetectionEnabled && currentSensorMode == SensorMode.AUTO_DETECT && connected == Connected.True) {
                autoDetectionHandler.postDelayed(this, 5000);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        auth = FirebaseAuth.getInstance();
        Bundle args = getArguments();
        deviceAddress = args != null ? args.getString("device") : null;

        if (args != null && args.containsKey("character")) {
            String character = args.getString("character");
            setCharacterFromString(character);
        } else {
            // Load from SharedPreferences if not passed via arguments
            loadCharacterFromPreferences();
        }

        initializeCharacterAnimations();
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

        Log.i("TerminalFragment", "🎭 Character selected: " + selectedCharacter);
    }

    private void loadCharacterFromPreferences() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("KineticPulsePrefs", Context.MODE_PRIVATE);
            String savedCharacter = prefs.getString("selected_character", "boy");
            setCharacterFromString(savedCharacter);
            Log.i("TerminalFragment", "🎭 Loaded character from preferences: " + savedCharacter);
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
        characterAnimations.put("BOY_UP", "video_jump");
        characterAnimations.put("BOY_BACK", "video_jump");
        characterAnimations.put("BOY_DANCE", "video_up");

        List<String> boyAnimations = new ArrayList<>();
        boyAnimations.add("video_left");
        boyAnimations.add("video_right");
        boyAnimations.add("video_jump");
        characterAnimationsList.put("BOY", boyAnimations);
    }

    private String getCharacterAnimation(String movement) {
        String key = selectedCharacter.name() + "_" + movement.toUpperCase();
        String animation = characterAnimations.get(key);

        if (animation == null) {
            // Fallback to BOY animations if specific character animation not found
            animation = characterAnimations.get("BOY_" + movement.toUpperCase());
        }

        Log.d("TerminalFragment", "🎬 Animation key: " + key + " -> " + animation);
        return animation;
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False) disconnect();
        Activity a = getActivity();
        if (a != null) a.stopService(new Intent(a, SerialService.class));
        reconnectHandler.removeCallbacksAndMessages(null);
        autoDetectionHandler.removeCallbacksAndMessages(null);

        if (aiClassifier != null) {
            aiClassifier.close();
        }

        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null) {
            service.attach(this);
        } else {
            Activity a = getActivity();
            if (a != null) a.startService(new Intent(a, SerialService.class));
        }
    }

    @Override
    public void onStop() {
        Activity a = getActivity();
        if(service != null && a != null && !a.isChangingConfigurations()) service.detach();
        autoDetectionHandler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        activity.bindService(new Intent(activity, SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        Activity a = getActivity();
        try { if (a != null) a.unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null && isAdded()) {
            initialStart = false;
            Activity a = getActivity();
            if (a != null) a.runOnUiThread(this::connect);
        }

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

        if (currentSensorMode == SensorMode.AUTO_DETECT) {
            startAutoDetection();
        }
        if (dataSyncManager != null) dataSyncManager.syncPendingData();
        updateNetworkStatusUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        autoDetectionHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed() && isAdded()) {
            initialStart = false;
            Activity a = getActivity();
            if (a != null) {
                a.runOnUiThread(() -> {
                    String savedAddress = getSavedDeviceAddress();
                    if (savedAddress != null) {
                        attemptAutoConnect();
                    } else {
                        connect();
                    }
                });
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);

        aiClassifier = new AIMovementClassifier(getActivity());
        dataSyncManager = new DataSyncManager(getActivity());

        btnTestAI = view.findViewById(R.id.btnTestAI);
        tvAIStatus = view.findViewById(R.id.tvAIStatus);

        if (btnTestAI != null) {
            btnTestAI.setOnClickListener(v -> testAIClassifier());
        }

        updateNetworkStatusUI();
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

        btnWhite.setOnClickListener(v -> sendIfConnected("w"));
        btnRed.setOnClickListener(v -> sendIfConnected("r"));
        btnBlue.setOnClickListener(v -> sendIfConnected("b"));
        btnGreen.setOnClickListener(v -> sendIfConnected("g"));
        btnTopaz.setOnClickListener(v -> sendIfConnected("t"));
        btnLilac.setOnClickListener(v -> sendIfConnected("l"));
        btnRainbow.setOnClickListener(v -> sendIfConnected("a"));
        btnSeizureMode.setOnClickListener(v -> sendIfConnected("m"));
        btnLEDOff.setOnClickListener(v -> sendIfConnected("o"));

        btnModeToggle.setOnClickListener(v -> toggleSensorMode());
        updateModeToggleButton();

        btnMusicPlayer.setOnClickListener(v -> openMusicAppChooser());

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

    private void setupCharacterDisplay() {
        // Set character name
        if (tvCharacterName != null) {
            String charName = selectedCharacter.name();
            tvCharacterName.setText("✨ " + charName + " ✨");
        }

        // Show idle animation for selected character
        showIdleAnimation();
    }

    private void showIdleAnimation() {
        String idleAnimation = getCharacterAnimation("LEFT");
        if (idleAnimation != null) {
            // Set placeholder image based on character
            setCharacterPlaceholderImage();
            Log.d("TerminalFragment", "🎭 Setting up character: " + selectedCharacter + " with idle: " + idleAnimation);
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
            // Fallback to default placeholder
            ivCharacterPlaceholder.setImageResource(R.drawable.character_placeholder);
        }
    }

    private void testAIClassifier() {
        Log.d("TerminalFragment", "🧪 Testing AI Classifier...");

        if (aiClassifier == null) {
            showAITestResult("❌ AI Classifier is null");
            return;
        }

        if (!aiClassifier.isModelLoaded()) {
            showAITestResult("❌ AI Model failed to load");
            return;
        }

        String[] testMovements = {
                "Test 1 - Walking",
                "Test 2 - Running",
                "Test 3 - Boxing",
                "Test 4 - Clapping"
        };

        float[][] testData = {
                {0.5f, 0.3f, 0.2f},
                {1.5f, 1.2f, 0.8f},
                {3.0f, 0.5f, 0.3f},
                {0.8f, 0.8f, 0.8f}
        };

        StringBuilder results = new StringBuilder("🧪 AI Test Results:\n");

        for (int i = 0; i < testData.length; i++) {
            String result = aiClassifier.processSensorData(testData[i][0], testData[i][1], testData[i][2]);
            results.append(testMovements[i]).append(": ").append(result).append("\n");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        showAITestResult(results.toString());
    }

    private void showAITestResult(String message) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                if (tvAIStatus != null) {
                    tvAIStatus.setText(message);
                }
            });
        }
    }

    private void updateAIStatus() {
        if (aiClassifier != null) {
            String status = aiClassifier.isModelLoaded() ?
                    "✅ AI Model Loaded" : "❌ AI Model Failed";

            if (tvAIStatus != null) {
                tvAIStatus.setText(status);
            }
        }
    }

    private void processWithAI(float ax, float ay, float az) {
        if (aiClassifier != null && aiClassifier.isModelLoaded()) {
            String detectedActivity = aiClassifier.processSensorData(ax, ay, az);

            if (!detectedActivity.equals("collecting_data") &&
                    !detectedActivity.equals("uncertain") &&
                    !detectedActivity.equals("error") &&
                    !detectedActivity.equals("ai_not_loaded")) {

                handleAIDetection(detectedActivity);
            }
        }
    }

    private void handleAIDetection(String activity) {
        Log.i("TerminalFragment", "🎯 AI Detected: " + activity);

        switch (activity.toLowerCase()) {
            case "walking":
            case "running":
                if (isManualModeActive && "FORWARD".equals(selectedMovement)) {
                    handleForwardMovement();
                }
                break;
            case "boxing":
                if (isManualModeActive) {
                    if ("LEFT".equals(selectedMovement)) handleLeftMovement();
                    else if ("RIGHT".equals(selectedMovement)) handleRightMovement();
                }
                break;
            case "clapping":
            case "dancing": // Add this case for dance detection
                if (isManualModeActive && "DANCE".equals(selectedMovement)) {
                    handleDanceMovement();
                } else {
                    playSpecialAnimation();
                    sendIfConnected("a");
                    Toast.makeText(getContext(), "💃 Dance detected!", Toast.LENGTH_SHORT).show();
                }
                break;
            case "standing up":
                if (isManualModeActive && "UP".equals(selectedMovement)) {
                    handleForwardMovement();
                }
                break;
            case "sitting down":
                if (isManualModeActive && "BACK".equals(selectedMovement)) {
                    handleBackMovement();
                }
                break;
        }

        updateLastMovementText("🤖 AI: " + activity.toUpperCase(), "#FF6B6B");
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

    private void selectMovement(String movement) {
        selectedMovement = movement;
        isManualModeActive = true;

        String animationFile = getCharacterAnimation(movement);
        if (animationFile != null) {
            showMovementVideo(animationFile);
        }

        Toast.makeText(getContext(), "Now perform your " + movement + " movement!", Toast.LENGTH_SHORT).show();
        Log.i("TerminalFragment", "🎯 Manual mode: " + movement + " with " + selectedCharacter);
    }

    private void toggleSensorMode() {
        switch (currentSensorMode) {
            case AUTO_DETECT:
                currentSensorMode = SensorMode.ESP32_ADXL345;
                stopAutoDetection();
                Toast.makeText(getActivity(), "Switched to ESP32 Mode", Toast.LENGTH_SHORT).show();
                updateLastMovementText("🔧 ESP32 active", "#667eea");
                break;

            case ESP32_ADXL345:
                currentSensorMode = SensorMode.PHONE_GYRO;
                gyroEnabled = true;
                initializePhoneGyroSensors();
                Toast.makeText(getActivity(), "Switched to Phone Gyroscope", Toast.LENGTH_SHORT).show();
                updateLastMovementText("📱 Phone gyroscope active", "#667eea");
                break;

            case PHONE_GYRO:
                currentSensorMode = SensorMode.AUTO_DETECT;
                gyroEnabled = false;
                if (sensorManager != null && phoneSensorListener != null) {
                    sensorManager.unregisterListener(phoneSensorListener);
                }
                startAutoDetection();
                Toast.makeText(getActivity(), "Switched to Auto Detect 🎯", Toast.LENGTH_SHORT).show();
                updateLastMovementText("🎯 Auto detection active", "#667eea");
                break;
        }
        updateModeToggleButton();
    }

    private void updateModeToggleButton() {
        if (btnModeToggle != null) {
            String aiStatus = (aiClassifier != null && aiClassifier.isModelLoaded()) ? " 🧠" : "";
            switch (currentSensorMode) {
                case AUTO_DETECT:
                    btnModeToggle.setText("🎯 Auto Detect" + aiStatus);
                    break;
                case PHONE_GYRO:
                    btnModeToggle.setText("📱 Phone Gyro" + aiStatus);
                    break;
                case ESP32_ADXL345:
                    btnModeToggle.setText("🔧 ESP32" + aiStatus);
                    break;
            }
        }
    }

    private void openMusicAppChooser() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_MUSIC);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "Choose Music App 🎵"));
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("audio/*");
                startActivity(Intent.createChooser(intent, "Choose Music App 🎵"));
            } catch (Exception ex) {
                Toast.makeText(getActivity(), "No music player found 📵", Toast.LENGTH_SHORT).show();
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

    private void sendIfConnected(String str) {
        if (connected == Connected.True && service != null) {
            send(str);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.hex).setChecked(hexEnabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menu.findItem(R.id.backgroundNotification).setChecked(service != null && service.areNotificationsEnabled());
        } else {
            menu.findItem(R.id.backgroundNotification).setChecked(true);
            menu.findItem(R.id.backgroundNotification).setEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            item.setChecked(hexEnabled);
            return true;
        } else if (id == R.id.backgroundNotification) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!service.areNotificationsEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
                } else {
                    showNotificationSettings();
                }
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                status("Bluetooth not available");
                return;
            }
            if (deviceAddress == null) {
                status("No device selected");
                return;
            }
            Activity a = getActivity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (a != null && a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQ_BT_CONNECT);
                    return;
                }
            }
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            connected = Connected.Pending;
            if (a == null || service == null) return;
            SerialSocket socket = new SerialSocket(a.getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void status(String str) {
        if (receiveText != null) {
            SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        if (service != null) service.disconnect();
        stopAutoDetection();
        updateModeToggleButton();
    }

    private void send(String str) {
        if(connected != Connected.True || service == null) {
            return;
        }
        try {
            byte[] data = (str + newline).getBytes();
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(ArrayDeque<byte[]> datas) {
        for (byte[] data : datas) {
            String msg = new String(data);

            if (currentSensorMode == SensorMode.ESP32_ADXL345 || currentSensorMode == SensorMode.AUTO_DETECT) {
                processESP32MovementData(msg);
            }
        }
    }

    private void processESP32MovementData(String msg) {
        try {
            if (msg.startsWith("ACCEL:")) {
                parseAccelerometerData(msg);
            } else if (msg.startsWith("MOVE:")) {
                parseMovementCommand(msg);
            } else if (msg.contains("Movement Detected") || msg.contains("Jump detected")) {
                parseMovementText(msg);
            }
        } catch (Exception e) {
            Log.e("TerminalFragment", "ESP32 DATA ERROR: " + e.getMessage());
        }
    }

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
    private void handleDanceMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) {
            return;
        }

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("DANCE")) {
            return;
        }

        lastMovementTime = currentTime;
        // You might want to track dance movements separately or increment a counter
        // For now, let's increment jumpBack to keep the score consistent
        jumpBack++;
        updateJumpLabels();
        updateLastMovementText("💃 DANCE TIME!", "#FF6B35");
        saveJumpDataToAPI();
        sendIfConnected("a"); // Rainbow effect for dance!

        String animation = getCharacterAnimation("DANCE");
        if (animation != null) {
            showMovementVideo(animation);
        } else {
            Log.w("TerminalFragment", "No DANCE animation found for " + selectedCharacter);
            // Fallback to special animation
            playSpecialAnimation();
        }

        checkMovementCompletion("DANCE");
        checkAchievements();
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

    private void parseMovementText(String textMessage) {
        if (textMessage.contains("Lateral-Left")) handleLeftMovement();
        else if (textMessage.contains("Yahoo")) handleForwardMovement();
        else if (textMessage.contains("Lateral- Right")) handleRightMovement();
        else if (textMessage.contains("Back")) handleBackMovement();
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

        processWithAI(accelX, accelY, accelZ);
    }

    private void handleLeftMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) {
            return;
        }

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("LEFT")) {
            return;
        }

        lastMovementTime = currentTime;
        jumpLeft++;
        updateJumpLabels();
        updateLastMovementText("⬅️ LEFT MOVEMENT", "#667eea");
        saveJumpDataToAPI();
        sendIfConnected("b");

        String animation = getCharacterAnimation("LEFT");
        if (animation != null) {
            showMovementVideo(animation);
        } else {
            Log.w("TerminalFragment", "No LEFT animation found for " + selectedCharacter);
        }

        checkMovementCompletion("LEFT");
        checkAchievements();
    }

    private void handleRightMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) return;

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("RIGHT")) {
            return;
        }

        lastMovementTime = currentTime;
        jumpRight++;
        updateJumpLabels();
        updateLastMovementText("➡️ RIGHT MOVEMENT", "#667eea");
        saveJumpDataToAPI();
        sendIfConnected("g");

        String animation = getCharacterAnimation("RIGHT");
        if (animation != null) {
            showMovementVideo(animation);
        } else {
            Log.w("TerminalFragment", "No RIGHT animation found for " + selectedCharacter);
        }

        checkMovementCompletion("RIGHT");
        checkAchievements();
    }

    private void handleForwardMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) return;

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("UP") && !selectedMovement.equals("FORWARD")) {
            return;
        }

        lastMovementTime = currentTime;
        jumpUp++;
        updateJumpLabels();
        updateLastMovementText("⬆️ FORWARD MOVEMENT", "#667eea");
        saveJumpDataToAPI();
        sendIfConnected("w");

        String animation = getCharacterAnimation("UP");
        if (animation != null) {
            showMovementVideo(animation);
        } else {
            Log.w("TerminalFragment", "No UP animation found for " + selectedCharacter);
        }

        checkMovementCompletion("UP");
        checkMovementCompletion("FORWARD");
        checkAchievements();
    }

    private void handleBackMovement() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) return;

        if (!isManualModeActive && currentSensorMode != SensorMode.AUTO_DETECT) return;

        if (selectedMovement != null && !selectedMovement.equals("BACK")) {
            return;
        }

        lastMovementTime = currentTime;
        jumpBack++;
        updateJumpLabels();
        updateLastMovementText("⬇️ BACK MOVEMENT", "#667eea");
        saveJumpDataToAPI();
        sendIfConnected("r");

        String animation = getCharacterAnimation("BACK");
        if (animation != null) {
            showMovementVideo(animation);
        } else {
            Log.w("TerminalFragment", "No BACK animation found for " + selectedCharacter);
        }

        checkMovementCompletion("BACK");
        checkAchievements();
    }

    private void checkMovementCompletion(String detectedMovement) {
        if (selectedMovement != null && selectedMovement.equals(detectedMovement)) {
            Toast.makeText(getContext(), "✅ Perfect! Movement completed!", Toast.LENGTH_LONG).show();
            selectedMovement = null;
            isManualModeActive = false;
        }
    }

    private void checkAchievements() {
        int totalMovements = jumpLeft + jumpRight + jumpUp + jumpBack;

        if (totalMovements >= ACHIEVEMENT_100 && !achievement100Shown) {
            showAchievement("🏆 LEGEND! 🏆", "100 movements achieved!", "🌟🌟🌟🌟🌟");
            achievement100Shown = true;
        } else if (totalMovements >= ACHIEVEMENT_50 && !achievement50Shown) {
            showAchievement("🎖️ CHAMPION! 🎖️", "50 movements achieved!", "⭐⭐⭐⭐");
            achievement50Shown = true;
        } else if (totalMovements >= ACHIEVEMENT_20 && !achievement20Shown) {
            showAchievement("🏅 PRO MOVER! 🏅", "20 movements achieved!", "⭐⭐⭐");
            achievement20Shown = true;
        } else if (totalMovements >= ACHIEVEMENT_10 && !achievement10Shown) {
            showAchievement("🎯 RISING STAR! 🎯", "10 movements achieved!", "⭐⭐");
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
            } else {
                Log.e("TerminalFragment", "Video file not found: " + videoFileName);
                // Show a toast message for missing video
                Toast.makeText(getContext(), "Animation not available: " + videoFileName, Toast.LENGTH_SHORT).show();
            }
        }
    }

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

    private void showNotificationSettings() {
        Activity a = getActivity();
        if (a == null) return;
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("android.provider.extra.APP_PACKAGE", a.getPackageName());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(Arrays.equals(permissions, new String[]{Manifest.permission.POST_NOTIFICATIONS}) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && service != null && !service.areNotificationsEnabled())
            showNotificationSettings();
        if (requestCode == REQ_BT_CONNECT && Arrays.equals(permissions, new String[]{Manifest.permission.BLUETOOTH_CONNECT})) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connect();
            } else {
                status("Bluetooth permission denied");
            }
        }
    }

    private void saveDeviceAddress() {
        if (service != null && connected == Connected.True && deviceAddress != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREF_DEVICE_ADDRESS, deviceAddress).apply();
        }
    }

    private String getSavedDeviceAddress() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_DEVICE_ADDRESS, null);
    }

    private void attemptAutoConnect() {
        String savedAddress = getSavedDeviceAddress();
        if (savedAddress == null || currentConnectAttempt >= MAX_CONNECT_ATTEMPTS) {
            showDeviceSelectionDialog();
            return;
        }
        currentConnectAttempt++;
        isAutoConnecting = true;
        deviceAddress = savedAddress;
        connect();
    }

    private void showDeviceSelectionDialog() {
        isAutoConnecting = false;
        currentConnectAttempt = 0;
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
        updateModeToggleButton();
        saveDeviceAddress();
        currentConnectAttempt = 0;
        isAutoConnecting = false;

        if (currentSensorMode == SensorMode.AUTO_DETECT) {
            startAutoDetection();
        }
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
        if (isAutoConnecting && currentConnectAttempt < MAX_CONNECT_ATTEMPTS) {
            new Handler().postDelayed(() -> attemptAutoConnect(), 2000);
        } else if (isAutoConnecting) {
            showDeviceSelectionDialog();
        } else {
            String savedAddress = getSavedDeviceAddress();
            if (savedAddress != null) {
                reconnectHandler.removeCallbacks(reconnectRunnable);
                reconnectHandler.postDelayed(reconnectRunnable, 1500);
            }
        }
    }

    @Override
    public void onSerialRead(byte[] data) {
        ArrayDeque<byte[]> datas = new ArrayDeque<>();
        datas.add(data);
        receive(datas);
    }

    public void onSerialRead(ArrayDeque<byte[]> datas) {
        receive(datas);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectHandler.postDelayed(reconnectRunnable, 1500);
    }

    private void saveJumpDataToAPI() {
        if (dataSyncManager != null) {
            dataSyncManager.saveMovementData(jumpLeft, jumpRight, jumpUp, jumpBack);

            if (!dataSyncManager.isOnline()) {
                Toast.makeText(getContext(), "📱 Saved offline - will sync when online", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateNetworkStatusUI() {
        if (dataSyncManager != null && tvLastMovement != null) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (dataSyncManager.isOnline()) {
                        tvLastMovement.setText("🟢 ONLINE - Real-time sync");
                        tvLastMovement.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else {
                        tvLastMovement.setText("🔴 OFFLINE - Saving locally");
                        tvLastMovement.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                });
            }
        }
    }
}