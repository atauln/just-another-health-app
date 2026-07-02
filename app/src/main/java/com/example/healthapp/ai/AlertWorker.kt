package com.example.healthapp.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healthapp.data.HealthManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "AlertWorker"
    private val channelId = "health_alerts_channel"

    private val queryHealthHistoryDeclaration = defineFunction(
        name = "queryHealthHistory",
        description = "Query the user's historical daily health metrics (steps, calories, sodium, water, weight) for the last N days.",
        listOf(Schema.int(name = "daysBack", description = "The number of days back to query (e.g., 7 or 14)"))
    )

    private val healthTools = listOf(Tool(listOf(queryHealthHistoryDeclaration)))

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(tag, "Starting background health alert check...")

        val sharedPrefs = appContext.getSharedPreferences("health_app_prefs", Context.MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isBlank()) {
            Log.w(tag, "API Key is missing. Background alerts check skipped.")
            return@withContext Result.success()
        }

        val alertsJson = sharedPrefs.getString("alerts_list", "[]") ?: "[]"
        val alerts = try {
            Gson().fromJson(alertsJson, Array<String>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }

        if (alerts.isEmpty()) {
            Log.d(tag, "No registered alerts to evaluate.")
            return@withContext Result.success()
        }

        val healthManager = HealthManager(appContext)
        healthManager.connect()

        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            tools = healthTools
        )

        for (alertText in alerts) {
            try {
                evaluateAlert(model, alertText, healthManager)
            } catch (e: Exception) {
                Log.e(tag, "Error evaluating alert: '$alertText'", e)
            }
        }

        Result.success()
    }

    private suspend fun evaluateAlert(
        model: GenerativeModel,
        alertText: String,
        healthManager: HealthManager
    ) {
        val chat = model.startChat()
        val prompt = """
            You are a background daemon evaluating a user-configured health alert.
            Alert condition: "$alertText"
            
            Evaluate if this condition is currently true. You MUST query the historical health data using the 'queryHealthHistory' function to assess the condition (e.g., if the user asks about sodium, steps, or calorie averages, run a query covering the relevant number of days).
            
            Once you have retrieved the necessary historical data, determine if the condition is met (true/false).
            
            Return your answer ONLY as a raw JSON object in this format:
            {
              "isTriggered": true,
              "notificationTitle": "Alert: [Reason for trigger]",
              "notificationMessage": "[Explain clearly what triggered the alert, referencing specific metrics and averages]"
            }
            Do not wrap it in markdown code blocks, do not return any other text.
        """.trimIndent()

        var response = chat.sendMessage(prompt)
        var iterations = 0

        // Handle function calling loop
        while (response.functionCalls.isNotEmpty() && iterations < 3) {
            iterations++
            val call = response.functionCalls.first()
            val result = if (call.name == "queryHealthHistory") {
                val daysBack = call.args["daysBack"]?.toIntOrNull() ?: 7
                
                val history = healthManager.fetchHistory(daysBack)
                
                mapOf("history" to history.map { summary ->
                    mapOf(
                        "date" to summary.date.toString(),
                        "steps" to summary.activity.steps,
                        "activeCaloriesBurned" to summary.activity.activeCalories,
                        "exerciseMinutes" to summary.activity.exerciseMinutes,
                        "caloriesConsumed" to summary.nutrition.calories,
                        "sodiumMg" to summary.nutrition.sodiumMg,
                        "waterMl" to summary.nutrition.waterMl,
                        "weightKg" to summary.nutrition.weightKg
                    )
                })
            } else {
                mapOf("error" to "Unknown function: ${call.name}")
            }

            response = chat.sendMessage(
                content("function") {
                    part(FunctionResponsePart(call.name, JSONObject(result)))
                }
            )
        }

        val responseText = response.text ?: return
        Log.d(tag, "Alert evaluation response: $responseText")

        try {
            val jsonObject = JsonParser.parseString(responseText.trim()).asJsonObject
            val isTriggered = jsonObject.get("isTriggered")?.asBoolean ?: false
            if (isTriggered) {
                val title = jsonObject.get("notificationTitle")?.asString ?: "Health Alert"
                val message = jsonObject.get("notificationMessage")?.asString ?: "Trigger condition met."
                showNotification(title, message)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse JSON response from alert evaluator: $responseText", e)
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Health Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Sends smart notifications generated by AI background analysis."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Generate a unique ID based on the title's hash
        notificationManager.notify(title.hashCode(), notification)
    }
}
