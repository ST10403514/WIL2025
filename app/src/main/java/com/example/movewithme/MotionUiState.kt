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