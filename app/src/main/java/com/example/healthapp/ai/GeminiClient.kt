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
            Schema.int(name = "fat", description = "Daily fat goal in grams")
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
    fun generateDailyInsights(
        apiKey: String,
        today: HealthSummary,
        yesterday: HealthSummary
    ): Flow<String> = flow {
        if (apiKey.isBlank()) {
            emit(generateMockInsights(today, yesterday))
            return@flow
        }

        try {
            val model = getModel(apiKey)
            val prompt = """
                Analyze the health and nutrition metrics of the user for Today vs Yesterday:
                
                Today:
                - Steps: ${today.activity.steps}
                - Calories burned: ${today.activity.activeCalories} kcal
                - Exercise duration: ${today.activity.exerciseMinutes} min
                - Calories eaten: ${today.nutrition.calories} kcal
                - Sodium: ${today.nutrition.sodiumMg} mg
                - Water intake: ${today.nutrition.waterMl} ml
                - Weight: ${today.nutrition.weightKg} kg
                
                Yesterday:
                - Steps: ${yesterday.activity.steps}
                - Calories burned: ${yesterday.activity.activeCalories} kcal
                - Exercise duration: ${yesterday.activity.exerciseMinutes} min
                - Calories eaten: ${yesterday.nutrition.calories} kcal
                - Sodium: ${yesterday.nutrition.sodiumMg} mg
                - Water intake: ${yesterday.nutrition.waterMl} ml
                - Weight: ${yesterday.nutrition.weightKg} kg
                
                Generate a concise, premium 2-paragraph summary comparing their progress. 
                Identify key changes (e.g., step count drop/increase, high sodium intake, water hydration levels) 
                and provide one actionable health tip. Keep the tone encouraging and analytical.
            """.trimIndent()

            val response = model.generateContent(prompt)
            emit(response.text ?: "Failed to generate insights from Gemini.")
        } catch (e: Exception) {
            Log.e(tag, "Gemini API call failed, using mock insights", e)
            emit("Error generating insights: ${e.localizedMessage}\n\n${generateMockInsights(today, yesterday)}")
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
                            "weightKg" to summary.nutrition.weightKg
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
        healthManager: HealthManager
    ): ChatResult {
        if (apiKey.isBlank()) {
            return ChatResult(
                reply = "API Key not set.",
                newHistory = historyList
            )
        }

        try {
            val model = getTargetModel(apiKey)
            val chat = model.startChat(historyList)

            val finalMessage = if (historyList.isEmpty()) {
                val profileIntro = """
                    [User Profile Context from Samsung Health]
                    Height: ${profile.height} cm
                    Weight: ${profile.weight} kg
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
                    }
                    "saveHealthTargets" -> {
                        val steps = call.args["steps"]?.toIntOrNull() ?: 10000
                        val cals = call.args["calories"]?.toIntOrNull() ?: 2200
                        val sodium = call.args["sodium"]?.toIntOrNull() ?: 2300
                        val water = call.args["water"]?.toIntOrNull() ?: 2000
                        val weight = call.args["weight"]?.toFloatOrNull() ?: 75.0f
                        
                        val protein = call.args["protein"]?.toIntOrNull() ?: 100
                        val carbs = call.args["carbs"]?.toIntOrNull() ?: 200
                        val fat = call.args["fat"]?.toIntOrNull() ?: 70

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
                        }.apply()

                        mapOf("success" to true, "message" to "All targets (including protein, carbs, fat) saved successfully to the app.")
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
