//Code Attribution
//For the below code we have used these sources to help guide us:
// TensorFlow, 2018. Add TensorFlow Lite to your Android App (TensorFlow Tip of the Week). [video online] Available at: < https://youtu.be/RhjBDxpAOIc?si=hRXBQqA-vKmhZW4m> [Accessed 22 March 2025].
// Mammothlnteractive, 2021. Build Android App with TensorFlow Lite Machine Learning Model. [video online] Available at: < https://youtu.be/o5c2BLrxNyA?si=6FQGl8tCBWBbOB1B> [Accessed 22 March 2025]. 
// DIY TechRush, 2022. ESP32 Bluetooth Classic - ESP32 Beginner's Guide. [video online] Available at: < https://youtu.be/EWxM8Ixnrqo?si=SnWwI6mTLC3qOQze> [Accessed 16 March 2025].
//DroneBot Workshop, 2024. Bluetooth Classic & BLE with ESP32. [video online] Available at: < https://youtu.be/0Q_4q1zU6Zc?si=eCU72Qw5J1fP4eLG> [Accessed 16 March 2025].
// ATECHS, 2025. MPU6050 Explained | Motion Detection with ESP32 and Arduino!?. [video online] Available at: < https://youtu.be/rpCcZZJeodY?si=Ka3BX6-anw7aJjSY> [Accessed 17 March 2025].

package com.example.movewithme

enum class SensorMode {
    ESP32_ADXL345,
    PHONE_GYRO,
    SECOND_PHONE_GYRO
}

data class MotionUiState(
    val connectionState: String = "Disconnected",
    val connectionError: String? = null,
    val imuStatus: String = "Unknown",
    val gyroState: String = "OFF",
    val whoAmI: String? = null,
    val firmwareVersion: String? = null,
    val leftCount: Int = 0,
    val rightCount: Int = 0,
    val forwardCount: Int = 0,
    val backCount: Int = 0,
    val lastEvent: String? = null,
    val lastAx: Float = 0f,
    val lastAy: Float = 0f,
    val lastAz: Float = 0f,
    val lastGx: Float = 0f,
    val lastGy: Float = 0f,
    val lastGz: Float = 0f,
    val brightness: Int = 128,
    val sampleRateHz: Int = 50,
    val logLines: List<String> = emptyList(),

    // New fields for enhanced UI
    val lastMovementText: String = "No movement detected yet",
    val lastMovementColor: Long = 0xFFFFFFFF,
    val lastMovementTimestamp: String = "",
    val calibrationPromptText: String = "",
    val calibrationPromptColor: Long = 0xFFFFFFFF,
    val calibrationPromptVisible: Boolean = false,
    val sensorMode: SensorMode = SensorMode.ESP32_ADXL345,
    val isProductionMode: Boolean = true,
    val ledOptionsVisible: Boolean = false,
    val animationResourceName: String? = null,
    val animationVisible: Boolean = false,

    // AI Movement Classification fields
    val aiActivity: String? = null,
    val aiConfidence: Float = 0f,
    val aiModelLoaded: Boolean = false,
    val selectedCharacter: String = "lion"
)
