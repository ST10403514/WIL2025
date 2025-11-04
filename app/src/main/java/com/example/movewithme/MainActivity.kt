package com.example.movewithme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val bleManager: BleManager by lazy { BleManager(applicationContext) }
    private val gyroManager: GyroManager by lazy { GyroManager(applicationContext) }

    private var pendingPermissionAction: (() -> Unit)? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val granted = results.values.all { it }
            val action = pendingPermissionAction
            pendingPermissionAction = null
            if (granted) {
                action?.invoke()
            } else {
                Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get selected character from intent
        val selectedCharacter = intent.getStringExtra("selected_character")
            ?: CharacterSelectionActivity.getSelectedCharacter(applicationContext)

        setContent {
            MaterialTheme {
                val viewModel: MotionViewModel = viewModel(
                    factory = MotionViewModelFactory(bleManager, applicationContext, gyroManager)
                )

                // Set selected character in ViewModel
                viewModel.setSelectedCharacter(selectedCharacter)

                val uiState by viewModel.uiState.collectAsState()

                KineticPulseScreen(
                    uiState = uiState,
                    selectedCharacter = selectedCharacter,
                    onConnect = { ensurePermissions { viewModel.connect() } },
                    onDisconnect = { viewModel.disconnect() },
                    onModeToggle = { viewModel.toggleSensorMode() },
                    onStart = { viewModel.sendCoreCommand("ON") },
                    onCalibrate = { viewModel.sendCoreCommand("CAL") },
                    onStop = { viewModel.sendCoreCommand("OFF") },
                    onResetCounts = { viewModel.resetCounts() },
                    onMovementSelect = { movement ->
                        // Trigger manual movement selection for practice
                        when (movement) {
                            "left" -> viewModel.sendLedCommand('b')
                            "right" -> viewModel.sendLedCommand('g')
                            "up" -> viewModel.sendLedCommand('w')
                            "dance" -> viewModel.sendLedCommand('n')
                        }
                    },
                    onLedCommand = { viewModel.sendLedCommand(it) }
                )
            }
        }
    }

    private fun ensurePermissions(onGranted: () -> Unit) {
        val needed = requiredPermissions().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            onGranted()
        } else {
            pendingPermissionAction = onGranted
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun requiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }
        permissions += Manifest.permission.ACCESS_FINE_LOCATION
        return permissions
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamlineScreen(
    uiState: MotionUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onPing: () -> Unit,
    onCalibrate: () -> Unit,
    onStreamToggle: () -> Unit,
    onResetCounts: () -> Unit,
    onSetSampleRate: (Int) -> Unit,
    onSendLedCommand: (Char) -> Unit,
    onSetBrightness: (Int) -> Unit,
    onClearLog: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ESP32 Gyro Stream") })
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectionCard(uiState, onConnect, onDisconnect, onPing, onCalibrate, onStreamToggle, onResetCounts, onSetSampleRate)
            TelemetryCard(uiState)
            LedControlCard(onSendLedCommand, onSetBrightness, uiState.brightness)
            DebugConsoleCard(uiState.logLines, onClearLog)
        }
    }
}

@Composable
private fun ConnectionCard(
    uiState: MotionUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onPing: () -> Unit,
    onCalibrate: () -> Unit,
    onStreamToggle: () -> Unit,
    onResetCounts: () -> Unit,
    onSetSampleRate: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Connection",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            val connectionColor = when (uiState.connectionState) {
                "Connected" -> Color(0xFF4CAF50)
                "Connecting", "Scanning" -> Color(0xFFFFA000)
                else -> Color(0xFFB00020)
            }

            Text(
                text = uiState.connectionState,
                fontSize = 16.sp,
                color = connectionColor
            )

            Text(text = "Firmware: ${uiState.firmwareVersion ?: "-"}", fontSize = 14.sp, color = Color.Black)
            Text(text = "WHO_AM_I: ${uiState.whoAmI ?: "-"}", fontSize = 14.sp, color = Color.Black)
            Text(text = "IMU Status: ${uiState.imuStatus}", fontSize = 14.sp, color = Color.Black)
            Text(text = "Device responsiveness: OK", fontSize = 14.sp, color = Color.Black)
            Text(text = "Last command: -", fontSize = 14.sp, color = Color.Black)

            Spacer(modifier = Modifier.height(4.dp))

            // Connect + Ping + STRICT mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isConnected = uiState.connectionState == "Connected"
                Button(
                    onClick = { if (isConnected) onDisconnect() else onConnect() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isConnected) "Disconnect" else "Connect")
                }

                Button(onClick = onPing, modifier = Modifier.weight(1f)) {
                    Text("Ping")
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF444444))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "STRICT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Calibrate + Start Stream
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onCalibrate, modifier = Modifier.weight(1f)) {
                    Text("Calibrate")
                }

                Button(onClick = onStreamToggle, modifier = Modifier.weight(1f)) {
                    Text(if (uiState.gyroState == "ON") "Stop Stream" else "Start Stream")
                }
            }

            // Reset Counts
            Button(onClick = onResetCounts, modifier = Modifier.fillMaxWidth()) {
                Text("Reset Counts")
            }

            // Sample Rate
            var rateText by remember { mutableStateOf("") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it.filter { c -> c.isDigit() }.take(3) },
                    placeholder = { Text("Sample rate (Hz)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Button(onClick = {
                    val rate = rateText.toIntOrNull()
                    if (rate != null && rate in 10..200) {
                        onSetSampleRate(rate)
                    }
                }) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
