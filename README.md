# Just Another Health App (Samsung Health + Gemini AI)

An elegant Android application designed in Jetpack Compose that interfaces with the **Samsung Health Data SDK** to read daily fitness, nutrition, and weight metrics, and processes this data using the **Gemini 1.5 Flash** model to provide real-time coaching insights and dynamic, background-evaluated alerts.

---

## Features

1. **Dashboard (Today vs Yesterday)**: Displays current-day health metrics (steps, water, sodium, calories, weight) and maps a direct comparative performance against yesterday's logs.
2. **Analytics & Canvas Trends**: Renders custom-drawn Canvas trend bar charts for step counts, sodium levels, water consumption, and calories over the past 7 days.
3. **AI Health Coach & Ingestion**: Chat with an LLM coach that has native access to your health history via function calling. You can attach simulated reports (lab results, cholesterol levels, meal screenshots) to get tailored guidance.
4. **AI Smart Alerts & Reminders**: Register arbitrary natural language alerts (e.g. *"Warn me if my sodium averages start increasing"*). A background WorkManager task runs daily, allowing Gemini to query your health database to check if the condition is met and issue system notifications.
5. **Settings Hub**: Access system status, toggling developer mode, and security configurations.

---

## Tech Stack

- **UI Framework**: Jetpack Compose (Kotlin) with material icons and custom canvas graphics.
- **Health Data**: Samsung Health Data SDK (v1.1.0) with local `.aar` bindings.
- **AI Processing**: Google AI Client SDK (Gemini API with Tool Use / Function Calling).
- **Background Tasks**: Android Jetpack WorkManager.
- **Serialization & Logic**: Gson.
