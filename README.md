<a id="readme-top"></a>

<br />

<!--
Code attribution:
For this ReadMe this is the template I used:
othneildrew., 2024. Best-README-Template (version 1.1.2) [Source code]. Available at:< https://github.com/othneildrew/Best-README-Template.git> Accessed 26 April 2025].
-->

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=30&pause=1000&color=00F57C&center=true&vCenter=true&width=500&lines=Welcome+to+Kinetic+Cape!;Smart+Motion+Tracking!;BLE+Connected+Experience!⚙️" alt="Typing SVG" />
</p>

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

## 🔍 References

- Espressif Systems, 2024. *ESP32 Documentation.* [Online] Available at: <https://www.espressif.com/>  
- TDK InvenSense, 2024. *MPU6050 Product Datasheet.* [Online] Available at: <https://invensense.tdk.com/>  
- NimBLE-Arduino Library, 2023. [Source Code] Available at: <https://github.com/h2zero/NimBLE-Arduino>  
- Adafruit NeoPixel Library, 2024. [Online] Available at: <https://github.com/adafruit/Adafruit_NeoPixel>  
- Android Developers, 2024. *Bluetooth Low Energy Overview.* [Online] Available at: <https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview>  
- othneildrew., 2024. *Best-README-Template (version 1.1.2).* [Source code] Available at: <https://github.com/othneildrew/Best-README-Template.git>

---

<p align="center">
  <i>“Innovation happens when technology meets purpose.”</i><br>
  <sub>© 2025 Kinetic Cape Project | WIL2025</sub>
</p>

<p align="right">(<a href="#readme-top">back to top</a>)</p>
