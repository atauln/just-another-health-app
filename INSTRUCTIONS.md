# Development & Setup Instructions

Follow these steps to configure, build, and run **Just Another Health App**.

---

## 1. SDK Library Setup (Local AAR)

The Samsung Health Data SDK is proprietary and must be integrated manually:
1. Download the SDK `.zip` file from the [Samsung Developers Portal](https://developer.samsung.com/health).
2. Extract the archive and copy `samsung-health-data-api-1.1.0.aar` into the **`app/libs/`** directory.
3. Make sure the filename matches your declaration in `app/build.gradle.kts` (already configured for version `1.1.0`).

*Note: A placeholder folder has been created under `app/libs/`.*

---

## 2. Samsung Health App Developer Mode

Because Samsung's SDK restricts access on standard devices and emulators, you must enable Developer Mode on a physical Samsung phone:
1. Open **Samsung Health** on your phone.
2. Go to **Settings > About Samsung Health**.
3. Tap the **Version** number quickly **10+ times**.
4. In the hidden menu that appears, select **Developer mode (Samsung Health Data SDK)**.

---

## 3. Gemini API Key Configuration

1. Get a free API Key from Google AI Studio.
2. Run the application.
3. Go to the **Settings** tab.
4. Enter your API Key into the field. It will be stored securely in private `SharedPreferences` for both chat queries and background alerts.

---

## 4. Registering and Testing AI Alerts

1. Go to the **Alerts** tab.
2. Type an alert condition in plain English, for example:
   * *"Notify me if my sodium average is above 2000mg"*
   * *"Alert me if I took fewer than 5000 steps today"*
3. Click the **`+`** button to register it.
4. To test the evaluation loop immediately without waiting for the daily morning trigger, click **Evaluate Alerts Now**.
5. Check your Android system tray for notifications if conditions are met!

---

## 5. Compilation & Building

To clean and compile the project, run:
```bash
./gradlew build
```
