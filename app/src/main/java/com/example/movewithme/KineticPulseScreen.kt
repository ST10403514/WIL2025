//Code Attribution
//For the below code we have used these sources to help guide us:
// TensorFlow, 2018. Add TensorFlow Lite to your Android App (TensorFlow Tip of the Week). [video online] Available at: < https://youtu.be/RhjBDxpAOIc?si=hRXBQqA-vKmhZW4m> [Accessed 22 March 2025].
// Mammothlnteractive, 2021. Build Android App with TensorFlow Lite Machine Learning Model. [video online] Available at: < https://youtu.be/o5c2BLrxNyA?si=6FQGl8tCBWBbOB1B> [Accessed 22 March 2025]. 
// DIY TechRush, 2022. ESP32 Bluetooth Classic - ESP32 Beginner's Guide. [video online] Available at: < https://youtu.be/EWxM8Ixnrqo?si=SnWwI6mTLC3qOQze> [Accessed 16 March 2025].
//DroneBot Workshop, 2024. Bluetooth Classic & BLE with ESP32. [video online] Available at: < https://youtu.be/0Q_4q1zU6Zc?si=eCU72Qw5J1fP4eLG> [Accessed 16 March 2025].
// ATECHS, 2025. MPU6050 Explained | Motion Detection with ESP32 and Arduino!?. [video online] Available at: < https://youtu.be/rpCcZZJeodY?si=Ka3BX6-anw7aJjSY> [Accessed 17 March 2025].

package com.example.movewithme

