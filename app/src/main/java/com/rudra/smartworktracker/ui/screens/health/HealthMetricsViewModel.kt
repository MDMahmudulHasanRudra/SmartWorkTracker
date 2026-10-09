package com.rudra.smartworktracker.ui.screens.health

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.HealthRepository
import com.rudra.smartworktracker.model.HealthMetric
import com.rudra.smartworktracker.model.HealthMetricType
import com.rudra.smartworktracker.model.MoodType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import kotlin.math.*

class HealthMetricsViewModel(application: Application) : AndroidViewModel(application) {

    private val healthRepository = HealthRepository(AppDatabase.getDatabase(application).healthMetricDao())
    private val sharedPrefs = application.getSharedPreferences("work_health", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private val _healthAnalytics = MutableStateFlow(HealthAnalytics())
    val healthAnalytics: StateFlow<HealthAnalytics> = _healthAnalytics.asStateFlow()

    private val _goals = MutableStateFlow(
        mapOf(
            HealthMetricType.WEIGHT to 70.0,
            HealthMetricType.WATER to 3000.0,
            HealthMetricType.SLEEP to 7.5,
            HealthMetricType.BREAKS to 8.0,
            HealthMetricType.EXERCISE to 30.0,
            HealthMetricType.STEPS to 10000.0,
            HealthMetricType.PROTEIN to 80.0,
            HealthMetricType.FIBER to 30.0,
            HealthMetricType.CALORIES to 2500.0
        )
    )
    val goals: StateFlow<Map<HealthMetricType, Double>> = _goals.asStateFlow()

    private val _dailyWorkRoutine = MutableStateFlow(DailyWorkRoutine())
    val dailyWorkRoutine: StateFlow<DailyWorkRoutine> = _dailyWorkRoutine.asStateFlow()

    val healthData: StateFlow<HealthData> = healthRepository.getAllMetrics()
        .map { metrics -> processHealthData(metrics) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HealthData())

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadGoals()
        viewModelScope.launch {
            healthData.collect { data ->
                _healthAnalytics.value = calculateAnalytics(data)
                updateDailyWorkRoutine(data)
            }
        }
        startRealTimeTimer()
    }

    private fun loadGoals() {
        val savedGoals = mutableMapOf<HealthMetricType, Double>()
        HealthMetricType.entries.forEach { type ->
            val key = "goal_${type.name}"
            val value = sharedPrefs.getFloat(key, -1f)
            if (value > 0) {
                savedGoals[type] = value.toDouble()
            }
        }
        if (savedGoals.isNotEmpty()) {
            // Merge: saving one goal used to drop every other default goal
            _goals.value = _goals.value + savedGoals
        }
    }

