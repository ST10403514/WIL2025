package com.example.kineticpulsemobileapp;

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
import android.content.res.ColorStateList;
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
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import android.widget.Switch;
import android.speech.tts.TextToSpeech;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.content.pm.PackageManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;



import java.util.ArrayDeque;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }
    private enum SensorMode { PHONE_GYRO, ESP32_ADXL345, SECOND_PHONE_GYRO }
    
    private FirebaseAuth auth;
    private String deviceAddress;
    private SerialService service;
    private SensorMode currentSensorMode = SensorMode.ESP32_ADXL345; // Default to ESP32

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;
    
    // Phone sensor variables
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private Sensor accelerometerSensor;
    private boolean gyroEnabled = false;
    
    private static final int REQ_BT_CONNECT = 1001;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private final Runnable reconnectRunnable = () -> {
        if (connected != Connected.True) connect();
    };

    private int jumpLeft = 0;
    private int jumpRight = 0;
    private int jumpUp = 0; // simplified: just one up counter
    private int jumpBack = 0;
    // remembers the currently selected movement from the UI (null when none)
    private String selectedMovement = null;
    // Toggles automatic motion detection
    private boolean isAutoDetectionEnabled = true;

    private Button btnLEDOptions;
    private View ledOptionsView;
    //LED Buttons-----------------------
    private Button btnWhite;
    private Button btnRed;
    private Button btnBlue;
    private Button btnGreen;
    private Button btnTopaz;
    private Button btnLilac;
    private Button btnRainbow;
    private Button btnSeizureMode;
    private Button btnLEDOff;
    //LED Buttons-----------------------

    private Button btnLogin;
    private Button btnGyroToggle;
    private Switch switchMode; // Debug-only Dev/Prod toggle

    private Button btnJumpUp;

    private Button btnJumpLeft;

    private Button btnJumpRight;

    private View buttonsView;

    private View hideView;

    private View showView;

    private ImageView ivJump;
    private ImageView ivMovementFlash;

    private TextView tvJumpLeft;

    private TextView tvJumpRight;

    private TextView tvJumpMiddle;
    
    private TextView tvJumpBack;
    
    private TextView tvLastMovement;
    
    private TextView tvCalibrationPrompt;
    
    // Mode + TTS
    private static final String PREFS_APP = "AppPrefs";
    private static final String PREF_PROD_MODE = "prod_mode";
    private boolean isProductionMode = true; // default
    private TextToSpeech tts;
    
    // Auto-connect functionality
    private static final String PREFS_NAME = "BluetoothPrefs";
    private static final String PREF_DEVICE_ADDRESS = "saved_device_address";
    private static final int MAX_CONNECT_ATTEMPTS = 3;
    private int currentConnectAttempt = 0;
    private boolean isAutoConnecting = false;
    private boolean isManualModeActive = false;
    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        auth = FirebaseAuth.getInstance();
        Bundle args = getArguments();
        deviceAddress = args != null ? args.getString("device") : null;
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            try { tts.stop(); } catch (Exception ignored) {}
            try { tts.shutdown(); } catch (Exception ignored) {}
        }
        if (connected != Connected.False) disconnect();
        Activity a = getActivity();
        if (a != null) a.stopService(new Intent(a, SerialService.class));
        reconnectHandler.removeCallbacksAndMessages(null);
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
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
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
        
        // Re-enable gyroscope when resuming if it was previously enabled
        if (gyroEnabled) {
            Activity a = getActivity();
            if (a instanceof MainActivity) {
                GyroManager gm = ((MainActivity) a).getGyroManager();
                if (gm != null && gm.isAvailable()) {
                    gm.setMovementListener(gyroListener);
                    gm.setProcessingEnabled(true);
                    gm.start(); // Start the gyroscope sensor
                    Log.i("TerminalFragment", "Gyroscope re-enabled on resume");
                }
            }
        }
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
                    // Try auto-connect first, fallback to manual connect if no saved device
                    String savedAddress = getSavedDeviceAddress();
                    if (savedAddress != null) {
                        Log.i("TerminalFragment", "📱 Found saved device address, attempting auto-connect");
                        attemptAutoConnect();
                    } else {
                        Log.i("TerminalFragment", "📱 No saved device address, starting manual connection");
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

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        ledOptionsView = view.findViewById(R.id.ledOptions);
        btnLEDOptions = view.findViewById(R.id.hideLEDView);
        //LED Buttons-----------------------
        btnWhite = view.findViewById(R.id.btnWhite);
        btnRed = view.findViewById(R.id.btnRed);
        btnGreen = view.findViewById(R.id.btnGreen);
        btnBlue = view.findViewById(R.id.btnBlue);
        btnTopaz = view.findViewById(R.id.btnTopaz);
        btnLilac = view.findViewById(R.id.btnLilac);
        btnRainbow = view.findViewById(R.id.btnRainbow);
        btnSeizureMode = view.findViewById(R.id.btnSeizureMode);
        btnLEDOff = view.findViewById(R.id.btnLEDOff);
        //LED Buttons-----------------------

        btnLogin = view.findViewById(R.id.btnLogin);
    btnGyroToggle = view.findViewById(R.id.btnGyroToggle);
    switchMode = view.findViewById(R.id.switchMode);
        ivJump = view.findViewById(R.id.ivJump);
        ivMovementFlash = view.findViewById(R.id.ivMovementFlash);
        showView = view.findViewById(R.id.showView);
        hideView = view.findViewById(R.id.hideView);
        buttonsView = view.findViewById(R.id.buttonsView);
        btnJumpLeft = view.findViewById(R.id.btnJumpLeft);
        btnJumpRight = view.findViewById(R.id.btnJumpRight);
        btnJumpUp = view.findViewById(R.id.btnJumpUp);
        tvJumpLeft = view.findViewById(R.id.tvJumpLeft);
        tvJumpRight = view.findViewById(R.id.tvJumpRight);
        tvJumpMiddle = view.findViewById(R.id.tvJumpMiddle);
        tvJumpBack = view.findViewById(R.id.tvJumpBack);
        tvLastMovement = view.findViewById(R.id.tvLastMovement);
        tvCalibrationPrompt = view.findViewById(R.id.tvCalibrationPrompt);
        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        // Remove old button functionality since we're using automatic detection only
        btnLogin.setOnClickListener(v -> {
            Intent loginIntent = new Intent(getActivity(), LoginScreen.class);
            startActivity(loginIntent);
        });

        btnLEDOptions.setOnClickListener(v -> {
            if (ledOptionsView.getVisibility() == View.GONE) {
                ledOptionsView.setVisibility(View.VISIBLE);
            } else {
                ledOptionsView.setVisibility(View.GONE);
            }
        });

        //LED Buttons-----------------------
        btnWhite.setOnClickListener(v ->{
            send("w");
        });
        btnRed.setOnClickListener(v ->{
            send("r");
        });
        btnBlue.setOnClickListener(v ->{
            send("b");
        });
        btnGreen.setOnClickListener(v ->{
            send("g");
        });
        btnTopaz.setOnClickListener(v ->{
            send("t");
        });
        btnLilac.setOnClickListener(v ->{
            send("l");
        });
        btnRainbow.setOnClickListener(v ->{
            send("a");
        });
        btnSeizureMode.setOnClickListener(v ->{
            send("m");
        });
        btnLEDOff.setOnClickListener(v ->{
            send("o");
        });



        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> {
            String text = sendText.getText().toString().trim();
            if ("TEST".equalsIgnoreCase(text)) {
                sendText.setText("");
                runManualTests();
            } else if ("LEDTEST".equalsIgnoreCase(text)) {
                sendText.setText("");
                runLEDTestSequence();
            } else if ("ACCELTEST".equalsIgnoreCase(text)) {
                sendText.setText("");
                runAccelTestSequence();
            } else {
                send(text);
            }
        });

        btnGyroToggle.setOnClickListener(v -> toggleGyro());
        
    // Initialize sensor mode button text
    updateGyroToggleButton();
    // Initialize mode switch (debug-only) and TTS
    initModeSwitch(view.getContext());
    initTts(view.getContext());
        
        // Check if gyroscope is available and show status
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            GyroManager gm = ((MainActivity) activity).getGyroManager();
            if (gm == null || !gm.isAvailable()) {
                btnGyroToggle.setEnabled(false);
                btnGyroToggle.setText("Gyro: UNAVAILABLE");
            } else {
                // Auto-start gyroscope when it's available
                initializeGyroAutomatically();
                // Also test basic device sensors
                testDeviceSensors();
            }
        }
        // ===== MOVEMENT SELECTION BUTTONS =====
        Button btnJumpLeftSelect = view.findViewById(R.id.btnJumpLeftSelect);
        Button btnJumpRightSelect = view.findViewById(R.id.btnJumpRightSelect);
        Button btnJumpUpSelect = view.findViewById(R.id.btnJumpUpSelect);
        Button btnJumpFrontSelect = view.findViewById(R.id.btnJumpFrontSelect);

        btnJumpLeftSelect.setOnClickListener(v -> {
            selectedMovement = "LEFT";
            isManualModeActive = true;
            showMovementFlash("mwm_dress_left2");
            Toast.makeText(getContext(), "Now perform your LEFT jump!", Toast.LENGTH_SHORT).show();
        });

        btnJumpRightSelect.setOnClickListener(v -> {
            selectedMovement = "RIGHT";
            isManualModeActive = true;
            showMovementFlash("mwm_jump_right2");
            Toast.makeText(getContext(), "Now perform your RIGHT jump!", Toast.LENGTH_SHORT).show();
        });

        btnJumpUpSelect.setOnClickListener(v -> {
            selectedMovement = "UP";
            isManualModeActive = true;
            showMovementFlash("mwm_jump_bounce2");
            Toast.makeText(getContext(), "Now perform your UP jump!", Toast.LENGTH_SHORT).show();
        });

        btnJumpFrontSelect.setOnClickListener(v -> {
            selectedMovement = "FORWARD";
            isManualModeActive = true;
            showMovementFlash("mwm_jump_bounce2");
            Toast.makeText(getContext(), "Now perform your FORWARD jump!", Toast.LENGTH_SHORT).show();
        });

