package com.example.healthapp.ai

import android.content.Context
import android.util.Log
import com.example.healthapp.data.HealthManager
import com.example.healthapp.data.HealthSummary
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlobPart
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject

class GeminiClient(private val context: Context) {

    private val tag = "GeminiClient"
    private val gson = Gson()

    // 1. Define the tool/function declaration for history querying
    private val queryHealthHistoryDeclaration = defineFunction(
        name = "queryHealthHistory",
        description = "Query the user's historical daily health metrics (steps, calories, sodium, water, weight) for the last N days.",
        listOf(Schema.int(name = "daysBack", description = "The number of days back to query (e.g., 7 or 14)"))
    )

    private val saveHealthTargetsDeclaration = defineFunction(
        name = "saveHealthTargets",
        description = "Save the generated daily health targets approved by the user.",
        listOf(
            Schema.int(name = "steps", description = "Daily steps goal"),
            Schema.int(name = "calories", description = "Daily calorie intake limit"),
            Schema.int(name = "sodium", description = "Daily sodium limit in mg"),
            Schema.int(name = "water", description = "Daily water goal in ml"),
            Schema.double(name = "weight", description = "Target body weight in kg"),
            Schema.int(name = "protein", description = "Daily protein goal in grams"),
            Schema.int(name = "carbs", description = "Daily carbohydrates goal in grams"),
            Schema.int(name = "fat", description = "Daily fat goal in grams"),
            Schema.int(name = "sugars", description = "Daily sugars limit in grams"),
            Schema.int(name = "fiber", description = "Daily dietary fiber goal in grams"),
            Schema.int(name = "saturatedFat", description = "Daily saturated fat limit in grams")
        )
    )

    private val healthTools = listOf(Tool(listOf(queryHealthHistoryDeclaration)))
    private val targetTools = listOf(Tool(listOf(queryHealthHistoryDeclaration, saveHealthTargetsDeclaration)))

