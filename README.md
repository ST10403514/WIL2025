<a id="readme-top"></a>

<br />

<!--
Code attribution:
For this ReadMe this is the template I used:
othneildrew., 2024. Best-README-Template (version 1.1.2) [Source code]. Available at:< https://github.com/othneildrew/Best-README-Template.git> Accessed 26 April 2025].
-->


<h3 align="center">Kinetic Cape - Motion-Based Smart Wearable System</h3>

<p align="center">
  <a href="https://github.com/ST10403514/WIL2025"><strong>Explore the Docs »</strong></a>
  <br />
  <br />
  <a href="https://github.com/ST10403514/WIL2025">View Demo</a>
  ·
  <a href="https://github.com/ST10403514/WIL2025/issues">Report Bug</a>
  ·
  <a href="https://github.com/ST10403514/WIL2025/issues">Request Feature</a>
</p>

---


## 📖 About The Project

<p align="center">
  <img src="https://github.com/user-attachments/assets/f35abda8-6cce-494f-a5e4-572d2091fb09" alt="Logo" style="width: 170px; height: 170px; border-radius: 50%; object-fit: cover; border: 2px solid #ddd;">
</p>

The **Kinetic Cape Project** is an innovative wearable system designed to encourage children’s movement and play through motion detection, real-time feedback, and interactive visuals.  
It integrates an **ESP32 microcontroller** with an **MPU6050 gyroscope and accelerometer** to track motion and send data to an **Android mobile app** via **Bluetooth Low Energy (BLE)**.  
The app provides animations, LED color control, local data storage, and even built-in music playback — all fully offline and without Firebase.

This project was developed for the **WIL2025 work-integrated learning module**, focusing on teamwork, embedded programming, Android development, and system integration.

---
## Application Preview

## Old App (2024)
<p align="center">
  <table>
    <tr>
      <td align="center">
        <img src="https://github.com/user-attachments/assets/0f43d9e8-3367-44e2-ae74-172249eef426" alt="Old Register" width="250"><br>
        <sub>🔐 Old Register</sub>
      </td>
      <td align="center">
        <img src="https://github.com/user-attachments/assets/1aba04e8-c7e8-424a-85ca-aa25ddab1201" alt="Old Login" width="250"><br>
        <sub>🔐 Old Login</sub>
      </td>
      <td align="center">
        <img src="https://github.com/user-attachments/assets/87fa9cf1-f74f-462f-8a96-73c497b25108" alt="Old Dashboard" width="250"><br>
        <sub>📊 Old Dashboard</sub>
      </td>
      <td align="center">
        <img src="https://github.com/user-attachments/assets/a38a9c4d-35eb-468a-83d3-66652180356f" alt="Old Bluetooth Screen" width="250"><br>
        <sub>📱 Old Bt Screen</sub>
      </td>
    </tr>
  </table>
</p>

## New App (2025)
<p align="center">
  <table>
    <tr>
      <td align="center" width="33%">
        <img src="https://github.com/user-attachments/assets/b03e97ef-1a5a-4e05-9a44-53a09ef34ec3" alt="New Splashscreen" width="280"><br>
        <sub>🔐 New Splashscreen</sub>
      </td>
      <td align="center" width="33%">
        <img src="https://github.com/user-attachments/assets/9595d863-495a-4a7c-8971-df3904825ac2" alt="New Dashboard" width="280"><br>
        <sub>📊 New Dashboard</sub>
      </td>
      <td align="center" width="33%">
        <img src="https://github.com/user-attachments/assets/0585db87-ed40-4087-bf0d-56fae4a5f830" alt="New Bluetooth Screen" width="280"><br>
        <sub>📱 New Bt Screen</sub>
      </td>
    </tr>
    <tr>
      <td align="center" width="33%">
        <img src="https://github.com/user-attachments/assets/700ede1b-e365-4040-8a32-1534f6bd07fd" alt="Characters" width="280"><br>
        <sub>🦁 Characters</sub>
      </td>
      <td align="center" width="33%">
        <img src="https://github.com/user-attachments/assets/3e23fbe7-d522-434f-bd0e-d33d796d972f" alt="LED Control" width="280"><br>
        <sub>🚥 LED Control</sub>
      </td>
      <td align="center" width="33%">
        <!-- Empty cell for symmetry -->
      </td>
    </tr>
  </table>
</p>

---
### ✨ Core Features

<details>
  <summary><b>Motion Tracking with MPU6050</b></summary>
  <p>The ESP32 collects gyroscope and accelerometer data from the MPU6050 sensor, processing it to determine activity intensity and direction. This data is streamed in real time to the Android app.</p>
</details>

<details>
  <summary><b>Bluetooth Low Energy (BLE) Communication</b></summary>
  <p>Using the Nordic UART Service (NUS), the ESP32 transmits live sensor readings to the mobile application, allowing seamless motion detection and feedback without an internet connection.</p>
</details>

<details>
  <summary><b>AI-Driven Animations</b></summary>
  <p>The Android app uses an AI model to dynamically update character animations based on movement intensity and direction, replacing static images from the original version.</p>
</details>

<details>
  <summary><b>LED Control and Feedback</b></summary>
  <p>The app allows users to change LED colors manually or automatically adjust based on movement patterns detected by the ESP32.</p>
</details>

<details>
  <summary><b>Local Data Storage</b></summary>
  <p>All motion, session, and preference data are stored locally using SharedPreferences — ensuring privacy and offline functionality.</p>
</details>

<details>
  <summary><b>Custom Character Selection</b></summary>
  <p>Children can choose and personalize a character avatar that visually reacts to motion.</p>
</details>

<details>
  <summary><b>Built-in Music Player</b></summary>
  <p>The app includes a built-in music player that enhances engagement during active play sessions.</p>
</details>

---

## 🧩 System Architecture Overview

The Kinetic Cape System has three core layers:

