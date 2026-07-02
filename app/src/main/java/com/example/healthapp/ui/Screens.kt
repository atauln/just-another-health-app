package com.example.healthapp.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.healthapp.*
import com.example.healthapp.ai.AlertWorker
import com.example.healthapp.ai.GeminiClient
import com.example.healthapp.data.HealthManager
import com.example.healthapp.data.HealthSummary
import com.google.ai.client.generativeai.type.Content
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

// Helper extension to access API key and alerts
fun Context.getPrefs() = getSharedPreferences("health_app_prefs", Context.MODE_PRIVATE)

fun Context.getApiKey(): String = getPrefs().getString("gemini_api_key", "") ?: ""

fun Context.saveApiKey(key: String) {
    getPrefs().edit().putString("gemini_api_key", key).apply()
}

fun Context.getAlerts(): List<String> {
    val json = getPrefs().getString("alerts_list", "[]") ?: "[]"
    return try {
        Gson().fromJson(json, Array<String>::class.java).toList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun Context.saveAlerts(alerts: List<String>) {
    val json = Gson().toJson(alerts)
    getPrefs().edit().putString("alerts_list", json).apply()
}

// ----------------------------------------------------
// 1. Dashboard Screen (Landing/Insights)
// ----------------------------------------------------
@Composable
fun DashboardScreen(healthManager: HealthManager, geminiClient: GeminiClient) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var today by remember { mutableStateOf<HealthSummary?>(null) }
    var yesterday by remember { mutableStateOf<HealthSummary?>(null) }

    LaunchedEffect(healthManager) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            today = healthManager.fetchSummary(LocalDate.now())
            yesterday = healthManager.fetchSummary(LocalDate.now().minusDays(1))
        }
    }

    var aiInsights by remember { mutableStateOf("Click 'Generate AI Insights' below to analyze today's metrics compared with yesterday's.") }
    var isGenerating by remember { mutableStateOf(false) }

    val todayData = today
    val yesterdayData = yesterday
    if (todayData == null || yesterdayData == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AccentCyan)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Today's Insights",
                color = TextLight,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Comparative Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricComparisonCard(
                    title = "Activity (Steps)",
                    todayValue = "${todayData.activity.steps}",
                    yesterdayValue = "${yesterdayData.activity.steps}",
                    unit = "steps",
                    color = AccentCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricComparisonCard(
                    title = "Active Calories",
                    todayValue = String.format("%.0f", todayData.activity.activeCalories),
                    yesterdayValue = String.format("%.0f", yesterdayData.activity.activeCalories),
                    unit = "kcal",
                    color = AccentViolet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricComparisonCard(
                    title = "Calorie Intake",
                    todayValue = String.format("%.0f", todayData.nutrition.calories),
                    yesterdayValue = String.format("%.0f", yesterdayData.nutrition.calories),
                    unit = "kcal",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                MetricComparisonCard(
                    title = "Sodium Level",
                    todayValue = String.format("%.0f", todayData.nutrition.sodiumMg),
                    yesterdayValue = String.format("%.0f", yesterdayData.nutrition.sodiumMg),
                    unit = "mg",
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricComparisonCard(
                    title = "Water Intake",
                    todayValue = String.format("%.0f", todayData.nutrition.waterMl),
                    yesterdayValue = String.format("%.0f", yesterdayData.nutrition.waterMl),
                    unit = "ml",
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                MetricComparisonCard(
                    title = "Body Weight",
                    todayValue = String.format("%.1f", todayData.nutrition.weightKg),
                    yesterdayValue = String.format("%.1f", yesterdayData.nutrition.weightKg),
                    unit = "kg",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // AI Coaching Insights block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AI Health Companion",
                        color = AccentCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isGenerating) {
                        CircularProgressIndicator(color = AccentCyan)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Analyzing health metrics...", color = TextMuted, fontSize = 14.sp)
                    } else {
                        Text(
                            text = aiInsights,
                            color = TextLight,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                geminiClient.generateDailyInsights(
                                    apiKey = context.getApiKey(),
                                    today = todayData,
                                    yesterday = yesterdayData
                                ).collect { insights ->
                                    aiInsights = insights
                                    isGenerating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        enabled = !isGenerating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate AI Insights", color = DarkBgEnd, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricComparisonCard(
    title: String,
    todayValue: String,
    yesterdayValue: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            
            Column {
                Text(
                    text = "$todayValue $unit",
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Yesterday: $yesterdayValue $unit",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ----------------------------------------------------
// 2. Charts & Analytics Screen
// ----------------------------------------------------
@Composable
fun AnalyticsScreen(healthManager: HealthManager) {
    var history by remember { mutableStateOf<List<HealthSummary>?>(null) }
    var selectedMetric by remember { mutableStateOf("Steps") }

    LaunchedEffect(healthManager) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            history = healthManager.fetchHistory(7)
        }
    }

    val historyData = history
    if (historyData == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AccentCyan)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Analytics & Trends",
            color = TextLight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Dropdown selection row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val metricsList = listOf("Steps", "Sodium", "Water", "Calories")
            metricsList.forEach { metric ->
                val selected = selectedMetric == metric
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) AccentCyan else CardDark,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedMetric = metric }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = metric,
                        color = if (selected) DarkBgEnd else TextLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$selectedMetric - Last 7 Days",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Custom Graphic Chart using Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(top = 16.dp, bottom = 16.dp)
                ) {
                    val values = historyData.map { summary ->
                        when (selectedMetric) {
                            "Steps" -> summary.activity.steps.toFloat()
                            "Sodium" -> summary.nutrition.sodiumMg.toFloat()
                            "Water" -> summary.nutrition.waterMl.toFloat()
                            else -> summary.nutrition.calories.toFloat()
                        }
                    }
                    val maxValue = values.maxOrNull() ?: 1f
                    val chartColor = when (selectedMetric) {
                        "Steps" -> AccentCyan
                        "Sodium" -> Color(0xFFEF4444)
                        "Water" -> Color(0xFF3B82F6)
                        else -> Color(0xFFF59E0B)
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val barCount = historyData.size
                        val spacing = 24.dp.toPx()
                        val totalBarSpacing = spacing * (barCount - 1)
                        val barWidth = (width - totalBarSpacing) / barCount

                        for (i in 0 until barCount) {
                            val value = values[i]
                            val barHeight = (value / maxValue) * (height * 0.85f)
                            val x = i * (barWidth + spacing)
                            val y = height - barHeight

                            // Draw rounded bars
                            drawRoundRect(
                                color = chartColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                    }
                }

                // Date Label Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    historyData.forEach { summary ->
                        Text(
                            text = "${summary.date.dayOfMonth}/${summary.date.monthValue}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. AI Chat Screen with Doc Import
// ----------------------------------------------------
data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun GeminiChatScreen(healthManager: HealthManager, geminiClient: GeminiClient) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val chatMessages = remember { mutableStateListOf(
        ChatMessage("Hello! I am your AI Coach. Ask me anything about your steps, nutrition, weight logs, or attach a health document (lab report, meal screenshot) to discuss!", false)
    ) }
    val chatHistory = remember { mutableStateOf<List<Content>>(emptyList()) }

    var userMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Simulated Document Ingestion selection
    var showDocSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AI Health Coach",
            color = TextLight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Chat History List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { msg ->
                    ChatBubble(msg)
                }
            }
        }

        // Inline Document Selector Popup/Card
        AnimatedVisibility(visible = showDocSelector) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Select a simulated document to attach:", color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val sampleDocs = listOf(
                        "Lab Report: Elevated Cholesterol (06/15/2026)",
                        "Blood Panel: Normal Sodium (06/28/2026)",
                        "Meal Screenshot: 850kcal Salad/Lunch (Today)"
                    )
                    sampleDocs.forEach { doc ->
                        Text(
                            text = doc,
                            color = TextLight,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showDocSelector = false
                                    chatMessages.add(ChatMessage("[Attached Context Document: $doc]", true))
                                    chatMessages.add(ChatMessage("Analyzing attached report. Ask me a question about it!", false))
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Message Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { showDocSelector = !showDocSelector },
                colors = IconButtonDefaults.iconButtonColors(contentColor = AccentViolet)
            ) {
                Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach Document")
            }

            TextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text("Ask your Coach...", color = TextMuted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CardDark,
                    unfocusedContainerColor = CardDark,
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    cursorColor = AccentCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (userMessage.isNotBlank() && !isSending) {
                        val msg = userMessage
                        userMessage = ""
                        isSending = true
                        chatMessages.add(ChatMessage(msg, true))

                        coroutineScope.launch {
                            val result = geminiClient.chatWithModel(
                                apiKey = context.getApiKey(),
                                historyList = chatHistory.value,
                                newMessage = msg,
                                healthManager = healthManager
                            )
                            chatMessages.add(ChatMessage(result.reply, false))
                            chatHistory.value = result.newHistory
                            isSending = false
                            listState.animateScrollToItem(chatMessages.size - 1)
                        }
                    }
                })
            )

            IconButton(
                onClick = {
                    if (userMessage.isNotBlank() && !isSending) {
                        val msg = userMessage
                        userMessage = ""
                        isSending = true
                        chatMessages.add(ChatMessage(msg, true))

                        coroutineScope.launch {
                            val result = geminiClient.chatWithModel(
                                apiKey = context.getApiKey(),
                                historyList = chatHistory.value,
                                newMessage = msg,
                                healthManager = healthManager
                            )
                            chatMessages.add(ChatMessage(result.reply, false))
                            chatHistory.value = result.newHistory
                            isSending = false
                            listState.animateScrollToItem(chatMessages.size - 1)
                        }
                    }
                },
                enabled = userMessage.isNotBlank() && !isSending,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = AccentCyan,
                    contentColor = DarkBgEnd
                )
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (msg.isUser) AccentCyan else CardDark
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                color = if (msg.isUser) DarkBgEnd else TextLight,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// ----------------------------------------------------
// 4. Reminders & Alerts Page
// ----------------------------------------------------
@Composable
fun RemindersScreen() {
    val context = LocalContext.current
    var alertsList by remember { mutableStateOf(context.getAlerts()) }
    var newAlertText by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AI Alert Monitors",
            color = TextLight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Define checks evaluated every morning. Gemini queries history to determine alerts.",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Add Alert Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newAlertText,
                onValueChange = { newAlertText = it },
                placeholder = { Text("e.g. Notify if sodium average is increasing", color = TextMuted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CardDark,
                    unfocusedContainerColor = CardDark,
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    if (newAlertText.isNotBlank()) {
                        val updated = alertsList + newAlertText
                        alertsList = updated
                        context.saveAlerts(updated)
                        newAlertText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(52.dp)
                    .background(AccentCyan, RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alert", tint = DarkBgEnd)
            }
        }

        // Active Alerts List
        Text("Registered Monitored Alerts", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alertsList) { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(alert, color = TextLight, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            text = "Remove",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    val updated = alertsList - alert
                                    alertsList = updated
                                    context.saveAlerts(updated)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger Alert Evaluation Now button (Development helper)
        Button(
            onClick = {
                isChecking = true
                val request = OneTimeWorkRequestBuilder<AlertWorker>().build()
                WorkManager.getInstance(context).enqueue(request)
                Toast.makeText(context, "Alert Worker triggered successfully!", Toast.LENGTH_SHORT).show()
                isChecking = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Evaluate Alerts Now", color = TextLight, fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------------------------------------------
// 5. Settings Screen
// ----------------------------------------------------
@Composable
fun SettingsScreen(healthManager: HealthManager) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var apiKeyInput by remember { mutableStateOf(context.getApiKey()) }
    val sdkState by healthManager.connectionState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "App Settings",
            color = TextLight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // API Key Section
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gemini API Configuration", color = AccentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("An API key is required to analyze comparisons and run active health alerts.", color = TextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        context.saveApiKey(it)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("Enter Gemini API Key...", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBgStart,
                        unfocusedContainerColor = DarkBgStart,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Connection status card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Samsung Health Connection", color = AccentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                val stateLabel: String
                val stateColor: Color
                when (sdkState) {
                    is HealthManager.ConnectionState.Connected -> {
                        stateLabel = "CONNECTED"
                        stateColor = Color(0xFF10B981)
                    }
                    is HealthManager.ConnectionState.Connecting -> {
                        stateLabel = "CONNECTING..."
                        stateColor = Color(0xFFF59E0B)
                    }
                    is HealthManager.ConnectionState.Disconnected -> {
                        stateLabel = "DISCONNECTED"
                        stateColor = TextMuted
                    }
                    is HealthManager.ConnectionState.Failed -> {
                        stateLabel = "FAILED (Mock Fallback Active)"
                        stateColor = Color(0xFFEF4444)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(stateColor, RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stateLabel, color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        activity?.let { act ->
                            healthManager.checkAndRequestPermissions(act) { success ->
                                android.widget.Toast.makeText(
                                    context,
                                    if (success) "Samsung Health permissions connected!" else "Permissions request failed or denied.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sync Permissions", color = DarkBgEnd, fontWeight = FontWeight.Bold)
                }

                if (sdkState is HealthManager.ConnectionState.Failed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (sdkState as HealthManager.ConnectionState.Failed).message,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Dev Mode Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Developer Instructions", color = AccentViolet, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val instructions = listOf(
                    "1. Open the Samsung Health app on your physical test phone.",
                    "2. Open Settings > About Samsung Health.",
                    "3. Tap the version number quickly 10 times or more.",
                    "4. In the developer menu, enable 'Developer mode (Samsung Health Data SDK)'."
                )
                instructions.forEach { line ->
                    Text(line, color = TextLight, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