    /**
     * Helper to get or create GenerativeModel with the given API key.
     */
    private fun getModel(apiKey: String): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            tools = healthTools
        )
    }

    private fun getTargetModel(apiKey: String): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            tools = targetTools
        )
    }

    /**
     * Generate daily comparison insights.
     */
    fun generateWeeklyInsights(
        apiKey: String,
        history: List<HealthSummary>
    ): Flow<String> = flow {
        if (apiKey.isBlank()) {
            emit("Gemini API key not set. Please update configuration under system calibrations tab.")
            return@flow
        }

        emit("STAGE 1/4: Structuring 7-day health trends history...")
        delay(600)

        try {
            val model = getModel(apiKey)
            val summaryText = history.joinToString("\n\n") { day ->
                val dateStr = day.date.toString()
                
                val steps = if (day.activity.steps > 0) "${day.activity.steps}" else "[Not Tracked]"
                val activeCal = if (day.activity.activeCalories > 0.0) "${day.activity.activeCalories.toInt()} kcal" else "[Not Tracked]"
                val exerciseMin = if (day.activity.exerciseMinutes > 0) "${day.activity.exerciseMinutes} min" else "[Not Tracked]"
                
                val cal = if (day.nutrition.calories > 0.0) "${day.nutrition.calories.toInt()} kcal" else "[Not Tracked]"
                val sodium = if (day.nutrition.sodiumMg > 0.0) "${day.nutrition.sodiumMg.toInt()} mg" else "[Not Tracked]"
                val water = if (day.nutrition.waterMl > 0.0) "${day.nutrition.waterMl.toInt()} ml" else "[Not Tracked]"
                val weight = if (day.nutrition.weightKg > 0.0) "${String.format("%.1f", day.nutrition.weightKg * 2.20462)} lbs" else "[Not Tracked]"
                
                val protein = if (day.nutrition.proteinG > 0.0) "${day.nutrition.proteinG.toInt()} g" else "[Not Tracked]"
                val carbs = if (day.nutrition.carbsG > 0.0) "${day.nutrition.carbsG.toInt()} g" else "[Not Tracked]"
                val fat = if (day.nutrition.fatG > 0.0) "${day.nutrition.fatG.toInt()} g" else "[Not Tracked]"
                
                val sugars = if (day.nutrition.sugarsG > 0.0) "${day.nutrition.sugarsG.toInt()} g" else "[Not Tracked]"
                val fiber = if (day.nutrition.fiberG > 0.0) "${day.nutrition.fiberG.toInt()} g" else "[Not Tracked]"
                val satFat = if (day.nutrition.saturatedFatG > 0.0) "${day.nutrition.saturatedFatG.toInt()} g" else "[Not Tracked]"

                """
                Date: $dateStr
                - Steps: $steps
                - Calories Burned: $activeCal
                - Exercise Duration: $exerciseMin
                - Calories Eaten: $cal
                - Sodium: $sodium
                - Water: $water
                - Weight: $weight
                - Protein: $protein
                - Carbs: $carbs
                - Fat: $fat
                - Sugars: $sugars
                - Fiber: $fiber
                - Saturated Fat: $satFat
                """.trimIndent()
            }

            emit("STAGE 2/4: Reading calorie, step, and sodium thresholds...")
            delay(600)

            val prompt = """
                Analyze the user's health and nutrition history over the last 7 completed days:
                
                $summaryText
                
                Generate a concise, premium 2-paragraph weekly trend summary. 
                Identify key trends (e.g. macro distribution, hydration levels, step volume trends, sugars/fiber/saturated fat quality limits) and target breaches.
                Ensure you ignore any metrics marked as '[Not Tracked]'. Do NOT assume they are 0; treat them as missing data.
                Provide one actionable health recommendation for the coming week.
                Keep the tone encouraging, analytical, and professional like a trading terminal audit.
            """.trimIndent()

            emit("STAGE 3/4: RAG - Evaluating macro balances (sugars, fiber, fats)...")
            delay(600)

            emit("STAGE 4/4: Generating weekly insights dashboard audit...")
            val response = model.generateContent(prompt)
            emit(response.text ?: "Failed to generate weekly trend insights.")
        } catch (e: Exception) {
            Log.e(tag, "Gemini weekly insights call failed", e)
            emit("Error generating weekly insights: ${e.localizedMessage}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Send message in a chat session. Automatically executes and resolves function calls.
     */
    suspend fun chatWithModel(
        apiKey: String,
        historyList: List<com.google.ai.client.generativeai.type.Content>,
        newMessage: String,
        healthManager: HealthManager,
        attachmentBytes: ByteArray? = null,
        attachmentMimeType: String? = null
    ): ChatResult {
        if (apiKey.isBlank()) {
            return ChatResult(
                reply = "API Key not set. Go to the Settings tab to enter your Gemini API key.\n\n" +
                        "Simulated Coach: I see you asked a question. Once connected to Gemini, " +
                        "I can query your steps and nutritional records automatically to answer questions like this!",
                newHistory = historyList
            )
        }

        try {
            val model = getModel(apiKey)
            val chat = model.startChat(historyList)

            val initialContent = if (attachmentBytes != null && attachmentMimeType != null) {
                content("user") {
                    part(BlobPart(attachmentMimeType, attachmentBytes))
                    text(newMessage)
                }
            } else {
                content("user") {
                    text(newMessage)
                }
            }

            var response = chat.sendMessage(initialContent)
            var iterations = 0

            // Loop to handle up to 3 back-and-forth function calls
            while (response.functionCalls.isNotEmpty() && iterations < 3) {
                iterations++
                val call = response.functionCalls.first()
                Log.d(tag, "Received function call request from Gemini: ${call.name} with args ${call.args}")

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
                            "weightLbs" to summary.nutrition.weightKg * 2.20462
                        )
                    })
                } else {
                    mapOf("error" to "Unknown function: ${call.name}")
                }

                // Send the function output back to the model
                response = chat.sendMessage(
                    content("function") {
                        part(FunctionResponsePart(call.name, JSONObject(result)))
                    }
                )
            }

            return ChatResult(
                reply = response.text ?: "No response from coach.",
                newHistory = chat.history
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed chat with model", e)
            return ChatResult(
                reply = "Error: ${e.localizedMessage}",
                newHistory = historyList
            )
        }
    }

    suspend fun chatWithTargetConsultant(
        apiKey: String,
        historyList: List<com.google.ai.client.generativeai.type.Content>,
        newMessage: String,
        profile: com.example.healthapp.data.UserProfile,
        context: Context,
        healthManager: HealthManager,
        onProgress: (String) -> Unit = {}
    ): ChatResult {
        if (apiKey.isBlank()) {
            return ChatResult(
                reply = "API Key not set.",
                newHistory = historyList
            )
        }

        onProgress("STAGE 1/4: Connecting to Gemini model...")
        try {
            val model = getTargetModel(apiKey)
            val chat = model.startChat(historyList)

            if (historyList.isEmpty()) {
                onProgress("STAGE 2/4: Packing physical profile data (height, weight, age)...")
            } else {
                onProgress("STAGE 2/4: Appending chat message context...")
            }

            val finalMessage = if (historyList.isEmpty()) {
                val profileIntro = """
                    [User Profile Context from Samsung Health]
                    Height: ${profile.height} cm
                    Weight: ${String.format("%.1f", profile.weight * 2.20462)} lbs
                    Gender: ${profile.gender}
                    BirthDate: ${profile.birthDate}
                    Nickname: ${profile.nickname}
                    
                    Hello AI Target Consultant! Recommend custom daily targets for me. My height and weight details are above.
                    User Message: $newMessage
                """.trimIndent()
                profileIntro
            } else {
                newMessage
            }

            var response = chat.sendMessage(finalMessage)
            var iterations = 0

            while (response.functionCalls.isNotEmpty() && iterations < 3) {
                iterations++
                val call = response.functionCalls.first()
                Log.d(tag, "Target Consultant received function call request: ${call.name} with args ${call.args}")

                val result = when (call.name) {
                    "queryHealthHistory" -> {
                        val daysBack = call.args["daysBack"]?.toIntOrNull() ?: 7
                        onProgress("STAGE 3/4 [RAG]: Querying $daysBack days of steps, calories, and sodium history...")
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
                                "weightLbs" to summary.nutrition.weightKg * 2.20462,
                                "sugarsG" to summary.nutrition.sugarsG,
                                "fiberG" to summary.nutrition.fiberG,
                                "saturatedFatG" to summary.nutrition.saturatedFatG
                            )
                        })
                    }
                    "saveHealthTargets" -> {
                        val steps = call.args["steps"]?.toIntOrNull() ?: 10000
                        val cals = call.args["calories"]?.toIntOrNull() ?: 2200
                        val sodium = call.args["sodium"]?.toIntOrNull() ?: 2300
                        val water = call.args["water"]?.toIntOrNull() ?: 2000
                        val weight = call.args["weight"]?.toFloatOrNull() ?: 165.0f
                        
                        val protein = call.args["protein"]?.toIntOrNull() ?: 100
                        val carbs = call.args["carbs"]?.toIntOrNull() ?: 200
                        val fat = call.args["fat"]?.toIntOrNull() ?: 70

                        val sugars = call.args["sugars"]?.toIntOrNull() ?: 50
                        val fiber = call.args["fiber"]?.toIntOrNull() ?: 30
                        val saturatedFat = call.args["saturatedFat"]?.toIntOrNull() ?: 20

                        onProgress("STAGE 3/4 [TOOL]: Writing updated step, calorie, and nutrient targets...")
                        val prefs = context.getSharedPreferences("health_app_prefs", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putInt("target_steps", steps)
                            putInt("target_calories", cals)
                            putInt("target_sodium", sodium)
                            putInt("target_water", water)
                            putFloat("target_weight", weight)
                            putInt("target_protein", protein)
                            putInt("target_carbs", carbs)
                            putInt("target_fat", fat)
                            putInt("target_sugars", sugars)
                            putInt("target_fiber", fiber)
                            putInt("target_saturated_fat", saturatedFat)
                        }.apply()

                        mapOf("success" to true, "message" to "All targets (including protein, carbs, fat, sugars, fiber, saturated fat) saved successfully to the app.")
                    }
                    else -> {
                        mapOf("error" to "Unknown function: ${call.name}")
                    }
                }

                response = chat.sendMessage(
                    content("function") {
                        part(FunctionResponsePart(call.name, JSONObject(result)))
                    }
                )
            }

            onProgress("STAGE 4/4: Formulating calibration splits...")
            return ChatResult(
                reply = response.text ?: "No response from targets coach.",
                newHistory = chat.history
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed chat with target consultant", e)
            return ChatResult(
                reply = "Error: ${e.localizedMessage}",
                newHistory = historyList
            )
        }
    }

    private fun generateMockInsights(today: HealthSummary, yesterday: HealthSummary): String {
        val stepDiff = today.activity.steps - yesterday.activity.steps
        val sodiumState = if (today.nutrition.sodiumMg > 2300.0) "slightly elevated" else "normal"
        val stepsAdvice = if (stepDiff >= 0) {
            "You increased your steps by ${stepDiff} compared to yesterday. Excellent momentum!"
        } else {
            "Steps are down by ${Math.abs(stepDiff)} compared to yesterday. Try taking a short walk after dinner."
        }

        return """
            Comparing today with yesterday, your activity is showing steady progress. $stepsAdvice Your water intake is currently at ${today.nutrition.waterMl.toInt()} ml.
            
            On the nutrition front, your sodium levels are $sodiumState at ${today.nutrition.sodiumMg.toInt()} mg today. Actionable Tip: Keep drinking water to maintain proper hydration and balance your sodium levels.
        """.trimIndent()
    }
}

data class ChatResult(
    val reply: String,
    val newHistory: List<com.google.ai.client.generativeai.type.Content>
)