- **Hardware Layer (ESP32 + MPU6050)** – Captures motion and drives LEDs.  
- **Communication Layer (Bluetooth Low Energy)** – Transmits sensor data to the app.  
- **Application Layer (Android App)** – Displays real-time animations, manages LEDs, and stores data locally.

<p align="center">
  <img src="https://github.com/user-attachments/assets/5f136753-b576-4c15-b6cc-dd2b8ed671f9" width="600" alt="System Architecture Diagram">
</p>

---

## 🧠 Technology Stack

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![ESP32](https://img.shields.io/badge/ESP32-Microcontroller-00979D?style=for-the-badge&logo=espressif&logoColor=white)](https://www.espressif.com/)
[![Arduino IDE](https://img.shields.io/badge/Arduino-IDE-00979D?style=for-the-badge&logo=arduino&logoColor=white)](https://www.arduino.cc/)
[![Bluetooth](https://img.shields.io/badge/Bluetooth-LE-0A66C2?style=for-the-badge&logo=bluetooth&logoColor=white)](https://www.bluetooth.com/)
[![MPU6050](https://img.shields.io/badge/MPU6050-Sensor-4CAF50?style=for-the-badge)](https://invensense.tdk.com/products/motion-tracking/6-axis/mpu-6050/)
[![Android Studio](https://img.shields.io/badge/Android-Studio-3DDC84?style=for-the-badge&logo=android-studio&logoColor=white)](https://developer.android.com/studio)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![JSON](https://img.shields.io/badge/JSON-000000?style=for-the-badge&logo=json&logoColor=white)]()
[![MVVM Architecture](https://img.shields.io/badge/MVVM-Architecture-brightgreen?style=for-the-badge)]()
[![Shared Preferences](https://img.shields.io/badge/Shared%20Preferences-Local%20Storage-4CAF50?style=for-the-badge)]()
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)

---

## 🚀 Getting Started

Follow these steps to get the **Kinetic Cape Project** up and running.

# How to Run

<details>
  <summary style="font-weight: bold; color: #2196F3;">Overview</summary>
  <div style="color: #333;">
    <p>Follow these steps to install and set up the <strong>Kinetic Pulse Mobile App</strong> on your local machine. This app integrates Bluetooth communication with ESP32 hardware and gyroscope-based movement detection for tracking athletic movements.</p>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">1. Clone the Repository</summary>
  <div style="color: #333;">
    <p>To get the source code, first clone the repository from GitHub:</p>
    <pre><code>git clone https://github.com/yourusername/KineticPulseMobileApp.git</code></pre>
    <p>This command will create a local copy of the repository on your machine. You can now navigate to the project folder.</p>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">2. Open the Project in Android Studio</summary>
  <div style="color: #333;">
    <p>1. Launch <strong>Android Studio</strong> (Minimum version: Arctic Fox or later recommended).</p>
    <p>2. In Android Studio, go to <strong>File</strong> → <strong>Open</strong>.</p>
    <p>3. Navigate to the folder where you cloned the repository (<strong>KineticPulseMobileApp</strong>).</p>
    <p>4. Select the project folder and click <strong>OK</strong>. Android Studio will sync the project with Gradle.</p>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">3. Install Dependencies</summary>
  <div style="color: #333;">
    <p>Once the project is opened, you need to install all the necessary dependencies and libraries for the app. Android Studio should automatically prompt you to sync the project with Gradle. If this doesn't happen, follow these steps:</p>
    <p>1. In Android Studio, click <strong>File</strong> → <strong>Sync Project with Gradle Files</strong>.</p>
    <p>2. Wait for the Gradle build process to finish.</p>
    <p>This will download and install all required libraries including:</p>
    <ul>
      <li><strong>Glide</strong> - For loading GIF/WebP movement animations</li>
      <li><strong>Bluetooth Libraries</strong> - For ESP32 communication</li>
      <li><strong>Text-to-Speech (TTS)</strong> - For voice feedback during movements</li>
    </ul>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">4. Set Up ESP32 Hardware (Optional but Recommended)</summary>
  <div style="color: #333;">
    <p>The app supports three sensor modes: <strong>ESP32 + ADXL345</strong>, <strong>Phone Gyroscope</strong>, and <strong>Second Phone Gyroscope</strong>.</p>
    <p>To use the ESP32 + ADXL345 mode:</p>
    <ol>
      <li>Flash your ESP32 with the appropriate firmware that:
        <ul>
          <li>Reads ADXL345 accelerometer data via I2C</li>
          <li>Sends movement data via Bluetooth Serial in format: <code>ACCEL:x,y,z</code> or <code>MOVE:DIRECTION</code></li>
          <li>Receives LED control commands: <code>w</code> (white), <code>r</code> (red), <code>g</code> (green), <code>b</code> (blue), <code>t</code> (topaz/yellow), <code>l</code> (lilac/pink), <code>a</code> (rainbow), <code>m</code> (seizure mode), <code>o</code> (off)</li>
        </ul>
      </li>
      <li>Pair your ESP32 Bluetooth device with your Android phone in <strong>Settings</strong> → <strong>Bluetooth</strong>.</li>
      <li>Note the device name/address for connecting within the app.</li>
    </ol>
    <p><strong>Note:</strong> If you don't have ESP32 hardware, you can still use the app with Phone Gyroscope mode for movement detection.</p>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">5. Grant Required Permissions</summary>
  <div style="color: #333;">
    <p>The app requires several runtime permissions. Make sure to grant them when prompted:</p>
    <ul>
      <li><strong>Bluetooth Connect</strong> (Android 12+) - For ESP32 communication</li>
      <li><strong>Bluetooth Scan</strong> (Android 12+) - For discovering Bluetooth devices</li>
      <li><strong>Location</strong> (Android 9-11) - Required for Bluetooth scanning on older Android versions</li>
      <li><strong>Post Notifications</strong> (Android 13+) - For background connection notifications</li>
    </ul>
    <p>These permissions are handled in the app, but ensure your device settings allow them.</p>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">6. Configure Android Emulator (Limited Functionality)</summary>
  <div style="color: #333;">
    <p><strong>⚠️ Important:</strong> The emulator has limited functionality since it cannot access Bluetooth or hardware sensors reliably. For full testing, use a physical Android device.</p>
    <p>If you still want to use an emulator for UI testing:</p>
    <ol>
      <li>In Android Studio, go to <strong>Tools</strong> → <strong>Device Manager</strong>.</li>
      <li>Click on <strong>Create Virtual Device</strong>.</li>
      <li>Choose a device model with <strong>API Level 31 (Android 12)</strong> or higher.</li>
      <li>Follow the prompts to create and start the emulator.</li>
    </ol>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">7. Add Movement Animation Assets</summary>
  <div style="color: #333;">
    <p>The app uses animated GIF/WebP files for movement visual feedback. Ensure these files are in the <code>res/raw/</code> folder:</p>
    <ul>
      <li><code>mwm_dress_left3.gif</code> or <code>.webp</code> - Left movement animation</li>
      <li><code>mwm_jump_right3.gif</code> or <code>.webp</code> - Right movement animation</li>
      <li><code>mwm_jump_bounce3.gif</code> or <code>.webp</code> - Forward/Up/Back movement animation</li>
    </ul>
    <p>If these files are missing, the app will log errors but continue to function without visual feedback.</p>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">8. Run the Application</summary>
  <div style="color: #333;">
    <p>Once everything is set up, you can run the app:</p>
    <ol>
      <li>Connect your Android device via USB (recommended) or use an emulator.</li>
      <li>Enable <strong>Developer Options</strong> and <strong>USB Debugging</strong> on your Android device.</li>
      <li>Click the <strong>Run</strong> button (green triangle) in Android Studio.</li>
      <li>Select your connected device from the device list.</li>
      <li>Wait for the app to build and launch.</li>
    </ol>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">9. First-Time Setup in the App</summary>
  <div style="color: #333;">
    <p>When you first launch the app:</p>
    <ol>
      <li>Navigate to the <strong>Devices</strong> screen to select your Bluetooth device (ESP32).</li>
      <li>The app will attempt to auto-connect to previously paired devices on subsequent launches.</li>
      <li>Use the <strong>Mode Toggle</strong> button to switch between:
        <ul>
          <li><strong>ESP32 + ADXL345 mode</strong> - Uses external accelerometer (blue button)</li>
          <li><strong>Phone Gyroscope mode</strong> - Uses your phone's built-in gyroscope (green button)</li>
          <li><strong>Second Phone Gyroscope mode</strong> - Uses a second phone as sensor (purple button)</li>
        </ul>
      </li>
      <li>Enable/disable <strong>Auto Detection</strong> using the toggle button to control when movements are tracked.</li>
      <li>Perform movements to see detection in action!</li>
    </ol>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">10. Verify Installation</summary>
  <div style="color: #333;">
    <p>After running the app, verify the following:</p>
    <ul>
      <li>✅ The app starts without any crashes or errors.</li>
      <li>✅ Bluetooth connects to ESP32 (if using hardware mode).</li>
      <li>✅ Phone gyroscope detects movements (if using phone mode).</li>
      <li>✅ LED commands are sent and received by ESP32.</li>
      <li>✅ Jump counters increment correctly for Left, Right, Forward, and Back movements.</li>
      <li>✅ Text-to-Speech provides audio feedback (in production mode).</li>
      <li>✅ Movement animations display when jumps are detected.</li>
      <li>✅ Auto-detection toggle works correctly.</li>
    </ul>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">11. Testing & Debugging</summary>
  <div style="color: #333;">
    <p>Use these built-in test commands in the terminal input field:</p>
    <ul>
      <li><code>TEST</code> - Runs manual diagnostic tests for sensors and Bluetooth</li>
      <li><code>LEDTEST</code> - Cycles through all LED colors to verify ESP32 connection</li>
      <li><code>ACCELTEST</code> - Simulates ESP32 accelerometer data to test movement detection</li>
    </ul>
    <p>Check Android Studio's <strong>Logcat</strong> for detailed logs with tags like:</p>
    <ul>
      <li><code>TerminalFragment</code> - Movement detection and Bluetooth logs</li>
      <li><code>GyroManager</code> - Gyroscope calibration and sensor events</li>
      <li><code>SerialService</code> - Bluetooth connection status</li>
    </ul>
  </div>
</details>

<details>
  <summary style="font-weight: bold; color: #2196F3;">12. Troubleshooting</summary>
  <div style="color: #333;">
    <p><strong>Bluetooth won't connect:</strong></p>
    <ul>
      <li>Ensure ESP32 is paired in Android Bluetooth settings first</li>
      <li>Check Bluetooth permissions are granted in app settings</li>
      <li>Try manually selecting device from Devices screen</li>
      <li>Restart Bluetooth on your phone</li>
    </ul>
    <p><strong>Gyroscope not detecting movements:</strong></p>
    <ul>
      <li>Verify device has gyroscope sensor (check Logcat for sensor list)</li>
      <li>Ensure <strong>Auto Detection</strong> toggle is enabled (green)</li>
      <li>Check that you're in the correct sensor mode (Phone Gyro mode)</li>
      <li>Allow the gyroscope to calibrate (hold phone still for a few seconds)</li>
    </ul>
    <p><strong>LED not changing colors:</strong></p>
    <ul>
      <li>Verify Bluetooth connection is active (check status in terminal)</li>
      <li>Check ESP32 firmware is correctly programmed</li>
      <li>Run <code>LEDTEST</code> command to verify communication</li>
      <li>Ensure ESP32 power supply is adequate for LED strip</li>
    </ul>
    <p><strong>Auto-detection not respecting toggle:</strong></p>
    <ul>
      <li>Ensure you're using the latest code with cooldown fixes</li>
      <li>Check Logcat for "Auto detection enabled/disabled" messages</li>
      <li>Restart the app after toggling to ensure state is reset</li>
    </ul>
  </div>
</details>

---

## Quick Start Summary

1. Clone repo and open in Android Studio
2. Sync Gradle dependencies
3. (Optional) Pair ESP32 Bluetooth device in phone settings
4. Add movement animation files to `res/raw/` folder
5. Run on physical Android device
6. Select Bluetooth device or use Phone Gyroscope mode
7. Enable Auto Detection toggle
8. Start detecting movements! 🎯

---

## System Requirements

- **Android Studio:** Arctic Fox or later
- **Minimum Android Version:** API 21 (Android 5.0)
- **Target Android Version:** API 33 (Android 13)
- **Recommended:** Physical Android device with gyroscope sensor
- **Optional Hardware:** ESP32 with ADXL345 accelerometer and RGB LED strip

---

## Features Overview

- **2 Sensor Modes:** ESP32 ADXL345, Phone Gyroscope
- **4 Movement Types:** Left, Right, Forward, Back with LED feedback
- **Auto-Detection Toggle:** Enable/disable automatic movement tracking
- **Visual Feedback:** Animated GIFs for each movement type
- **Audio Feedback:** Text-to-Speech announcements (Production mode)
- **LED Control:** 9 color options for ESP32 LED strip
- **Auto-Connect:** Remembers last connected Bluetooth device
- **Movement Cooldown:** 800ms delay prevents duplicate detections
- 
### Prerequisites

- Android Studio (latest version)
- Kotlin SDK
- ESP32 development board
- MPU6050 motion sensor
- Arduino IDE with required libraries installed

---

### 🧰 Setup the ESP32

1. Open the `supercape.ino` file in **Arduino IDE**.  
2. Install the following dependencies:
   - `NimBLE-Arduino`
   - `Adafruit NeoPixel`
3. Select your ESP32 board and COM port.  
4. Upload the sketch to your ESP32.  
5. Once uploaded, the ESP32 will broadcast via Bluetooth and control the LEDs based on motion.

---

### 📱 Setup the Android App

1. Open the Android project in **Android Studio**.  
2. Sync Gradle and build the project.  
3. Run the app on your device (Android 9+).  
4. Grant Bluetooth permissions when prompted.

---

### 🔗 Pair and Test

1. Power on your ESP32 smart cape.  
2. Open the Kinetic Cape mobile app.  
3. Connect to your ESP32 device via BLE.  
4. Move the cape — the LEDs and animations should react instantly!

---

## 🧑‍💻 Contributors

<p align="center">
  <a href="https://github.com/ST10290935" target="_blank">
    <img src="https://avatars.githubusercontent.com/u/128598477?v=4" width="80" height="80" style="border-radius: 50%;" alt="Aiden Reddy"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/ST10143151" target="_blank">
    <img src="https://avatars.githubusercontent.com/u/128127914?v=4" width="80" height="80" style="border-radius: 50%;" alt="Ananta Reddy"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/ST10248202" target="_blank">
    <img src="https://avatars.githubusercontent.com/u/128582074?v=4" width="80" height="80" style="border-radius: 50%;" alt="Dheyan Ramballi"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/ST10249644" target="_blank">
    <img src="https://avatars.githubusercontent.com/u/128413984?v=4" width="80" height="80" style="border-radius: 50%;" alt="Shreya Dhawrajh"/>
  </a>
   &nbsp;&nbsp;
  <a href="https://github.com/ST10143151" target="_blank">
    <img src="https://avatars.githubusercontent.com/u/127833023?v=4" width="80" height="80" style="border-radius: 50%;" alt="Matthew Mason"/>
  </a>
</p>

---

## 📬 Contact

<div style="border: 2px solid #4CAF50; border-radius: 10px; padding: 20px; background-color: #f0f9f0; text-align: center;">

  <h2 style="color: #4CAF50;">Team Members</h2>

  <table style="margin-left: auto; margin-right: auto; text-align: center; border-collapse: collapse;">
    <tr>
      <th>Name</th>
      <th>Student Number</th>
      <th>Email</th>
    </tr>
    <tr>
      <td>Aiden Reddy</td>
      <td>ST10290935</td>
      <td>aidenreddyalt@gmail.com</td>
    </tr>
    <tr>
      <td>Ananta Reddy</td>
      <td>ST10143151</td>
      <td>st10143151@vcconnect.edu.za</td>
    </tr>
    <tr>
      <td>Dheyan Ramballi</td>
      <td>ST10248202</td>
      <td>dheyanramballi02@gmail.com</td>
    </tr>
    <tr>
      <td>Shreya Dhawrajh</td>
      <td>ST10249644</td>
      <td>st10249644@vcconnect.edu.za</td>
    </tr>
    <tr>
      <td>Matthew Mason</td>
      <td>ST10403514</td>
      <td>ST10403514@vcconnect.edu.za</td>
    </tr>
  </table>

</div>

---
## 📺 YouTube Demo

🎬 **Watch our full demonstration video here:**  
https://youtu.be/CvzNfTqnxUU?si=aARr4WCpNzfQdY9C

---

## 🔍 Acknowledgements

Reference List
Andreas Spiess, 2017. #173 ESP32 Bluetooth BLE with Arduino IDE (Tutorial) and Polar H7. [video online] Available at : < https://youtu.be/2mePPqiocUE?si=Up7wtPTHQwVUEOI8> [Accessed 12 September 2025].

Android Developers, 2025. Add menus. [online] Available at: <https://developer.android.com/develop/ui/views/components/menus#fragments> [Accessed 12 July 2025].

Android Developers, 2025. Asynchronous work with Java threads. [online] Available at: <https://developer.android.com/develop/background-work/background-tasks/asynchronous/java-threads> [Accessed 02 September 2025].

Android Developers, 2025. Behavior changes: Apps targeting Android 12. [online] Available at: <https://developer.android.com/about/versions/12/behavior-changes-12> [Accessed 20 July 2025].

Android Developers, 2025. Bluetooth permissions. [online] Available at: <https://developer.android.com/develop/connectivity/bluetooth/bt-permissions> [Accessed 12 September 2025].

Android Developers, 2025. Color. [online] Available at: <https://developer.android.com/reference/android/graphics/Color#parseColor(java.lang.String)> [Accessed 20 April 2025].

Android Developers, 2025. Enum. [online] Available at: <https://developer.android.com/reference/java/lang/Enum> [Accessed 09 August 2025].

Android Developers, 2025. Localize your app. [online] Available at: <https://developer.android.com/guide/topics/resources/localization> [Accessed 12 July 2025].

Android Developers, 2025. Motion sensors. [online] Available at: <https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion> [Accessed 15 August 2025].

Android Developers, 2025. Request runtime permissions. [online] Available at: <https://developer.android.com/training/permissions/requesting> [Accessed 09 August 2025].

Android Developers, 2025. SensorManager. [online] Available at: <https://developer.android.com/reference/android/hardware/SensorManager> [Accessed 15 August 2025].

Android Developers, 2025. Sensors Overview. [online] Available at: <https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview> [Accessed 18 August 2025].

Android Developers, 2025. SpannableStringBuilder. [online] Available at: <https://developer.android.com/reference/android/text/SpannableStringBuilder> [Accessed 02 September 2025].

Android knowledge, 2023. Fragment in Android Studio using Kotlin | Android Knowledge. [video online] Available at: < https://youtu.be/fT4MVQZWPRg?si=q2bBwA3vmjdSwaNN> [Accessed 12 July 2025].

Arduino Titan, 2024. MPU6050 Gyroscope+OLED display Interfacing with ESP32 | Wokwi Esp32 simulator | Arduino Titan. [video online] Available at: < https://youtu.be/KRoAsYJScEc?si=WdXZIPNDkbzj0guN> [Accessed 09 August 2025].

ATECHS, 2025. MPU6050 Explained | Motion Detection with ESP32 and Arduino!?. [video online] Available at: < https://youtu.be/rpCcZZJeodY?si=Ka3BX6-anw7aJjSY> [Accessed 09 August 2025].

BINARYUPDATES, 2023. How to Setup and Program ESP32 Microcontroller– Complete Guide. [video online] Available at: < https://youtu.be/AitCKcyjHuQ?si=kBwSdBYFv9w4CAeU> [Accessed 12 September 2025].

Bharath kumar, 2023. ESP-32 with MPU-6050 Accelerometer, Gyroscope and Temperature Sensor (Arduino). [video online] Available at: < https://youtu.be/1Su4HbPHZn0?si=p3EW1X_HQyGMlSwu> [Accessed 12 September 2025].

Brand Store, 2024. How to Get google-services.json File from Firebase for Your App (Step-by-Step Guide). [video online] Available at: < https://youtu.be/w1EPb-Jl1_U?si=wKFNSWMm6zv4f2sk> [Accessed 15 April 2025].

C# Corner, 2022. How to use weightSum and layout_weight In Linear Layout. [online] Available at: <https://www.c-sharpcorner.com/article/how-to-use-weightsum-and-layoutweight-in-linear-layout/> [Accessed 12 July 2025].

CapTech, 2020. Android Activity Result Contracts. [online] Available at: <https://www.captechconsulting.com/technical/android-activity-result-contracts> [Accessed 15 July 2025].

Chris Maher, 2022. How To Install WLED on an ESP32 Board and Connect / Control Addressable LEDs. [video online] Available at: < https://youtu.be/TOEnFKLm9Sw?si=P9IigSvD8HCnyAGy> [Accessed 12 September 2025].

CodePath Cliffnotes, 2015. Listening to Sensors using SensorManager. [online] Available at: <https://guides.codepath.com/android/Listening-to-Sensors-using-SensorManager> [Accessed 18 August 2025].

Core Electronics, 2022. How to Easily Control Addressable LEDs with an ESP32 or ESP8266 | WLED Project. [video online] Available at: < https://youtu.be/GYxctjukehY?si=xNlYUkqMdVG-c4uX> [Accessed 13 September 2025].

CS CORNER Sunita Rai, 2025. How to Connect Mobile Phone with Android Studio to Run App [2025] |Connect Phone With Android Studio. [video online] Available at: < https://youtu.be/9Tt8uLUgSj8?si=y_yUgMfCP_UcPWGN> [Accessed 20 March 2025].

Desktop Make, 2022. #2 How to connect ESP32 to ESP32 by Bluetooth (ESP32 Programming). [video online] Available at: < https://youtu.be/3SjGH0J7KPY?si=3tfhtDyAHBYSE6Z2> [Accessed 13 September March 2025].

DigitalOcean, 2022. Retrofit Android Example Tutorial. [online] Available at: <https://www.digitalocean.com/community/tutorials/retrofit-android-example-tutorial> [Accessed  20 July 2025].

DIY TechRush, 2022. ESP32 Bluetooth Classic - ESP32 Beginner's Guide. [video online] Available at: < https://youtu.be/EWxM8Ixnrqo?si=SnWwI6mTLC3qOQze> [Accessed 12 September 2025].

DEV Community, 2025. Serial communication with Android device. [online] Available at: <https://dev.to/bleuiot/serial-communication-with-android-device-481o> [Accessed 10 July 2025].

DroneBot Workshop, 2024. Bluetooth Classic & BLE with ESP32. [video online] Available at: < https://youtu.be/0Q_4q1zU6Zc?si=eCU72Qw5J1fP4eLG> [Accessed 13 September 2025].
Elconics, 2023. Send Sensor data to Firebase using ESP32 | Cloud setup | Arduino Coding | Firebase Realtime Database. [video online] Available at: < https://youtu.be/cm-Qe2HMJGk?si=abRgEFNIQMgLDNc1> [Accessed 13 September 2025].

Foxandroid, 2023. Fragments Implementation using Kotlin || Fragments using Kotlin || Android Studio Tutorial || 2023. [video online] Available at: < https://youtu.be/h-NcxT697Nk?si=eMUYyDbMERJUqhAW> [Accessed 12 July 2025].

GeeksforGeeks, 2025. Foreground Service in Android. [online] Available at: <https://www.geeksforgeeks.org/android/foreground-service-in-android/> [Accessed 10 July 2025].

GeeksforGeeks, 2025. How to Build an Application to Test Motion Sensors in Android?. [online] Available at: <https://www.geeksforgeeks.org/android/how-to-build-an-application-to-test-motion-sensors-in-android/> [Accessed 15 August 2025].

GeeksforGeeks, 2025. How to Display the List of Sensors Present in an Android Device Programmatically?. [online] Available at: <https://www.geeksforgeeks.org/android/how-to-display-the-list-of-sensors-present-in-an-android-device-programmatically/> [Accessed 15 August 2025].

GeeksforGeeks, 2022. How to Create an Alert Dialog Box in Android?. [online] Available at: <https://www.geeksforgeeks.org/android/how-to-create-an-alert-dialog-box-in-android/> [Accessed 10 May 2025].

GeeksforGeeks, 2025. How to Call a Method After a Delay in Android?. [online] Available at: <https://www.geeksforgeeks.org/kotlin/how-to-call-a-method-after-a-delay-in-android/> [Accessed 04 September 2025].

Github, 2025. Glide. [online] Available at: <https://github.com/bumptech/glide> [Accessed 02 October 2025].

How Do You? DIY, 2020. Desoldering | Soldering Basics | Soldering for Beginners. [video online] Available at: < https://youtu.be/bG7yW9FigJA?si=PA6MNCmEAtXlbG6S> [Accessed 02 August 2025].

InfyOm Technologies, 2021. How to Change App Language in Android Programmatically?. [online] Available at: <https://infyom.com/blog/how-to-change-app-language-in-android-programmatically/> [Accessed 02 September 2025].

KB CODER, 2021. Play Audio in Android Studio | Kotlin | Android Tutorials. [video online] Available at: < https://youtu.be/gXbrjvUaHYc?si=BYq0DLBNXu_fAjpU> [Accessed 20 September 2025].

LeftyMaker, 2017. Breadboard tutorial: How to use a breadboard (for beginners). [video online] Available at: < https://youtu.be/W6mixXsn-Vc?si=6BlaoWsYZWSnxQSw> [Accessed 02 August 2025].

MAINFRAME, 2021. Setting up an ESP32 with Arduino IDE. [video online] Available at: < https://youtu.be/CD8VJl27n94?si=vpaU8xNPWKMLQYdY> [Accessed 09 August 2025].

Mammothlnteractive, 2021. Build Android App with TensorFlow Lite Machine Learning Model. [video online] Available at: < https://youtu.be/o5c2BLrxNyA?si=6FQGl8tCBWBbOB1B> [Accessed 15 September 2025].

Medium, 2021. Android Runtime Permissions using registerForActivityResult. [online] Available at: <https://medium.com/codex/android-runtime-permissions-using-registerforactivityresult-68c4eb3c0b61> [Accessed 09 August 2025].

Medium, 2024. Establishing Connection with Bluetooth Classic in Android. [online] Available at: <https://blog.stackademic.com/establishing-connection-with-bluetooth-classic-in-android-1f9f17c9a452> [Accessed 12 September 2025].

Medium, 2023. Image Loading with Jetpack Compose; Glide-Coil Libraries. [online] Available at: <https://medium.com/@selinihtiyar/image-loading-with-jetpack-compose-glide-coil-359c8d05c944> [Accessed 02 October 2025].

Medium, 2024. Understanding Handlers, Loopers, and Message Queues in Android: A Practical Guide with Code Examples. [online] Available at: <https://medium.com/@mohamed.ma872/understanding-handlers-loopers-and-message-queues-in-android-a-practical-guide-with-code-515162d1d792> [Accessed 05 September 2025].

Microsoft Learn, [s.a.]. BluetoothDevice.Name Property. [online] Available at: <https://learn.microsoft.com/en-us/dotnet/api/android.bluetooth.bluetoothdevice.name?view=net-android-35.0> [Accessed 12 September 2025].

Nelson Darwin Pak Tech, 2023. simulation of esp32 with mpu6050 | simulation of esp32 with gyroscope and accelerometer. [video online] Available at: < https://youtu.be/v5a7aZJiI-k?si=hFn18iTR5Piq7eXB> [Accessed 09 August 2025].
Oracle, [s.a.]. String (Java Platform SE 8 ) - Oracle. [online] Available at: <https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#String-byte:A-> [Accessed 02 September 2025].

Oracle, [s.a.]. Float (Java Platform SE 8 ) - Oracle. [online] Available at: <https://docs.oracle.com/javase/8/docs/api/java/lang/Float.html#parseFloat-java.lang.String-> [Accessed 02 September 2025].

Programming Concepts, 2025. ESP32 IoT Project using Firebase, Android Studio and Arduino IDE | Programming Concepts. [video online] Available at: < https://youtu.be/Z4UilZuN1cU?si=sOYQfro5EXLI8BZE> [Accessed 15 April 2025].

Rui Santos, 2019. ESP32 Bluetooth Classic with Arduino IDE - Getting Started. [video online] Available at: < https://youtu.be/RStncO3zb8g?si=pOiaO_HdAWeglaDn> [Accessed 05 September 2025].

Robocraze, 2019. GETTING STARTED WITH ESP-NOW. [video online] Available at: < https://youtu.be/B6NZWLKGoCA?si=FMnKa7NBqculLYwP> [Accessed 15 April 2025].

Rui Santos, 2018. Getting Started with ESP32 Bluetooth Low Energy (BLE) on Arduino IDE. [video online] Available at: < https://youtu.be/wkO-ytWVvC0?si=C8SPcKPqYNiYX_Uw> [Accessed 25 August 2025].

Ralph S Bacon, 2022. #239 Using An ESP32 with 🟢🔴🔵NeoPixels - What Went Wrong? Simple Fix!. [video online] Available at: < https://youtu.be/SStRG-_1wXc?si=Zdai79N_7MJUJsfJ> [Accessed 13 September 2025].

Ratchets and Wrenches, 2015. How to Use a Multimeter for Beginners - How to Measure Voltage, Resistance, Continuity and Amps. [video online] Available at: < https://youtu.be/TdUK6RPdIrA?si=KrW1cMTpsUOqKYtq> [Accessed 22 March 2025].

Stack Overflow, 2025. How do we use runOnUiThread in Android?. [online] Available at: <https://stackoverflow.com/questions/11140285/how-do-we-use-runonuithread-in-android> [Accessed 09 August 2025].

Superb Tech, 2021. MPU6050 Sensor Arduino Tutorial. [video online] Available at: < https://youtu.be/a37xWuNJsQI?si=Lc78AH2PCpMzIIBb> [Accessed 20 June 2025].
SriTu Hobby, 2022. What is the ESP32 board and how to set up it with Arduino IDE?. [video online] Available at: < https://youtu.be/wsKTmlipQOE?si=5WpvQltEB_nNlixh> [Accessed 09 August 2025].

Taqnix, 2025. How to Generate Google Service JSON File for Firebase – Step-by-Step Guide!. [video online] Available at: < https://youtu.be/ixp7X9eaVFc?si=KM_1eRwNF01LbzBP> [Accessed 15 April 2025].

TensorFlow, 2018. Add TensorFlow Lite to your Android App (TensorFlow Tip of the Week). [video online] Available at: < https://youtu.be/RhjBDxpAOIc?si=hRXBQqA-vKmhZW4m> [Accessed 15 September 2025].

The Frugal Engineer, 2022. Android Studio Tutorial: Create an app to connect the Arduino using Bluetooth and RxAndroid. [video online] Available at: < https://youtu.be/aE8EbDmrUfQ?si=czHGNL6GKnFt8-mH> [Accessed 12 September 2025].

Trevor Elkins Blog, 2015. Should I use Enums in Android. [online] Available at: <https://www.telkins.dev/blog/should-i-use-enums-in-android> [Accessed 09 August 2025].

Tutlane, [s.a.]. Android Toast with Examples. [online] Available at: <https://www.tutlane.com/tutorial/Android/android-toast-with-examples#:~:text=In%20android%2C%20we%20can%20create%20a%20Toast%20by,the%20Toast%20notification%20by%20using%20show%20%28%29%20method> [Accessed 10 July August 2025].

Vogella, 2016. Android logging - Tutorial. [online] Available at: <https://www.vogella.com/tutorials/AndroidLogging/article.html> [Accessed 12 July 2025].

Zeeshan Academy, 2021. How to use fragments in Android Studio | Understanding Fragments for Multi Layout App. [video online] Available at: < https://youtu.be/PiExmkR3aps?si=CCyNts92yudaIv87> [Accessed 12 July 2025].







Annexure
• Title: Disclosure of AI Usage in my Assessment. 
• Section(s) within the assessment in which generative AI was used: ESP32 Guidance
• Name of AI tool(s) used: ChatGPT
• Purpose/intention behind use: Seeking knowledge about viewing ESP32 code. 
• Date(s) in which generative AI was used: 05 September 2025
• A link to the actual generative AI chat, and screenshots of the chat: https://chatgpt.com/share/690a212e-cdec-8010-8514-0352a87da9ad 


<img width="938" height="478" alt="image" src="https://github.com/user-attachments/assets/340bcd74-fd2b-410d-ad34-40cfb6f081e5" />
<img width="940" height="483" alt="image" src="https://github.com/user-attachments/assets/26a915c0-3752-4d33-bfb9-ac9fb625c817" />
<img width="940" height="481" alt="image" src="https://github.com/user-attachments/assets/b4e9e9a4-d686-4bd3-b8e7-97ea987fd2b1" />






























• Title: Disclosure of AI Usage in my Assessment. 
• Section(s) within the assessment in which generative AI was used: Adding GIFs to Android project.
• Name of AI tool(s) used: ChatGPT
• Purpose/intention behind use: Seeking Information on how to add GIFs to an android project. 
• Date(s) in which generative AI was used: 16 August 2025
• A link to the actual generative AI chat, and screenshots of the chat: https://chatgpt.com/share/690a219a-8ee8-8010-b336-257f44e213b3 


<img width="938" height="465" alt="image" src="https://github.com/user-attachments/assets/88ec8d6e-33c3-4a0d-8013-c789aa748179" />
<img width="940" height="474" alt="image" src="https://github.com/user-attachments/assets/67b3b9e4-8ee9-41f4-990c-55d1e8dacea8" />
<img width="938" height="474" alt="image" src="https://github.com/user-attachments/assets/0360d113-079d-409c-b1e3-726fc249122a" />
<img width="937" height="467" alt="image" src="https://github.com/user-attachments/assets/c8cbe68a-5019-4e56-8eb6-3a89f52c7ced" />
<img width="939" height="470" alt="image" src="https://github.com/user-attachments/assets/fad0d638-c68d-463c-ac06-ae2e65c61581" />
<img width="939" height="465" alt="image" src="https://github.com/user-attachments/assets/f327afbd-1d17-4a3b-ae9a-f6316b7d5f8f" />
<img width="939" height="468" alt="image" src="https://github.com/user-attachments/assets/458dd954-0ea0-445f-98cb-a27387644c3d" />
<img width="940" height="471" alt="image" src="https://github.com/user-attachments/assets/f8bb6794-df36-4617-a560-f06e7138b9c5" />
<img width="938" height="468" alt="image" src="https://github.com/user-attachments/assets/be6233c9-14c3-461f-8950-65575c3cfaa3" />
<img width="939" height="466" alt="image" src="https://github.com/user-attachments/assets/373c9de4-6324-4d4f-aac4-50e806e11522" />
<img width="939" height="467" alt="image" src="https://github.com/user-attachments/assets/074a1b7c-8476-4157-85b5-8f6ae0c1239f" />
<img width="939" height="469" alt="image" src="https://github.com/user-attachments/assets/dbc8328c-5fb2-440d-a44e-32ab5bbf55b5" />
<img width="939" height="466" alt="image" src="https://github.com/user-attachments/assets/34c373c4-8cbf-4a1b-ae9c-6ccd805a7e45" />
<img width="938" height="462" alt="image" src="https://github.com/user-attachments/assets/1e9884d0-7a2c-4051-bc6d-7c66e1ebdb2e" />
<img width="939" height="470" alt="image" src="https://github.com/user-attachments/assets/908f08ef-2bf9-4eec-ab46-78b43b236ca3" />
<img width="937" height="465" alt="image" src="https://github.com/user-attachments/assets/9dbd103f-aba6-4b5c-a1ee-5d1cb317d644" />
<img width="940" height="403" alt="image" src="https://github.com/user-attachments/assets/25d32f43-1b70-40ac-b9b0-213a760cfd62" />















































































































































































• Title: Disclosure of AI Usage in my Assessment. 
• Section(s) within the assessment in which generative AI was used: Breadboard/Hardware
• Name of AI tool(s) used: ChatGPT
• Purpose/intention behind use: Seeking information on a scenario regarding setup of components. 
• Date(s) in which generative AI was used: 05 August 2025
• A link to the actual generative AI chat, and screenshots of the chat: https://chatgpt.com/share/690a21b0-55e4-8010-bcc0-84172d178330 


<img width="939" height="464" alt="image" src="https://github.com/user-attachments/assets/388bfde7-3df3-4c08-9d4d-ad3dba167091" />















• Title: Disclosure of AI Usage in my Assessment. 
• Section(s) within the assessment in which generative AI was used: ESP32/Hardware
• Name of AI tool(s) used: ChatGPT
• Purpose/intention behind use: Seeking information on connecting batteries to ESP32. 
• Date(s) in which generative AI was used: 10 September 2025
• A link to the actual generative AI chat, and screenshots of the chat: https://chatgpt.com/share/68ebb7fe-121c-8010-ba54-17a572c6e884 


<img width="938" height="464" alt="image" src="https://github.com/user-attachments/assets/451fc44b-28d8-4830-a972-57f78fe23caf" />
<img width="937" height="462" alt="image" src="https://github.com/user-attachments/assets/61a72442-c342-45d3-a46b-1b49b1df29e8" />
<img width="939" height="462" alt="image" src="https://github.com/user-attachments/assets/19a69402-c74c-4c17-b6eb-2d27885b94d9" />
<img width="940" height="467" alt="image" src="https://github.com/user-attachments/assets/b4897cbb-e655-409d-ab33-753ce0595e3c" />
<img width="939" height="465" alt="image" src="https://github.com/user-attachments/assets/fa18cfd7-7aac-4f23-a1ec-7f67d1c75492" />
<img width="937" height="261" alt="image" src="https://github.com/user-attachments/assets/c7336567-27b6-4831-b976-334b1f80a1a2" />





















































• Title: Disclosure of AI Usage in my Assessment. 
• Section(s) within the assessment in which generative AI was used: Google Services error
• Name of AI tool(s) used: ChatGPT
• Purpose/intention behind use: Assistance troubleshooting code error 
• Date(s) in which generative AI was used: 01 August 2025
• A link to the actual generative AI chat, and screenshots of the chat: https://chatgpt.com/share/690a21e9-ac74-8010-8ee9-c3ad922d864e  


<img width="939" height="461" alt="image" src="https://github.com/user-attachments/assets/4232fc38-d24d-4e93-94b0-e35608f9be4a" />
<img width="939" height="472" alt="image" src="https://github.com/user-attachments/assets/7781fa82-f504-467e-81aa-60085d5bfe74" />
<img width="938" height="466" alt="image" src="https://github.com/user-attachments/assets/88d2e89b-ef29-454f-8497-96df0930f39a" />
<img width="938" height="365" alt="image" src="https://github.com/user-attachments/assets/48f1d519-94d1-4e05-85bf-2c3ceadf5ac8" />




















































• Title: Disclosure of AI Usage in my Assessment. 
• Section(s) within the assessment in which generative AI was used: Git commands
• Name of AI tool(s) used: ChatGPT
• Purpose/intention behind use: Assistance troubleshooting Git terminal fetch error. 
• Date(s) in which generative AI was used: 04 October 2025
• A link to the actual generative AI chat, and screenshots of the chat: https://chatgpt.com/share/690a2202-bf38-8010-882f-9076fba4523f 

<img width="940" height="462" alt="image" src="https://github.com/user-attachments/assets/f3740a71-05b2-4c2c-a475-df257104a338" />
<img width="940" height="467" alt="image" src="https://github.com/user-attachments/assets/b94d5d07-faf8-4437-95d9-65bc441a2b07" />
<img width="940" height="402" alt="image" src="https://github.com/user-attachments/assets/91b59240-b983-4aad-826f-77ac371e9293" />































• Title: Disclosure of AI Usage in my Assessment. 
• Section(s) within the assessment in which generative AI was used: Gyroscope code
• Name of AI tool(s) used: ChatGPT
• Purpose/intention behind use: Questioning if gyroscope code will work with application
• Date(s) in which generative AI was used: 11 October 2025
• A link to the actual generative AI chat, and screenshots of the chat: https://chatgpt.com/share/690a2225-bc70-8010-a11c-998a1fdaca1f 

<img width="939" height="466" alt="image" src="https://github.com/user-attachments/assets/e930e0c5-fd0b-4331-998c-5897527de523" />
<img width="939" height="463" alt="image" src="https://github.com/user-attachments/assets/53e77af9-df2b-4734-9ee8-14f9d610c554" />







































---

<p align="center">
  <i>“Innovation happens when technology meets purpose.”</i><br>
  <sub>© 2025 Kinetic Cape Project | WIL2025</sub>
</p>

<p align="right">(<a href="#readme-top">back to top</a>)</p>
