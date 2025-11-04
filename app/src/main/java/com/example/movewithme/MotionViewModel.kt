//Code Attribution
//For the below code we have used these sources to help guide us:
// TensorFlow, 2018. Add TensorFlow Lite to your Android App (TensorFlow Tip of the Week). [video online] Available at: < https://youtu.be/RhjBDxpAOIc?si=hRXBQqA-vKmhZW4m> [Accessed 22 March 2025].
// Mammothlnteractive, 2021. Build Android App with TensorFlow Lite Machine Learning Model. [video online] Available at: < https://youtu.be/o5c2BLrxNyA?si=6FQGl8tCBWBbOB1B> [Accessed 22 March 2025]. 
// DIY TechRush, 2022. ESP32 Bluetooth Classic - ESP32 Beginner's Guide. [video online] Available at: < https://youtu.be/EWxM8Ixnrqo?si=SnWwI6mTLC3qOQze> [Accessed 16 March 2025].
//DroneBot Workshop, 2024. Bluetooth Classic & BLE with ESP32. [video online] Available at: < https://youtu.be/0Q_4q1zU6Zc?si=eCU72Qw5J1fP4eLG> [Accessed 16 March 2025].
// ATECHS, 2025. MPU6050 Explained | Motion Detection with ESP32 and Arduino!?. [video online] Available at: < https://youtu.be/rpCcZZJeodY?si=Ka3BX6-anw7aJjSY> [Accessed 17 March 2025].

package com.example.movewithme

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val LOG_TAG = "MotionViewModel"
private const val LOG_LIMIT = 100

