package com.example.healthapp.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.font.FontFamily
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

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.DashPathEffect
import android.content.Intent
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.window.Dialog

// Helper extension to access API key and alerts
fun Context.getPrefs() = getSharedPreferences("health_app_prefs", Context.MODE_PRIVATE)

fun Context.getApiKey(): String = getPrefs().getString("gemini_api_key", "")?.trim() ?: ""

fun Context.saveApiKey(key: String) {
    getPrefs().edit().putString("gemini_api_key", key.trim()).apply()
}

fun Context.getTargetSteps(): Int = getPrefs().getInt("target_steps", 10000)
fun Context.saveTargetSteps(steps: Int) = getPrefs().edit().putInt("target_steps", steps).apply()

fun Context.getTargetCalories(): Int = getPrefs().getInt("target_calories", 2200)
fun Context.saveTargetCalories(cals: Int) = getPrefs().edit().putInt("target_calories", cals).apply()

fun Context.getTargetSodium(): Int = getPrefs().getInt("target_sodium", 2300)
fun Context.saveTargetSodium(sodium: Int) = getPrefs().edit().putInt("target_sodium", sodium).apply()

fun Context.getTargetWater(): Int = getPrefs().getInt("target_water", 2000)
fun Context.saveTargetWater(water: Int) = getPrefs().edit().putInt("target_water", water).apply()

fun Context.getTargetWeight(): Float = getPrefs().getFloat("target_weight", 75.0f)
fun Context.saveTargetWeight(weight: Float) = getPrefs().edit().putFloat("target_weight", weight).apply()

fun Context.getTargetProtein(): Int = getPrefs().getInt("target_protein", 100)
fun Context.saveTargetProtein(p: Int) = getPrefs().edit().putInt("target_protein", p).apply()

fun Context.getTargetCarbs(): Int = getPrefs().getInt("target_carbs", 200)
fun Context.saveTargetCarbs(c: Int) = getPrefs().edit().putInt("target_carbs", c).apply()

fun Context.getTargetFat(): Int = getPrefs().getInt("target_fat", 70)
fun Context.saveTargetFat(f: Int) = getPrefs().edit().putInt("target_fat", f).apply()

// Visibility getters/setters
fun Context.getShowSteps(): Boolean = getPrefs().getBoolean("show_metric_steps", true)
fun Context.saveShowSteps(b: Boolean) = getPrefs().edit().putBoolean("show_metric_steps", b).apply()

fun Context.getShowCalories(): Boolean = getPrefs().getBoolean("show_metric_calories", true)
fun Context.saveShowCalories(b: Boolean) = getPrefs().edit().putBoolean("show_metric_calories", b).apply()

fun Context.getShowSodium(): Boolean = getPrefs().getBoolean("show_metric_sodium", true)
fun Context.saveShowSodium(b: Boolean) = getPrefs().edit().putBoolean("show_metric_sodium", b).apply()

fun Context.getShowWater(): Boolean = getPrefs().getBoolean("show_metric_water", true)
fun Context.saveShowWater(b: Boolean) = getPrefs().edit().putBoolean("show_metric_water", b).apply()

fun Context.getShowWeight(): Boolean = getPrefs().getBoolean("show_metric_weight", true)
fun Context.saveShowWeight(b: Boolean) = getPrefs().edit().putBoolean("show_metric_weight", b).apply()

fun Context.getShowActiveCalories(): Boolean = getPrefs().getBoolean("show_metric_active_calories", true)
fun Context.saveShowActiveCalories(b: Boolean) = getPrefs().edit().putBoolean("show_metric_active_calories", b).apply()

fun Context.getShowProtein(): Boolean = getPrefs().getBoolean("show_metric_protein", true)
fun Context.saveShowProtein(b: Boolean) = getPrefs().edit().putBoolean("show_metric_protein", b).apply()

fun Context.getShowCarbs(): Boolean = getPrefs().getBoolean("show_metric_carbs", true)
fun Context.saveShowCarbs(b: Boolean) = getPrefs().edit().putBoolean("show_metric_carbs", b).apply()

fun Context.getShowFat(): Boolean = getPrefs().getBoolean("show_metric_fat", true)
fun Context.saveShowFat(b: Boolean) = getPrefs().edit().putBoolean("show_metric_fat", b).apply()


