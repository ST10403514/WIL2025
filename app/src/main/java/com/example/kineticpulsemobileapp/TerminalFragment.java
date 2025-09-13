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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import java.util.ArrayDeque;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.json.JSONObject;

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
    // Threshold for optional GYRO line parsing (accelerometer axes)
    private static final float ACC_THRESHOLD = 2.5f; // tune per device

    private int jumpLeft = 0;
    private int jumpRight = 0;
    private int middleJump = 0; // up
    private int jumpFront = 0;
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
    private TextView tvJumpFront;
    private TextView tvJumpBack;
    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        // Initialize Firebase Auth for API submissions tied to a user
        auth = FirebaseAuth.getInstance();
        deviceAddress = getArguments().getString("device");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
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
        tvJumpFront = view.findViewById(R.id.tvJumpFront);
        tvJumpBack = view.findViewById(R.id.tvJumpBack);
        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        showView.setOnClickListener(v -> {
            buttonsView.setVisibility(View.VISIBLE);
        });

        // Hide buttons when hideView is clicked
        hideView.setOnClickListener(v -> {
            buttonsView.setVisibility(View.GONE);
        });

        btnJumpLeft.setOnClickListener(v -> {
            sendText.setText("1");
            send("1");
            jumpLeft++;
            if (tvJumpLeft != null) tvJumpLeft.setText(String.valueOf(jumpLeft));
            saveJumpDataToAPI();
        });
        btnJumpUp.setOnClickListener(v -> {
            sendText.setText("2");
            send("2");
            middleJump++;
            if (tvJumpMiddle != null) tvJumpMiddle.setText(String.valueOf(middleJump));
            saveJumpDataToAPI();
        });
        btnJumpRight.setOnClickListener(v -> {
            sendText.setText("3");
            send("3");
            jumpRight++;
            if (tvJumpRight != null) tvJumpRight.setText(String.valueOf(jumpRight));
            saveJumpDataToAPI();
        });
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
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));
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
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
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

            // Optional: parse raw gyro lines if the device streams them
            if (msg.startsWith("GYRO") || msg.startsWith("ACC")) {
                detectFromSensorLine(msg);
            }
            // Optional: parse simple JSON messages, e.g. {"ax":0.3,"ay":-2.9} or {"direction":"Front"}
            String trimmed = msg.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                try {
                    JSONObject o = new JSONObject(trimmed);
                    if (o.has("direction")) {
                        String d = o.getString("direction").toLowerCase();
                        if (d.contains("left")) { ivJump.setImageResource(R.drawable.stickmanjumpleft); jumpLeft++; if (tvJumpLeft!=null) tvJumpLeft.setText(String.valueOf(jumpLeft)); saveJumpDataToAPI(); }
                        else if (d.contains("right")) { ivJump.setImageResource(R.drawable.stickmanjumpright); jumpRight++; if (tvJumpRight!=null) tvJumpRight.setText(String.valueOf(jumpRight)); saveJumpDataToAPI(); }
                        else if (d.contains("front") || d.contains("forward")) { ivJump.setImageResource(R.drawable.stickmanjumpup); jumpFront++; if (tvJumpFront!=null) tvJumpFront.setText(String.valueOf(jumpFront)); saveJumpDataToAPI(); }
                        else if (d.contains("back")) { ivJump.setImageResource(R.drawable.stickmanjumpup); jumpBack++; if (tvJumpBack!=null) tvJumpBack.setText(String.valueOf(jumpBack)); saveJumpDataToAPI(); }
                        else if (d.contains("up") || d.contains("middle")) { ivJump.setImageResource(R.drawable.stickmanjumpup); middleJump++; if (tvJumpMiddle!=null) tvJumpMiddle.setText(String.valueOf(middleJump)); saveJumpDataToAPI(); }
                    } else if (o.has("ax") || o.has("ay")) {
                        float ax = (float) o.optDouble("ax", 0);
                        float ay = (float) o.optDouble("ay", 0);
                        if (ax > ACC_THRESHOLD) { ivJump.setImageResource(R.drawable.stickmanjumpright); jumpRight++; if (tvJumpRight!=null) tvJumpRight.setText(String.valueOf(jumpRight)); saveJumpDataToAPI(); }
                        else if (ax < -ACC_THRESHOLD) { ivJump.setImageResource(R.drawable.stickmanjumpleft); jumpLeft++; if (tvJumpLeft!=null) tvJumpLeft.setText(String.valueOf(jumpLeft)); saveJumpDataToAPI(); }
                        if (ay > ACC_THRESHOLD) { ivJump.setImageResource(R.drawable.stickmanjumpup); jumpFront++; if (tvJumpFront!=null) tvJumpFront.setText(String.valueOf(jumpFront)); saveJumpDataToAPI(); }
                        else if (ay < -ACC_THRESHOLD) { ivJump.setImageResource(R.drawable.stickmanjumpup); jumpBack++; if (tvJumpBack!=null) tvJumpBack.setText(String.valueOf(jumpBack)); saveJumpDataToAPI(); }
                    }
                } catch (Exception ignored) {}
            }

            // Check if the message contains the specific word "jump"
            if (msg.contains("Lateral-Left Movement Detected") || msg.contains("Left")) {
                ivJump.setImageResource(R.drawable.stickmanjumpleft);
                jumpLeft++;
                if (tvJumpLeft != null) tvJumpLeft.setText(String.valueOf(jumpLeft));
                saveJumpDataToAPI();
            }
            if (msg.contains("Jump detected! Yahoo! ^^") || msg.contains("Up") || msg.contains("Middle")) {
                ivJump.setImageResource(R.drawable.stickmanjumpup);
                middleJump++;
                if (tvJumpMiddle != null) tvJumpMiddle.setText(String.valueOf(middleJump));
                saveJumpDataToAPI();
            }
            if (msg.contains("Lateral- Right Movement Detected") || msg.contains("Right")) {
                ivJump.setImageResource(R.drawable.stickmanjumpright);
                jumpRight++;
                if (tvJumpRight != null) tvJumpRight.setText(String.valueOf(jumpRight));
                saveJumpDataToAPI();
            }
            if (msg.contains("Forward") || msg.contains("Front")) {
                // no specific image resource provided for front; reuse up image for visualization
                ivJump.setImageResource(R.drawable.stickmanjumpup);
                jumpFront++;
                if (tvJumpFront != null) tvJumpFront.setText(String.valueOf(jumpFront));
                saveJumpDataToAPI();
            }
            if (msg.contains("Backward") || msg.contains("Back")) {
                // no specific image resource provided for back; reuse up image for visualization
                ivJump.setImageResource(R.drawable.stickmanjumpup);
                jumpBack++;
                if (tvJumpBack != null) tvJumpBack.setText(String.valueOf(jumpBack));
                saveJumpDataToAPI();
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

    /*
     * starting with Android 14, notifications are not shown in notification bar by default when App is in background
     */

    private void showNotificationSettings() {
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("android.provider.extra.APP_PACKAGE", getActivity().getPackageName());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(Arrays.equals(permissions, new String[]{Manifest.permission.POST_NOTIFICATIONS}) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !service.areNotificationsEnabled())
            showNotificationSettings();
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
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
    }

    private void saveJumpDataToAPI() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // For compatibility with current API, send middleJump as combined up/front/back
            int combinedMiddle = middleJump + jumpFront + jumpBack;
            JumpDataRequest jumpDataRequest = new JumpDataRequest(
                    jumpLeft,
                    jumpRight,
                    combinedMiddle,
                    currentUser.getUid()
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



    // Very lightweight parser for lines like: "GYRO ax=0.3 ay=-3.2 az=9.7 gx=.. gy=.. gz=.."
    private void detectFromSensorLine(String line) {
        try {
            float ax = extractValue(line, "ax");
            float ay = extractValue(line, "ay");
            // Simple threshold-based direction detection using accelerometer axes
            if (ax > ACC_THRESHOLD) {
                ivJump.setImageResource(R.drawable.stickmanjumpright);
                jumpRight++;
                if (tvJumpRight != null) tvJumpRight.setText(String.valueOf(jumpRight));
                saveJumpDataToAPI();
            } else if (ax < -ACC_THRESHOLD) {
                ivJump.setImageResource(R.drawable.stickmanjumpleft);
                jumpLeft++;
                if (tvJumpLeft != null) tvJumpLeft.setText(String.valueOf(jumpLeft));
                saveJumpDataToAPI();
            }
            if (ay > ACC_THRESHOLD) {
                ivJump.setImageResource(R.drawable.stickmanjumpup);
                jumpFront++;
                if (tvJumpFront != null) tvJumpFront.setText(String.valueOf(jumpFront));
                saveJumpDataToAPI();
            } else if (ay < -ACC_THRESHOLD) {
                ivJump.setImageResource(R.drawable.stickmanjumpup);
                jumpBack++;
                if (tvJumpBack != null) tvJumpBack.setText(String.valueOf(jumpBack));
                saveJumpDataToAPI();
            }
        } catch (Exception ignored) {
        }
    }

    private float extractValue(String line, String key) {
        int i = line.indexOf(key);
        if (i < 0) throw new IllegalArgumentException();
        int start = -1, end = -1;
        for (int p = i + key.length(); p < line.length(); p++) {
            char c = line.charAt(p);
            if ((c == ':' || c == '=') && start < 0) {
                // move to first digit sign after separator
                for (int q = p + 1; q < line.length(); q++) {
                    char d = line.charAt(q);
                    if ((d >= '0' && d <= '9') || d == '-' || d == '+') { start = q; break; }
                }
            } else if (start >= 0) {
                if (!((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+')) { end = p; break; }
            }
        }
        if (start < 0) throw new IllegalArgumentException();
        if (end < 0) end = line.length();
        return Float.parseFloat(line.substring(start, end).trim());
    }
}



