package com.example.healthapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.healthapp.ai.AlertWorker
import com.example.healthapp.ai.GeminiClient
import com.example.healthapp.data.HealthManager
import com.example.healthapp.ui.AnalyticsScreen
import com.example.healthapp.ui.DashboardScreen
import com.example.healthapp.ui.GeminiChatScreen
import com.example.healthapp.ui.RemindersScreen
import com.example.healthapp.ui.SettingsScreen
import java.util.concurrent.TimeUnit

// Sleek Premium Dark Color Palette
val DarkBgStart = Color(0xFF0F172A) // Deep Slate Blue
val DarkBgEnd = Color(0xFF020617)   // Rich Dark Blue
val AccentCyan = Color(0xFF22D3EE)  // Vibrant Cyan
val AccentViolet = Color(0xFFA78BFA) // Pastel Purple
val CardDark = Color(0xFF1E293B)     // Dark Card slate
val TextLight = Color(0xFFF8FAFC)    // Almost White
val TextMuted = Color(0xFF94A3B8)    // Soft Gray

class MainActivity : ComponentActivity() {

    private lateinit var healthManager: HealthManager
    private lateinit var geminiClient: GeminiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthManager = HealthManager(this)
        healthManager.connect()
        geminiClient = GeminiClient(this)

        // Schedule WorkManager daily morning alerts check
        scheduleDailyAlertChecks()

        setContent {
            AppTheme {
                MainLayout(healthManager, geminiClient)
            }
        }
    }

    private fun scheduleDailyAlertChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<AlertWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyHealthAlerts",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBgStart, DarkBgEnd)
                )
            )
    ) {
        content()
    }
}

@Composable
fun MainLayout(healthManager: HealthManager, geminiClient: GeminiClient) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            TerminalNavBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(healthManager, geminiClient)
                1 -> AnalyticsScreen(healthManager)
                2 -> GeminiChatScreen(healthManager, geminiClient)
                3 -> RemindersScreen()
                4 -> SettingsScreen(healthManager, geminiClient)
            }
        }
    }
}

@Composable
fun TerminalNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf("DASH", "ANLYT", "COACH", "ALRTS", "CONF")
    val labels = listOf("Dashboard", "Analytics", "AI Coach", "Alerts", "Settings")
    val icons = listOf(
        Icons.Default.Dashboard,
        Icons.Default.Analytics,
        Icons.AutoMirrored.Filled.Chat,
        Icons.Default.Notifications,
        Icons.Default.Settings
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF020617))
            .border(width = 1.dp, color = Color(0xFF1E293B), shape = androidx.compose.ui.graphics.RectangleShape)
    ) {
        // Top separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF1E293B))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { index, shortLabel ->
                val isSelected = selectedTab == index

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = labels[index],
                        tint = if (isSelected) AccentCyan else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isSelected) "[$shortLabel]" else shortLabel,
                        color = if (isSelected) AccentCyan else TextMuted,
                        fontSize = 8.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    // Active indicator dot
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 1.dp)
                            .background(
                                if (isSelected) AccentCyan else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}
