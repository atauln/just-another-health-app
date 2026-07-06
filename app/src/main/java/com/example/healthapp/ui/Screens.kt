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
import androidx.compose.ui.graphics.nativeCanvas
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

fun Context.getTargetSugars(): Int = getPrefs().getInt("target_sugars", 50)
fun Context.saveTargetSugars(s: Int) = getPrefs().edit().putInt("target_sugars", s).apply()

fun Context.getTargetFiber(): Int = getPrefs().getInt("target_fiber", 30)
fun Context.saveTargetFiber(f: Int) = getPrefs().edit().putInt("target_fiber", f).apply()

fun Context.getTargetSaturatedFat(): Int = getPrefs().getInt("target_saturated_fat", 20)
fun Context.saveTargetSaturatedFat(sf: Int) = getPrefs().edit().putInt("target_saturated_fat", sf).apply()

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

fun Context.getShowSugars(): Boolean = getPrefs().getBoolean("show_metric_sugars", true)
fun Context.saveShowSugars(b: Boolean) = getPrefs().edit().putBoolean("show_metric_sugars", b).apply()

fun Context.getShowFiber(): Boolean = getPrefs().getBoolean("show_metric_fiber", true)
fun Context.saveShowFiber(b: Boolean) = getPrefs().edit().putBoolean("show_metric_fiber", b).apply()

fun Context.getShowSaturatedFat(): Boolean = getPrefs().getBoolean("show_metric_saturated_fat", true)
fun Context.saveShowSaturatedFat(b: Boolean) = getPrefs().edit().putBoolean("show_metric_saturated_fat", b).apply()


