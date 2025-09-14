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
import com.google.firebase.auth.FirebaseUser;


import java.util.ArrayDeque;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }
    private FirebaseAuth auth;
    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;
    private static final int REQ_BT_CONNECT = 1001;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private final Runnable reconnectRunnable = () -> {
        if (connected != Connected.True) connect();
    };

    private int jumpLeft = 0;
    private int jumpRight = 0;
    private int jumpUp = 0; // simplified: just one up counter
    private int jumpBack = 0;

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

    private Button btnJumpUp;

    private Button btnJumpLeft;

    private Button btnJumpRight;

    private View buttonsView;

    private View hideView;

    private View showView;

    private ImageView ivJump;

    private TextView tvJumpLeft;

    private TextView tvJumpRight;

    private TextView tvJumpMiddle;
    
    private TextView tvJumpBack;
    
    private TextView tvLastMovement;
    
    private TextView tvCalibrationPrompt;
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
            if (a != null) a.runOnUiThread(this::connect);
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
        ivJump = view.findViewById(R.id.ivJump);
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
            } else {
                send(text);
            }
        });

        btnGyroToggle.setOnClickListener(v -> toggleGyro());
        
        // Initialize gyro button text
        btnGyroToggle.setText(gyroEnabled ? "Gyro: ON" : "Gyro: OFF");
        
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

    private void disconnect() {
        connected = Connected.False;
        if (service != null) service.disconnect();
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

            // Check if the message contains the specific word "jump"
            if (msg.contains("Lateral-Left Movement Detected")) {
                if (ivJump != null) ivJump.setImageResource(R.drawable.stickmanjumpleft);
                jumpLeft++;
                updateJumpLabels();
                saveJumpDataToAPI();
                setLEDForLeftJump(); // Set LED to YELLOW
            }
            if (msg.contains("Jump detected! Yahoo! ^^")) {
                if (ivJump != null) ivJump.setImageResource(R.drawable.stickmanjumpup);
                jumpUp++;
                updateJumpLabels();
                saveJumpDataToAPI();
                setLEDForForwardJump(); // Set LED to WHITE
            }
            if (msg.contains("Lateral- Right Movement Detected")) {
                if (ivJump != null) ivJump.setImageResource(R.drawable.stickmanjumpright);
                jumpRight++;
                updateJumpLabels();
                saveJumpDataToAPI();
                setLEDForRightJump(); // Set LED to GREEN
            }
            // Check for back jump detection (if you have this message)
            if (msg.contains("Back Movement Detected") || msg.contains("Backward Jump")) {
                if (ivJump != null) ivJump.setImageResource(R.drawable.stickmanjumpup);
                jumpBack++;
                updateJumpLabels();
                saveJumpDataToAPI();
                setLEDForBackJump(); // Set LED to PINK
            }
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

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    private boolean gyroEnabled = false;
    private final GyroManager.MovementListener gyroListener = new GyroManager.MovementListener() {
        @Override public void onLeft() {
            Log.i("TerminalFragment", "🎯 GYRO EVENT: LEFT movement detected!");
            if (ivJump != null) ivJump.setImageResource(R.drawable.stickmanjumpleft);
            jumpLeft++;
            updateJumpLabels();
            updateLastMovementText("⬅️ LEFT MOVEMENT DETECTED", "#FFD700");
            saveJumpDataToAPI();
            setLEDForLeftJump(); // Set LED to YELLOW
            showToast("Left jump detected! LED: YELLOW");
        }
        @Override public void onRight() {
            Log.i("TerminalFragment", "🎯 GYRO EVENT: RIGHT movement detected!");
            if (ivJump != null) ivJump.setImageResource(R.drawable.stickmanjumpright);
            jumpRight++;
            updateJumpLabels();
            updateLastMovementText("➡️ RIGHT MOVEMENT DETECTED", "#00FF00");
            saveJumpDataToAPI();
            setLEDForRightJump(); // Set LED to GREEN
            showToast("Right jump detected! LED: GREEN");
        }
        @Override public void onMiddle() {
            Log.i("TerminalFragment", "🎯 GYRO EVENT: FORWARD movement detected!");
            if (ivJump != null) ivJump.setImageResource(R.drawable.stickmanjumpup);
            jumpUp++;
            updateJumpLabels();
            updateLastMovementText("⬆️ FORWARD MOVEMENT DETECTED", "#FFFFFF");
            saveJumpDataToAPI();
            setLEDForForwardJump(); // Set LED to WHITE
            showToast("Up jump detected! LED: WHITE");
        }
        @Override public void onBack() {
            Log.i("TerminalFragment", "🎯 GYRO EVENT: BACK movement detected!");
            if (ivJump != null) ivJump.setImageResource(R.drawable.stickmanjumpup);
            jumpBack++;
            updateJumpLabels();
            updateLastMovementText("⬇️ BACK MOVEMENT DETECTED", "#FF69B4");
            saveJumpDataToAPI();
            setLEDForBackJump(); // Set LED to PINK
            showToast("Back jump detected! LED: PINK");
        }
        @Override public void onRaw(float x, float y, float z) {
            // Log raw gyroscope data periodically to verify sensor is working
            if (System.currentTimeMillis() % 2000 < 50) { // Log every ~2 seconds
                Log.d("TerminalFragment", String.format("🔄 GYRO RAW: x=%.3f y=%.3f z=%.3f", x, y, z));
            }
        }
    };
    
    private final GyroManager.CalibrationListener calibrationListener = new GyroManager.CalibrationListener() {
        @Override
        public void onCalibrationStart() {
            getActivity().runOnUiThread(() -> {
                tvCalibrationPrompt.setVisibility(View.VISIBLE);
                tvCalibrationPrompt.setText("🔧 Calibration Starting...");
                tvCalibrationPrompt.setTextColor(0xFFFFFFFF); // white
            });
        }

        @Override
        public void onCalibrationPhase(String instruction, int color) {
            getActivity().runOnUiThread(() -> {
                tvCalibrationPrompt.setVisibility(View.VISIBLE);
                tvCalibrationPrompt.setText(instruction);
                tvCalibrationPrompt.setTextColor(color);
            });
        }

        @Override
        public void onCalibrationComplete() {
            getActivity().runOnUiThread(() -> {
                tvCalibrationPrompt.setText("✅ Calibration Complete! Motion detection active.");
                tvCalibrationPrompt.setTextColor(0xFF00FF00); // green
                new Handler().postDelayed(() -> {
                    tvCalibrationPrompt.setVisibility(View.GONE);
                }, 2000); // Hide after 2 seconds
            });
        }
    };

    private void toggleGyro() {
        Activity a = getActivity();
        if (a instanceof MainActivity) {
            GyroManager gm = ((MainActivity) a).getGyroManager();
            if (gm == null || !gm.isAvailable()) {
                Toast.makeText(a, "Gyroscope not available on this device", Toast.LENGTH_LONG).show();
                Log.w("TerminalFragment", "Gyroscope not available");
                return;
            }
            
            gyroEnabled = !gyroEnabled;
            Log.i("TerminalFragment", "Gyro toggled: " + (gyroEnabled ? "ON" : "OFF"));
            
            if (gyroEnabled) {
                gm.setMovementListener(gyroListener);
                gm.setCalibrationListener(calibrationListener);
                gm.setProcessingEnabled(true);
                gm.start(); // Start the gyroscope sensor
                Toast.makeText(a, "Gyroscope enabled - move your device to detect jumps!", Toast.LENGTH_LONG).show();
                btnGyroToggle.setText("Gyro: ON");
            } else {
                gm.setMovementListener(null);
                gm.setProcessingEnabled(false);
                gm.stop(); // Stop the gyroscope sensor
                Toast.makeText(a, "Gyroscope disabled", Toast.LENGTH_SHORT).show();
                btnGyroToggle.setText("Gyro: OFF");
            }
        } else {
            Toast.makeText(a, "MainActivity not found", Toast.LENGTH_SHORT).show();
            Log.e("TerminalFragment", "Activity is not MainActivity");
        }
    }

    private void initializeGyroAutomatically() {
        Activity a = getActivity();
        Log.d("TerminalFragment", "🔧 GYRO INIT: Starting automatic gyroscope initialization...");
        
        if (a instanceof MainActivity) {
            GyroManager gm = ((MainActivity) a).getGyroManager();
            Log.d("TerminalFragment", "🔧 GYRO INIT: Activity is MainActivity ✓");
            Log.d("TerminalFragment", "🔧 GYRO INIT: GyroManager exists: " + (gm != null));
            
            if (gm != null) {
                Log.d("TerminalFragment", "🔧 GYRO INIT: Gyroscope sensor available: " + gm.isAvailable());
                Log.d("TerminalFragment", "🔧 GYRO INIT: Current gyroEnabled state: " + gyroEnabled);
                
                if (gm.isAvailable() && !gyroEnabled) {
                    Log.i("TerminalFragment", "🔧 GYRO INIT: ✅ All conditions met, enabling gyroscope...");
                    
                    // Automatically enable gyroscope when the fragment is created
                    gyroEnabled = true;
                    gm.setMovementListener(gyroListener);
                    gm.setCalibrationListener(calibrationListener);
                    gm.setProcessingEnabled(true);
                    gm.start(); // ⭐ IMPORTANT: Start the gyroscope sensor
                    btnGyroToggle.setText("Gyro: ON");
                    
                    Log.i("TerminalFragment", "🔧 GYRO INIT: ✅ Gyroscope automatically initialized and enabled");
                    Log.i("TerminalFragment", "🔧 GYRO INIT: ✅ Movement listener set: " + (gyroListener != null));
                    Log.i("TerminalFragment", "🔧 GYRO INIT: ✅ Processing enabled: true");
                    Log.i("TerminalFragment", "🔧 GYRO INIT: ✅ Sensor started: true");
                    
                    Toast.makeText(a, "Gyroscope auto-enabled - ready to detect movements!", Toast.LENGTH_SHORT).show();
                    
                    // Test LED connection immediately after gyro init
                    testLEDConnection();
                } else {
                    Log.w("TerminalFragment", "🔧 GYRO INIT: ❌ Conditions not met:");
                    if (!gm.isAvailable()) Log.w("TerminalFragment", "🔧 GYRO INIT: - Gyroscope sensor not available");
                    if (gyroEnabled) Log.w("TerminalFragment", "🔧 GYRO INIT: - Gyroscope already enabled");
                }
            } else {
                Log.e("TerminalFragment", "🔧 GYRO INIT: ❌ GyroManager is null");
            }
        } else {
            Log.e("TerminalFragment", "🔧 GYRO INIT: ❌ Activity is not MainActivity: " + (a != null ? a.getClass().getSimpleName() : "null"));
        }
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
            if (ivJump != null) ivJump.setImageResource(R.drawable.stickmanjumpleft);
            jumpLeft++;
            updateJumpLabels();
            setLEDForLeftJump();
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
        ivJump.setImageResource(R.drawable.stickmanjumpleft);
        jumpLeft++;
        updateJumpLabels();
        
        // You can call this method to test if the UI updates work
        Activity a = getActivity();
        if (a != null) {
            Toast.makeText(a, "Testing left jump animation", Toast.LENGTH_SHORT).show();
        }
    }

    // LED control methods for different jump types and connection status
    private void setLEDForLeftJump() {
        send("t"); // Topaz/Yellow for left jump
        Log.d("TerminalFragment", "LED set to YELLOW for left jump");
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
        send("l"); // Lilac/Pink for back jump
        Log.d("TerminalFragment", "LED set to PINK for back jump");
    }

    private void setLEDForConnectionLost() {
        send("r"); // Red for connection lost
        Log.d("TerminalFragment", "LED set to RED for connection lost");
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
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
        Log.i("TerminalFragment", "✅ Bluetooth connected successfully");
        
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
        // try to auto-reconnect after short delay
        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectHandler.postDelayed(reconnectRunnable, 1500);
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

}



