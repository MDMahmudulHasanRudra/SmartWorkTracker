package com.rudra.smartworktracker.ui.screens.health

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.ExerciseType
import com.rudra.smartworktracker.model.HealthMetricType
import com.rudra.smartworktracker.model.MoodType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthMetricsScreen(viewModel: HealthMetricsViewModel = viewModel()) {
    val healthData by viewModel.healthData.collectAsState()
    val healthAnalytics by viewModel.healthAnalytics.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val dailyWorkRoutine by viewModel.dailyWorkRoutine.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showBreakReminder by remember { mutableStateOf(false) }
    var showHydrationPopup by remember { mutableStateOf(false) }
    var showMoodPopup by remember { mutableStateOf(false) }
    var showStressPopup by remember { mutableStateOf(false) }
    var showVitalsPopup by remember { mutableStateOf(false) }
    var showExercisePopup by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(backgroundGradient)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            WorkHealthHeader(
                dailyStreak = healthAnalytics.dailyStreak,
                workHoursToday = dailyWorkRoutine.currentWorkHours,
                nextBreakIn = dailyWorkRoutine.nextBreakInMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                val tabs = listOf("Dashboard", "Activity", "Nutrition", "Mind & Body")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> DashboardTab(
                        healthData = healthData,
                        healthAnalytics = healthAnalytics,
                        goals = goals,
                        dailyRoutine = dailyWorkRoutine,
                        onMetricClick = { viewModel.showMetricInput(it) },
                        onAddBreak = { viewModel.logBreak(); showBreakReminder = false },
                        onAddWater = { viewModel.logWater(250.0); showHydrationPopup = false },
                        onStretch = { viewModel.logExercise(5.0, "Stretching") },
                        onEyeExercise = { viewModel.logBreak(BreakType.EYE) },
                        onLogWork = { viewModel.showMetricInput(HealthMetricType.SCREEN_TIME) },
                        onLogSleep = { viewModel.showMetricInput(HealthMetricType.SLEEP) },
                        onMoodClick = { showMoodPopup = true },
                        onStressClick = { showStressPopup = true },
                        onVitalsClick = { showVitalsPopup = true },
                        onExerciseClick = { showExercisePopup = true },
                        onStepsClick = { viewModel.showMetricInput(HealthMetricType.STEPS) },
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> ActivityTab(
                        workSessionStats = healthData.workSessionStats,
                        dailyRoutine = dailyWorkRoutine,
                        recentEntries = healthData.recentEntries,
                        onLogBreak = { viewModel.logBreak() },
                        onLogWork = { viewModel.showMetricInput(HealthMetricType.SCREEN_TIME) },
                        onLogExercise = { showExercisePopup = true },
                        onLogSteps = { viewModel.showMetricInput(HealthMetricType.STEPS) },
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> NutritionTab(
                        nutritionData = healthData.nutritionData,
                        dailyGoals = healthAnalytics.nutritionGoals,
                        onAddMeal = { viewModel.showMealInput() },
                        modifier = Modifier.fillMaxSize()
                    )
                    3 -> MindBodyTab(
                        healthData = healthData,
                        healthAnalytics = healthAnalytics,
                        onMoodClick = { showMoodPopup = true },
                        onStressClick = { showStressPopup = true },
                        onMeditationClick = { viewModel.showMetricInput(HealthMetricType.MEDITATION) },
                        onSleepClick = { viewModel.showMetricInput(HealthMetricType.SLEEP) },
                        onVitalsClick = { showVitalsPopup = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showBreakReminder && dailyWorkRoutine.shouldTakeBreak) {
            BreakReminderPopup(
                breakType = dailyWorkRoutine.nextBreakType,
                onTakeBreak = { viewModel.logBreak(); showBreakReminder = false },
                onSnooze = { showBreakReminder = false },
                onDismiss = { showBreakReminder = false }
            )
        }

        if (showHydrationPopup) {
            WaterLogPopup(
                onLogWater = { viewModel.logWater(it); showHydrationPopup = false },
                onDismiss = { showHydrationPopup = false }
            )
        }

        uiState.selectedMetric?.let { metricType ->
            if (uiState.showInputDialog) {
                HealthInputDialog(
                    metricType = metricType,
                    onSave = { value ->
                        viewModel.saveHealthMetric(metricType, value)
                        viewModel.showMetricInput(metricType)
                    },
                    onDismiss = { viewModel.showMetricInput(metricType) }
                )
            }
        }

        if (uiState.showMealDialog) {
            MealInputDialog(
                onSave = { c, p, cr, f, fat ->
                    viewModel.logNutrition(c, p, cr, f, fat)
                    viewModel.showMealInput()
                },
                onDismiss = { viewModel.showMealInput() }
            )
        }

        if (showMoodPopup) {
            MoodInputDialog(
                onSave = { mood, notes ->
                    viewModel.logMood(mood, notes)
                    showMoodPopup = false
                },
                onDismiss = { showMoodPopup = false }
            )
        }

        if (showStressPopup) {
            StressInputDialog(
                onSave = { level, notes ->
                    viewModel.logStress(level, notes)
                    showStressPopup = false
                },
                onDismiss = { showStressPopup = false }
            )
        }

        if (showVitalsPopup) {
            VitalsInputDialog(
                onSave = { heartRate, systolic, diastolic, oxygen ->
                    heartRate?.let { viewModel.logHeartRate(it) }
                    if (systolic != null && diastolic != null) {
                        viewModel.logBloodPressure(systolic, diastolic)
                    }
                    showVitalsPopup = false
                },
                onDismiss = { showVitalsPopup = false }
            )
        }

        if (showExercisePopup) {
            ExerciseInputDialog(
                onSave = { minutes, type ->
                    viewModel.logExercise(minutes, type)
                    showExercisePopup = false
                },
                onDismiss = { showExercisePopup = false }
            )
        }
    }
}

@Composable
fun WorkHealthHeader(
    dailyStreak: Int,
    workHoursToday: Int,
    nextBreakIn: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Work Wellness",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))} \u2022 Today: ${workHoursToday}h",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { nextBreakIn / 50f },
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFFFF9800),
                        strokeWidth = 4.dp,
                        trackColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$nextBreakIn",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF9800)
                        )
                        Text("min", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color(0xFFFF9800))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$dailyStreak", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
                        Text("DAYS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), fontSize = 8.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    healthData: HealthData,
    healthAnalytics: HealthAnalytics,
    goals: Map<HealthMetricType, Double>,
    dailyRoutine: DailyWorkRoutine,
    onMetricClick: (HealthMetricType) -> Unit,
    onAddBreak: () -> Unit,
    onAddWater: () -> Unit,
    onStretch: () -> Unit,
    onEyeExercise: () -> Unit,
    onLogWork: () -> Unit,
    onLogSleep: () -> Unit,
    onMoodClick: () -> Unit,
    onStressClick: () -> Unit,
    onVitalsClick: () -> Unit,
    onExerciseClick: () -> Unit,
    onStepsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MoodCard(
                    currentMood = healthData.recentEntries.find { it.type == HealthMetricType.MOOD }?.mood,
                    onClick = onMoodClick,
                    modifier = Modifier.weight(1f)
                )
                StressCard(
                    currentStress = healthData.recentEntries.find { it.type == HealthMetricType.STRESS }?.value?.toInt(),
                    onClick = onStressClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HealthStatCard(
                    title = "Screen Time",
                    value = "${String.format(Locale.getDefault(), "%.1f", dailyRoutine.screenTimeHours)}h",
                    target = "\u22648h",
                    icon = Icons.Outlined.Computer,
                    color = Color(0xFF4A90E2),
                    trend = dailyRoutine.screenTimeTrend,
                    modifier = Modifier.weight(1f)
                )

                HealthStatCard(
                    title = "Exercise",
                    value = "${String.format(Locale.getDefault(), "%.0f", dailyRoutine.exerciseMinutes)}m",
                    target = "30m",
                    icon = Icons.Default.FitnessCenter,
                    color = Color(0xFF7B61FF),
                    trend = 0f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ProductivitySection(
                productivityScore = dailyRoutine.productivityScore,
                focusTime = dailyRoutine.focusHours,
                exerciseMinutes = dailyRoutine.exerciseMinutes,
                meditationMinutes = dailyRoutine.meditationMinutes,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HydrationCard(
                    currentWater = dailyRoutine.waterConsumed,
                    targetWater = dailyRoutine.waterGoal,
                    onAddWater = onAddWater,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                StepsCard(
                    currentSteps = (healthData.currentValues[HealthMetricType.STEPS] as? Double)?.toInt() ?: 0,
                    targetSteps = (goals[HealthMetricType.STEPS] ?: 10000.0).toInt(),
                    onClick = onStepsClick,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        item {
            VitalsQuickCard(
                vitalData = healthData.vitalData,
                onClick = onVitalsClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            QuickWorkActions(
                onLogBreak = onAddBreak,
                onStretch = onStretch,
                onEyeExercise = onEyeExercise,
                onHydrate = onAddWater,
                onLogWork = onLogWork,
                onLogSleep = onLogSleep,
                onExercise = onExerciseClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            EyeHealthSection(
                eyeStrainLevel = dailyRoutine.eyeStrainLevel,
                lastEyeBreak = dailyRoutine.lastEyeBreakMinutes,
                recommendedEyeExercises = listOf("20-20-20 Rule", "Eye Rolling", "Blinking"),
                onDoExercise = onEyeExercise,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (healthAnalytics.recommendations.isNotEmpty()) {
            item {
                RecommendationsCard(
                    recommendations = healthAnalytics.recommendations,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun HealthStatCard(
    title: String,
    value: String,
    target: String,
    icon: ImageVector,
    color: Color,
    trend: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
                if (abs(trend) > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (trend > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            null,
                            tint = if (trend > 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "${abs(trend)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (trend > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Goal: $target", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProductivitySection(
    productivityScore: Int,
    focusTime: Double,
    exerciseMinutes: Double,
    meditationMinutes: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                CircularProgressIndicator(
                    progress = { productivityScore / 100f },
                    modifier = Modifier.size(90.dp),
                    strokeWidth = 10.dp,
                    color = getProductivityColor(productivityScore),
                    trackColor = getProductivityColor(productivityScore).copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$productivityScore%", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text("Score", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.width(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily Wellness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("${String.format("%.1f", focusTime)}h Focus", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(16.dp), tint = Color(0xFF7B61FF))
                    Spacer(Modifier.width(8.dp))
                    Text("${exerciseMinutes.toInt()}m Exercise", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SelfImprovement, null, modifier = Modifier.size(16.dp), tint = Color(0xFFE91E63))
                    Spacer(Modifier.width(8.dp))
                    Text("${meditationMinutes.toInt()}m Meditation", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun QuickWorkActions(
    onLogBreak: () -> Unit,
    onStretch: () -> Unit,
    onEyeExercise: () -> Unit,
    onHydrate: () -> Unit,
    onLogWork: () -> Unit,
    onLogSleep: () -> Unit,
    onExercise: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionTile("Break", Icons.Default.Coffee, Color(0xFFFF9800), onLogBreak, Modifier.weight(1f))
                    ActionTile("Stretch", Icons.Default.FitnessCenter, Color(0xFF4CAF50), onStretch, Modifier.weight(1f))
                    ActionTile("Eye Care", Icons.Default.Visibility, Color(0xFF2196F3), onEyeExercise, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionTile("Water", Icons.Default.LocalDrink, Color(0xFF03A9F4), onHydrate, Modifier.weight(1f))
                    ActionTile("Work", Icons.Default.Work, Color(0xFF795548), onLogWork, Modifier.weight(1f))
                    ActionTile("Sleep", Icons.Default.Bedtime, Color(0xFF673AB7), onLogSleep, Modifier.weight(1f))
                }
                if (onExercise != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionTile("Exercise", Icons.Default.FitnessCenter, Color(0xFF7B61FF), onExercise, Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(2f))
                    }
                }
            }
        }
    }
}

@Composable
fun ActionTile(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .height(85.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun DailyGoalsProgress(
    completedGoals: Int,
    totalGoals: Int,
    goals: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Daily Health Checklist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${(completedGoals.toFloat()/totalGoals * 100).toInt()}% Achievement", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
                Text("$completedGoals/$totalGoals", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { completedGoals.toFloat() / totalGoals },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(16.dp))
            goals.forEachIndexed { index, goal ->
                val isDone = index < completedGoals
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                    Icon(
                        if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (isDone) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        goal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textDecoration = if (isDone) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityTab(
    workSessionStats: WorkSessionStats,
    dailyRoutine: DailyWorkRoutine,
    recentEntries: List<HealthMetricEntry>,
    onLogBreak: () -> Unit,
    onLogWork: () -> Unit,
    onLogExercise: () -> Unit,
    onLogSteps: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Activity Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MetricItem("Screen", "${String.format("%.1f", workSessionStats.totalScreenTime / 60)}h", Icons.Default.Computer, Color(0xFF2196F3))
                        MetricItem("Focus", "${String.format("%.1f", workSessionStats.totalFocusTime / 60)}h", Icons.Default.Timer, Color(0xFF4CAF50))
                        MetricItem("Breaks", "${workSessionStats.totalBreaks}", Icons.Default.Coffee, Color(0xFFFF9800))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MetricItem("Exercise", "${workSessionStats.totalExercise.toInt()}m", Icons.Default.FitnessCenter, Color(0xFF7B61FF))
                        MetricItem("Meditation", "${workSessionStats.totalMeditation.toInt()}m", Icons.Default.SelfImprovement, Color(0xFFE91E63))
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onLogBreak,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Coffee, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Break")
                }
                Button(
                    onClick = onLogWork,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Icon(Icons.Default.Computer, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Work")
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onLogExercise,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exercise")
                }
                OutlinedButton(
                    onClick = onLogSteps,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.DirectionsWalk, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Steps")
                }
            }
        }

        item {
            Text("Recent Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
        }

        items(recentEntries) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(getMetricIcon(entry.type), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.type.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("MMM d, hh:mm a")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${entry.value.toInt()}${entry.type.unit}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun MindBodyTab(
    healthData: HealthData,
    healthAnalytics: HealthAnalytics,
    onMoodClick: () -> Unit,
    onStressClick: () -> Unit,
    onMeditationClick: () -> Unit,
    onSleepClick: () -> Unit,
    onVitalsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("Mental Wellness", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MoodCardLarge(
                    moodTrend = healthData.moodTrend,
                    onClick = onMoodClick,
                    modifier = Modifier.weight(1f)
                )
                StressCardLarge(
                    stressTrend = healthData.stressTrend,
                    onClick = onStressClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MeditationCard(
                totalMinutes = healthData.workSessionStats.totalMeditation,
                onClick = onMeditationClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            SleepCard(
                lastSleep = (healthData.currentValues[HealthMetricType.SLEEP] as? Double) ?: 0.0,
                sleepPattern = healthData.sleepPattern,
                onClick = onSleepClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Vitals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }

        item {
            VitalsCard(
                vitalData = healthData.vitalData,
                onClick = onVitalsClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            QuickWellnessActions(
                onMood = onMoodClick,
                onStress = onStressClick,
                onMeditation = onMeditationClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun getMetricIcon(type: HealthMetricType): ImageVector = when(type) {
    HealthMetricType.WATER -> Icons.Default.LocalDrink
    HealthMetricType.SLEEP -> Icons.Default.Bedtime
    HealthMetricType.EXERCISE -> Icons.Default.FitnessCenter
    HealthMetricType.CALORIES -> Icons.Default.LocalFireDepartment
    HealthMetricType.SCREEN_TIME -> Icons.Default.Computer
    else -> Icons.Default.History
}

@Composable
fun getProductivityColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF4CAF50)
    score >= 60 -> Color(0xFFFF9800)
    else -> Color(0xFFF44336)
}

@Composable
fun getEyeStrainColor(level: Int): Color = when (level) {
    1 -> Color(0xFF4CAF50)
    2 -> Color(0xFFFF9800)
    3 -> Color(0xFFF44336)
    else -> MaterialTheme.colorScheme.outline
}

@Composable
fun HealthInputDialog(
    metricType: HealthMetricType,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(getMetricIcon(metricType), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Log ${metricType.displayName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Enter ${metricType.displayName} (${metricType.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { value.toDoubleOrNull()?.let { onSave(it) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun BreakReminderPopup(
    breakType: String,
    onTakeBreak: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFFF9800).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Timer, null, tint = Color(0xFFFF9800), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Time for a $breakType!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Refresh your mind and body to maintain peak focus.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Text("Snooze")
                    }
                    Button(onClick = onTakeBreak, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                        Text("Take Break")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecommendationsCard(
    recommendations: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
            Spacer(modifier = Modifier.height(12.dp))
            recommendations.forEach { recommendation ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(recommendation, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