fun shareChart(context: Context, history: List<HealthSummary>, metric: String) {
    val cleanMetric = metric.uppercase().replace("(", "").replace(")", "")
    val targetValue = when (cleanMetric) {
        "STEPS" -> context.getTargetSteps().toFloat()
        "SODIUM" -> context.getTargetSodium().toFloat()
        "WATER" -> context.getTargetWater().toFloat()
        "PROTEIN" -> context.getTargetProtein().toFloat()
        "CARBS" -> context.getTargetCarbs().toFloat()
        "FAT" -> context.getTargetFat().toFloat()
        "SUGARS" -> context.getTargetSugars().toFloat()
        "FIBER" -> context.getTargetFiber().toFloat()
        "SATURATED_FAT", "SATFAT", "SAT" -> context.getTargetSaturatedFat().toFloat()
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
    canvas.drawText("$cleanMetric Chart - Last ${history.size} Days", 40f, 60f, titlePaint)
    
    val values = history.map { summary ->
        when (cleanMetric) {
            "STEPS" -> summary.activity.steps.toFloat()
            "SODIUM" -> summary.nutrition.sodiumMg.toFloat()
            "WATER" -> summary.nutrition.waterMl.toFloat()
            "PROTEIN" -> summary.nutrition.proteinG.toFloat()
            "CARBS" -> summary.nutrition.carbsG.toFloat()
            "FAT" -> summary.nutrition.fatG.toFloat()
            "SUGARS" -> summary.nutrition.sugarsG.toFloat()
            "FIBER" -> summary.nutrition.fiberG.toFloat()
            "SATURATED_FAT", "SATFAT", "SAT" -> summary.nutrition.saturatedFatG.toFloat()
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
        
        val isLimit = cleanMetric == "SODIUM" || cleanMetric == "CALORIES" || cleanMetric == "SUGARS" || cleanMetric == "SATURATED_FAT" || cleanMetric == "SATFAT" || cleanMetric == "SAT"
        val isExceeded = if (isLimit) value > targetValue else value < targetValue
        val hexColor = if (isLimit) {
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
    var weeklyHistory by remember { mutableStateOf<List<HealthSummary>>(emptyList()) }

    LaunchedEffect(healthManager) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            today = healthManager.fetchSummary(LocalDate.now().minusDays(1)) // Yesterday (Audit Target)
            yesterday = healthManager.fetchSummary(LocalDate.now().minusDays(2)) // Previous Day (Comparative)
            weeklyHistory = healthManager.fetchHistory(7)
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
    val showSugars = remember { context.getShowSugars() }
    val showFiber = remember { context.getShowFiber() }
    val showSaturatedFat = remember { context.getShowSaturatedFat() }

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
    if (showSugars) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Sugar Intake",
                todayValue = String.format("%.0f", todayData.nutrition.sugarsG),
                yesterdayValue = String.format("%.0f", yesterdayData.nutrition.sugarsG),
                unit = "g",
                color = AccentCyan,
                modifier = modifier
            )
        })
    }
    if (showFiber) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Fiber Intake",
                todayValue = String.format("%.0f", todayData.nutrition.fiberG),
                yesterdayValue = String.format("%.0f", yesterdayData.nutrition.fiberG),
                unit = "g",
                color = AccentViolet,
                modifier = modifier
            )
        })
    }
    if (showSaturatedFat) {
        enabledCards.add(@Composable { modifier ->
            MetricComparisonCard(
                title = "Sat Fat Intake",
                todayValue = String.format("%.0f", todayData.nutrition.saturatedFatG),
                yesterdayValue = String.format("%.0f", yesterdayData.nutrition.saturatedFatG),
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
        // System Focus Bulletin
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "HISTORICAL AUDIT ACTIVE: DAILY PORTFOLIO PORTRAYS COMPT LOGS FROM YESTERDAY COMPARED TO PREVIOUS DAY. LIVE INCOMPLETE FEED FILTERED.",
                    color = Color(0xFFF87171),
                    fontSize = 8.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

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
                val tickerItems = ArrayList<Pair<String, String>>()
                if (showCalories) {
                    tickerItems.add("CAL" to "${todayData.nutrition.calories.toInt()}/${context.getTargetCalories()} kcal")
                }
                if (showActiveCals) {
                    tickerItems.add("ACT" to "${todayData.activity.activeCalories.toInt()} kcal")
                }
                if (showProtein) {
                    tickerItems.add("PRO" to "${todayData.nutrition.proteinG.toInt()}/${context.getTargetProtein()}g")
                }
                if (showCarbs) {
                    tickerItems.add("CARB" to "${todayData.nutrition.carbsG.toInt()}/${context.getTargetCarbs()}g")
                }
                if (showFat) {
                    tickerItems.add("FAT" to "${todayData.nutrition.fatG.toInt()}/${context.getTargetFat()}g")
                }
                if (showSugars) {
                    tickerItems.add("SUG" to "${todayData.nutrition.sugarsG.toInt()}/${context.getTargetSugars()}g")
                }
                if (showFiber) {
                    tickerItems.add("FIB" to "${todayData.nutrition.fiberG.toInt()}/${context.getTargetFiber()}g")
                }
                if (showSaturatedFat) {
                    tickerItems.add("SFAT" to "${todayData.nutrition.saturatedFatG.toInt()}/${context.getTargetSaturatedFat()}g")
                }
                if (showWeight) {
                    tickerItems.add("WGT" to "${todayData.nutrition.weightKg}kg")
                }

                tickerItems.forEach { (label, valStr) ->
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
                text = "PORTFOLIO INSIGHTS (YESTERDAY)",
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
                            if (!isGenerating && weeklyHistory.isNotEmpty()) {
                                isGenerating = true
                                coroutineScope.launch {
                                    geminiClient.generateWeeklyInsights(
                                        apiKey = context.getApiKey(),
                                        history = weeklyHistory
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
                text = "YST: $todayValue $unit".uppercase(),
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Text(
                text = "PRV: $yesterdayValue $unit".uppercase(),
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
    
    val showSugars = remember { context.getShowSugars() }
    val showFiber = remember { context.getShowFiber() }
    val showSaturatedFat = remember { context.getShowSaturatedFat() }
    val showSodium = remember { context.getShowSodium() }
    val showWeight = remember { context.getShowWeight() }

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
                            
                            val paddingLeft = 35.dp.toPx()
                            val chartWidth = w - paddingLeft
                            val barWidth = (chartWidth - (spacing * (barCount - 1))) / (barCount * 3)

                            for (i in 0 until barCount) {
                                val xGroup = paddingLeft + i * (barWidth * 3 + spacing)
                                
                                // Protein bar (Cyan)
                                val py = h - (proteinValues[i] / maxVal) * (h * 0.85f)
                                drawRect(
                                    color = AccentCyan,
                                    topLeft = Offset(xGroup, py),
                                    size = Size(barWidth, h - py)
                                )

                                // Carbs bar (Violet)
                                val cy = h - (carbsValues[i] / maxVal) * (h * 0.85f)
                                drawRect(
                                    color = AccentViolet,
                                    topLeft = Offset(xGroup + barWidth, cy),
                                    size = Size(barWidth, h - cy)
                                )

                                // Fat bar (Orange)
                                val fy = h - (fatValues[i] / maxVal) * (h * 0.85f)
                                drawRect(
                                    color = Color(0xFFF59E0B),
                                    topLeft = Offset(xGroup + barWidth * 2, fy),
                                    size = Size(barWidth, h - fy)
                                )
                            }

                            // ── Draw Vertical Y-Axis Line ─────────────────────────
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(paddingLeft, 0f),
                                end = Offset(paddingLeft, h),
                                strokeWidth = 1.dp.toPx()
                            )

                            // ── Draw Horizontal Gridlines & Monospace Labels ──────
                            val gridLines = listOf(
                                h to 0.0,
                                (h - 0.5f * (h * 0.85f)) to (maxVal * 0.5),
                                (h * 0.15f) to maxVal.toDouble()
                            )

                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#94A3B8") // TextMuted
                                textSize = 7.sp.toPx()
                                typeface = android.graphics.Typeface.MONOSPACE
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }

                            gridLines.forEach { (y, value) ->
                                drawLine(
                                    color = Color(0xFF334155).copy(alpha = 0.4f),
                                    start = Offset(paddingLeft, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                )
                                
                                val label = if (value >= 1000) String.format("%.1fk", value / 1000.0) else String.format("%.0f", value)
                                drawContext.canvas.nativeCanvas.drawText(
                                    label,
                                    paddingLeft - 6.dp.toPx(),
                                    y + 2.5f.dp.toPx(),
                                    paint
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom Axis dates Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 35.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val step = if (historyData.size > 14) 5 else if (historyData.size > 7) 2 else 1
                        historyData.forEachIndexed { index, summary ->
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

                    Spacer(modifier = Modifier.height(8.dp))

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

        // Segment 5: Nutrient Quality (Sugars / Fiber / Saturated Fat)
        if (showSugars || showFiber || showSaturatedFat) {
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
                            text = "CH-05: NUTRIENT QUALITY LEDGER (G)",
                            color = AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val sugarsValues = historyData.map { it.nutrition.sugarsG.toFloat() }
                        val fiberValues = historyData.map { it.nutrition.fiberG.toFloat() }
                        val satFatValues = historyData.map { it.nutrition.saturatedFatG.toFloat() }
                        val maxVal = (sugarsValues + fiberValues + satFatValues).maxOrNull()?.coerceAtLeast(1f) ?: 1f

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
                                
                                val paddingLeft = 35.dp.toPx()
                                val chartWidth = w - paddingLeft
                                val barWidth = (chartWidth - (spacing * (barCount - 1))) / (barCount * 3)

                                for (i in 0 until barCount) {
                                    val xGroup = paddingLeft + i * (barWidth * 3 + spacing)
                                    
                                    // Sugars bar (Cyan)
                                    val sy = h - (sugarsValues[i] / maxVal) * (h * 0.85f)
                                    drawRect(
                                        color = AccentCyan,
                                        topLeft = Offset(xGroup, sy),
                                        size = Size(barWidth, h - sy)
                                    )

                                    // Fiber bar (Violet)
                                    val fy = h - (fiberValues[i] / maxVal) * (h * 0.85f)
                                    drawRect(
                                        color = AccentViolet,
                                        topLeft = Offset(xGroup + barWidth, fy),
                                        size = Size(barWidth, h - fy)
                                    )

                                    // Saturated Fat bar (Orange)
                                    val sfy = h - (satFatValues[i] / maxVal) * (h * 0.85f)
                                    drawRect(
                                        color = Color(0xFFF59E0B),
                                        topLeft = Offset(xGroup + barWidth * 2, sfy),
                                        size = Size(barWidth, h - sfy)
                                    )
                                }

                                // ── Draw Vertical Y-Axis Line ─────────────────────
                                drawLine(
                                    color = Color(0xFF1E293B),
                                    start = Offset(paddingLeft, 0f),
                                    end = Offset(paddingLeft, h),
                                    strokeWidth = 1.dp.toPx()
                                )

                                // ── Draw Horizontal Gridlines & Monospace Labels ──
                                val gridLines = listOf(
                                    h to 0.0,
                                    (h - 0.5f * (h * 0.85f)) to (maxVal * 0.5),
                                    (h * 0.15f) to maxVal.toDouble()
                                )

                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#94A3B8") // TextMuted
                                    textSize = 7.sp.toPx()
                                    typeface = android.graphics.Typeface.MONOSPACE
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }

                                gridLines.forEach { (y, value) ->
                                    drawLine(
                                        color = Color(0xFF334155).copy(alpha = 0.4f),
                                        start = Offset(paddingLeft, y),
                                        end = Offset(w, y),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                    )
                                    
                                    val label = if (value >= 1000) String.format("%.1fk", value / 1000.0) else String.format("%.0f", value)
                                    drawContext.canvas.nativeCanvas.drawText(
                                        label,
                                        paddingLeft - 6.dp.toPx(),
                                        y + 2.5f.dp.toPx(),
                                        paint
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom Axis dates Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 35.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val step = if (historyData.size > 14) 5 else if (historyData.size > 7) 2 else 1
                            historyData.forEachIndexed { index, summary ->
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (showSugars) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(AccentCyan))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SUG (MAX: ${context.getTargetSugars()}g)", color = TextMuted, fontSize = 8.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                            if (showFiber) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(AccentViolet))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("FIB (MIN: ${context.getTargetFiber()}g)", color = TextMuted, fontSize = 8.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                            if (showSaturatedFat) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFF59E0B)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SFAT (MAX: ${context.getTargetSaturatedFat()}g)", color = TextMuted, fontSize = 8.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Segment 6: Sodium Discovery (Sodium)
        if (showSodium) {
            item {
                BrokerageChartCard(
                    title = "CH-06: SODIUM DISCOVERY (SODIUM)",
                    history = historyData,
                    targetValue = context.getTargetSodium().toFloat(),
                    unit = "mg",
                    colorStart = Color(0xFFEF4444),
                    metricSelector = { it.nutrition.sodiumMg.toFloat() }
                )
            }
        }

        // Segment 7: Body Composition Ledger (Weight)
        if (showWeight) {
            item {
                BrokerageChartCard(
                    title = "CH-07: BODY COMPOSITION LEDGER (WEIGHT)",
                    history = historyData,
                    targetValue = context.getTargetWeight(),
                    unit = "kg",
                    colorStart = Color(0xFF10B981),
                    metricSelector = { it.nutrition.weightKg.toFloat() }
                )
            }
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
                    
                    val paddingLeft = 35.dp.toPx()
                    val chartWidth = w - paddingLeft
                    val barWidth = (chartWidth - (spacing * (barCount - 1))) / barCount

                    val points = mutableListOf<Offset>()

                    for (i in 0 until barCount) {
                        val value = values[i]
                        val barHeight = (value / maxVal) * (h * 0.85f)
                        val x = paddingLeft + i * (barWidth + spacing)
                        val y = h - barHeight

                        drawRect(
                            color = colorStart.copy(alpha = 0.2f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )

                        points.add(Offset(x + barWidth / 2, y))
                    }

                    // ── Draw Vertical Y-Axis Line ─────────────────────────────
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(paddingLeft, 0f),
                        end = Offset(paddingLeft, h),
                        strokeWidth = 1.dp.toPx()
                    )

                    // ── Draw Horizontal Gridlines & Monospace Labels ──────────
                    val gridLines = listOf(
                        h to 0.0,
                        (h - (targetValue / maxVal) * (h * 0.85f)) to targetValue.toDouble(),
                        (h * 0.15f) to maxVal.toDouble()
                    )

                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8") // TextMuted
                        textSize = 7.sp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }

                    gridLines.forEach { (y, value) ->
                        drawLine(
                            color = if (value == targetValue.toDouble()) Color(0xFFEF4444).copy(alpha = 0.6f) else Color(0xFF334155).copy(alpha = 0.4f),
                            start = Offset(paddingLeft, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )
                        
                        val label = if (value >= 1000) String.format("%.1fk", value / 1000.0) else String.format("%.0f", value)
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            paddingLeft - 6.dp.toPx(),
                            y + 2.5f.dp.toPx(),
                            paint
                        )
                    }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 35.dp),
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
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[AI ANALYST CHAT - TERMINAL CORE]",
                color = TextLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("SECURE-LINK", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat History List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatMessages) { msg ->
                    ChatBubble(msg)
                }
            }
        }

        // Selected Attachment Preview Card
        AnimatedVisibility(visible = selectedAttachmentUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = selectedAttachmentName ?: "File attached",
                                color = TextLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                              )
                            Text(
                                text = selectedAttachmentMimeType ?: "application/octet-stream",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            selectedAttachmentUri = null
                            selectedAttachmentMimeType = null
                            selectedAttachmentName = null
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Message Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = { filePickerLauncher.launch("*/*") },
                colors = IconButtonDefaults.iconButtonColors(contentColor = AccentViolet),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach Document", modifier = Modifier.size(16.dp))
            }

            TextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text("ASK CORE TERMINAL...", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF020617),
                    unfocusedContainerColor = Color(0xFF020617),
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    cursorColor = AccentCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(4.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp)),
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
                ),
                modifier = Modifier
                    .size(36.dp)
                    .background(AccentCyan, RoundedCornerShape(4.dp))
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                .border(1.dp, if (msg.isUser) AccentCyan else Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Text(
                text = msg.text,
                color = TextLight,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontFamily = FontFamily.Monospace
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
            .padding(10.dp)
    ) {
        Text(
            text = "[ALERTS CONTROL CENTRE - ACTIVE POLICIES]",
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Gemini queries metrics history daily to flag anomalies or breaches.",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Add Alert Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newAlertText,
                onValueChange = { newAlertText = it },
                placeholder = { Text("ADD ANOMALY POLICY CHECK...", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF020617),
                    unfocusedContainerColor = Color(0xFF020617),
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(4.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
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
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .size(44.dp)
                    .background(AccentCyan, RoundedCornerShape(4.dp))
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alert", tint = DarkBgEnd, modifier = Modifier.size(16.dp))
            }
        }

        // Active Alerts List Header
        Text(
            text = "[REGISTERED MONITORING POLICIES]",
            color = AccentCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(alertsList) { alert ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = alert.uppercase(),
                            color = TextLight,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "[DISCONNECT]",
                            color = Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable {
                                    val updated = alertsList - alert
                                    alertsList = updated
                                    context.saveAlerts(updated)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Trigger Alert Evaluation Now button
        Button(
            onClick = {
                isChecking = true
                val request = OneTimeWorkRequestBuilder<AlertWorker>().build()
                WorkManager.getInstance(context).enqueue(request)
                Toast.makeText(context, "Alert Worker triggered successfully!", Toast.LENGTH_SHORT).show()
                isChecking = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            Text("RUN INSTANT SYSTEM AUDIT", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
    var sugarsInput by remember { mutableStateOf(context.getTargetSugars().toString()) }
    var fiberInput by remember { mutableStateOf(context.getTargetFiber().toString()) }
    var saturatedFatInput by remember { mutableStateOf(context.getTargetSaturatedFat().toString()) }

    var showSteps by remember { mutableStateOf(context.getShowSteps()) }
    var showCalories by remember { mutableStateOf(context.getShowCalories()) }
    var showSodium by remember { mutableStateOf(context.getShowSodium()) }
    var showWater by remember { mutableStateOf(context.getShowWater()) }
    var showWeight by remember { mutableStateOf(context.getShowWeight()) }
    var showActiveCalories by remember { mutableStateOf(context.getShowActiveCalories()) }
    var showProtein by remember { mutableStateOf(context.getShowProtein()) }
    var showCarbs by remember { mutableStateOf(context.getShowCarbs()) }
    var showFat by remember { mutableStateOf(context.getShowFat()) }
    var showSugars by remember { mutableStateOf(context.getShowSugars()) }
    var showFiber by remember { mutableStateOf(context.getShowFiber()) }
    var showSaturatedFat by remember { mutableStateOf(context.getShowSaturatedFat()) }

    var showTargetConsultant by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF020617),
        unfocusedContainerColor = Color(0xFF020617),
        focusedTextColor = TextLight,
        unfocusedTextColor = TextLight,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = AccentCyan
    )
    val fieldBorder = Modifier
        .fillMaxWidth()
        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
    val fieldTextStyle = androidx.compose.ui.text.TextStyle(
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "[SYSTEM CONFIGURATION PANEL]",
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // ── API Gateway ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("[API GATEWAY — GEMINI]", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Required for AI analysis, portfolio insights and alert evaluation.", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                TextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it; context.saveApiKey(it) },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("ENTER GEMINI API KEY...", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = fieldColors,
                    shape = RoundedCornerShape(4.dp),
                    textStyle = fieldTextStyle,
                    modifier = fieldBorder
                )
            }
        }

        // ── Samsung Health SDK Status ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("[SAMSUNG HEALTH SDK — CONNECTION STATUS]", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                val stateLabel: String
                val stateColor: Color
                when (sdkState) {
                    is HealthManager.ConnectionState.Connected -> { stateLabel = "CONNECTED"; stateColor = Color(0xFF10B981) }
                    is HealthManager.ConnectionState.Connecting -> { stateLabel = "CONNECTING..."; stateColor = Color(0xFFF59E0B) }
                    is HealthManager.ConnectionState.Disconnected -> { stateLabel = "DISCONNECTED"; stateColor = TextMuted }
                    is HealthManager.ConnectionState.Failed -> { stateLabel = "FAILED (MOCK FALLBACK ACTIVE)"; stateColor = Color(0xFFEF4444) }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(stateColor, RoundedCornerShape(4.dp)))
                    Text(stateLabel, color = stateColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                if (sdkState is HealthManager.ConnectionState.Failed) {
                    Text(
                        text = (sdkState as HealthManager.ConnectionState.Failed).message,
                        color = Color(0xFFEF4444),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

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
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(34.dp)
                ) {
                    Text("SYNC PERMISSIONS", color = DarkBgEnd, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // ── Metric Channel Visibility ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("[METRIC CHANNEL VISIBILITY]", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Toggle which metrics are monitored and rendered across all tabs.", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(2.dp))

                listOf(
                    Triple(showSteps, "Daily Steps") { b: Boolean -> showSteps = b; context.saveShowSteps(b) },
                    Triple(showCalories, "Calorie Intake (Food)") { b: Boolean -> showCalories = b; context.saveShowCalories(b) },
                    Triple(showSodium, "Sodium Level") { b: Boolean -> showSodium = b; context.saveShowSodium(b) },
                    Triple(showWater, "Water Intake") { b: Boolean -> showWater = b; context.saveShowWater(b) },
                    Triple(showWeight, "Body Weight") { b: Boolean -> showWeight = b; context.saveShowWeight(b) },
                    Triple(showActiveCalories, "Exercise Calories Burned") { b: Boolean -> showActiveCalories = b; context.saveShowActiveCalories(b) },
                    Triple(showProtein, "Daily Protein") { b: Boolean -> showProtein = b; context.saveShowProtein(b) },
                    Triple(showCarbs, "Daily Carbohydrates") { b: Boolean -> showCarbs = b; context.saveShowCarbs(b) },
                    Triple(showFat, "Daily Fat") { b: Boolean -> showFat = b; context.saveShowFat(b) },
                    Triple(showSugars, "Daily Sugars") { b: Boolean -> showSugars = b; context.saveShowSugars(b) },
                    Triple(showFiber, "Dietary Fiber") { b: Boolean -> showFiber = b; context.saveShowFiber(b) },
                    Triple(showSaturatedFat, "Saturated Fat") { b: Boolean -> showSaturatedFat = b; context.saveShowSaturatedFat(b) }
                ).forEach { (isChecked, label, onCheckChanged) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCheckChanged(!isChecked) }
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onCheckChanged(it) },
                            colors = CheckboxDefaults.colors(checkedColor = AccentCyan, uncheckedColor = TextMuted),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label.uppercase(),
                            color = if (isChecked) TextLight else TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // ── Daily Threshold Parameters ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("[DAILY THRESHOLD PARAMETERS]", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Configure budgets and target levels for all tracked metrics.", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)

                data class TargetField(val label: String, val value: String, val onChange: (String) -> Unit)
                val fields = listOf(
                    TargetField("STEPS GOAL", stepsInput) { v -> stepsInput = v; v.toIntOrNull()?.let { context.saveTargetSteps(it) } },
                    TargetField("CALORIE TARGET (kcal)", caloriesInput) { v -> caloriesInput = v; v.toIntOrNull()?.let { context.saveTargetCalories(it) } },
                    TargetField("SODIUM LIMIT (mg)", sodiumInput) { v -> sodiumInput = v; v.toIntOrNull()?.let { context.saveTargetSodium(it) } },
                    TargetField("WATER GOAL (ml)", waterInput) { v -> waterInput = v; v.toIntOrNull()?.let { context.saveTargetWater(it) } },
                    TargetField("PROTEIN GOAL (g)", proteinInput) { v -> proteinInput = v; v.toIntOrNull()?.let { context.saveTargetProtein(it) } },
                    TargetField("CARBS GOAL (g)", carbsInput) { v -> carbsInput = v; v.toIntOrNull()?.let { context.saveTargetCarbs(it) } },
                    TargetField("FAT GOAL (g)", fatInput) { v -> fatInput = v; v.toIntOrNull()?.let { context.saveTargetFat(it) } },
                    TargetField("SUGARS LIMIT (g)", sugarsInput) { v -> sugarsInput = v; v.toIntOrNull()?.let { context.saveTargetSugars(it) } },
                    TargetField("FIBER GOAL (g)", fiberInput) { v -> fiberInput = v; v.toIntOrNull()?.let { context.saveTargetFiber(it) } },
                    TargetField("SAT FAT LIMIT (g)", saturatedFatInput) { v -> saturatedFatInput = v; v.toIntOrNull()?.let { context.saveTargetSaturatedFat(it) } },
                    TargetField("TARGET WEIGHT (kg)", weightInput) { v -> weightInput = v; v.toFloatOrNull()?.let { context.saveTargetWeight(it) } }
                )

                fields.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { f ->
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(f.label, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                TextField(
                                    value = f.value,
                                    onValueChange = f.onChange,
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(4.dp),
                                    textStyle = fieldTextStyle,
                                    modifier = fieldBorder,
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("IMPORT FROM SDK", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = { showTargetConsultant = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("AI TARGET CONSULT", color = DarkBgEnd, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // ── Developer Notes ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("[DEVELOPER NOTES — SDK SETUP]", color = AccentViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                listOf(
                    "1. Open the Samsung Health app on your phone.",
                    "2. Go to Settings > About Samsung Health.",
                    "3. Tap the version number 10+ times to unlock developer menu.",
                    "4. Enable 'Developer mode (Samsung Health Data SDK)'."
                ).forEach { line ->
                    Text(line, color = TextLight, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
    if (showTargetConsultant) {
        Dialog(onDismissRequest = { showTargetConsultant = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .background(Color(0xFF020617), RoundedCornerShape(4.dp))
                    .border(1.dp, AccentCyan, RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[AI TARGET CONSULTANT — THRESHOLD CALIBRATION]",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showTargetConsultant = false }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Dialog", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = "Describe your goals. AI will set steps, calories, macros, sodium, and weight targets automatically.",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
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
                            .padding(bottom = 6.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextField(
                            value = consultantMessageInput,
                            onValueChange = { consultantMessageInput = it },
                            placeholder = { Text("DESCRIBE YOUR GOALS...", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0D1B2A),
                                unfocusedContainerColor = Color(0xFF0D1B2A),
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                cursorColor = AccentCyan,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(4.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
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
                                            sugarsInput = context.getTargetSugars().toString()
                                            fiberInput = context.getTargetFiber().toString()
                                            saturatedFatInput = context.getTargetSaturatedFat().toString()
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
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentCyan, RoundedCornerShape(4.dp))
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