private fun TelemetryCard(uiState: MotionUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Live Telemetry",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Left count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Left", fontSize = 16.sp, color = Color.Black)
                Text(text = "${uiState.leftCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            // Right count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Right", fontSize = 16.sp, color = Color.Black)
                Text(text = "${uiState.rightCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            // Forward count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Forward", fontSize = 16.sp, color = Color.Black)
                Text(text = "${uiState.forwardCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            // Divider
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFCCCCCC))
            )

            // Acceleration
            Text(
                text = "Acceleration (g)",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "ax: ${uiState.lastAx.format(2)}   ay: ${uiState.lastAy.format(2)}   az: ${uiState.lastAz.format(2)}",
                fontSize = 14.sp,
                color = Color.Black
            )

            // Gyroscope
            Text(
                text = "Gyro (°/s)",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "gx: ${uiState.lastGx.format(2)}   gy: ${uiState.lastGy.format(2)}   gz: ${uiState.lastGz.format(2)}",
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun LedControlCard(
    onSendLedCommand: (Char) -> Unit,
    onSetBrightness: (Int) -> Unit,
    currentBrightness: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "LED Control",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Row 1: Red, Green, Blue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onSendLedCommand('r') }, modifier = Modifier.weight(1f)) {
                    Text("Red")
                }
                Button(onClick = { onSendLedCommand('g') }, modifier = Modifier.weight(1f)) {
                    Text("Green")
                }
                Button(onClick = { onSendLedCommand('b') }, modifier = Modifier.weight(1f)) {
                    Text("Blue")
                }
            }

            // Row 2: White, Yellow, Topaz
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onSendLedCommand('w') }, modifier = Modifier.weight(1f)) {
                    Text("White")
                }
                Button(onClick = { onSendLedCommand('y') }, modifier = Modifier.weight(1f)) {
                    Text("Yellow")
                }
                Button(onClick = { onSendLedCommand('t') }, modifier = Modifier.weight(1f)) {
                    Text("Topaz")
                }
            }

            // Row 3: Lilac, Rainbow, Strobe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onSendLedCommand('l') }, modifier = Modifier.weight(1f)) {
                    Text("Lilac")
                }
                Button(onClick = { onSendLedCommand('a') }, modifier = Modifier.weight(1f)) {
                    Text("Rainbow")
                }
                Button(onClick = { onSendLedCommand('m') }, modifier = Modifier.weight(1f)) {
                    Text("Strobe")
                }
            }

            // LED Off button
            Button(onClick = { onSendLedCommand('o') }, modifier = Modifier.fillMaxWidth()) {
                Text("LED Off")
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Brightness control
            var sliderValue by remember(currentBrightness) { mutableFloatStateOf(currentBrightness.toFloat()) }
            Text(text = "Brightness: ${sliderValue.roundToInt()}", color = Color.Black)
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onSetBrightness(sliderValue.roundToInt()) },
                valueRange = 0f..255f
            )
        }
    }
}

@Composable
private fun DebugConsoleCard(logLines: List<String>, onClearLog: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Debug Console",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Button(onClick = onClearLog, modifier = Modifier.padding(top = 8.dp)) {
                Text("Clear Log")
            }

            Text(
                text = "Bias: -",
                fontSize = 14.sp,
                color = Color.Black,
                modifier = Modifier.padding(top = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFFF5F5F5))
                    .padding(8.dp)
            ) {
                LazyColumn {
                    items(logLines) { line ->
                        Text(
                            text = line,
                            fontSize = 13.sp,
                            color = Color.Black,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private fun Float.format(decimals: Int): String {
    return String.format(java.util.Locale.US, "%.${decimals}f", this)
}