fun shareChart(context: Context, history: List<HealthSummary>, metric: String) {
    val targetValue = when (metric) {
        "Steps" -> context.getTargetSteps().toFloat()
        "Sodium" -> context.getTargetSodium().toFloat()
        "Water" -> context.getTargetWater().toFloat()
        "Protein" -> context.getTargetProtein().toFloat()
        "Carbs" -> context.getTargetCarbs().toFloat()
        "Fat" -> context.getTargetFat().toFloat()
        else -> context.getTargetCalories().toFloat()
    }
    
    val width = 800
    val height = 600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#0F172A") }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    
    val titlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("$metric Chart - Last ${history.size} Days", 40f, 60f, titlePaint)
    
    val values = history.map { summary ->
        when (metric) {
            "Steps" -> summary.activity.steps.toFloat()
            "Sodium" -> summary.nutrition.sodiumMg.toFloat()
            "Water" -> summary.nutrition.waterMl.toFloat()
            "Protein" -> summary.nutrition.proteinG.toFloat()
            "Carbs" -> summary.nutrition.carbsG.toFloat()
            "Fat" -> summary.nutrition.fatG.toFloat()
            else -> summary.nutrition.calories.toFloat()
        }
    }
    val maxValue = (values.maxOrNull() ?: 1f).coerceAtLeast(targetValue).coerceAtLeast(1f)
    
    val barCount = history.size
    val chartHeight = height - 200f
    val chartWidth = width - 80f
    val barSpacing = 24f
    val barWidth = (chartWidth - (barSpacing * (barCount - 1))) / barCount
    
    val barPaint = Paint().apply { isAntiAlias = true }
    val linePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#22D3EE")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    val targetPaint = Paint().apply {
        color = android.graphics.Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
        isAntiAlias = true
    }
    val textPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#94A3B8")
        textSize = 20f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    val points = ArrayList<android.graphics.PointF>()
    
    for (i in 0 until barCount) {
        val value = values[i]
        val barH = (value / maxValue) * (chartHeight * 0.85f)
        val left = 40f + i * (barWidth + barSpacing)
        val right = left + barWidth
        val bottom = height - 100f
        val top = bottom - barH
        
        val isExceeded = if (metric == "Sodium" || metric == "Calories") value > targetValue else value < targetValue
        val hexColor = if (metric == "Sodium" || metric == "Calories") {
            if (isExceeded) "#EF4444" else "#10B981"
        } else {
            if (isExceeded) "#F59E0B" else "#06B6D4"
        }
        barPaint.color = android.graphics.Color.parseColor(hexColor)
        
        canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, barPaint)
        
        points.add(android.graphics.PointF(left + barWidth / 2, top))
        
        val summary = history[i]
        canvas.drawText("${summary.date.dayOfMonth}/${summary.date.monthValue}", left + barWidth / 2, bottom + 40f, textPaint)
    }
    
    val path = Path()
    if (points.isNotEmpty()) {
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        canvas.drawPath(path, linePaint)
        
        val pointPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        for (pt in points) {
            canvas.drawCircle(pt.x, pt.y, 8f, pointPaint)
        }
    }
    
    val yTarget = (height - 100f) - (targetValue / maxValue) * (chartHeight * 0.85f)
    canvas.drawLine(40f, yTarget, width - 40f, yTarget, targetPaint)
    
    val labelPaint = Paint().apply {
        color = android.graphics.Color.RED
        textSize = 22f
        isAntiAlias = true
    }
    canvas.drawText("Target: ${targetValue.toInt()}", width - 160f, yTarget - 10f, labelPaint)
    
    try {
        val imagesDir = File(context.cacheDir, "shared_images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val file = File(imagesDir, "${metric.lowercase()}_chart.png")
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()
        
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "My $metric Chart")
            putExtra(Intent.EXTRA_TEXT, "Check out my $metric trend chart from my health app!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Chart"))
    } catch (e: java.lang.Exception) {
        Toast.makeText(context, "Failed to share chart: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
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

    var aiInsights by remember { mutableStateOf("Click 'RUN PORTFOLIO ANALYSIS' below to evaluate health indicators.") }
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

    val showSteps = remember { context.getShowSteps() }
    val showCalories = remember { context.getShowCalories() }
    val showSodium = remember { context.getShowSodium() }
    val showWater = remember { context.getShowWater() }
    val showWeight = remember { context.getShowWeight() }
    val showActiveCals = remember { context.getShowActiveCalories() }
    val showProtein = remember { context.getShowProtein() }
    val showCarbs = remember { context.getShowCarbs() }
    val showFat = remember { context.getShowFat() }

    val enabledCards = ArrayList<@Composable (Modifier) -> Unit>()
    if (showSteps) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Daily Steps",
                todayValue = "${todayData.activity.steps}",
                yesterdayValue = "${yesterdayData.activity.steps}",
                unit = "steps",
                color = AccentCyan,
                modifier = modifier
            )
        })
    }
    if (showActiveCals) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Active Calories",
                todayValue = String.format("%.0f", todayData.activity.activeCalories),
                yesterdayValue = String.format("%.0f", yesterdayData.activity.activeCalories),
                unit = "kcal",
                color = AccentViolet,
                modifier = modifier
            )
        })
    }
    if (showCalories) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Calorie Intake",
                todayValue = String.format("%.0f", todayData.nutrition.calories),
                yesterdayValue = String.format("%.0f", yesterdayData.nutrition.calories),
                unit = "kcal",
                color = Color(0xFFF59E0B),
                modifier = modifier
            )
        })
    }
    if (showSodium) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Sodium Level",
                todayValue = String.format("%.0f", todayData.nutrition.sodiumMg),
                yesterdayValue = String.format("%.0f", yesterdayData.nutrition.sodiumMg),
                unit = "mg",
                color = Color(0xFFEF4444),
                modifier = modifier
            )
        })
    }
    if (showWater) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Water Intake",
                todayValue = String.format("%.0f", todayData.nutrition.waterMl),
                yesterdayValue = String.format("%.0f", yesterdayData.nutrition.waterMl),
                unit = "ml",
                color = Color(0xFF3B82F6),
                modifier = modifier
            )
        })
    }
    if (showWeight) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Body Weight",
                todayValue = String.format("%.1f", todayData.nutrition.weightKg),
                yesterdayValue = String.format("%.1f", yesterdayData.nutrition.weightKg),
                unit = "kg",
                color = Color(0xFF10B981),
                modifier = modifier
            )
        })
    }
    if (showProtein) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Protein Intake",
                todayValue = String.format("%.0f", todayData.nutrition.proteinG),
                yesterdayValue = String.format("%.0f", yesterdayData.nutrition.proteinG),
                unit = "g",
                color = AccentCyan,
                modifier = modifier
            )
        })
    }
    if (showCarbs) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Carb Intake",
                todayValue = String.format("%.0f", todayData.nutrition.carbsG),
                yesterdayValue = String.format("%.0f", yesterdayData.nutrition.carbsG),
                unit = "g",
                color = AccentViolet,
                modifier = modifier
            )
        })
    }
    if (showFat) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Fat Intake",
                todayValue = String.format("%.0f", todayData.nutrition.fatG),
                yesterdayValue = String.format("%.0f", yesterdayData.nutrition.fatG),
                unit = "g",
                color = Color(0xFFF59E0B),
                modifier = modifier
            )
        })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Ticker Summary Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "CAL" to "${todayData.nutrition.calories.toInt()}/${context.getTargetCalories()} kcal",
                    "ACT" to "${todayData.activity.activeCalories.toInt()} kcal",
                    "PRO" to "${todayData.nutrition.proteinG.toInt()}/${context.getTargetProtein()}g",
                    "CARB" to "${todayData.nutrition.carbsG.toInt()}/${context.getTargetCarbs()}g",
                    "FAT" to "${todayData.nutrition.fatG.toInt()}/${context.getTargetFat()}g",
                    "WGT" to "${todayData.nutrition.weightKg}kg"
                ).forEach { (label, valStr) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, color = AccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Text(text = valStr, color = TextLight, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }

        item {
            Text(
                text = "PORTFOLIO INSIGHTS",
                color = TextLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        // Render enabled cards in Rows of 2
        val rows = enabledCards.chunked(2)
        rows.forEach { rowCards ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (rowCards.size == 2) {
                        rowCards[0](Modifier.weight(1f))
                        rowCards[1](Modifier.weight(1f))
                    } else if (rowCards.size == 1) {
                        rowCards[0](Modifier.weight(0.5f))
                        Spacer(modifier = Modifier.weight(0.5f))
                    }
                }
            }
        }

        // AI Coaching Insights block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[AI ANALYST INSIGHTS - REALTIME REPORT]",
                            color = AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("LIVE", color = Color(0xFF10B981), fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isGenerating) {
                        CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("COMPUTING PORTFOLIO METRICS...", color = TextMuted, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    } else {
                        Text(
                            text = aiInsights,
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (!isGenerating) {
                                isGenerating = true
                                coroutineScope.launch {
                                    geminiClient.generateDailyInsights(
                                        apiKey = context.getApiKey(),
                                        today = todayData,
                                        yesterday = yesterdayData
                                    ).collect { chunk ->
                                        aiInsights = chunk
                                        isGenerating = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Text("RUN PORTFOLIO ANALYSIS", color = DarkBgEnd, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
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
    val todayNum = todayValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    val yesterdayNum = yesterdayValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    val percentChange = if (yesterdayNum > 0) {
        ((todayNum - yesterdayNum) / yesterdayNum) * 100
    } else {
        0.0
    }

    Box(
        modifier = modifier
            .background(Color(0xFF020617), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                val (trendText, trendColor) = when {
                    percentChange > 0 -> "+" + String.format("%.1f", percentChange) + "%" to Color(0xFF10B981)
                    percentChange < 0 -> String.format("%.1f", percentChange) + "%" to Color(0xFFEF4444)
                    else -> "0.0%" to TextMuted
                }
                Text(
                    text = trendText,
                    color = trendColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Text(
                text = "$todayValue $unit".uppercase(),
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Text(
                text = "YST: $yesterdayValue $unit".uppercase(),
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

// ----------------------------------------------------
// 2. Charts & Analytics Screen
// ----------------------------------------------------
@Composable
fun AnalyticsScreen(healthManager: HealthManager) {
    val context = LocalContext.current
    var timelineDays by remember { mutableStateOf(7) }
    var history by remember { mutableStateOf<List<HealthSummary>?>(null) }

    LaunchedEffect(healthManager, timelineDays) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            history = healthManager.fetchHistory(timelineDays)
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Heading & Timeframe switcher row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANALYTICS DECK [H1-REPORT]",
                    color = TextLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(7 to "7D", 14 to "14D", 30 to "30D").forEach { (days, label) ->
                        val selected = timelineDays == days
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) AccentCyan else Color(0xFF020617),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                .clickable { timelineDays = days }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) DarkBgEnd else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Segment 1: Energy & Intake Chart Deck (Calories vs Target)
        item {
            BrokerageChartCard(
                title = "CH-01: ENERGY ACCOUNTING (CALORIES)",
                history = historyData,
                targetValue = context.getTargetCalories().toFloat(),
                unit = "kcal",
                colorStart = Color(0xFFF59E0B),
                metricSelector = { it.nutrition.calories.toFloat() }
            )
        }

        // Segment 2: Macronutrient Breakdown (Protein / Carbs / Fat)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "CH-02: MACRONUTRIENT LEDGERS (G)",
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val proteinValues = historyData.map { it.nutrition.proteinG.toFloat() }
                    val carbsValues = historyData.map { it.nutrition.carbsG.toFloat() }
                    val fatValues = historyData.map { it.nutrition.fatG.toFloat() }
                    val maxVal = (proteinValues + carbsValues + fatValues).maxOrNull()?.coerceAtLeast(1f) ?: 1f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val barCount = historyData.size
                            val spacing = 6.dp.toPx()
                            val barWidth = (w - (spacing * (barCount - 1))) / (barCount * 3)

                            for (i in 0 until barCount) {
                                val xGroup = i * (barWidth * 3 + spacing)
                                
                                // Protein bar (Cyan)
                                val py = h - (proteinValues[i] / maxVal) * h
                                drawRect(
                                    color = AccentCyan,
                                    topLeft = Offset(xGroup, py),
                                    size = Size(barWidth, h - py)
                                )

                                // Carbs bar (Violet)
                                val cy = h - (carbsValues[i] / maxVal) * h
                                drawRect(
                                    color = AccentViolet,
                                    topLeft = Offset(xGroup + barWidth, cy),
                                    size = Size(barWidth, h - cy)
                                )

                                // Fat bar (Orange)
                                val fy = h - (fatValues[i] / maxVal) * h
                                drawRect(
                                    color = Color(0xFFF59E0B),
                                    topLeft = Offset(xGroup + barWidth * 2, fy),
                                    size = Size(barWidth, h - fy)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(AccentCyan))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PRO (TGT: ${context.getTargetProtein()}g)", color = TextMuted, fontSize = 8.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(AccentViolet))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CARB (TGT: ${context.getTargetCarbs()}g)", color = TextMuted, fontSize = 8.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFFF59E0B)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FAT (TGT: ${context.getTargetFat()}g)", color = TextMuted, fontSize = 8.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Segment 3: Hydration Metrics Chart (Water)
        item {
            BrokerageChartCard(
                title = "CH-03: HYDRATION REGISTER (WATER)",
                history = historyData,
                targetValue = context.getTargetWater().toFloat(),
                unit = "ml",
                colorStart = Color(0xFF3B82F6),
                metricSelector = { it.nutrition.waterMl.toFloat() }
            )
        }

        // Segment 4: Cardiovascular Activity (Steps)
        item {
            BrokerageChartCard(
                title = "CH-04: CARDIOVASCULAR VOLUME (STEPS)",
                history = historyData,
                targetValue = context.getTargetSteps().toFloat(),
                unit = "steps",
                colorStart = AccentCyan,
                metricSelector = { it.activity.steps.toFloat() }
            )
        }
    }
}

@Composable
fun BrokerageChartCard(
    title: String,
    history: List<HealthSummary>,
    targetValue: Float,
    unit: String,
    colorStart: Color,
    metricSelector: (HealthSummary) -> Float
) {
    val context = LocalContext.current
    val values = history.map(metricSelector)
    val maxVal = (values.maxOrNull() ?: 1f).coerceAtLeast(targetValue).coerceAtLeast(1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "TGT: ${targetValue.toInt()} $unit".uppercase(),
                        color = Color(0xFFEF4444),
                        fontSize = 8.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                androidx.compose.material3.IconButton(
                    onClick = { shareChart(context, history, title.split(" ").last()) },
                    modifier = Modifier.size(24.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = AccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barCount = history.size
                    val spacing = 6.dp.toPx()
                    val barWidth = (w - (spacing * (barCount - 1))) / barCount

                    val points = mutableListOf<Offset>()

                    for (i in 0 until barCount) {
                        val value = values[i]
                        val barHeight = (value / maxVal) * (h * 0.85f)
                        val x = i * (barWidth + spacing)
                        val y = h - barHeight

                        drawRect(
                            color = colorStart.copy(alpha = 0.2f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )

                        points.add(Offset(x + barWidth / 2, y))
                    }

                    // Dash Target Line
                    val targetY = h - (targetValue / maxVal) * (h * 0.85f)
                    drawLine(
                        color = Color(0xFFEF4444).copy(alpha = 0.5f),
                        start = Offset(0f, targetY),
                        end = Offset(w, targetY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Line Chart Connecting Points
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = colorStart,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(
                            color = colorStart,
                            radius = 2.dp.toPx(),
                            center = points[i]
                        )
                    }
                    if (points.isNotEmpty()) {
                        drawCircle(
                            color = colorStart,
                            radius = 2.dp.toPx(),
                            center = points.last()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val step = if (history.size > 14) 5 else if (history.size > 7) 2 else 1
                history.forEachIndexed { index, summary ->
                    if (index % step == 0) {
                        Text(
                            text = "${summary.date.dayOfMonth}/${summary.date.monthValue}",
                            color = TextMuted,
                            fontSize = 8.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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

    // Real Document Picker state
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAttachmentMimeType by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAttachmentUri = uri
            selectedAttachmentMimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            var name = "Attached File"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                name = uri.lastPathSegment ?: "Attached File"
            }
            selectedAttachmentName = name
        }
    }

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

        // Selected Attachment Preview Card
        AnimatedVisibility(visible = selectedAttachmentUri != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attached file",
                            tint = AccentViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedAttachmentName ?: "File attached",
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = selectedAttachmentMimeType ?: "application/octet-stream",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            selectedAttachmentUri = null
                            selectedAttachmentMimeType = null
                            selectedAttachmentName = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = Color(0xFFEF4444)
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
                onClick = { filePickerLauncher.launch("*/*") },
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
                    if ((userMessage.isNotBlank() || selectedAttachmentUri != null) && !isSending) {
                        val msg = userMessage
                        userMessage = ""
                        isSending = true

                        val uri = selectedAttachmentUri
                        val mime = selectedAttachmentMimeType
                        val name = selectedAttachmentName

                        selectedAttachmentUri = null
                        selectedAttachmentMimeType = null
                        selectedAttachmentName = null

                        if (uri != null) {
                            chatMessages.add(ChatMessage("[Sent file: $name]\n$msg", true))
                        } else {
                            chatMessages.add(ChatMessage(msg, true))
                        }

                        coroutineScope.launch {
                            try {
                                var bytes: ByteArray? = null
                                if (uri != null) {
                                    context.contentResolver.openInputStream(uri)?.use { stream ->
                                        bytes = stream.readBytes()
                                    }
                                }

                                val result = geminiClient.chatWithModel(
                                    apiKey = context.getApiKey(),
                                    historyList = chatHistory.value,
                                    newMessage = msg.ifBlank { "Analyze this attached file." },
                                    healthManager = healthManager,
                                    attachmentBytes = bytes,
                                    attachmentMimeType = mime
                                )
                                chatMessages.add(ChatMessage(result.reply, false))
                                chatHistory.value = result.newHistory
                            } catch (e: Exception) {
                                chatMessages.add(ChatMessage("Error: ${e.localizedMessage}", false))
                            } finally {
                                isSending = false
                                listState.animateScrollToItem(chatMessages.size - 1)
                            }
                        }
                    }
                })
            )

            IconButton(
                onClick = {
                    if ((userMessage.isNotBlank() || selectedAttachmentUri != null) && !isSending) {
                        val msg = userMessage
                        userMessage = ""
                        isSending = true

                        val uri = selectedAttachmentUri
                        val mime = selectedAttachmentMimeType
                        val name = selectedAttachmentName

                        selectedAttachmentUri = null
                        selectedAttachmentMimeType = null
                        selectedAttachmentName = null

                        if (uri != null) {
                            chatMessages.add(ChatMessage("[Sent file: $name]\n$msg", true))
                        } else {
                            chatMessages.add(ChatMessage(msg, true))
                        }

                        coroutineScope.launch {
                            try {
                                var bytes: ByteArray? = null
                                if (uri != null) {
                                    context.contentResolver.openInputStream(uri)?.use { stream ->
                                        bytes = stream.readBytes()
                                    }
                                }

                                val result = geminiClient.chatWithModel(
                                    apiKey = context.getApiKey(),
                                    historyList = chatHistory.value,
                                    newMessage = msg.ifBlank { "Analyze this attached file." },
                                    healthManager = healthManager,
                                    attachmentBytes = bytes,
                                    attachmentMimeType = mime
                                )
                                chatMessages.add(ChatMessage(result.reply, false))
                                chatHistory.value = result.newHistory
                            } catch (e: Exception) {
                                chatMessages.add(ChatMessage("Error: ${e.localizedMessage}", false))
                            } finally {
                                isSending = false
                                listState.animateScrollToItem(chatMessages.size - 1)
                            }
                        }
                    }
                },
                enabled = (userMessage.isNotBlank() || selectedAttachmentUri != null) && !isSending,
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
fun SettingsScreen(healthManager: HealthManager, geminiClient: GeminiClient) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var apiKeyInput by remember { mutableStateOf(context.getApiKey()) }
    val sdkState by healthManager.connectionState.collectAsState()

    var stepsInput by remember { mutableStateOf(context.getTargetSteps().toString()) }
    var caloriesInput by remember { mutableStateOf(context.getTargetCalories().toString()) }
    var sodiumInput by remember { mutableStateOf(context.getTargetSodium().toString()) }
    var waterInput by remember { mutableStateOf(context.getTargetWater().toString()) }
    var weightInput by remember { mutableStateOf(context.getTargetWeight().toString()) }

    var proteinInput by remember { mutableStateOf(context.getTargetProtein().toString()) }
    var carbsInput by remember { mutableStateOf(context.getTargetCarbs().toString()) }
    var fatInput by remember { mutableStateOf(context.getTargetFat().toString()) }

    var showSteps by remember { mutableStateOf(context.getShowSteps()) }
    var showCalories by remember { mutableStateOf(context.getShowCalories()) }
    var showSodium by remember { mutableStateOf(context.getShowSodium()) }
    var showWater by remember { mutableStateOf(context.getShowWater()) }
    var showWeight by remember { mutableStateOf(context.getShowWeight()) }
    var showActiveCalories by remember { mutableStateOf(context.getShowActiveCalories()) }
    var showProtein by remember { mutableStateOf(context.getShowProtein()) }
    var showCarbs by remember { mutableStateOf(context.getShowCarbs()) }
    var showFat by remember { mutableStateOf(context.getShowFat()) }

    var showTargetConsultant by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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

        // Tracked Metrics Customization Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tracked Metrics Visibility", color = AccentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Configure which metrics are monitored and displayed across the app.", color = TextMuted, fontSize = 12.sp)

                listOf(
                    Triple(showSteps, "Daily Steps") { b: Boolean -> showSteps = b; context.saveShowSteps(b) },
                    Triple(showCalories, "Calorie Intake (Food)") { b: Boolean -> showCalories = b; context.saveShowCalories(b) },
                    Triple(showSodium, "Sodium Level") { b: Boolean -> showSodium = b; context.saveShowSodium(b) },
                    Triple(showWater, "Water Intake") { b: Boolean -> showWater = b; context.saveShowWater(b) },
                    Triple(showWeight, "Body Weight") { b: Boolean -> showWeight = b; context.saveShowWeight(b) },
                    Triple(showActiveCalories, "Exercise Calories Burned") { b: Boolean -> showActiveCalories = b; context.saveShowActiveCalories(b) },
                    Triple(showProtein, "Daily Protein") { b: Boolean -> showProtein = b; context.saveShowProtein(b) },
                    Triple(showCarbs, "Daily Carbohydrates") { b: Boolean -> showCarbs = b; context.saveShowCarbs(b) },
                    Triple(showFat, "Daily Fat") { b: Boolean -> showFat = b; context.saveShowFat(b) }
                ).forEach { (isChecked, label, onCheckChanged) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCheckChanged(!isChecked) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onCheckChanged(it) },
                            colors = CheckboxDefaults.colors(checkedColor = AccentCyan, uncheckedColor = TextMuted)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, color = TextLight, fontSize = 14.sp)
                    }
                }
            }
        }

        // Daily Health Targets Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Daily Health Targets", color = AccentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Configure your daily target levels and macro budgets.", color = TextMuted, fontSize = 12.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Steps Goal", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = stepsInput,
                            onValueChange = {
                                stepsInput = it
                                it.toIntOrNull()?.let { steps -> context.saveTargetSteps(steps) }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBgStart,
                                unfocusedContainerColor = DarkBgStart,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Calorie Target", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = caloriesInput,
                            onValueChange = {
                                caloriesInput = it
                                it.toIntOrNull()?.let { cals -> context.saveTargetCalories(cals) }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBgStart,
                                unfocusedContainerColor = DarkBgStart,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sodium Limit (mg)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = sodiumInput,
                            onValueChange = {
                                sodiumInput = it
                                it.toIntOrNull()?.let { sod -> context.saveTargetSodium(sod) }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBgStart,
                                unfocusedContainerColor = DarkBgStart,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Water Goal (ml)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = waterInput,
                            onValueChange = {
                                waterInput = it
                                it.toIntOrNull()?.let { wat -> context.saveTargetWater(wat) }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBgStart,
                                unfocusedContainerColor = DarkBgStart,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Protein Goal (g)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = proteinInput,
                            onValueChange = {
                                proteinInput = it
                                it.toIntOrNull()?.let { p -> context.saveTargetProtein(p) }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBgStart,
                                unfocusedContainerColor = DarkBgStart,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Carbs Goal (g)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = carbsInput,
                            onValueChange = {
                                carbsInput = it
                                it.toIntOrNull()?.let { c -> context.saveTargetCarbs(c) }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBgStart,
                                unfocusedContainerColor = DarkBgStart,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fat Goal (g)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = fatInput,
                            onValueChange = {
                                fatInput = it
                                it.toIntOrNull()?.let { f -> context.saveTargetFat(f) }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBgStart,
                                unfocusedContainerColor = DarkBgStart,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Target Weight (kg)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextField(
                            value = weightInput,
                            onValueChange = {
                                weightInput = it
                                it.toFloatOrNull()?.let { wt -> context.saveTargetWeight(wt) }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBgStart,
                                unfocusedContainerColor = DarkBgStart,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = {
                                val sdkTargets = healthManager.fetchTargets()
                                context.saveTargetSteps(sdkTargets.steps)
                                context.saveTargetCalories(sdkTargets.calories)
                                context.saveTargetWater(sdkTargets.water)
                                stepsInput = sdkTargets.steps.toString()
                                caloriesInput = sdkTargets.calories.toString()
                                waterInput = sdkTargets.water.toString()
                                Toast.makeText(context, "Targets imported from Samsung Health!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Import Targets", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showTargetConsultant = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Consult AI for Targets", color = DarkBgEnd, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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

    if (showTargetConsultant) {
        Dialog(onDismissRequest = { showTargetConsultant = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBgStart),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
                    .border(1.dp, AccentCyan, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Target Consultant",
                            color = AccentCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showTargetConsultant = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Dialog", tint = Color.Red)
                        }
                    }
                    Text(
                        text = "Discuss with the AI to tailor steps, calorie limits, macros, and sodium bounds based on height, weight, and history.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val consultantHistory = remember { mutableStateOf<List<Content>>(emptyList()) }
                    val consultantMessages = remember { mutableStateListOf(
                        ChatMessage("Hello! I am your AI Target Consultant. Based on your profile and history, I will suggest optimal daily step goals, calorie/sodium limits, and carb/protein/fat macro splits. What goals are we shooting for today?", false)
                    ) }
                    var consultantMessageInput by remember { mutableStateOf("") }
                    var isConsultantSending by remember { mutableStateOf(false) }
                    val userProfile = remember { healthManager.fetchUserProfile() }

                    // Scrollable Chat area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(consultantMessages) { msg ->
                                ChatBubble(msg)
                            }
                        }
                    }

                    // Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = consultantMessageInput,
                            onValueChange = { consultantMessageInput = it },
                            placeholder = { Text("Say something...", color = TextMuted) },
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
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (consultantMessageInput.isNotBlank() && !isConsultantSending) {
                                    val msg = consultantMessageInput
                                    consultantMessageInput = ""
                                    isConsultantSending = true
                                    consultantMessages.add(ChatMessage(msg, true))

                                    coroutineScope.launch {
                                        try {
                                            val result = geminiClient.chatWithTargetConsultant(
                                                apiKey = context.getApiKey(),
                                                historyList = consultantHistory.value,
                                                newMessage = msg,
                                                profile = userProfile,
                                                context = context,
                                                healthManager = healthManager
                                            )
                                            consultantMessages.add(ChatMessage(result.reply, false))
                                            consultantHistory.value = result.newHistory
                                            
                                            // Refresh local settings inputs
                                            stepsInput = context.getTargetSteps().toString()
                                            caloriesInput = context.getTargetCalories().toString()
                                            sodiumInput = context.getTargetSodium().toString()
                                            waterInput = context.getTargetWater().toString()
                                            weightInput = context.getTargetWeight().toString()
                                            proteinInput = context.getTargetProtein().toString()
                                            carbsInput = context.getTargetCarbs().toString()
                                            fatInput = context.getTargetFat().toString()
                                        } catch (e: Exception) {
                                            consultantMessages.add(ChatMessage("Error: ${e.localizedMessage}", false))
                                        } finally {
                                            isConsultantSending = false
                                        }
                                    }
                                }
                            },
                            enabled = consultantMessageInput.isNotBlank() && !isConsultantSending,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = AccentCyan,
                                contentColor = DarkBgEnd
                            )
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}