    private fun startRealTimeTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(60000)
                updateDailyWorkRoutine(healthData.value)
            }
        }
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun saveHealthMetric(type: HealthMetricType, value: Double, secondaryValue: Double? = null, notes: String = "") {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                if (!isValidMetric(type, value)) {
                    _uiState.update { it.copy(isLoading = false, error = "Please enter a valid ${type.displayName} value.") }
                    return@launch
                }

                val metric = HealthMetric(
                    type = type,
                    value = value,
                    secondaryValue = secondaryValue,
                    timestamp = System.currentTimeMillis(),
                    notes = if (notes.isNotEmpty()) notes else null
                )
                healthRepository.insertMetric(metric)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        saveSuccess = true,
                        lastSavedMetric = type to value,
                        showConfetti = type == HealthMetricType.BREAKS || type == HealthMetricType.WATER || type == HealthMetricType.EXERCISE
                    )
                }

                launch {
                    delay(3000)
                    _uiState.update { it.copy(saveSuccess = false, showConfetti = false) }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to save ${type.displayName}: ${e.message}") }
            }
        }
    }

    private fun isValidMetric(type: HealthMetricType, value: Double): Boolean {
        return when (type) {
            HealthMetricType.WEIGHT -> value in 20.0..300.0
            HealthMetricType.HEIGHT -> value in 50.0..300.0
            HealthMetricType.WATER -> value in 0.0..10000.0
            HealthMetricType.SLEEP -> value in 0.0..24.0
            HealthMetricType.SLEEP_QUALITY -> value in 1.0..10.0
            HealthMetricType.BREAKS -> value in 0.0..20.0
            HealthMetricType.EXERCISE -> value in 0.0..480.0
            HealthMetricType.SCREEN_TIME -> value in 0.0..1440.0
            HealthMetricType.POSTURE -> value in 0.0..1.0
            HealthMetricType.FOCUS -> value in 0.0..1440.0
            HealthMetricType.CALORIES -> value in 0.0..10000.0
            HealthMetricType.PROTEIN -> value in 0.0..500.0
            HealthMetricType.CARBS -> value in 0.0..1000.0
            HealthMetricType.FIBER -> value in 0.0..200.0
            HealthMetricType.FAT -> value in 0.0..500.0
            HealthMetricType.HEART_RATE -> value in 30.0..220.0
            HealthMetricType.BLOOD_PRESSURE -> value in 60.0..250.0
            HealthMetricType.BLOOD_OXYGEN -> value in 70.0..100.0
            HealthMetricType.STEPS -> value in 0.0..100000.0
            HealthMetricType.MOOD -> value in 0.0..9.0
            HealthMetricType.STRESS -> value in 1.0..10.0
            HealthMetricType.MEDITATION -> value in 0.0..180.0
            else -> true
        }
    }

    fun logBreak(breakType: BreakType = BreakType.SHORT) {
        viewModelScope.launch {
            healthRepository.logBreak(breakType.name)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logWater(amount: Double) {
        viewModelScope.launch {
            healthRepository.logWater(amount)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logExercise(minutes: Double, exerciseType: String? = null) {
        viewModelScope.launch {
            healthRepository.logExercise(minutes, exerciseType)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logSleep(hours: Double, quality: Int? = null) {
        viewModelScope.launch {
            healthRepository.logSleep(hours, quality)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logMood(mood: MoodType, notes: String? = null) {
        viewModelScope.launch {
            healthRepository.logMood(mood, notes)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logStress(level: Int, notes: String? = null) {
        viewModelScope.launch {
            healthRepository.logStress(level, notes)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logWeight(weight: Double) {
        viewModelScope.launch {
            healthRepository.logWeight(weight)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logSteps(steps: Double) {
        viewModelScope.launch {
            healthRepository.logSteps(steps)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logHeartRate(rate: Double) {
        viewModelScope.launch {
            healthRepository.logHeartRate(rate)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logBloodPressure(systolic: Double, diastolic: Double) {
        viewModelScope.launch {
            healthRepository.logBloodPressure(systolic, diastolic)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logNutrition(calories: Double, protein: Double, carbs: Double, fiber: Double, fat: Double? = null) {
        viewModelScope.launch {
            healthRepository.logNutrition(calories, protein, carbs, fiber, fat)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logMeditation(minutes: Double, notes: String? = null) {
        viewModelScope.launch {
            healthRepository.logMeditation(minutes, notes)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logSkinCare(product: String, applied: Boolean = true) {
        viewModelScope.launch {
            healthRepository.logSkinCare(product, applied)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun logScreenTime(minutes: Double) {
        viewModelScope.launch {
            healthRepository.logScreenTime(minutes)
            _uiState.update { it.copy(saveSuccess = true) }
            delay(2000)
            _uiState.update { it.copy(saveSuccess = false) }
        }
    }

    fun updateGoal(type: HealthMetricType, value: Double) {
        viewModelScope.launch {
            _goals.update { current -> current.toMutableMap().apply { this[type] = value } }
            sharedPrefs.edit().putFloat("goal_${type.name}", value.toFloat()).apply()
            _uiState.update { it.copy(goalUpdated = true) }
            delay(2000)
            _uiState.update { it.copy(goalUpdated = false) }
        }
    }

    private fun processHealthData(metrics: List<HealthMetric>): HealthData {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todaysMetrics = metrics.filter { it.timestamp >= startOfDay }
        val todaysGrouped = todaysMetrics.groupBy { it.type }

        val currentValues = HealthMetricType.entries.associateWith { type ->
            val entriesForType = todaysGrouped[type] ?: emptyList()
            when {
                type in listOf(HealthMetricType.WATER, HealthMetricType.CALORIES, HealthMetricType.PROTEIN, 
                    HealthMetricType.CARBS, HealthMetricType.FIBER, HealthMetricType.FAT, HealthMetricType.EXERCISE,
                    HealthMetricType.SCREEN_TIME, HealthMetricType.FOCUS, HealthMetricType.STEPS) -> 
                    entriesForType.sumOf { it.value }
                type == HealthMetricType.BLOOD_PRESSURE -> 
                    entriesForType.maxByOrNull { it.timestamp }?.let { "${it.value.toInt()}/${it.secondaryValue?.toInt() ?: 0}" }
                // Body measurements carry over from the last time they were logged
                type == HealthMetricType.WEIGHT || type == HealthMetricType.HEIGHT ->
                    metrics.filter { it.type == type }.maxByOrNull { it.timestamp }?.value
                else -> entriesForType.maxByOrNull { it.timestamp }?.value
            }
        }

        val zone = ZoneId.systemDefault()
        fun dayOf(metric: HealthMetric) = Instant.ofEpochMilli(metric.timestamp).atZone(zone).toLocalDate()
        val lastWeek = (0L..6L).map { today.minusDays(it) }.toSet()
        // Share of the last 7 days with at least one entry of the type
        fun weeklyConsistency(type: HealthMetricType): Int =
            metrics.filter { it.type == type }.map(::dayOf).filter { it in lastWeek }.toSet().size * 100 / 7

        val weightProgress = metrics.filter { it.type == HealthMetricType.WEIGHT }
            .sortedBy { it.timestamp }
            .takeLast(30)
            .map {
                val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault()).toLocalDate()
                date to it.value
            }

        val moodTrend = metrics.filter { it.type == HealthMetricType.MOOD }
            .sortedBy { it.timestamp }
            .takeLast(7)
            .mapNotNull {
                it.mood?.let { mood ->
                    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault()).toLocalDate()
                    date to mood
                }
            }

        val stressTrend = metrics.filter { it.type == HealthMetricType.STRESS }
            .sortedBy { it.timestamp }
            .takeLast(7)
            .mapNotNull {
                it.stressLevel?.let { level ->
                    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault()).toLocalDate()
                    date to level
                }
            }

        val recentEntries = metrics.sortedByDescending { it.timestamp }
            .take(15)
            .map { metric ->
                HealthMetricEntry(
                    type = metric.type,
                    value = metric.value,
                    secondaryValue = metric.secondaryValue,
                    timestamp = metric.timestamp,
                    notes = metric.notes,
                    mood = metric.mood
                )
            }

        val workSessionStats = calculateWorkSessionStats(todaysMetrics)
        val sleepPattern = calculateSleepPattern(metrics.filter { it.type == HealthMetricType.SLEEP })
        val workHoursTrend = calculateWorkHoursTrend(metrics)
        val productivityTrend = calculateProductivityTrend(metrics)
        val nutritionData = calculateNutritionData(todaysMetrics)
        val vitalData = calculateVitalData(metrics, todaysMetrics)

        return HealthData(
            currentValues = currentValues,
            weightProgress = weightProgress,
            moodTrend = moodTrend,
            stressTrend = stressTrend,
            recentEntries = recentEntries,
            workSessionStats = workSessionStats,
            sleepPattern = sleepPattern,
            workHoursTrend = workHoursTrend,
            productivityTrend = productivityTrend,
            nutritionData = nutritionData,
            vitalData = vitalData,
            lastUpdated = metrics.maxOfOrNull { it.timestamp },
            waterConsistency = weeklyConsistency(HealthMetricType.WATER),
            sleepConsistency = weeklyConsistency(HealthMetricType.SLEEP),
            exerciseConsistency = weeklyConsistency(HealthMetricType.EXERCISE),
            activeDates = metrics.map(::dayOf).toSet()
        )
    }

    private fun calculateWorkSessionStats(metrics: List<HealthMetric>): WorkSessionStats {
        val screenTime = metrics.filter { it.type == HealthMetricType.SCREEN_TIME }.sumOf { it.value }
        val breaks = metrics.count { it.type == HealthMetricType.BREAKS }
        val focusTime = metrics.filter { it.type == HealthMetricType.FOCUS }.sumOf { it.value }
        val exercise = metrics.filter { it.type == HealthMetricType.EXERCISE }.sumOf { it.value }
        val meditation = metrics.filter { it.type == HealthMetricType.MEDITATION }.sumOf { it.value }

        return WorkSessionStats(
            totalScreenTime = screenTime,
            totalBreaks = breaks,
            totalFocusTime = focusTime,
            totalExercise = exercise,
            totalMeditation = meditation,
            averageFocusSession = if (metrics.any { it.type == HealthMetricType.FOCUS }) 
                focusTime / metrics.filter { it.type == HealthMetricType.FOCUS }.size else 0.0
        )
    }

    private fun calculateSleepPattern(metrics: List<HealthMetric>): List<Pair<LocalDate, Double>> {
        return metrics.sortedBy { it.timestamp }.takeLast(7).map {
            val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault()).toLocalDate()
            date to it.value
        }
    }

    private fun calculateWorkHoursTrend(metrics: List<HealthMetric>): List<Pair<LocalDate, Double>> {
        return metrics.filter { it.type == HealthMetricType.SCREEN_TIME }
            .groupBy { LocalDateTime.ofInstant(Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault()).toLocalDate() }
            .map { (date, m) -> date to m.sumOf { it.value } / 60.0 }
            .sortedBy { it.first }
            .takeLast(7)
    }

    private fun calculateProductivityTrend(metrics: List<HealthMetric>): List<Pair<LocalDate, Double>> {
        return metrics.filter { it.type == HealthMetricType.FOCUS }
            .groupBy { LocalDateTime.ofInstant(Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault()).toLocalDate() }
            .map { (date, m) -> date to m.sumOf { it.value } }
            .sortedBy { it.first }
            .takeLast(7)
    }

    private fun calculateNutritionData(metrics: List<HealthMetric>): NutritionData {
        return NutritionData(
            calories = metrics.filter { it.type == HealthMetricType.CALORIES }.sumOf { it.value },
            protein = metrics.filter { it.type == HealthMetricType.PROTEIN }.sumOf { it.value },
            carbs = metrics.filter { it.type == HealthMetricType.CARBS }.sumOf { it.value },
            fiber = metrics.filter { it.type == HealthMetricType.FIBER }.sumOf { it.value },
            fat = metrics.filter { it.type == HealthMetricType.FAT }.sumOf { it.value }
        )
    }

    private fun calculateVitalData(metrics: List<HealthMetric>, todaysMetrics: List<HealthMetric>): VitalData {
        val latestHeartRate = metrics.filter { it.type == HealthMetricType.HEART_RATE }
            .maxByOrNull { it.timestamp }?.value
        
        val latestBloodPressure = metrics.filter { it.type == HealthMetricType.BLOOD_PRESSURE }
            .maxByOrNull { it.timestamp }?.let { "${it.value.toInt()}/${it.secondaryValue?.toInt() ?: 0}" }
        
        // Today's steps (previously summed every step ever logged)
        val latestSteps = todaysMetrics.filter { it.type == HealthMetricType.STEPS }
            .sumOf { it.value }.toInt()

        return VitalData(
            heartRate = latestHeartRate,
            bloodPressure = latestBloodPressure,
            steps = latestSteps,
            bloodOxygen = metrics.filter { it.type == HealthMetricType.BLOOD_OXYGEN }
                .maxByOrNull { it.timestamp }?.value
        )
    }

    private fun calculateAnalytics(data: HealthData): HealthAnalytics {
        val weight = data.currentValues[HealthMetricType.WEIGHT] as? Double
        val height = data.currentValues[HealthMetricType.HEIGHT] as? Double
        val bmi = calculateBMI(weight, height)
        val bmiCategory = getBMICategory(bmi)
        val productivityScore = calculateProductivityScore(data.workSessionStats)
        val eyeStrainLevel = calculateEyeStrainLevel((data.currentValues[HealthMetricType.SCREEN_TIME] as? Double ?: 0.0) / 60.0)

        return HealthAnalytics(
            bmi = bmi,
            bmiCategory = bmiCategory,
            waterConsistency = data.waterConsistency,
            sleepConsistency = data.sleepConsistency,
            exerciseConsistency = data.exerciseConsistency,
            dailyStreak = calculateDailyStreak(data.activeDates),
            productivityScore = productivityScore,
            eyeStrainLevel = eyeStrainLevel,
            nutritionGoals = NutritionGoals(
                caloriesTarget = _goals.value[HealthMetricType.CALORIES] ?: 2500.0,
                proteinTarget = _goals.value[HealthMetricType.PROTEIN] ?: 80.0,
                fiberTarget = _goals.value[HealthMetricType.FIBER] ?: 30.0
            ),
            recommendations = generateRecommendations(data)
        )
    }

    private fun calculateBMI(weight: Double?, height: Double?): Double? {
        return if (weight != null && height != null && height > 0) {
            val heightInMeters = height / 100
            weight / (heightInMeters * heightInMeters)
        } else null
    }

    private fun getBMICategory(bmi: Double?): BMICategory {
        return when {
            bmi == null -> BMICategory.UNKNOWN
            bmi < 18.5 -> BMICategory.UNDERWEIGHT
            bmi < 25 -> BMICategory.NORMAL
            bmi < 30 -> BMICategory.OVERWEIGHT
            else -> BMICategory.OBESE
        }
    }

    /** Consecutive days with any logged metric, ending today (or yesterday if nothing is logged yet today). */
    private fun calculateDailyStreak(activeDates: Set<LocalDate>): Int {
        var day = LocalDate.now()
        if (day !in activeDates) day = day.minusDays(1)
        var streak = 0
        while (day in activeDates) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    private fun calculateProductivityScore(stats: WorkSessionStats): Int {
        val breakScore = min(100, stats.totalBreaks * 10)
        val focusScore = min(100, (stats.totalFocusTime / 480 * 100).toInt())
        val exerciseScore = min(100, (stats.totalExercise / 60 * 100).toInt())
        return (breakScore * 0.25 + focusScore * 0.5 + exerciseScore * 0.25).toInt()
    }

    private fun calculateEyeStrainLevel(screenTimeHours: Double): Int {
        return when {
            screenTimeHours < 4 -> 1
            screenTimeHours < 8 -> 2
            else -> 3
        }
    }

    private fun generateRecommendations(data: HealthData): List<String> {
        val recommendations = mutableListOf<String>()
        
        val water = data.currentValues[HealthMetricType.WATER] as? Double ?: 0.0
        val waterGoal = _goals.value[HealthMetricType.WATER] ?: 3000.0
        if (water < waterGoal * 0.5) {
            recommendations.add("Drink more water! You're at ${(water / waterGoal * 100).toInt()}% of your daily goal.")
        }
        
        if (data.workSessionStats.totalBreaks < 5) {
            recommendations.add("Take more breaks! Aim for at least 5 breaks throughout the day.")
        }
        
        val sleep = data.currentValues[HealthMetricType.SLEEP] as? Double ?: 0.0
        if (sleep < 7) {
            recommendations.add("Get more sleep! Aim for 7-8 hours for optimal health.")
        }
        
        if (data.workSessionStats.totalExercise < 30) {
            recommendations.add("Exercise more! Try to get at least 30 minutes of physical activity.")
        }
        
        return recommendations
    }

    private fun updateDailyWorkRoutine(data: HealthData) {
        val currentTime = LocalTime.now()
        val stats = data.workSessionStats
        val lastBreakTime = data.recentEntries.find { it.type == HealthMetricType.BREAKS }?.timestamp ?: 0L
        val minutesSinceBreak = if (lastBreakTime == 0L) 60 else (System.currentTimeMillis() - lastBreakTime) / (1000 * 60)
        val nextBreakIn = max(0, 50 - minutesSinceBreak.toInt())
        val waterConsumed = data.currentValues[HealthMetricType.WATER] as? Double ?: 0.0
        val waterGoal = _goals.value[HealthMetricType.WATER] ?: 3000.0

        _dailyWorkRoutine.update {
            it.copy(
                startTime = LocalTime.of(9, 0),
                currentWorkHours = calculateWorkHoursToday(LocalTime.of(9, 0), currentTime),
                nextBreakInMinutes = nextBreakIn,
                shouldTakeBreak = nextBreakIn <= 5,
                nextBreakType = if (stats.totalBreaks < 2) "Eye Break" else "Physical Break",
                breaksTaken = stats.totalBreaks,
                waterConsumed = waterConsumed,
                waterGoal = waterGoal,
                screenTimeHours = (data.currentValues[HealthMetricType.SCREEN_TIME] as? Double ?: 0.0) / 60.0,
                focusHours = stats.totalFocusTime / 60.0,
                exerciseMinutes = stats.totalExercise,
                meditationMinutes = stats.totalMeditation,
                productivityScore = calculateProductivityScore(stats),
                completedHealthGoals = calculateCompletedGoals(data),
                lastBreakTime = lastBreakTime,
                lastEyeBreakMinutes = minutesSinceBreak.toInt(),
                screenTimeTrend = calculateScreenTimeTrend(data),
                postureTrend = 0f,
                eyeStrainLevel = calculateEyeStrainLevel((data.currentValues[HealthMetricType.SCREEN_TIME] as? Double ?: 0.0) / 60.0)
            )
        }
    }

    private fun calculateWorkHoursToday(startTime: LocalTime, currentTime: LocalTime): Int {
        return if (currentTime.isAfter(startTime)) {
            Duration.between(startTime, currentTime).toHours().toInt()
        } else 0
    }

    private fun calculateScreenTimeTrend(data: HealthData): Float {
        val today = data.workHoursTrend.lastOrNull()?.second ?: 0.0
        val yesterday = data.workHoursTrend.dropLast(1).lastOrNull()?.second ?: today
        return if (yesterday > 0) ((today - yesterday) / yesterday * 100).toFloat() else 0f
    }

    /** Counts today's goals met, using the user's own targets. */
    private fun calculateCompletedGoals(data: HealthData): Int {
        val goals = _goals.value
        var completed = 0
        if ((data.currentValues[HealthMetricType.WATER] as? Double ?: 0.0) >= (goals[HealthMetricType.WATER] ?: 3000.0)) completed++
        if (data.workSessionStats.totalBreaks >= (goals[HealthMetricType.BREAKS] ?: 8.0)) completed++
        if (data.workSessionStats.totalExercise >= (goals[HealthMetricType.EXERCISE] ?: 30.0)) completed++
        if ((data.currentValues[HealthMetricType.SLEEP] as? Double ?: 0.0) >= (goals[HealthMetricType.SLEEP] ?: 7.5)) completed++
        if ((data.currentValues[HealthMetricType.STEPS] as? Double ?: 0.0) >= (goals[HealthMetricType.STEPS] ?: 10000.0)) completed++
        return completed
    }

    fun showMetricInput(type: HealthMetricType) {
        _uiState.update { it.copy(showInputDialog = !it.showInputDialog, selectedMetric = type) }
    }

    fun showMealInput() {
        _uiState.update { it.copy(showMealDialog = !it.showMealDialog) }
    }

    fun showMoodInput() {
        _uiState.update { it.copy(showMoodDialog = !it.showMoodDialog) }
    }

    fun showStressInput() {
        _uiState.update { it.copy(showStressDialog = !it.showStressDialog) }
    }

    fun showVitalsInput() {
        _uiState.update { it.copy(showVitalsDialog = !it.showVitalsDialog) }
    }

    fun getHealthTips(): List<String> = listOf(
        "Drink water every hour to stay hydrated",
        "Follow the 20-20-20 rule for eye health",
        "Take a walk every 2 hours to stay active",
        "Practice deep breathing to reduce stress",
        "Get 7-8 hours of quality sleep"
    )
}

data class DailyWorkRoutine(
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(17, 0),
    val currentWorkHours: Int = 0,
    val nextBreakInMinutes: Int = 50,
    val shouldTakeBreak: Boolean = false,
    val nextBreakType: String = "Short Break",
    val breaksTaken: Int = 0,
    val waterConsumed: Double = 0.0,
    val waterGoal: Double = 3000.0,
    val screenTimeHours: Double = 0.0,
    val goodPostureTime: Int = 75,
    val focusHours: Double = 0.0,
    val exerciseMinutes: Double = 0.0,
    val meditationMinutes: Double = 0.0,
    val completedTasks: Int = 0,
    val productivityScore: Int = 0,
    val completedHealthGoals: Int = 0,
    val totalHealthGoals: Int = 5,
    val lastBreakTime: Long = 0L,
    val lastEyeBreakMinutes: Int = 0,
    val screenTimeTrend: Float = 0f,
    val postureTrend: Float = 0f,
    val eyeStrainLevel: Int = 1
)

data class WorkSessionStats(
    val totalScreenTime: Double = 0.0,
    val totalBreaks: Int = 0,
    val totalFocusTime: Double = 0.0,
    val totalExercise: Double = 0.0,
    val totalMeditation: Double = 0.0,
    val averageFocusSession: Double = 0.0
)

data class NutritionData(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fiber: Double = 0.0,
    val fat: Double = 0.0
)

data class VitalData(
    val heartRate: Double? = null,
    val bloodPressure: String? = null,
    val steps: Int = 0,
    val bloodOxygen: Double? = null
)

data class NutritionGoals(
    val caloriesTarget: Double = 2500.0,
    val proteinTarget: Double = 80.0,
    val fiberTarget: Double = 30.0
)

enum class BreakType { SHORT, LONG, EYE, PHYSICAL }

data class HealthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val goalUpdated: Boolean = false,
    val showConfetti: Boolean = false,
    val showInputDialog: Boolean = false,
    val showMealDialog: Boolean = false,
    val showMoodDialog: Boolean = false,
    val showStressDialog: Boolean = false,
    val showVitalsDialog: Boolean = false,
    val selectedMetric: HealthMetricType? = null,
    val lastSavedMetric: Pair<HealthMetricType, Double>? = null
)

data class HealthData(
    val currentValues: Map<HealthMetricType, Any?> = emptyMap(),
    val weightProgress: List<Pair<LocalDate, Double>> = emptyList(),
    val moodTrend: List<Pair<LocalDate, MoodType>> = emptyList(),
    val stressTrend: List<Pair<LocalDate, Int>> = emptyList(),
    val recentEntries: List<HealthMetricEntry> = emptyList(),
    val waterConsistency: Int = 0,
    val sleepConsistency: Int = 0,
    val workSessionStats: WorkSessionStats = WorkSessionStats(),
    val sleepPattern: List<Pair<LocalDate, Double>> = emptyList(),
    val workHoursTrend: List<Pair<LocalDate, Double>> = emptyList(),
    val productivityTrend: List<Pair<LocalDate, Double>> = emptyList(),
    val nutritionData: NutritionData = NutritionData(),
    val vitalData: VitalData = VitalData(),
    val lastUpdated: Long? = null,
    val exerciseConsistency: Int = 0,
    val activeDates: Set<LocalDate> = emptySet()
)

data class HealthMetricEntry(
    val type: HealthMetricType,
    val value: Double,
    val secondaryValue: Double? = null,
    val timestamp: Long,
    val notes: String? = null,
    val mood: MoodType? = null
)

data class HealthAnalytics(
    val bmi: Double? = null,
    val bmiCategory: BMICategory = BMICategory.UNKNOWN,
    val waterConsistency: Int = 0,
    val sleepConsistency: Int = 0,
    val exerciseConsistency: Int = 0,
    val dailyStreak: Int = 0,
    val productivityScore: Int = 0,
    val eyeStrainLevel: Int = 1,
    val nutritionGoals: NutritionGoals = NutritionGoals(),
    val recommendations: List<String> = emptyList()
)

enum class BMICategory { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE, UNKNOWN }

val HealthMetricType.displayName: String
    get() = when (this) {
        HealthMetricType.WEIGHT -> "Weight"
        HealthMetricType.HEIGHT -> "Height"
        HealthMetricType.WATER -> "Water"
        HealthMetricType.SLEEP -> "Sleep"
        HealthMetricType.SLEEP_QUALITY -> "Sleep Quality"
        HealthMetricType.BREAKS -> "Breaks"
        HealthMetricType.EXERCISE -> "Exercise"
        HealthMetricType.EXERCISE_TYPE -> "Exercise Type"
        HealthMetricType.SCREEN_TIME -> "Screen Time"
        HealthMetricType.POSTURE -> "Posture"
        HealthMetricType.FOCUS -> "Focus"
        HealthMetricType.CALORIES -> "Calories"
        HealthMetricType.PROTEIN -> "Protein"
        HealthMetricType.CARBS -> "Carbs"
        HealthMetricType.FIBER -> "Fiber"
        HealthMetricType.FAT -> "Fat"
        HealthMetricType.HEART_RATE -> "Heart Rate"
        HealthMetricType.BLOOD_PRESSURE -> "Blood Pressure"
        HealthMetricType.BLOOD_OXYGEN -> "Blood Oxygen"
        HealthMetricType.STEPS -> "Steps"
        HealthMetricType.MOOD -> "Mood"
        HealthMetricType.STRESS -> "Stress"
        HealthMetricType.MEDITATION -> "Meditation"
        HealthMetricType.MOISTURIZER -> "Moisturizer"
        HealthMetricType.SUNSCREEN -> "Sunscreen"
        HealthMetricType.SKIN_CARE -> "Skin Care"
        HealthMetricType.HAIR_CARE -> "Hair Care"
        HealthMetricType.WATER_PH -> "Water pH"
        HealthMetricType.BODY_TEMPERATURE -> "Body Temperature"
        HealthMetricType.WAKE_UP_TIME -> "Wake Up Time"
        HealthMetricType.BED_TIME -> "Bed Time"
        HealthMetricType.MOOD_NOTES -> "Mood Notes"
    }

val HealthMetricType.unit: String
    get() = when (this) {
        HealthMetricType.WEIGHT -> "kg"
        HealthMetricType.HEIGHT -> "cm"
        HealthMetricType.WATER -> "ml"
        HealthMetricType.SLEEP -> "hrs"
        HealthMetricType.SLEEP_QUALITY -> "/10"
        HealthMetricType.BREAKS -> "breaks"
        HealthMetricType.EXERCISE -> "min"
        HealthMetricType.EXERCISE_TYPE -> ""
        HealthMetricType.SCREEN_TIME -> "min"
        HealthMetricType.POSTURE -> "%"
        HealthMetricType.FOCUS -> "min"
        HealthMetricType.CALORIES -> "kcal"
        HealthMetricType.PROTEIN -> "g"
        HealthMetricType.CARBS -> "g"
        HealthMetricType.FIBER -> "g"
        HealthMetricType.FAT -> "g"
        HealthMetricType.HEART_RATE -> "bpm"
        HealthMetricType.BLOOD_PRESSURE -> "mmHg"
        HealthMetricType.BLOOD_OXYGEN -> "%"
        HealthMetricType.STEPS -> "steps"
        HealthMetricType.MOOD -> ""
        HealthMetricType.STRESS -> "/10"
        HealthMetricType.MEDITATION -> "min"
        HealthMetricType.MOISTURIZER -> ""
        HealthMetricType.SUNSCREEN -> ""
        HealthMetricType.SKIN_CARE -> ""
        HealthMetricType.HAIR_CARE -> ""
        HealthMetricType.WATER_PH -> "pH"
        HealthMetricType.BODY_TEMPERATURE -> "°C"
        HealthMetricType.WAKE_UP_TIME -> ""
        HealthMetricType.BED_TIME -> ""
        HealthMetricType.MOOD_NOTES -> ""
    }