// ===== AUTO DETECTION TOGGLE BUTTON =====
        Button btnToggleAutoDetection = view.findViewById(R.id.btnToggleAutoDetection);

        btnToggleAutoDetection.setOnClickListener(v -> {
            // Toggle the state
            isAutoDetectionEnabled = !isAutoDetectionEnabled;

            // Update button text
            String status = isAutoDetectionEnabled ? "ON" : "OFF";
            btnToggleAutoDetection.setText("Auto Detection: " + status);

            // Optional visual feedback using color tint (no drawables needed)
            int color = isAutoDetectionEnabled
                    ? getResources().getColor(android.R.color.holo_green_light)
                    : getResources().getColor(android.R.color.darker_gray);

            btnToggleAutoDetection.setBackgroundTintList(ColorStateList.valueOf(color));

            // Show quick toast feedback
            Toast.makeText(
                    getContext(),
                    "Auto detection " + (isAutoDetectionEnabled ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT
            ).show();
        });


        return view;
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
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
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

    /*
     * Serial + UI
     */
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
                    status("Requesting Bluetooth permission...");
                    return;
                }
            }
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            if (a == null || service == null) {
                status("Service not available");
                return;
            }
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
        
        // Update button to show disconnected status
        updateGyroToggleButton();
    }

    private void send(String str) {
        Activity a = getActivity();
        if(connected != Connected.True || a == null || service == null) {
            if (a != null) Toast.makeText(a, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(ArrayDeque<byte[]> datas) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        for (byte[] data : datas) {
            String msg = new String(data);
            
            // Debug: Log all received messages from ESP32
            Log.d("TerminalFragment", "📥 ESP32 DATA: '" + msg + "'");

            // ===== ESP32 + ADXL345 MOVEMENT DETECTION =====
            if (currentSensorMode == SensorMode.ESP32_ADXL345) {
                processESP32MovementData(msg);
            }

            // Display received data in terminal
            if (hexEnabled) {
                spn.append(TextUtil.toHexString(data)).append('\n');
            } else {
                if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                    // Special handling if CR and LF come in separate fragments
                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                }
                spn.append(TextUtil.toCaretString(msg, newline.length() != 0));
            }
        }
        receiveText.append(spn);
    }

    // ===== ESP32 + ADXL345 DATA PROCESSING =====
    private void processESP32MovementData(String msg) {
        try {
            // Handle different ESP32 data formats:
            
            // Format 1: Raw accelerometer data "ACCEL:x,y,z"
            if (msg.startsWith("ACCEL:")) {
                parseAccelerometerData(msg);
            }
            // Format 2: Processed movement commands "MOVE:LEFT", "MOVE:RIGHT", etc.
            else if (msg.startsWith("MOVE:")) {
                parseMovementCommand(msg);
            }
            // Format 3: Legacy movement text messages (for compatibility)
            else if (msg.contains("Movement Detected") || msg.contains("Jump detected")) {
                parseMovementText(msg);
            }
            
        } catch (Exception e) {
            Log.e("TerminalFragment", "🔧 ESP32 DATA ERROR: Failed to process ESP32 data: " + msg + " - " + e.getMessage());
        }
    }

    private void parseAccelerometerData(String accelMessage) {
        // Expected format: "ACCEL:x,y,z" (e.g., "ACCEL:0.25,-0.13,0.07")
        if (!accelMessage.startsWith("ACCEL:")) return;
        
        String values = accelMessage.substring(6); // Remove "ACCEL:" prefix
        String[] parts = values.split(",");
        
        if (parts.length >= 3) {
            float accelX = Float.parseFloat(parts[0].trim());
            float accelY = Float.parseFloat(parts[1].trim());
            float accelZ = Float.parseFloat(parts[2].trim());
            
            // Log raw accelerometer data
            Log.d("TerminalFragment", String.format("🔄 ESP32 ACCEL RAW: x=%.3f y=%.3f z=%.3f", accelX, accelY, accelZ));
            
            // Process movement detection based on accelerometer thresholds
            processAccelerometerMovement(accelX, accelY, accelZ);
        }
    }

    private void parseMovementCommand(String moveMessage) {
        // Expected format: "MOVE:LEFT", "MOVE:RIGHT", "MOVE:FORWARD", "MOVE:BACK"
        if (!moveMessage.startsWith("MOVE:")) return;
        
        String direction = moveMessage.substring(5).trim().toUpperCase();
        Log.d("TerminalFragment", "🎯 ESP32 MOVE COMMAND: " + direction);
        
        switch (direction) {
            case "LEFT":
                handleLeftMovement();
                break;
            case "RIGHT":
                handleRightMovement();
                break;
            case "FORWARD":
            case "UP":
                handleForwardMovement();
                break;
            case "BACK":
            case "BACKWARD":
                handleBackMovement();
                break;
        }
    }

    private void parseMovementText(String textMessage) {
        // Handle legacy text formats for compatibility
        if (textMessage.contains("Lateral-Left Movement Detected")) {
            handleLeftMovement();
        } else if (textMessage.contains("Jump detected! Yahoo! ^^")) {
            handleForwardMovement();
        } else if (textMessage.contains("Lateral- Right Movement Detected")) {
            handleRightMovement();
        } else if (textMessage.contains("Back Movement Detected") || textMessage.contains("Backward Jump")) {
            handleBackMovement();
        }
    }

    private void processAccelerometerMovement(float accelX, float accelY, float accelZ) {
        // Define movement thresholds for ADXL345 accelerometer
        final float ACCEL_THRESHOLD = 2.0f; // Adjust based on your ESP32 sensitivity
        
        // Determine movement direction based on acceleration values
        // Note: These may need adjustment based on your ESP32 orientation
        
        if (Math.abs(accelX) > ACCEL_THRESHOLD) {
            if (accelX > ACCEL_THRESHOLD) {
                handleRightMovement();
            } else {
                handleLeftMovement();
            }
        }
        else if (Math.abs(accelY) > ACCEL_THRESHOLD) {
            if (accelY > ACCEL_THRESHOLD) {
                handleForwardMovement();
            } else {
                handleBackMovement();
            }
        }
    }

    // ===== MOVEMENT HANDLERS =====
    private void handleLeftMovement() {
        if (!isAutoDetectionEnabled && !isManualModeActive) return;
        Log.i("TerminalFragment", "🎯 ESP32 MOVEMENT: LEFT detected!");
        jumpLeft++;
        updateJumpLabels();
        updateLastMovementText("⬅️ LEFT MOVEMENT (ESP32)", "#0000FF");
        saveJumpDataToAPI();
        setLEDForLeftJump();
        showToast("Left movement detected! LED: BLUE");

        // Show flash animation for left movement using your GIF/WebP
        showMovementFlash("mwm_dress_left2");
        speakIfProd("Left movement detected");
    }

    private void handleRightMovement() {
        if (!isAutoDetectionEnabled && !isManualModeActive) return;
        Log.i("TerminalFragment", "🎯 ESP32 MOVEMENT: RIGHT detected!");
        jumpRight++;
        updateJumpLabels();
        updateLastMovementText("➡️ RIGHT MOVEMENT (ESP32)", "#00FF00");
        saveJumpDataToAPI();
        setLEDForRightJump();
        showToast("Right movement detected! LED: GREEN");

        // Show flash animation for right movement using your GIF/WebP
        showMovementFlash("mwm_jump_right2");
        speakIfProd("Right movement detected");
    }

    private void handleForwardMovement() {
        if (!isAutoDetectionEnabled && !isManualModeActive) return;
        Log.i("TerminalFragment", "🎯 ESP32 MOVEMENT: FORWARD detected!");
        jumpUp++;
        updateJumpLabels();
        updateLastMovementText("⬆️ FORWARD MOVEMENT (ESP32)", "#FFFFFF");
        saveJumpDataToAPI();
        setLEDForForwardJump();
        showToast("Forward movement detected! LED: WHITE");

        // Show flash animation for forward movement using your GIF/WebP
        showMovementFlash("mwm_jump_bounce2");
        speakIfProd("Forward movement detected");
    }

    private void handleBackMovement() {
        if (!isAutoDetectionEnabled && !isManualModeActive) return;
        Log.i("TerminalFragment", "🎯 ESP32 MOVEMENT: BACK detected!");
        jumpBack++;
        updateJumpLabels();
        updateLastMovementText("⬇️ BACK MOVEMENT (ESP32)", "#FF0000");
        saveJumpDataToAPI();
        setLEDForBackJump();
        showToast("Back movement detected! LED: RED");

        // Show flash animation for back movement using your GIF/WebP
        showMovementFlash("mwm_jump_bounce2"); // Reusing the bounce GIF for back movement
        speakIfProd("Back movement detected");
    }

    // ===== showMovementFlash =====
    private void showMovementFlash(String fileName) {
        if (ivMovementFlash != null && getContext() != null) {
            // Get resource ID from raw folder
            int resId = getResources().getIdentifier(fileName, "raw", getContext().getPackageName());

            if (resId != 0) {
                // Use Glide to load the animation (handles both GIF and WebP)
                Glide.with(this)
                        .load(resId)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(ivMovementFlash);

                ivMovementFlash.setVisibility(View.VISIBLE);

                // Hide after 3 seconds
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (ivMovementFlash != null) {
                        ivMovementFlash.setVisibility(View.GONE);
                    }
                }, 3000);
            } else {
                Log.e("TerminalFragment", "❌ Animation resource not found: " + fileName);
            }
        }
    }

    // ===== Gyro Listener =====
    private final GyroManager.MovementListener gyroListener = new GyroManager.MovementListener() {
        @Override public void onLeft() {
            if (!isAutoDetectionEnabled && !isManualModeActive) return;
            Log.i("TerminalFragment", "🎯 GYRO EVENT: LEFT movement detected!");
            jumpLeft++;
            updateJumpLabels();
            updateLastMovementText("⬅️ LEFT MOVEMENT DETECTED", "#FFD700");
            saveJumpDataToAPI();
            setLEDForLeftJump(); // Set LED to YELLOW
            showToast("Left jump detected! LED: YELLOW");
            showMovementFlash("mwm_dress_left2");
            speakIfProd("Left movement detected");

// confirm user-selected movement (if any)
            if (selectedMovement != null) {
                speakIfProd("You performed your " + selectedMovement.toLowerCase() + " jump!");
                selectedMovement = null;
            }
            isManualModeActive = false;
        }
        @Override public void onRight() {
            if (!isAutoDetectionEnabled && !isManualModeActive) return;
            Log.i("TerminalFragment", "🎯 GYRO EVENT: RIGHT movement detected!");
            jumpRight++;
            updateJumpLabels();
            updateLastMovementText("➡️ RIGHT MOVEMENT DETECTED", "#00FF00");
            saveJumpDataToAPI();
            setLEDForRightJump(); // Set LED to GREEN
            showToast("Right jump detected! LED: GREEN");
            showMovementFlash("mwm_jump_right2");
            speakIfProd("Right movement detected");

            // confirm user-selected movement (if any)
            if (selectedMovement != null) {
                speakIfProd("You performed your " + selectedMovement.toLowerCase() + " jump!");
                selectedMovement = null;
            }
            isManualModeActive = false;
        }
        @Override public void onMiddle() {
            if (!isAutoDetectionEnabled && !isManualModeActive) return;
            Log.i("TerminalFragment", "🎯 GYRO EVENT: FORWARD movement detected!");
            jumpUp++;
            updateJumpLabels();
            updateLastMovementText("⬆️ FORWARD MOVEMENT DETECTED", "#FFFFFF");
            saveJumpDataToAPI();
            setLEDForForwardJump(); // Set LED to WHITE
            showToast("Up jump detected! LED: WHITE");
            showMovementFlash("mwm_jump_bounce2");
            speakIfProd("Forward movement detected");

            // confirm user-selected movement (if any)
            if (selectedMovement != null) {
                speakIfProd("You performed your " + selectedMovement.toLowerCase() + " jump!");
                selectedMovement = null;
            }
            isManualModeActive = false;
        }
        @Override public void onBack() {
           // if (!isAutoDetectionEnabled) return;
            if (!isAutoDetectionEnabled && !isManualModeActive) return;
            Log.i("TerminalFragment", "🎯 GYRO EVENT: BACK movement detected!");
            jumpBack++;
            updateJumpLabels();
            updateLastMovementText("⬇️ BACK MOVEMENT DETECTED", "#FF69B4");
            saveJumpDataToAPI();
            setLEDForBackJump(); // Set LED to PINK
            showToast("Back jump detected! LED: PINK");
            showMovementFlash("mwm_jump_bounce2"); // Reusing bounce GIF
            speakIfProd("Back movement detected");

            // confirm user-selected movement (if any)
            if (selectedMovement != null) {
                speakIfProd("You performed your " + selectedMovement.toLowerCase() + " jump!");
                selectedMovement = null;
            }
            isManualModeActive = false;
        }

        @Override
        public void onRaw(float x, float y, float z) {
            // Log raw gyroscope data periodically to verify sensor is working
            if (System.currentTimeMillis() % 5000 < 50) {
                Log.d("TerminalFragment", String.format("🔄 GYRO RAW: x=%.3f y=%.3f z=%.3f", x, y, z));
            }
        }
    };
    
    private final GyroManager.CalibrationListener calibrationListener = new GyroManager.CalibrationListener() {
        @Override
        public void onCalibrationStart() {
            Activity activity = getActivity();
            if (activity != null && tvCalibrationPrompt != null) {
                activity.runOnUiThread(() -> {
                    tvCalibrationPrompt.setVisibility(View.VISIBLE);
                    tvCalibrationPrompt.setText("🔧 Calibration Starting...");
                    tvCalibrationPrompt.setTextColor(0xFFFFFFFF); // white
                });
            }
        }

        @Override
        public void onCalibrationPhase(String instruction, int color) {
            Activity activity = getActivity();
            if (activity != null && tvCalibrationPrompt != null) {
                activity.runOnUiThread(() -> {
                    tvCalibrationPrompt.setVisibility(View.VISIBLE);
                    tvCalibrationPrompt.setText(instruction);
                    tvCalibrationPrompt.setTextColor(color);
                });
            }
        }

        @Override
        public void onCalibrationComplete() {
            Activity activity = getActivity();
            if (activity != null && tvCalibrationPrompt != null) {
                activity.runOnUiThread(() -> {
                    tvCalibrationPrompt.setText("✅ Calibration Complete! Motion detection active.");
                    tvCalibrationPrompt.setTextColor(0xFF00FF00); // green
                    new Handler().postDelayed(() -> {
                        if (tvCalibrationPrompt != null) {
                            tvCalibrationPrompt.setVisibility(View.GONE);
                        }
                    }, 2000); // Hide after 2 seconds
                });
            }
        }
    };

    private void toggleGyro() {
        // Cycle through: ESP32 → Phone Gyro → Second Phone → ESP32...
        Log.i("TerminalFragment", "🔄 TOGGLE: Current mode before switch: " + currentSensorMode);
        switch (currentSensorMode) {
            case ESP32_ADXL345:
                // Switch to Primary Phone Gyroscope mode
                currentSensorMode = SensorMode.PHONE_GYRO;
                gyroEnabled = true;
                
                // Stop ESP32 processing, start phone gyroscope
                initializePhoneGyroSensors();
                
                Toast.makeText(getActivity(), "Switched to Primary Phone Gyroscope", Toast.LENGTH_SHORT).show();
                Log.i("TerminalFragment", "🔄 Switched to Primary Phone Gyroscope for movement detection");
                updateLastMovementText("📱 Primary phone gyroscope active", "#FFD700");
                Log.i("TerminalFragment", "🔄 TOGGLE: Switched to PHONE_GYRO mode");
                break;
            
            case PHONE_GYRO:
                // Switch to Second Phone Gyroscope mode
                currentSensorMode = SensorMode.SECOND_PHONE_GYRO;
                gyroEnabled = true;
                
                // Stop phone gyroscope
                if (sensorManager != null && phoneSensorListener != null) {
                    sensorManager.unregisterListener(phoneSensorListener);
                }
                
                Toast.makeText(getActivity(), "Switched to Second Phone Gyroscope mode", Toast.LENGTH_SHORT).show();
                Log.i("TerminalFragment", "📱📱 Switched to Second Phone Gyroscope for movement detection");
                updateLastMovementText("📱📱 Second phone gyroscope movement detection active", "#9C27B0");
                Log.i("TerminalFragment", "🔄 TOGGLE: Switched to SECOND_PHONE_GYRO mode");
                break;
            
            case SECOND_PHONE_GYRO:
                // Switch back to ESP32 mode
                currentSensorMode = SensorMode.ESP32_ADXL345;
                gyroEnabled = true;
                
                Toast.makeText(getActivity(), "Switched to ESP32 ADXL345 mode", Toast.LENGTH_SHORT).show();
                Log.i("TerminalFragment", "🔧 Switched to ESP32 ADXL345 for movement detection");
                updateLastMovementText("🔧 ESP32 ADXL345 movement detection active", "#FF5722");
                Log.i("TerminalFragment", "🔄 TOGGLE: Switched to ESP32_ADXL345 mode");
                break;
        }
        
        // Update button text and color
        updateGyroToggleButton();
    }
    
    private void updateGyroToggleButton() {
        Log.i("TerminalFragment", "🔄 BUTTON UPDATE: Current mode: " + currentSensorMode + ", gyroEnabled: " + gyroEnabled);
        if (btnGyroToggle != null) {
            switch (currentSensorMode) {
                case PHONE_GYRO:
                    btnGyroToggle.setText("Mode: PHONE " + (gyroEnabled ? "ON" : "OFF"));
                    btnGyroToggle.setBackgroundColor(gyroEnabled ? 0xFF4CAF50 : 0xFFFF5722);
                    Log.i("TerminalFragment", "🔄 BUTTON: Set to PHONE mode - " + (gyroEnabled ? "ON" : "OFF"));
                    break;
                case SECOND_PHONE_GYRO:
                    btnGyroToggle.setText("Mode: PHONE2 " + (gyroEnabled ? "ON" : "OFF"));
                    btnGyroToggle.setBackgroundColor(gyroEnabled ? 0xFF9C27B0 : 0xFFFF5722);
                    Log.i("TerminalFragment", "🔄 BUTTON: Set to PHONE2 mode - " + (gyroEnabled ? "ON" : "OFF"));
                    break;
                case ESP32_ADXL345:
                default:
                    btnGyroToggle.setText("Mode: ESP32 " + (connected == Connected.True ? "CONNECTED" : "DISCONNECTED"));
                    btnGyroToggle.setBackgroundColor(connected == Connected.True ? 0xFF2196F3 : 0xFFFF5722);
                    Log.i("TerminalFragment", "🔄 BUTTON: Set to ESP32 mode - " + (connected == Connected.True ? "CONNECTED" : "DISCONNECTED"));
                    break;
            }
        }
    }

    private void initModeSwitch(Context context) {
        // Hide in release builds
        if (switchMode == null) return;
        if (!BuildConfig.DEBUG) {
            switchMode.setVisibility(View.GONE);
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE);
        isProductionMode = prefs.getBoolean(PREF_PROD_MODE, true);
        switchMode.setChecked(isProductionMode);
        switchMode.setText(isProductionMode ? "Production Mode" : "Development Mode");
        switchMode.setOnCheckedChangeListener((btn, checked) -> {
            isProductionMode = checked;
            btn.setText(checked ? "Production Mode" : "Development Mode");
            prefs.edit().putBoolean(PREF_PROD_MODE, checked).apply();
            Toast.makeText(context, checked ? "Production mode ON" : "Development mode ON", Toast.LENGTH_SHORT).show();
        });
    }

    private void initTts(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                try {
                    tts.setLanguage(java.util.Locale.getDefault());
                } catch (Exception e) {
                    Log.w("TerminalFragment", "TTS language set failed: " + e.getMessage());
                }
            } else {
                Log.w("TerminalFragment", "TTS init failed with status " + status);
            }
        });
    }

    private void speakIfProd(String phrase) {
        if (!isProductionMode) return;
        try {
            if (tts != null) {
                tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "motion-" + System.currentTimeMillis());
            }
        } catch (Exception e) {
            Log.w("TerminalFragment", "TTS speak failed: " + e.getMessage());
        }
    }

    private void initializeGyroAutomatically() {
        // DISABLED: Phone gyroscope no longer used - using ESP32 + ADXL345 instead
        Log.i("TerminalFragment", "�➡️�🔧 Phone gyroscope disabled - using ESP32 + ADXL345 for movement detection");
        updateLastMovementText("🔧 ESP32 + ADXL345 movement detection active", "#00FFFF");
        
        // Still test LED connection since ESP32 controls LEDs
        testLEDConnection();
    }

    private void testLEDConnection() {
        Log.d("TerminalFragment", "Testing LED connection...");
        Log.d("TerminalFragment", "Bluetooth connected: " + (connected == Connected.True));
        Log.d("TerminalFragment", "Service available: " + (service != null));
        
        if (connected == Connected.True && service != null) {
            Log.i("TerminalFragment", "🔵 Testing LED - sending BLUE command");
            send("b"); // Send blue as test
            
            // Schedule a sequence of color tests
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                Log.i("TerminalFragment", "🔴 Testing LED - sending RED command");
                send("r");
            }, 1000);
            
            handler.postDelayed(() -> {
                Log.i("TerminalFragment", "🟢 Testing LED - sending GREEN command"); 
                send("g");
            }, 2000);
            
            handler.postDelayed(() -> {
                Log.i("TerminalFragment", "⚪ Testing LED - sending WHITE command");
                send("w");
            }, 3000);
        } else {
            Log.w("TerminalFragment", "❌ Cannot test LED - Bluetooth not connected");
            if (connected != Connected.True) {
                Log.w("TerminalFragment", "- Connection status: " + connected);
                setLEDForConnectionLost();
            }
            if (service == null) {
                Log.w("TerminalFragment", "- Serial service is null");
            }
        }
    }

    private void testDeviceSensors() {
        Activity a = getActivity();
        if (a != null) {
            android.hardware.SensorManager sm = (android.hardware.SensorManager) a.getSystemService(Context.SENSOR_SERVICE);
            if (sm != null) {
                java.util.List<android.hardware.Sensor> sensors = sm.getSensorList(android.hardware.Sensor.TYPE_ALL);
                Log.i("TerminalFragment", "📱 Device has " + sensors.size() + " sensors total");
                
                android.hardware.Sensor gyro = sm.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
                android.hardware.Sensor accel = sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
                
                Log.i("TerminalFragment", "🔄 Gyroscope sensor: " + (gyro != null ? gyro.getName() : "NOT AVAILABLE"));
                Log.i("TerminalFragment", "📐 Accelerometer sensor: " + (accel != null ? accel.getName() : "NOT AVAILABLE"));
                
                if (gyro == null) {
                    Toast.makeText(a, "❌ No gyroscope sensor found on this device!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(a, "✅ Gyroscope sensor detected: " + gyro.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void runManualTests() {
        Log.i("TerminalFragment", "🧪 Running manual diagnostic tests...");
        Toast.makeText(getActivity(), "Running diagnostic tests...", Toast.LENGTH_SHORT).show();
        
        // Test 1: Check gyroscope status
        Activity a = getActivity();
        if (a instanceof MainActivity) {
            GyroManager gm = ((MainActivity) a).getGyroManager();
            Log.i("TerminalFragment", "🔍 GyroManager exists: " + (gm != null));
            if (gm != null) {
                Log.i("TerminalFragment", "🔍 Gyroscope available: " + gm.isAvailable());
                Log.i("TerminalFragment", "🔍 Current gyroEnabled: " + gyroEnabled);
            }
        }
        
        // Test 2: Check Bluetooth connection
        Log.i("TerminalFragment", "🔍 Bluetooth connected: " + (connected == Connected.True));
        Log.i("TerminalFragment", "🔍 Service available: " + (service != null));
        

        // Test 3: Try to manually trigger a gyro event
        if (gyroEnabled) {
            Log.i("TerminalFragment", "🧪 Manually triggering left jump...");

            jumpLeft++;
            updateJumpLabels();
            setLEDForLeftJump();
            showMovementFlash("mwm_dress_left"); // Use your new animation instead
            Toast.makeText(a, "Manual left jump triggered!", Toast.LENGTH_SHORT).show();
        }
        
        // Test 4: Try sending basic LED command
        if (connected == Connected.True) {
            Log.i("TerminalFragment", "🧪 Sending test LED command (blue)...");
            send("b");
        }
    }

    private void runLEDTestSequence() {
        Log.i("TerminalFragment", "🧪 Running manual LED test sequence...");
        if (connected != Connected.True || service == null) {
            Log.w("TerminalFragment", "❌ Cannot run LED test - Bluetooth not connected");
            Toast.makeText(getActivity(), "LED Test Failed - Bluetooth not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getActivity(), "Running LED Test Sequence...", Toast.LENGTH_SHORT).show();
        
        Handler handler = new Handler();
        
        // Red
        handler.postDelayed(() -> {
            send("r");
            Log.i("TerminalFragment", "🔴 LED Test: RED");
        }, 0);
        
        // Green  
        handler.postDelayed(() -> {
            send("g");
            Log.i("TerminalFragment", "🟢 LED Test: GREEN");
        }, 1000);
        
        // Blue
        handler.postDelayed(() -> {
            send("b");
            Log.i("TerminalFragment", "🔵 LED Test: BLUE");
        }, 2000);
        
        // White
        handler.postDelayed(() -> {
            send("w");
            Log.i("TerminalFragment", "⚪ LED Test: WHITE");
        }, 3000);
        
        // Yellow/Topaz
        handler.postDelayed(() -> {
            send("t");
            Log.i("TerminalFragment", "🟡 LED Test: YELLOW");
        }, 4000);
        
        // Pink/Lilac
        handler.postDelayed(() -> {
            send("l");
            Log.i("TerminalFragment", "🩷 LED Test: PINK");
        }, 5000);
        
        // Off
        handler.postDelayed(() -> {
            send("o");
            Log.i("TerminalFragment", "⚫ LED Test: OFF");
            Toast.makeText(getActivity(), "LED Test Complete!", Toast.LENGTH_SHORT).show();
        }, 6000);
    }

    private void runAccelTestSequence() {
        Log.i("TerminalFragment", "🧪 Running ESP32 accelerometer data simulation test...");
        Toast.makeText(getActivity(), "Testing ESP32 Movement Detection...", Toast.LENGTH_SHORT).show();
        
        Handler handler = new Handler();
        
        // Test LEFT movement
        handler.postDelayed(() -> {
            Log.i("TerminalFragment", "🧪 Simulating LEFT movement: ACCEL:3.0,0.5,0.2");
            processESP32MovementData("ACCEL:3.0,0.5,0.2");
        }, 500);
        
        // Test RIGHT movement  
        handler.postDelayed(() -> {
            Log.i("TerminalFragment", "🧪 Simulating RIGHT movement: ACCEL:-3.0,0.5,0.2");
            processESP32MovementData("ACCEL:-3.0,0.5,0.2");
        }, 2000);
        
        // Test FORWARD movement
        handler.postDelayed(() -> {
            Log.i("TerminalFragment", "🧪 Simulating FORWARD movement: ACCEL:0.2,3.0,0.5");
            processESP32MovementData("ACCEL:0.2,3.0,0.5");
        }, 3500);
        
        // Test BACK movement
        handler.postDelayed(() -> {
            Log.i("TerminalFragment", "🧪 Simulating BACK movement: ACCEL:0.2,-3.0,0.5");
            processESP32MovementData("ACCEL:0.2,-3.0,0.5");
        }, 5000);
        
        // Test direct commands
        handler.postDelayed(() -> {
            Log.i("TerminalFragment", "🧪 Testing direct command: MOVE:LEFT (should be BLUE)");
            processESP32MovementData("MOVE:LEFT");
        }, 6500);
        
        handler.postDelayed(() -> {
            Log.i("TerminalFragment", "🧪 Testing direct command: MOVE:BACK (should be RED)");
            processESP32MovementData("MOVE:BACK");
        }, 8000);
        
        handler.postDelayed(() -> {
            Toast.makeText(getActivity(), "ESP32 Movement Test Complete!", Toast.LENGTH_SHORT).show();
            Log.i("TerminalFragment", "✅ ESP32 accelerometer simulation test complete");
        }, 9500);
    }

    private void updateLastMovementText(String movement, String colorHex) {
        if (tvLastMovement != null) {
            tvLastMovement.setText(movement);
            tvLastMovement.setTextColor(android.graphics.Color.parseColor(colorHex));
            
            // Add timestamp
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
            String timestamp = sdf.format(new java.util.Date());
            tvLastMovement.setText(movement + "\n" + timestamp);
        }
    }

    private void updateJumpLabels() {
        if (tvJumpLeft != null) tvJumpLeft.setText(String.valueOf(jumpLeft));
        if (tvJumpRight != null) tvJumpRight.setText(String.valueOf(jumpRight));
        if (tvJumpMiddle != null) tvJumpMiddle.setText(String.valueOf(jumpUp));
        if (tvJumpBack != null) tvJumpBack.setText(String.valueOf(jumpBack));
        Log.d("TerminalFragment", String.format("Jump counts - Left: %d, Right: %d, Up: %d, Back: %d", 
            jumpLeft, jumpRight, jumpUp, jumpBack));
    }

    private void showToast(String message) {
        Activity a = getActivity();
        if (a != null) {
            Toast.makeText(a, message, Toast.LENGTH_SHORT).show();
        }
    }

    // Test method to manually trigger jump animations (for debugging)
    private void testJumpAnimations() {
        Log.d("TerminalFragment", "Testing jump animations");

        // Test left jump

        jumpLeft++;
        updateJumpLabels();
        showMovementFlash("mwm_dress_left"); // Use your new animation instead

        // You can call this method to test if the UI updates work
        Activity a = getActivity();
        if (a != null) {
            Toast.makeText(a, "Testing left jump animation", Toast.LENGTH_SHORT).show();
        }
    }

    // LED control methods for different jump types and connection status
    private void setLEDForLeftJump() {
        send("b"); // Blue for left jump (since yellow not available)
        Log.d("TerminalFragment", "LED set to BLUE for left jump");
    }

    private void setLEDForRightJump() {
        send("g"); // Green for right jump
        Log.d("TerminalFragment", "LED set to GREEN for right jump");
    }

    private void setLEDForForwardJump() {
        send("w"); // White for forward jump
        Log.d("TerminalFragment", "LED set to WHITE for forward jump");
    }

    private void setLEDForBackJump() {
        send("r"); // Red for back jump (since pink not available)
        Log.d("TerminalFragment", "LED set to RED for back jump");
    }

    private boolean isInConnectionLostState = false;
    
    private void setLEDForConnectionLost() {
        if (isInConnectionLostState) {
            Log.d("TerminalFragment", "Already in connection lost state - skipping LED command");
            return;
        }
        
        isInConnectionLostState = true;
        try {
            if (connected == Connected.True && service != null) {
                send("r"); // Red for connection lost
                Log.d("TerminalFragment", "LED set to RED for connection lost");
            } else {
                Log.d("TerminalFragment", "Cannot send LED command - not connected");
            }
        } catch (Exception e) {
            Log.w("TerminalFragment", "Failed to set LED for connection lost: " + e.getMessage());
        }
    }

    private void sendLEDCommand(String command, String description) {
        try {
            send(command);
            Log.d("TerminalFragment", "LED command sent: " + command + " (" + description + ")");
        } catch (Exception e) {
            Log.e("TerminalFragment", "Failed to send LED command: " + command, e);
        }
    }

    /*
     * starting with Android 14, notifications are not shown in notification bar by default when App is in background
     */

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
    
    /*
     * Auto-connect functionality
     */
    private void saveDeviceAddress() {
        if (service != null && connected == Connected.True && deviceAddress != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREF_DEVICE_ADDRESS, deviceAddress).apply();
            Log.i("TerminalFragment", "📱 Saved device address for auto-connect: " + deviceAddress);
        }
    }
    
    private String getSavedDeviceAddress() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_DEVICE_ADDRESS, null);
    }
    
    private void attemptAutoConnect() {
        String savedAddress = getSavedDeviceAddress();
        if (savedAddress == null) {
            Log.i("TerminalFragment", "📱 No saved device address - showing device selection");
            showDeviceSelectionDialog();
            return;
        }
        
        if (currentConnectAttempt >= MAX_CONNECT_ATTEMPTS) {
            Log.i("TerminalFragment", "📱 Max auto-connect attempts reached - showing device selection");
            showDeviceSelectionDialog();
            return;
        }
        
        currentConnectAttempt++;
        isAutoConnecting = true;
        Log.i("TerminalFragment", "📱 Auto-connect attempt " + currentConnectAttempt + "/" + MAX_CONNECT_ATTEMPTS + " to: " + savedAddress);
        
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w("TerminalFragment", "📱 Bluetooth not available/enabled - showing device selection");
            showDeviceSelectionDialog();
            return;
        }
        
        try {
            // Set the device address and use existing connect method
            deviceAddress = savedAddress;
            connect();
        } catch (Exception e) {
            Log.e("TerminalFragment", "📱 Auto-connect failed: " + e.getMessage());
            onSerialConnectError(e);
        }
    }
    
    private void showDeviceSelectionDialog() {
        // Reset auto-connect state
        isAutoConnecting = false;
        currentConnectAttempt = 0;
        
        // Navigate back to DevicesFragment for device selection
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
            Log.i("TerminalFragment", "📱 Navigated back to device selection");
        }
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
        isInConnectionLostState = false; // Reset connection lost state
        Log.i("TerminalFragment", "✅ Bluetooth connected successfully");
        
        // Update button to show connection status
        updateGyroToggleButton();
        
        // Save device address for auto-connect
        saveDeviceAddress();
        
        // Reset auto-connect attempt counter on successful connection
        currentConnectAttempt = 0;
        isAutoConnecting = false;
        
        // Test LED connection when Bluetooth connects
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            testLEDConnection();
        }, 500); // Small delay to ensure connection is stable
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        setLEDForConnectionLost(); // Set LED to RED
        disconnect();
        
        if (isAutoConnecting && currentConnectAttempt < MAX_CONNECT_ATTEMPTS) {
            // Retry auto-connect
            Log.i("TerminalFragment", "📱 Auto-connect failed, retrying in 2 seconds... (attempt " + currentConnectAttempt + "/" + MAX_CONNECT_ATTEMPTS + ")");
            Handler handler = new Handler();
            handler.postDelayed(() -> attemptAutoConnect(), 2000);
        } else if (isAutoConnecting) {
            // Max attempts reached, show device selection
            Log.i("TerminalFragment", "📱 Auto-connect failed after " + MAX_CONNECT_ATTEMPTS + " attempts - showing device selection");
            showDeviceSelectionDialog();
        } else {
            // Regular connection error - try to auto-reconnect if we have a saved address
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
        setLEDForConnectionLost(); // Set LED to RED
        disconnect();
        // try to auto-reconnect after short delay
        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectHandler.postDelayed(reconnectRunnable, 1500);
    }
    
    private void saveJumpDataToAPI() {
        if (auth == null) auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth != null ? auth.getCurrentUser() : null;
        if (currentUser != null) {
            JumpDataRequest jumpDataRequest = new JumpDataRequest(
                    jumpLeft,   // Send leftJump score
                    jumpRight,  // Send rightJump score
                    jumpUp,     // Send jumpUp score (was middleJump)
                    currentUser.getUid() // Send the current user's UID
            );

            // Use getApi() to get the APIHandler instance
            RetrofitInstance.getApi().saveJumpData(jumpDataRequest).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful()) {
                        // Handle successful response
                        Log.d("JumpDataAPI", "Score saved successfully: " + response.body().getMessage());
                    } else {
                        // Handle unsuccessful response
                        Log.e("JumpDataAPI", "Failed to save score: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    // Handle failure
                    Log.e("JumpDataAPI", "Error: " + t.getMessage());
                }
            });
        }
    }

    public void changeLanguage(String languageCode) {
        LocaleHelper.setLocale(getContext(), languageCode);


        // Restart activity to apply changes
        Intent intent = requireActivity().getIntent();
        requireActivity().finish();
        startActivity(intent);
    }
    
    private void initializePhoneGyroSensors() {
        Log.i("TerminalFragment", "📱 Initializing phone gyroscope for movement detection");
        
        // Phone Gyroscope initialization logic
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        
        // Log available sensors
        if (sensorManager != null) {
            Log.i("TerminalFragment", "📱 Device has " + sensorManager.getSensorList(Sensor.TYPE_ALL).size() + " sensors total");
            
            // Initialize gyroscope
            gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyroSensor != null) {
                Log.i("TerminalFragment", "🔄 Gyroscope sensor: " + gyroSensor.getName());
                sensorManager.registerListener(phoneSensorListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
                gyroEnabled = true;
            } else {
                Log.w("TerminalFragment", "❌ No gyroscope sensor available");
                Toast.makeText(getActivity(), "No gyroscope sensor available", Toast.LENGTH_SHORT).show();
            }
            
            // Initialize accelerometer for reference
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometerSensor != null) {
                Log.i("TerminalFragment", "📐 Accelerometer sensor: " + accelerometerSensor.getName());
            }
        }
        
        updateLastMovementText("📱 Phone gyroscope initialized", "#FFD700");
        updateGyroToggleButton();
        
        // Test LED connection 
        testLEDConnection();
    }
    
    // Phone sensor event listener
    private final SensorEventListener phoneSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (currentSensorMode != SensorMode.PHONE_GYRO) return;
            
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                
                // Basic threshold-based movement detection
                float threshold = 2.0f;
                
                if (Math.abs(x) > threshold) {
                    if (x > 0) {
                        handleLeftMovement();
                    } else {
                        handleRightMovement();
                    }
                } else if (Math.abs(y) > threshold) {
                    if (y > 0) {
                        handleForwardMovement();
                    } else {
                        handleBackMovement();
                    }
                }
            }
        }
        
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if needed
        }
    };

}



