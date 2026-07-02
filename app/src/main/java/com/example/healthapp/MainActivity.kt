package com.example.healthapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
            NavigationBar(
                containerColor = CardDark,
                tonalElevation = 8.dp
            ) {
                val items = listOf("Dashboard", "Analytics", "AI Coach", "Alerts", "Settings")
                val icons = listOf(
                    Icons.Default.Dashboard,
                    Icons.Default.Analytics,
                    Icons.AutoMirrored.Filled.Chat,
                    Icons.Default.Notifications,
                    Icons.Default.Settings
                )

                items.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label, color = if (selectedTab == index) AccentCyan else TextMuted) },
                        icon = {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = label
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentCyan,
                            unselectedIconColor = TextMuted,
                            selectedTextColor = AccentCyan,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color(0x1F22D3EE)
                        )
                    )
                }
            }
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
