# Copilot Instructions for WIL2025

## What this app is
Android app (Java, Gradle Kotlin scripts) that connects to an ESP32 over Bluetooth, visualizes motion, and records jump stats via a REST API. Main code in `app/src/main/java/com/example/kineticpulsemobileapp/`. Microcontroller code in `esp32-gyro/`.

## Architecture and data flow
- Entry: `MainActivity.java` hosts fragments. Device picking lives in `DevicesFragment.java`, which navigates to `TerminalFragment.java`.
- Bluetooth: Classic Bluetooth via a serial service. `DevicesFragment` lists bonded devices; `TerminalFragment` binds `SerialService`, creates a `SerialSocket`, and handles `SerialListener` callbacks. Look for `send(String)` and `receive(ArrayDeque<byte[]>)` in `TerminalFragment`.
- Motion sources:
	- Primary: ESP32 ADXL345 over Bluetooth. `TerminalFragment` parses messages like `ACCEL:x,y,z` and `MOVE:LEFT|RIGHT|FORWARD|BACK` (see `processESP32MovementData`, `parseAccelerometerData`, `parseMovementCommand`).
	- Optional: Phone gyroscope via `GyroManager.java`. `TerminalFragment` can switch modes and listens to `GyroManager.MovementListener` for `onLeft/onRight/onMiddle/onBack`.
- UI reactions to motion: `TerminalFragment` updates counters (`tvJumpLeft/Right/Middle/Back`), sets LED colors via `send()` to ESP32, shows a flash animation using `res/anim/movement_flash.xml` into `ivMovementFlash`, and (in production mode) uses Text-to-Speech to announce movement.
- API: `RetrofitInstance.java` exposes the API, `APIHandler.java` defines calls. Jump counts are posted in `TerminalFragment.saveJumpDataToAPI()`.
- Resources: Layout `fragment_terminal.xml` contains the terminal UI, movement counters, LED controls, flash area, and a debug-only Dev/Prod switch.

## New dev/prod toggle and behavior
- Debug-only toggle: `fragment_terminal.xml` includes `@id/switchMode`. `TerminalFragment` shows it only when `BuildConfig.DEBUG` is true; it’s hidden in release builds. Stored in `SharedPreferences` (`PREFS_APP`/`prod_mode`).
- Behavior:
	- Production mode: On any detected motion (ESP32 or phone gyro), the app flashes the corresponding image and speaks the movement out loud via Android TTS.
	- Development mode: Same visuals; TTS announcements are suppressed.
- Touchpoints: See `initModeSwitch`, `initTts`, and calls to `speakIfProd(...)` and `showMovementFlash(...)` inside movement handlers in `TerminalFragment`.

## Developer workflows
- Build locally:
	- Requires `google-services.json` under `app/` (Firebase is used by `TerminalFragment` for `FirebaseAuth`). Without it, Gradle fails at `:app:processDebugGoogleServices`.
	- Build: `./gradlew :app:assembleDebug` (or use Android Studio). Tests: `./gradlew test`.
- Run/Debug: Use Android Studio with a device. Choose a bonded Bluetooth device in `DevicesFragment` → `TerminalFragment` auto-connects and shows motion logs.
- Motion debug tips:
	- Type `TEST` or `LEDTEST` or `ACCELTEST` in the send box to run built-in sequences in `TerminalFragment`.
	- Use the mode button `btnGyroToggle` to switch between ESP32 and phone gyroscope modes.

## Project conventions and patterns
- Keep motion strings consistent with parser: `ACCEL:x,y,z`, `MOVE:LEFT|RIGHT|FORWARD|BACK`, and legacy text substrings (“Lateral-Left Movement Detected”, “Jump detected! Yahoo! ^^”).
- LED commands are single letters from `TerminalFragment` (`r,g,b,w,t,l,a,o`). Update both UI handlers and ESP32 code (`esp32-gyro/main.ino`) together.
- Calibration: `GyroManager` runs a fast, guided calibration sequence and debounced movement detection; thresholds are tuned (≈6°) for responsiveness.
- Localization exists (`LocaleHelper.java` and `values-zu/`). If adding text, place in `res/values/strings.xml`.

## Key files to read first
- `app/src/main/java/com/example/kineticpulsemobileapp/TerminalFragment.java` — movement processing, UI, Bluetooth send/receive, dev/prod switch, TTS.
- `app/src/main/java/com/example/kineticpulsemobileapp/GyroManager.java` — phone gyro integration and calibration flow.
- `app/src/main/java/com/example/kineticpulsemobileapp/DevicesFragment.java` — device selection and navigation.
- `app/src/main/java/com/example/kineticpulsemobileapp/RetrofitInstance.java` and `APIHandler.java` — API configuration and requests.
- `app/src/main/res/layout/fragment_terminal.xml` — terminal UI, counters, LED controls, and flash animation view.

## Gotchas
- Firebase: Missing `google-services.json` blocks builds. Either provide a project file or disable the plugin for local-only builds.
- Bluetooth permissions differ for Android 12+; the code requests `BLUETOOTH_CONNECT` where needed.
- `BuildConfig.DEBUG` controls visibility of the dev/prod switch; release builds always behave as production.

If anything is unclear or you need more context (e.g., Firebase setup or ESP32 protocol changes), ask to refine these notes.