package com.example.healthapp.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlin.random.Random

data class ActivityMetrics(
    val steps: Int,
    val activeCalories: Double,
    val exerciseMinutes: Int
)

data class NutritionMetrics(
    val calories: Double,
    val sodiumMg: Double,
    val carbsG: Double,
    val proteinG: Double,
    val fatG: Double,
    val waterMl: Double,
    val weightKg: Double
)

data class DailyTargets(
    val steps: Int,
    val calories: Int,
    val water: Int
)

data class UserProfile(
    val height: Float,
    val weight: Float,
    val gender: String,
    val birthDate: String,
    val nickname: String
)

data class HealthSummary(
    val date: LocalDate,
    val activity: ActivityMetrics,
    val nutrition: NutritionMetrics
)

class HealthManager(private val context: Context) {

    private val tag = "HealthManager"
    private var healthDataStore: HealthDataStore? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Failed(val message: String) : ConnectionState()
    }

    init {
        try {
            healthDataStore = HealthDataService.getStore(context)
            _connectionState.value = ConnectionState.Connected
            Log.d(tag, "Successfully initialized HealthDataStore")
        } catch (e: NoClassDefFoundError) {
            Log.e(tag, "Samsung Health Data SDK not found in classpath. Operating in Mock mode.", e)
            _connectionState.value = ConnectionState.Failed("SDK not found (Mock Mode)")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize HealthDataStore. Operating in Mock mode.", e)
            _connectionState.value = ConnectionState.Failed("Initialization failed (Mock Mode)")
        }
    }

    fun connect() {
        // No-op in new SDK: connection is established immediately upon initialization
    }

    fun checkAndRequestPermissions(activity: Activity, onResult: (Boolean) -> Unit) {
        val store = healthDataStore
        if (store == null || connectionState.value != ConnectionState.Connected) {
            onResult(true)
            return
        }

        try {
            val permissions = setOf(
                Permission.of(DataTypes.STEPS, AccessType.READ),
                Permission.of(DataTypes.NUTRITION, AccessType.READ),
                Permission.of(DataTypes.WATER_INTAKE, AccessType.READ),
                Permission.of(DataTypes.BODY_COMPOSITION, AccessType.READ),
                Permission.of(DataTypes.EXERCISE, AccessType.READ),
                Permission.of(DataTypes.STEPS_GOAL, AccessType.READ),
                Permission.of(DataTypes.NUTRITION_GOAL, AccessType.READ),
                Permission.of(DataTypes.WATER_INTAKE_GOAL, AccessType.READ),
                Permission.of(DataTypes.USER_PROFILE, AccessType.READ)
            )

            MainScope().launch {
                try {
                    val granted = store.requestPermissions(permissions, activity)
                    onResult(granted.containsAll(permissions))
                } catch (e: Exception) {
                    Log.e(tag, "Failed to request permissions", e)
                    onResult(false)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error setting up permissions", e)
            onResult(false)
        }
    }

    fun fetchTargets(): DailyTargets {
        val store = healthDataStore
        if (store != null && connectionState.value == ConnectionState.Connected) {
            try {
                return kotlinx.coroutines.runBlocking {
                    var steps = 10000
                    var calories = 2200
                    var water = 2000

                    val today = LocalDate.now()
                    val localDateFilter = LocalDateFilter.of(today, today)

                    // 1. Steps Goal
                    try {
                        val req = DataType.StepsGoalType.LAST.requestBuilder
                            .setLocalDateFilter(localDateFilter)
                            .build()
                        val resp = store.aggregateData(req)
                        steps = resp.dataList.firstOrNull()?.value?.toInt() ?: 10000
                    } catch (e: Exception) {
                        Log.e(tag, "Error querying steps goal", e)
                    }

                    // 2. Nutrition Goal (Calorie Target)
                    try {
                        val req = DataType.NutritionGoalType.LAST_CALORIES.requestBuilder
                            .setLocalDateFilter(localDateFilter)
                            .build()
                        val resp = store.aggregateData(req)
                        val calFloat = resp.dataList.firstOrNull()?.value
                        if (calFloat != null) {
                            calories = calFloat.toInt()
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error querying nutrition goal", e)
                    }

                    // 3. Water Intake Goal
                    try {
                        val req = DataType.WaterIntakeGoalType.LAST.requestBuilder
                            .setLocalDateFilter(localDateFilter)
                            .build()
                        val resp = store.aggregateData(req)
                        val watFloat = resp.dataList.firstOrNull()?.value
                        if (watFloat != null) {
                            water = watFloat.toInt()
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error querying water goal", e)
                    }

                    DailyTargets(steps, calories, water)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error fetching targets from SDK", e)
            }
        }
        return DailyTargets(10000, 2200, 2000)
    }

    fun fetchUserProfile(): UserProfile {
        val store = healthDataStore
        if (store != null && connectionState.value == ConnectionState.Connected) {
            try {
                return kotlinx.coroutines.runBlocking {
                    var height = 0.0f
                    var weight = 0.0f
                    var gender = "UNKNOWN"
                    var birthDate = ""
                    var nickname = ""

                    try {
                        val req = DataTypes.USER_PROFILE.readDataRequestBuilder.build()
                        val resp = store.readData(req)
                        val dp = resp.dataList.firstOrNull()
                        if (dp != null) {
                            height = dp.getValue(DataType.UserProfileDataType.HEIGHT) ?: 0.0f
                            weight = dp.getValue(DataType.UserProfileDataType.WEIGHT) ?: 0.0f
                            val g = dp.getValue(DataType.UserProfileDataType.GENDER)
                            gender = g?.name ?: "UNKNOWN"
                            birthDate = dp.getValue(DataType.UserProfileDataType.DATE_OF_BIRTH) ?: ""
                            nickname = dp.getValue(DataType.UserProfileDataType.NICKNAME) ?: ""
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error reading user profile", e)
                    }

                    UserProfile(height, weight, gender, birthDate, nickname)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error fetching user profile from SDK", e)
            }
        }
        return UserProfile(0.0f, 0.0f, "UNKNOWN", "", "")
    }

    /**
     * Fetch health summary for a specific date. Falls back to mock data if SDK is not connected.
     */
    fun fetchSummary(date: LocalDate): HealthSummary {
        val store = healthDataStore
        if (store != null && connectionState.value == ConnectionState.Connected) {
            try {
                return kotlinx.coroutines.runBlocking {
                    val startOfDay = date.atStartOfDay()
                    val endOfDay = date.atTime(23, 59, 59, 999999999)
                    val localTimeFilter = LocalTimeFilter.of(startOfDay, endOfDay)

                    // 1. Steps
                    var steps = 0
                    try {
                        val req = DataType.StepsType.TOTAL.requestBuilder
                            .setLocalTimeFilter(localTimeFilter)
                            .build()
                        val resp = store.aggregateData(req)
                        steps = resp.dataList.firstOrNull()?.value?.toInt() ?: 0
                    } catch (e: Exception) {
                        Log.e(tag, "Error querying steps", e)
                    }

                    // 2. Active Calories
                    var activeCalories = 0.0
                    try {
                        val req = DataType.ExerciseType.TOTAL_CALORIES.requestBuilder
                            .setLocalTimeFilter(localTimeFilter)
                            .build()
                        val resp = store.aggregateData(req)
                        activeCalories = resp.dataList.firstOrNull()?.value?.toDouble() ?: 0.0
                    } catch (e: Exception) {
                        Log.e(tag, "Error querying active calories", e)
                    }

                    // 3. Exercise Minutes
                    var exerciseMinutes = 0
                    try {
                        val req = DataType.ExerciseType.TOTAL_DURATION.requestBuilder
                            .setLocalDateFilter(LocalDateFilter.of(date, date))
                            .build()
                        val resp = store.aggregateData(req)
                        exerciseMinutes = resp.dataList.firstOrNull()?.value?.toMinutes()?.toInt() ?: 0
                    } catch (e: Exception) {
                        Log.e(tag, "Error querying exercise duration", e)
                    }

                    // 4. Nutrition (calories, sodium, carbs, protein, fat)
                    var calories = 0.0
                    var sodiumMg = 0.0
                    var carbsG = 0.0
                    var proteinG = 0.0
                    var fatG = 0.0
                    try {
                        val req = DataTypes.NUTRITION.readDataRequestBuilder
                            .setLocalTimeFilter(localTimeFilter)
                            .build()
                        val resp = store.readData(req)
                        for (dp in resp.dataList) {
                            calories += (dp.getValue(DataType.NutritionType.CALORIES) ?: 0f).toDouble()
                            sodiumMg += (dp.getValue(DataType.NutritionType.SODIUM) ?: 0f).toDouble()
                            carbsG += (dp.getValue(DataType.NutritionType.CARBOHYDRATE) ?: 0f).toDouble()
                            proteinG += (dp.getValue(DataType.NutritionType.PROTEIN) ?: 0f).toDouble()
                            fatG += (dp.getValue(DataType.NutritionType.TOTAL_FAT) ?: 0f).toDouble()
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error querying nutrition", e)
                    }

                    // 5. Water intake
                    var waterMl = 0.0
                    try {
                        val req = DataType.WaterIntakeType.TOTAL.requestBuilder
                            .setLocalTimeFilter(localTimeFilter)
                            .build()
                        val resp = store.aggregateData(req)
                        waterMl = resp.dataList.firstOrNull()?.value?.toDouble() ?: 0.0
                    } catch (e: Exception) {
                        Log.e(tag, "Error querying water intake", e)
                    }

                    // 6. Weight
                    var weightKg = 0.0
                    try {
                        val req = DataTypes.BODY_COMPOSITION.readDataRequestBuilder
                            .setLocalTimeFilter(localTimeFilter)
                            .setLimit(1)
                            .setOrdering(com.samsung.android.sdk.health.data.request.Ordering.DESC)
                            .build()
                        val resp = store.readData(req)
                        val w = resp.dataList.firstOrNull()?.getValue(DataType.BodyCompositionType.WEIGHT)
                        if (w != null) {
                            weightKg = w.toDouble()
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error querying weight", e)
                    }

                    if (steps > 0 || calories > 0 || waterMl > 0 || weightKg > 0) {
                        HealthSummary(
                            date = date,
                            activity = ActivityMetrics(steps, activeCalories, exerciseMinutes),
                            nutrition = NutritionMetrics(
                                calories = calories,
                                sodiumMg = sodiumMg,
                                carbsG = carbsG,
                                proteinG = proteinG,
                                fatG = fatG,
                                waterMl = waterMl,
                                weightKg = if (weightKg > 0) weightKg else 78.5
                            )
                        )
                    } else {
                        generateMockDataForDate(date)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error reading from Samsung Health, falling back to mock data", e)
            }
        }
        return generateMockDataForDate(date)
    }

    /**
     * Fetch historical records for the last N days.
     */
    fun fetchHistory(days: Int): List<HealthSummary> {
        val yesterday = LocalDate.now().minusDays(1)
        return (0 until days).map { i ->
            fetchSummary(yesterday.minusDays(i.toLong()))
        }.reversed()
    }

    private fun generateMockDataForDate(date: LocalDate): HealthSummary {
        // Use date's epoch day as a seed to ensure consistent data per date
        val seed = date.toEpochDay()
        val random = Random(seed)

        val targetWeight = 78.5
        val weight = targetWeight + random.nextDouble(-1.2, 1.2)

        // Seed higher sodium values on recent days to test the alert triggers
        val sodiumBase = if (date.dayOfMonth % 3 == 0) 2400.0 else 1800.0
        val sodium = sodiumBase + random.nextDouble(-400.0, 500.0)

        val steps = random.nextInt(4000, 13000)
        val activeCalories = steps * 0.04
        val exerciseMinutes = random.nextInt(15, 75)

        val calories = random.nextDouble(1800.0, 2700.0)
        val carbs = calories * 0.5 / 4.0
        val protein = calories * 0.25 / 4.0
        val fat = calories * 0.25 / 9.0

        val water = random.nextDouble(1200.0, 3000.0)

        return HealthSummary(
            date = date,
            activity = ActivityMetrics(steps, activeCalories, exerciseMinutes),
            nutrition = NutritionMetrics(
                calories = calories,
                sodiumMg = sodium,
                carbsG = carbs,
                proteinG = protein,
                fatG = fat,
                waterMl = water,
                weightKg = weight
            )
        )
    }
}