class MotionViewModel(
    private val bleManager: BleManager,
    private val context: Context,
    private val gyroManager: GyroManager?
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private var bleBuffer: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null

    // AI Movement Classifier
    private val aiClassifier: AIMovementClassifier = AIMovementClassifier(context)

    private val _uiState = MutableStateFlow(MotionUiState())
    val uiState: StateFlow<MotionUiState> = _uiState.asStateFlow()

    init {
        Log.d(LOG_TAG, "ViewModel initialized, starting notification collector...")

        // Initialize TTS
        initTts()

        // Set AI model loaded state
        updateState { it.copy(aiModelLoaded = aiClassifier.isModelLoaded()) }
        Log.d(LOG_TAG, "AI Classifier initialized: model loaded = ${aiClassifier.isModelLoaded()}")

        // Set up GyroManager listeners if available
        gyroManager?.let { setupGyroManager(it) }

        viewModelScope.launch {
            bleManager.notifications.collect { raw ->
                Log.d(LOG_TAG, "⬇️ Flow received notification chunk: '${raw.take(50)}...'")
                handleRawNotification(raw)
            }
        }

        viewModelScope.launch {
            bleManager.connectionState.collect { status ->
                updateState { state ->
                    when (status) {
                        is BleStatus.Disconnected -> state.copy(connectionState = "Disconnected", connectionError = null)
                        is BleStatus.Scanning -> state.copy(connectionState = "Scanning", connectionError = null)
                        is BleStatus.Connecting -> state.copy(connectionState = "Connecting", connectionError = null)
                        is BleStatus.Connected -> state.copy(connectionState = "Connected", connectionError = null)
                        is BleStatus.Error -> state.copy(connectionState = "Error", connectionError = status.message)
                    }
                }

                if (status is BleStatus.Error) {
                    addLog("[ERR] ${status.message}")
                }
            }
        }
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                try {
                    tts?.language = Locale.getDefault()
                    Log.d(LOG_TAG, "TTS initialized successfully")
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "TTS language set failed: ${e.message}")
                }
            } else {
                Log.w(LOG_TAG, "TTS init failed with status $status")
            }
        }
    }

    private fun setupGyroManager(gm: GyroManager) {
        gm.setMovementListener(object : GyroManager.MovementListener {
            override fun onLeft() {
                handleLeftMovement()
            }

            override fun onRight() {
                handleRightMovement()
            }

            override fun onMiddle() {
                handleForwardMovement()
            }

            override fun onBack() {
                handleBackMovement()
            }

            override fun onRaw(x: Float, y: Float, z: Float) {
                // Log raw gyro data if needed
            }
        })

        gm.setCalibrationListener(object : GyroManager.CalibrationListener {
            override fun onCalibrationStart() {
                mainHandler.post {
                    updateState { state ->
                        state.copy(
                            calibrationPromptVisible = true,
                            calibrationPromptText = "🔧 Calibration Starting...",
                            calibrationPromptColor = 0xFFFFFFFF
                        )
                    }
                }
            }

            override fun onCalibrationPhase(instruction: String, color: Int) {
                mainHandler.post {
                    updateState { state ->
                        state.copy(
                            calibrationPromptVisible = true,
                            calibrationPromptText = instruction,
                            calibrationPromptColor = color.toLong() or 0xFF000000L
                        )
                    }
                }
            }

            override fun onCalibrationComplete() {
                mainHandler.post {
                    updateState { state ->
                        state.copy(
                            calibrationPromptText = "✅ Calibration Complete! Motion detection active.",
                            calibrationPromptColor = 0xFF00FF00
                        )
                    }
                    // Hide after 2 seconds
                    mainHandler.postDelayed({
                        updateState { state ->
                            state.copy(calibrationPromptVisible = false)
                        }
                    }, 2000)
                }
            }
        })
    }

    fun connect() {
        bleManager.scanAndConnect()
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun sendCoreCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return
        addLog("[TX] $trimmed")
        when (trimmed.uppercase(Locale.US)) {
            "ON" -> updateState { it.copy(gyroState = "ON") }
            "OFF" -> updateState { it.copy(gyroState = "OFF") }
        }
        bleManager.sendCommand(trimmed)
    }

    fun sendLedCommand(letter: Char) {
        val command = letter.toString()
        addLog("[TX] $command")
        bleManager.sendCommand(command)
    }

    fun setBrightness(value: Int) {
        val clamped = value.coerceIn(0, 255)
        updateState { it.copy(brightness = clamped) }
        val command = "BRIGHT:$clamped"
        addLog("[TX] $command")
        bleManager.sendCommand(command)
    }

    fun setSampleRate(hz: Int) {
        val allowed = hz.coerceIn(10, 200)
        updateState { it.copy(sampleRateHz = allowed) }
        val command = "RATE $allowed"
        addLog("[TX] $command")
        bleManager.sendCommand(command)
    }

    fun toggleSensorMode() {
        val currentMode = _uiState.value.sensorMode
        val newMode = when (currentMode) {
            SensorMode.ESP32_ADXL345 -> SensorMode.PHONE_GYRO
            SensorMode.PHONE_GYRO -> SensorMode.SECOND_PHONE_GYRO
            SensorMode.SECOND_PHONE_GYRO -> SensorMode.ESP32_ADXL345
        }

        when (newMode) {
            SensorMode.PHONE_GYRO -> {
                gyroManager?.let {
                    if (it.isAvailable()) {
                        it.start()
                        it.setProcessingEnabled(true)
                        updateLastMovement("📱 Primary phone gyroscope active", 0xFFFFD700)
                    }
                }
            }
            SensorMode.SECOND_PHONE_GYRO -> {
                gyroManager?.stop()
                updateLastMovement("📱📱 Second phone gyroscope movement detection active", 0xFF9C27B0)
            }
            SensorMode.ESP32_ADXL345 -> {
                gyroManager?.stop()
                updateLastMovement("🔧 ESP32 ADXL345 movement detection active", 0xFFFF5722)
            }
        }

        updateState { it.copy(sensorMode = newMode) }
        Log.i(LOG_TAG, "Switched to sensor mode: $newMode")
    }

    fun toggleProductionMode() {
        updateState { it.copy(isProductionMode = !it.isProductionMode) }
    }

    fun toggleLedOptions() {
        updateState { it.copy(ledOptionsVisible = !it.ledOptionsVisible) }
    }

    fun resetCounts() {
        sendCoreCommand("RESET_COUNTS")
        updateState { state ->
            state.copy(
                leftCount = 0,
                rightCount = 0,
                forwardCount = 0,
                backCount = 0
            )
        }
    }

    private fun handleLeftMovement(fromESP32: Boolean = false, fromAI: Boolean = false) {
        val source = when {
            fromESP32 -> "ESP32"
            fromAI -> "AI"
            else -> "Phone Gyro"
        }
        Log.i(LOG_TAG, "🎯 MOVEMENT: LEFT detected from $source!")

        // Only increment count if from phone gyro or AI, not ESP32 (ESP32 sends its own counts)
        if (!fromESP32) {
            val newCount = _uiState.value.leftCount + 1
            updateState { state ->
                state.copy(leftCount = newCount)
            }
        }

        updateLastMovement(
            if (fromAI) "🤖 AI Predicted: LEFT" else "⬅️ LEFT MOVEMENT",
            0xFF0000FF
        )
        sendLedCommand('b') // Blue for left

        // Get character-specific animation
        val character = _uiState.value.selectedCharacter
        val animation = getAnimationForMovement("left", character)
        Log.d(LOG_TAG, "Left movement: character=$character, animation=$animation")
        if (animation != null) {
            showMovementFlash(animation)
        }
        speakIfProd("Left movement detected")
    }

    private fun handleRightMovement(fromESP32: Boolean = false, fromAI: Boolean = false) {
        val source = when {
            fromESP32 -> "ESP32"
            fromAI -> "AI"
            else -> "Phone Gyro"
        }
        Log.i(LOG_TAG, "🎯 MOVEMENT: RIGHT detected from $source!")

        // Only increment count if from phone gyro or AI, not ESP32 (ESP32 sends its own counts)
        if (!fromESP32) {
            val newCount = _uiState.value.rightCount + 1
            updateState { state ->
                state.copy(rightCount = newCount)
            }
        }

        updateLastMovement(
            if (fromAI) "🤖 AI Predicted: RIGHT" else "➡️ RIGHT MOVEMENT",
            0xFF00FF00
        )
        sendLedCommand('g') // Green for right

        // Get character-specific animation
        val character = _uiState.value.selectedCharacter
        val animation = getAnimationForMovement("right", character)
        Log.d(LOG_TAG, "Right movement: character=$character, animation=$animation")
        if (animation != null) {
            showMovementFlash(animation)
        }
        speakIfProd("Right movement detected")
    }

    private fun handleForwardMovement(fromESP32: Boolean = false, fromAI: Boolean = false) {
        val source = when {
            fromESP32 -> "ESP32"
            fromAI -> "AI"
            else -> "Phone Gyro"
        }
        Log.i(LOG_TAG, "🎯 MOVEMENT: FORWARD detected from $source!")

        // Only increment count if from phone gyro or AI, not ESP32 (ESP32 sends its own counts)
        if (!fromESP32) {
            val newCount = _uiState.value.forwardCount + 1
            updateState { state ->
                state.copy(forwardCount = newCount)
            }
        }

        updateLastMovement(
            if (fromAI) "🤖 AI Predicted: FORWARD" else "⬆️ FORWARD MOVEMENT",
            0xFFFFFFFF
        )
        sendLedCommand('w') // White for forward

        // Get character-specific animation
        val character = _uiState.value.selectedCharacter
        val animation = getAnimationForMovement("forward", character)
        Log.d(LOG_TAG, "Forward movement: character=$character, animation=$animation")
        if (animation != null) {
            showMovementFlash(animation)
        }
        speakIfProd("Forward movement detected")
    }

    private fun handleBackMovement(fromESP32: Boolean = false, fromAI: Boolean = false) {
        val source = when {
            fromESP32 -> "ESP32"
            fromAI -> "AI"
            else -> "Phone Gyro"
        }
        Log.i(LOG_TAG, "🎯 MOVEMENT: BACK detected from $source!")

        // Only increment count if from phone gyro or AI, not ESP32 (ESP32 sends its own counts)
        if (!fromESP32) {
            val newCount = _uiState.value.backCount + 1
            updateState { state ->
                state.copy(backCount = newCount)
            }
        }

        updateLastMovement(
            if (fromAI) "🤖 AI Predicted: DANCE" else "⬇️ BACK MOVEMENT",
            0xFFFF0000
        )
        sendLedCommand('r') // Red for back

        // Get character-specific animation
        val character = _uiState.value.selectedCharacter
        val animation = getAnimationForMovement("back", character)
        Log.d(LOG_TAG, "Back movement: character=$character, animation=$animation")
        if (animation != null) {
            showMovementFlash(animation)
        }
        speakIfProd("Back movement detected")
    }

    private fun updateLastMovement(text: String, color: Long) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        updateState { state ->
            state.copy(
                lastMovementText = text,
                lastMovementColor = color,
                lastMovementTimestamp = timestamp
            )
        }
    }

    private fun showMovementFlash(animationName: String) {
        updateState { state ->
            state.copy(
                animationResourceName = animationName,
                animationVisible = true
            )
        }
        // Hide animation after 3 seconds
        mainHandler.postDelayed({
            updateState { state ->
                state.copy(animationVisible = false)
            }
        }, 3000)
    }

    private fun speakIfProd(phrase: String) {
        if (!_uiState.value.isProductionMode) return
        try {
            tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "motion-${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.w(LOG_TAG, "TTS speak failed: ${e.message}")
        }
    }

    private fun processAIClassification(ax: Float, ay: Float, az: Float) {
        val result = aiClassifier.processSensorData(ax, ay, az)
        val confidence = aiClassifier.getLastMaxConfidence()

        // Update state with AI classification result
        when (result) {
            "collecting_data" -> {
                // Still collecting data - clear any previous activity
                Log.d(LOG_TAG, "AI: Collecting data (need 50 samples)")
                updateState { state ->
                    state.copy(aiActivity = null, aiConfidence = 0f)
                }
            }
            "ai_not_loaded", "error" -> {
                // Model not loaded or error
                Log.e(LOG_TAG, "AI status: $result")
                updateState { state ->
                    state.copy(aiActivity = null, aiConfidence = 0f)
                }
            }
            else -> {
                // Valid activity detected - map to directional movement
                Log.i(LOG_TAG, "🤖 AI DETECTED: $result (confidence: ${(confidence * 100).toInt()}%)")
                val predictedDirection = handleAIActivity(result, ax)
                if (predictedDirection != null) {
                    updateState { state ->
                        state.copy(
                            aiActivity = predictedDirection,
                            aiConfidence = confidence
                        )
                    }
                }
            }
        }
    }

    private fun handleAIActivity(activity: String, accelerometerX: Float): String? {
        // AI classification - DISPLAY ONLY (does not trigger movements)
        // Just map to directional names for the AI status card
        return when (activity.lowercase()) {
            "walking", "running" -> {
                Log.i(LOG_TAG, "🤖 AI classified: FORWARD (display only)")
                "FORWARD"
            }
            "boxing" -> {
                Log.i(LOG_TAG, "🤖 AI classified: LEFT (display only)")
                "LEFT"
            }
            "clapping" -> {
                Log.i(LOG_TAG, "🤖 AI classified: BACK (display only)")
                "BACK"
            }
            else -> {
                // Ignore sitting/standing
                Log.d(LOG_TAG, "🤖 AI detected: $activity (not displayed)")
                null
            }
        }
    }

    private fun getAnimationForActivity(activity: String, character: String): String? {
        return when (character) {
            "lion" -> {
                when (activity) {
                    "boxing" -> "lion_dance"
                    "clapping" -> "lion_clap"
                    "running" -> "lion_march"
                    "sitting down" -> "lion_jump_left"
                    "standing up" -> "lion_jump_up"
                    "walking" -> "lion_skip_right"
                    else -> null
                }
            }
            "girl" -> {
                when (activity) {
                    "boxing" -> "girl_1foot"
                    "clapping" -> "girl_jump_clap"
                    "running" -> "girl_skip_right"
                    "sitting down" -> "girl_jump"
                    "standing up" -> "girl_jump"
                    "walking" -> "girl_skip_left"
                    else -> null
                }
            }
            "boy" -> {
                // Boy character animations can use generic video animations
                when (activity) {
                    "boxing" -> "video_dance"
                    "clapping" -> "video_dance"
                    "running" -> "video_right"
                    "sitting down" -> "video_left"
                    "standing up" -> "video_up"
                    "walking" -> "video_right"
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun getAnimationForMovement(movement: String, character: String): String? {
        return when (character) {
            "lion" -> {
                when (movement) {
                    "left" -> "lion_jump_left"
                    "right" -> "lion_skip_right"
                    "forward" -> "lion_jump_up"
                    "back" -> "lion_dance"
                    else -> null
                }
            }
            "girl" -> {
                when (movement) {
                    "left" -> "girl_skip_left"
                    "right" -> "girl_skip_right"
                    "forward" -> "girl_jump"
                    "back" -> "girl_1foot"
                    else -> null
                }
            }
            "boy" -> {
                when (movement) {
                    "left" -> "video_left"
                    "right" -> "video_right"
                    "forward" -> "video_up"
                    "back" -> "video_dance"
                    else -> null
                }
            }
            else -> null
        }
    }

    fun setSelectedCharacter(character: String) {
        updateState { it.copy(selectedCharacter = character) }
        Log.i(LOG_TAG, "Selected character: $character")
    }

    override fun onCleared() {
        aiClassifier.close()
        tts?.stop()
        tts?.shutdown()
        gyroManager?.stop()
        bleManager.close()
        super.onCleared()
    }

    private fun handleRawNotification(chunk: String) {
        Log.d(LOG_TAG, "handleRawNotification: received ${chunk.length} bytes, buffer before: ${bleBuffer.length} bytes")
        if (chunk.isEmpty()) return

        bleBuffer += chunk
        Log.d(LOG_TAG, "Buffer after append: ${bleBuffer.length} bytes, content preview: '${bleBuffer.take(100)}'")

        var newlineIndex = bleBuffer.indexOf('\n')
        var linesProcessed = 0
        while (newlineIndex >= 0) {
            val rawLine = bleBuffer.substring(0, newlineIndex)
            bleBuffer = bleBuffer.substring(newlineIndex + 1)
            linesProcessed++

            val trimmed = rawLine.trim().trimEnd('\r')
            if (trimmed.isNotEmpty()) {
                Log.d(LOG_TAG, "✓ Complete line #$linesProcessed: $trimmed")

                updateState { state ->
                    val newLog = (state.logLines + "[RX] $trimmed").takeLast(LOG_LIMIT)
                    state.copy(logLines = newLog)
                }

                parseJson(trimmed)
            }

            newlineIndex = bleBuffer.indexOf('\n')
        }

        if (linesProcessed == 0) {
            Log.d(LOG_TAG, "No complete lines yet, buffer size: ${bleBuffer.length}")
        }
    }

    private fun parseJson(raw: String) {
        val element = try {
            json.parseToJsonElement(raw)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "parseJson error", e)
            addLog("[ERR] parse: ${e.message}")
            return
        }

        val obj = element.jsonObject

        handleBootPacket(obj)

        val event = obj["event"]?.jsonPrimitive?.contentOrNull

        if (event == "motion") {
            val left = obj.intValue("left_count")
            val right = obj.intValue("right_count")
            val forward = obj.intValue("forward_count")

            val ax = obj.floatValue("ax")
            val ay = obj.floatValue("ay")
            val az = obj.floatValue("az")
            val gx = obj.floatValue("gx")
            val gy = obj.floatValue("gy")
            val gz = obj.floatValue("gz")

            Log.d(
                LOG_TAG,
                "motion: L=$left R=$right F=$forward ax=$ax ay=$ay az=$az gx=$gx gy=$gy gz=$gz"
            )

            updateState { state ->
                state.copy(
                    leftCount = left ?: state.leftCount,
                    rightCount = right ?: state.rightCount,
                    forwardCount = forward ?: state.forwardCount,
                    lastAx = ax ?: state.lastAx,
                    lastAy = ay ?: state.lastAy,
                    lastAz = az ?: state.lastAz,
                    lastGx = gx ?: state.lastGx,
                    lastGy = gy ?: state.lastGy,
                    lastGz = gz ?: state.lastGz
                )
            }

            // Process accelerometer data through AI classifier
            if (ax != null && ay != null && az != null && aiClassifier.isModelLoaded()) {
                processAIClassification(ax, ay, az)
            }
        }

        if (event == "left" || event == "right" || event == "forward") {
            val left = obj.intValue("left_count")
            val right = obj.intValue("right_count")
            val forward = obj.intValue("forward_count")

            Log.d(LOG_TAG, "ESP32 movement event: $event, counts: L=$left R=$right F=$forward")

            updateState { state ->
                state.copy(
                    leftCount = left ?: state.leftCount,
                    rightCount = right ?: state.rightCount,
                    forwardCount = forward ?: state.forwardCount,
                    lastEvent = event
                )
            }

            // Trigger movement handlers for ESP32 events (with fromESP32=true to avoid double counting)
            when (event) {
                "left" -> handleLeftMovement(fromESP32 = true)
                "right" -> handleRightMovement(fromESP32 = true)
                "forward" -> handleForwardMovement(fromESP32 = true)
            }
        }

        if (event == "ack") {
            val cmd = obj["cmd"]?.jsonPrimitive?.contentOrNull ?: "?"
            addLog("[RX] ack cmd=$cmd")
            when (cmd.uppercase(Locale.US)) {
                "ON" -> updateState { state -> state.copy(gyroState = "ON") }
                "OFF" -> updateState { state -> state.copy(gyroState = "OFF") }
            }
        }

        obj["state"]?.jsonPrimitive?.contentOrNull?.let { stateStr ->
            val newGyroState = when (stateStr) {
                "GYRO_ON" -> "ON"
                "GYRO_OFF" -> "OFF"
                else -> null
            }
            if (newGyroState != null) {
                updateState { state -> state.copy(gyroState = newGyroState) }
                Log.d(LOG_TAG, "state message: $stateStr -> gyroState=$newGyroState")
            }
        }

        obj["error"]?.jsonPrimitive?.contentOrNull?.let { err ->
            addLog("[ERR] $err")
        }

        obj["imu"]?.jsonPrimitive?.contentOrNull?.let { imuMsg ->
            addLog("[RX] imu=$imuMsg")
        }

        obj["gyro"]?.jsonPrimitive?.contentOrNull?.let { gyroStr ->
            updateState { state ->
                state.copy(gyroState = gyroStr.uppercase(Locale.US))
            }
        }
    }

    private fun handleBootPacket(obj: JsonObject) {
        if (!obj.containsKey("boot")) return

        val gyro = obj["gyro"]?.jsonPrimitive?.contentOrNull
        val fw = obj["fw"]?.jsonPrimitive?.contentOrNull
        val whoInt = obj["who"]?.jsonPrimitive?.intOrNull

        updateState { state ->
            state.copy(
                imuStatus = when (gyro) {
                    "ON" -> "OK"
                    "OFF" -> "IMU OFF"
                    else -> state.imuStatus
                },
                gyroState = when (gyro) {
                    "ON" -> "ON"
                    "OFF" -> "OFF"
                    else -> state.gyroState
                },
                firmwareVersion = fw ?: state.firmwareVersion,
                whoAmI = whoInt?.let { "0x" + it.toString(16).uppercase(Locale.US) } ?: state.whoAmI
            )
        }
    }

    private fun addLog(line: String) {
        updateState { state ->
            val updated = (state.logLines + line).takeLast(LOG_LIMIT)
            state.copy(logLines = updated)
        }
    }

    private fun updateState(transform: (MotionUiState) -> MotionUiState) {
        val newState = transform(_uiState.value)
        _uiState.value = newState
        Log.d(
            LOG_TAG,
            "state update: L=${newState.leftCount} R=${newState.rightCount} F=${newState.forwardCount} B=${newState.backCount} gx=${newState.lastGx}"
        )
    }

}

class MotionViewModelFactory(
    private val bleManager: BleManager,
    private val context: Context,
    private val gyroManager: GyroManager?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MotionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MotionViewModel(bleManager, context, gyroManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun Json.parseToJsonElement(raw: String): JsonElement = decodeFromString(raw)

private fun JsonObject.intValue(key: String): Int? =
    this[key]?.jsonPrimitive?.toIntCompat()

private fun JsonObject.floatValue(key: String): Float? =
    this[key]?.jsonPrimitive?.toFloatCompat()

private fun JsonPrimitive.toDoubleCompat(): Double? =
    doubleOrNull ?: contentOrNull?.toDoubleOrNull()

private fun JsonPrimitive.toIntCompat(): Int? =
    intOrNull ?: toDoubleCompat()?.roundToInt()

private fun JsonPrimitive.toFloatCompat(): Float? =
    toDoubleCompat()?.toFloat()