import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun KineticPulseScreen(
    uiState: MotionUiState,
    selectedCharacter: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onModeToggle: () -> Unit,
    onStart: () -> Unit,
    onCalibrate: () -> Unit,
    onStop: () -> Unit,
    onResetCounts: () -> Unit,
    onMovementSelect: (String) -> Unit,
    onLedCommand: (Char) -> Unit
) {
    val scrollState = rememberScrollState()
    var isAutoMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFE5B4))
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        // Super Fun Header
        HeaderCard(
            onModeToggle = {
                isAutoMode = !isAutoMode
                onModeToggle()
            },
            isAutoMode = isAutoMode
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Connection Card
        ConnectionCard(
            connectionState = uiState.connectionState,
            onConnect = onConnect,
            onDisconnect = onDisconnect
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Control Card (Start/Calibrate/Stop/Reset)
        ControlCard(
            gyroState = uiState.gyroState,
            onStart = onStart,
            onCalibrate = onCalibrate,
            onStop = onStop,
            onResetCounts = onResetCounts
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Character Display
        CharacterDisplayCard(
            uiState = uiState,
            selectedCharacter = selectedCharacter
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Score Board
        ScoreBoardCard(uiState = uiState)

        Spacer(modifier = Modifier.height(12.dp))

        // Movement Selection (only show in manual mode)
        if (!isAutoMode) {
            MovementSelectionCard(onMovementSelect = onMovementSelect)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // AI Status Card (always show in auto mode)
        if (isAutoMode) {
            AIStatusCard(
                isModelLoaded = uiState.aiModelLoaded,
                activity = uiState.aiActivity,
                confidence = uiState.aiConfidence
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // LED Controls
        LedControlsCard(onLedCommand = onLedCommand)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HeaderCard(onModeToggle: () -> Unit, isAutoMode: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(25.dp)),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B35))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF6B35),
                            Color(0xFFE63946)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kinetic Pulse",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Move, Jump & Have Fun!",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onModeToggle,
                    modifier = Modifier
                        .weight(1f)
                        .height(65.dp)
                        .shadow(6.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoMode) Color(0xFF2ECC71) else Color(0xFF9B59B6)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isAutoMode) "🎯 Auto Mode" else "✋ Manual Mode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { /* Music player */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(65.dp)
                        .shadow(6.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🎵 Music",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(
    connectionState: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                "Connected" -> Color(0xFF2ECC71)
                "Connecting" -> Color(0xFFFFA500)
                "Disconnected" -> Color(0xFFE74C3C)
                else -> Color(0xFF95A5A6)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📡 ESP32 Connection",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = connectionState,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            if (connectionState == "Connected") {
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Disconnect",
                        color = Color(0xFFE74C3C),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Connect",
                        color = Color(0xFF2ECC71),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ControlCard(
    gyroState: String,
    onStart: () -> Unit,
    onCalibrate: () -> Unit,
    onStop: () -> Unit,
    onResetCounts: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE63946))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🎮 Sensor Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Status indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (gyroState == "ON") Color(0xFF2ECC71) else Color(0xFF95A5A6),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status: $gyroState",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // First row: Start and Calibrate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2ECC71)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "▶️",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "START",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = onCalibrate,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA500)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎯",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "CALIBRATE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second row: Stop and Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE74C3C)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⏹️",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "STOP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = onResetCounts,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9B59B6)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🔄",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "RESET",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AIStatusCard(
    isModelLoaded: Boolean,
    activity: String?,
    confidence: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activity != null) Color(0xFF9B59B6) else Color(0xFF6A0572)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title with status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isModelLoaded) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🤖 AI Movement Classifier",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Model status
            if (!isModelLoaded) {
                Text(
                    text = "⚠️ Model Not Loaded",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (activity == null) {
                Text(
                    text = "🔍 Analyzing Movement...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Collecting sensor data",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            } else {
                // Activity detected
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getActivityEmoji(activity) + " " + activity.uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Confidence: ${(confidence * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Detection info
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isModelLoaded)
                    "Classifies motion from sensor patterns (display only)"
                else
                    "Please restart app to load model",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun CharacterDisplayCard(
    uiState: MotionUiState,
    selectedCharacter: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .shadow(12.dp, RoundedCornerShape(30.dp)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4ECDC4))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // White background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(Color.White, RoundedCornerShape(26.dp))
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Character Name
                Text(
                    text = "✨ WATCH ME DANCE! ✨",
                    color = Color(0xFFFF6B35),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Video/Animation Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.animationVisible && uiState.animationResourceName != null) {
                        CharacterVideoView(
                            animationName = uiState.animationResourceName,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Placeholder when no animation
                        Text(
                            text = getCharacterEmoji(selectedCharacter),
                            fontSize = 120.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Status Text
                Text(
                    text = uiState.lastMovementText,
                    color = Color(color = uiState.lastMovementColor),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
fun CharacterVideoView(
    animationName: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Get resource ID from name
                val resId = context.resources.getIdentifier(
                    animationName,
                    "raw",
                    context.packageName
                )

                if (resId != 0) {
                    setVideoURI(Uri.parse("android.resource://${context.packageName}/$resId"))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.start()
                    }
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun ScoreBoardCard(uiState: MotionUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD166))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏆 Score Board 🏆",
                color = Color(0xFF2D3748),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreColumn(
                    score = uiState.leftCount,
                    label = "⬅️ Left",
                    color = Color(0xFFE74C3C)
                )
                ScoreColumn(
                    score = uiState.rightCount,
                    label = "➡️ Right",
                    color = Color(0xFF2ECC71)
                )
                ScoreColumn(
                    score = uiState.forwardCount,
                    label = "⬆️ Up",
                    color = Color(0xFF3498DB)
                )
                ScoreColumn(
                    score = uiState.backCount,
                    label = "💃 Dance",
                    color = Color(0xFF9B59B6)
                )
            }
        }
    }
}

@Composable
fun ScoreColumn(score: Int, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = score.toString(),
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color(0xFF2D3748),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MovementSelectionCard(onMovementSelect: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6A0572))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🎯 Select Movement to Practice",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MovementButton(
                    text = "⬅️\nLeft",
                    onClick = { onMovementSelect("left") },
                    modifier = Modifier.weight(1f)
                )
                MovementButton(
                    text = "➡️\nRight",
                    onClick = { onMovementSelect("right") },
                    modifier = Modifier.weight(1f)
                )
                MovementButton(
                    text = "⬆️\nUp",
                    onClick = { onMovementSelect("up") },
                    modifier = Modifier.weight(1f)
                )
                MovementButton(
                    text = "💃\nDance",
                    onClick = { onMovementSelect("dance") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MovementButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(60.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2ECC71)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LedControlsCard(onLedCommand: (Char) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF118AB2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "💡 LED Controls",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // First row: White, Red, Blue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LedButton(
                    emoji = "⚪",
                    onClick = { onLedCommand('w') },
                    modifier = Modifier.weight(1f)
                )
                LedButton(
                    emoji = "🔴",
                    onClick = { onLedCommand('r') },
                    modifier = Modifier.weight(1f)
                )
                LedButton(
                    emoji = "🔵",
                    onClick = { onLedCommand('b') },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second row: Green, Topaz, Lilac
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LedButton(
                    emoji = "🟢",
                    onClick = { onLedCommand('g') },
                    modifier = Modifier.weight(1f)
                )
                LedButton(
                    emoji = "💎",
                    onClick = { onLedCommand('t') },
                    modifier = Modifier.weight(1f)
                )
                LedButton(
                    emoji = "🟣",
                    onClick = { onLedCommand('l') },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Third row: Rainbow, Strobe, Off
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LedButton(
                    emoji = "🌈",
                    onClick = { onLedCommand('n') },
                    modifier = Modifier.weight(1f)
                )
                LedButton(
                    emoji = "⚡",
                    onClick = { onLedCommand('s') },
                    modifier = Modifier.weight(1f)
                )
                LedButton(
                    emoji = "❌",
                    onClick = { onLedCommand('o') },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LedButton(
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(55.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

fun getCharacterEmoji(character: String): String {
    return when (character.lowercase()) {
        "lion" -> "🦁"
        "girl" -> "👧"
        "boy" -> "👦"
        else -> "🤸"
    }
}

fun getActivityEmoji(activity: String): String {
    return when (activity.lowercase()) {
        "left" -> "⬅️"
        "right" -> "➡️"
        "forward", "up" -> "⬆️"
        "back", "dance" -> "⬇️"
        // Legacy activity names (in case they slip through)
        "boxing" -> "⬅️"
        "clapping" -> "⬇️"
        "running", "walking" -> "⬆️"
        else -> "🎯"
    }
}
