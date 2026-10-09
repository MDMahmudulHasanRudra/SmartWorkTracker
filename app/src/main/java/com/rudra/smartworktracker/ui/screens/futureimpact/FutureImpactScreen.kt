package com.rudra.smartworktracker.ui.screens.futureimpact

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.CheckInType
import com.rudra.smartworktracker.model.ConsequenceDebt
import com.rudra.smartworktracker.model.DailyCheckIn
import com.rudra.smartworktracker.model.Decision
import com.rudra.smartworktracker.model.DecisionCategory
import com.rudra.smartworktracker.model.DecisionType
import com.rudra.smartworktracker.model.FutureIdentity
import com.rudra.smartworktracker.model.UserHistory
import com.rudra.smartworktracker.model.WeeklyReport
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Good = Color(0xFF2E7D32)
private val Bad = Color(0xFFD32F2F)
private val Warn = Color(0xFFEF6C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureImpactScreen(
    viewModel: FutureImpactViewModel = viewModel(
        factory = FutureImpactViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val immediateFeedback by viewModel.immediateFeedback.collectAsStateWithLifecycle()
    val undoWindow by viewModel.undoWindow.collectAsStateWithLifecycle()
    val showCheckInPrompt by viewModel.showCheckInPrompt.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showConsequenceDialog by remember { mutableStateOf<DecisionType?>(null) }
    var showIdentityDialog by rememberSaveable { mutableStateOf(false) }
    var showLogDialog by rememberSaveable { mutableStateOf(false) }
    var manualCheckIn by remember { mutableStateOf<CheckInType?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf<DecisionCategory?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FutureImpactEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is FutureImpactEvent.Deleted -> {
                    val result = snackbarHostState.showSnackbar("\"${event.title}\" deleted", actionLabel = "Undo")
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                }
            }
        }
    }

    // Automatic prompt in the morning/night window, or one opened from the check-in chips
    val promptType = manualCheckIn ?: if (showCheckInPrompt) viewModel.getCheckInType() else null
    promptType?.let { type ->
        val existing = if (type == CheckInType.MORNING) state.morningCheckIn else state.nightCheckIn
        CheckInDialog(
            checkInType = type,
            existing = existing,
            onDismiss = {
                if (manualCheckIn != null) manualCheckIn = null else viewModel.dismissCheckInPrompt()
            },
            onSave = { mood, answer ->
                viewModel.saveCheckIn(type, mood, answer)
                manualCheckIn = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showLogDialog = true },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                text = { Text("Log decision") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FutureSelfHeader(
                    history = state.userHistory,
                    identity = state.identity,
                    onIdentityClick = { showIdentityDialog = true }
                )
            }

            item {
                CheckInRow(
                    morning = state.morningCheckIn,
                    night = state.nightCheckIn,
                    onOpen = { manualCheckIn = it }
                )
            }

            item {
                DailyRealityScoreCard(
                    score = state.dailyScore,
                    hasDecisions = state.todayDecisions.isNotEmpty(),
                    identity = state.identity,
                    identityMessage = state.identityMessage,
                    aligned = state.identityAligned
                )
            }

            item { StreakCard(streaks = state.streaks) }

            undoWindow?.let { window ->
                item(key = "undo") {
                    UndoWindowBanner(
                        undoWindow = window,
                        onRecover = viewModel::executeRecovery,
                        onDismiss = viewModel::dismissUndoWindow
                    )
                }
            }

            immediateFeedback?.let { feedback ->
                item(key = "feedback") { ImmediateFeedbackCard(feedback = feedback, onDismiss = viewModel::dismissFeedback) }
            }

            if (state.patternWarnings.isNotEmpty()) {
                item { SectionTitle("Reality alerts") }
                items(state.patternWarnings, key = { it.title }) { warning -> PatternWarningCard(warning) }
            }

            item { ImpactSummaryCard(stats = state.impactStats) }

            item { FutureCollapseCard(projection = state.projection) }

            if (state.totalDebt != 0f || state.debts.any { it.debtAmount != 0f }) {
                item { ConsequenceDebtCard(debts = state.debts, totalDebt = state.totalDebt) }
            }

            item {
                WeeklyRealityReportCard(
                    report = state.latestReport,
                    onGenerate = viewModel::generateWeeklyReport
                )
            }

            item { SectionTitle("Quick log") }
            item { CategorySelector(selectedCategory) { selectedCategory = it } }

            val quickTypes = DecisionType.entries
                .filter { it != DecisionType.CUSTOM }
                .filter { selectedCategory == null || it.category == selectedCategory }
            items(quickTypes.chunked(2), key = { row -> row.joinToString { it.name } }) { rowTypes ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowTypes.forEach { type ->
                        DecisionChip(
                            type = type,
                            onClick = { viewModel.addDecision(type) },
                            onInfo = { showConsequenceDialog = type },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item { SectionTitle("Today's decisions (${state.todayDecisions.size})") }
            if (state.todayDecisions.isEmpty()) {
                item { EmptyStateCard() }
            } else {
                items(state.todayDecisions, key = { it.id }) { decision ->
                    DecisionItem(
                        decision = decision,
                        onDelete = { viewModel.deleteDecision(decision) },
                        onInfo = { showConsequenceDialog = decision.decisionType },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    showConsequenceDialog?.let { type -> ConsequenceDialog(type) { showConsequenceDialog = null } }

    if (showIdentityDialog) {
        IdentitySelectionDialog(
            current = state.identity,
            onSelect = {
                viewModel.setIdentity(it)
                showIdentityDialog = false
            },
            onDismiss = { showIdentityDialog = false }
        )
    }

    if (showLogDialog) {
        LogDecisionDialog(
            onDismiss = { showLogDialog = false },
            onLog = { title, positive, category, notes ->
                viewModel.addDecision(DecisionType.CUSTOM, title, notes, positive, category)
                showLogDialog = false
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun FutureSelfHeader(history: UserHistory?, identity: FutureIdentity, onIdentityClick: () -> Unit) {
    Column {
        Text("Future Self", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Are your actions matching who you want to become?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = onIdentityClick,
                label = {
                    Text(
                        if (identity == FutureIdentity.NO_IDENTITY) "Choose your future identity" else identity.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            if (history != null && history.totalDecisions > 0) {
                Text(
                    "${history.totalDaysActive} days · ${history.totalDecisions} decisions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckInRow(morning: DailyCheckIn?, night: DailyCheckIn?, onOpen: (CheckInType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CheckInChip("Morning", morning?.morningMood, morning?.morningAnswer, Icons.Default.WbSunny, Modifier.weight(1f)) {
            onOpen(CheckInType.MORNING)
        }
        CheckInChip("Night", night?.nightMood, night?.nightAnswer, Icons.Default.Bedtime, Modifier.weight(1f)) {
            onOpen(CheckInType.NIGHT)
        }
    }
}

@Composable
private fun CheckInChip(
    label: String,
    mood: Int?,
    answer: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val done = mood != null && mood > 0
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("$label check-in", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(
                    text = if (done) "${moodEmoji(mood!!)} ${answer.orEmpty().ifBlank { "Done" }}" else "Tap to check in",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (done) Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Good, modifier = Modifier.size(18.dp))
        }
    }
}

private fun moodEmoji(mood: Int) = when (mood) {
    1 -> "😫"
    2 -> "😕"
    3 -> "😐"
    4 -> "🙂"
    else -> "😄"
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Good
    score >= 60 -> Color(0xFF7CB342)
    score >= 40 -> Color(0xFFF9A825)
    score >= 20 -> Warn
    else -> Bad
}

@Composable
fun DailyRealityScoreCard(score: Int, hasDecisions: Boolean, identity: FutureIdentity, identityMessage: String, aligned: Boolean) {
    val color = scoreColor(score)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily reality score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (hasDecisions) "Based on today's decisions and your recent streaks" else "Log a decision to move your score",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (identity != FutureIdentity.NO_IDENTITY) {
                        Spacer(Modifier.height(4.dp))
                        Text(identity.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(color, color.copy(alpha = 0.55f))))
                ) {
                    Text("$score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (identityMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = color.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    identityMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (aligned) Good else Bad,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StreakCard(streaks: StreakInfo) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StreakTile(
            label = "Discipline",
            days = streaks.currentDisciplineStreak,
            best = streaks.maxDisciplineStreak,
            active = streaks.isOnDisciplineStreak,
            emoji = if (streaks.currentDisciplineStreak >= 2) "🔥" else "💪",
            color = Good,
            modifier = Modifier.weight(1f)
        )
        StreakTile(
            label = "Damage",
            days = streaks.currentDamageStreak,
            best = streaks.maxDamageStreak,
            active = streaks.isOnDamageStreak,
            emoji = if (streaks.isOnDamageStreak) "⚠️" else "🛡️",
            color = Bad,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StreakTile(label: String, days: Int, best: Int, active: Boolean, emoji: String, color: Color, modifier: Modifier) {
    val scale = if (active) {
        val transition = rememberInfiniteTransition(label = "streak_$label")
        transition.animateFloat(1f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse").value
    } else 1f
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.scale(scale))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "$days day${if (days == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (active) color else MaterialTheme.colorScheme.onSurface
                )
                Text("Best $best", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ConsequenceDebtCard(debts: List<ConsequenceDebt>, totalDebt: Float) {
    val debtColor = when {
        totalDebt > 20 -> Bad
        totalDebt > 0 -> Warn
        else -> Good
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Balance, contentDescription = null, tint = debtColor)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Consequence debt", fontWeight = FontWeight.Bold)
                    Text(
                        if (totalDebt > 0) "Negative choices you still owe recovery for" else "You're in surplus — keep banking good choices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // A behaviour score, not money
                Text("${"%.0f".format(totalDebt)} pts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = debtColor)
            }
            val nonZero = debts.filter { it.debtAmount != 0f }.sortedByDescending { it.debtAmount }
            if (nonZero.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                nonZero.forEach { debt ->
                    val catColor = when {
                        debt.debtAmount > 10 -> Bad
                        debt.debtAmount > 0 -> Warn
                        else -> Good
                    }
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(debt.category.displayName, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${if (debt.debtAmount > 0) "+" else ""}${"%.0f".format(debt.debtAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = catColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FutureCollapseCard(projection: FutureProjection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("4-week projection", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    when (projection.trend) {
                        Trend.IMPROVING -> "Improving"
                        Trend.DECLINING -> "Declining"
                        Trend.STABLE -> "Stable"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = trendColor(projection.trend)
                )
            }
            Spacer(Modifier.height(12.dp))
            FutureGraph(projection = projection)
            Spacer(Modifier.height(12.dp))
            Text(projection.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun trendColor(trend: Trend) = when (trend) {
    Trend.IMPROVING -> Good
    Trend.DECLINING -> Bad
    Trend.STABLE -> Color(0xFFF9A825)
}

@Composable
fun FutureGraph(projection: FutureProjection) {
    val lineColor = trendColor(projection.trend)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val points = listOf(
        projection.currentScore, projection.week1Score, projection.week2Score, projection.week3Score, projection.week4Score
    ).map { it.toFloat() }

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(110.dp)
        .padding(vertical = 6.dp)) {
        val stepX = size.width / (points.size - 1)
        fun y(score: Float) = size.height - score / 100f * size.height
        // 50 = neutral line
        drawLine(gridColor, Offset(0f, y(50f)), Offset(size.width, y(50f)), strokeWidth = 2f)
        val path = Path()
        points.forEachIndexed { index, score ->
            if (index == 0) path.moveTo(0f, y(score)) else path.lineTo(index * stepX, y(score))
        }
        drawPath(path, lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
        points.forEachIndexed { index, score -> drawCircle(lineColor, 9f, Offset(index * stepX, y(score))) }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Now" to projection.currentScore, "1W" to projection.week1Score, "2W" to projection.week2Score, "3W" to projection.week3Score, "4W" to projection.week4Score)
            .forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$value", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
    }
}

@Composable
fun ImpactSummaryCard(stats: ImpactStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Last 7 days", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ImpactNumber("${stats.weekPositive}", "Positive", Good)
                ImpactNumber("${stats.weekNegative}", "Negative", Bad)
                ImpactNumber(
                    "${if (stats.weekNetImpact >= 0) "+" else ""}${stats.weekNetImpact}",
                    "Net",
                    if (stats.weekNetImpact >= 0) Good else Bad
                )
            }
            val total = stats.weekPositive + stats.weekNegative
            if (total > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { stats.weekPositive.toFloat() / total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Good,
                    trackColor = Bad.copy(alpha = 0.6f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${stats.monthPositive + stats.monthNegative} decisions in the last month · ${stats.monthBalance.toInt()}% positive",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImpactNumber(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WeeklyRealityReportCard(report: WeeklyReport?, onGenerate: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Weekly reality report", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    if (report != null) {
                        Text(
                            "${dateFormat.format(Date(report.weekStartDate))} – ${dateFormat.format(Date(report.weekEndDate))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                TextButton(onClick = onGenerate) { Text(if (report == null) "Generate" else "Refresh") }
            }
            if (report == null) {
                Text(
                    "Create a summary of this week's decisions. Last week's report is written automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text(report.summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Best habit: ${report.bestImprovement}", style = MaterialTheme.typography.labelMedium, color = Good)
                        Text("Biggest slip: ${report.biggestMistake}", style = MaterialTheme.typography.labelMedium, color = Bad)
                        Text("Weakest area: ${report.worstDay}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${report.positiveDecisions} positive", style = MaterialTheme.typography.labelMedium)
                        Text("${report.negativeDecisions} negative", style = MaterialTheme.typography.labelMedium)
                        Text("Avg ${report.averageDailyScore}/100", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (report.identityAlignment > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("Identity alignment: ${report.identityAlignment}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun CheckInDialog(checkInType: CheckInType, existing: DailyCheckIn?, onDismiss: () -> Unit, onSave: (Int, String) -> Unit) {
    val initialMood = (if (checkInType == CheckInType.MORNING) existing?.morningMood else existing?.nightMood)?.takeIf { it > 0 } ?: 3
    val initialAnswer = (if (checkInType == CheckInType.MORNING) existing?.morningAnswer else existing?.nightAnswer).orEmpty()
    var mood by rememberSaveable { mutableIntStateOf(initialMood) }
    var answer by rememberSaveable { mutableStateOf(initialAnswer) }

    val question = when (checkInType) {
        CheckInType.MORNING -> "What kind of day will you have?"
        CheckInType.NIGHT -> "Did your actions match your future self?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (checkInType == CheckInType.MORNING) Icons.Default.WbSunny else Icons.Default.Bedtime, contentDescription = null) },
        title = { Text(if (checkInType == CheckInType.MORNING) "Morning check-in" else "Night check-in") },
        text = {
            Column {
                Text(question, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 16.dp))
                Text("How do you feel?", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    (1..5).forEach { m ->
                        val scale by animateFloatAsState(
                            targetValue = if (mood == m) 1.25f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "mood"
                        )
                        Surface(
                            onClick = { mood = m },
                            modifier = Modifier.scale(scale),
                            shape = CircleShape,
                            color = if (mood == m) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Text(moodEmoji(m), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it.take(200) },
                    label = { Text(if (checkInType == CheckInType.MORNING) "Your intention for today" else "One honest reflection") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(mood, answer) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (existing == null) "Skip" else "Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogDecisionDialog(onDismiss: () -> Unit, onLog: (String, Boolean, DecisionCategory, String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var positive by rememberSaveable { mutableStateOf(true) }
    var category by rememberSaveable { mutableStateOf(DecisionCategory.PERSONAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log a decision") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("What did you decide?") },
                    placeholder = { Text("e.g. Skipped dessert") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = positive,
                        onClick = { positive = true },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        icon = { Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    ) { Text("Good for me") }
                    SegmentedButton(
                        selected = !positive,
                        onClick = { positive = false },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        icon = { Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    ) { Text("Not great") }
                }
                Text("Area", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DecisionCategory.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.displayName) })
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(200) },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onLog(title, positive, category, notes) }, enabled = title.isNotBlank()) { Text("Log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("No decisions logged today", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Tap a quick-log button above or log your own.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PatternWarningCard(warning: PatternWarning) {
    val (container, accent, icon) = when (warning.severity) {
        PatternSeverity.HIGH -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Default.Warning)
        PatternSeverity.MEDIUM -> Triple(Warn.copy(alpha = 0.14f), Warn, Icons.Default.Info)
        PatternSeverity.LOW -> Triple(Good.copy(alpha = 0.12f), Good, Icons.Default.EmojiEvents)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(warning.title, fontWeight = FontWeight.Bold, color = accent)
            }
            Spacer(Modifier.height(4.dp))
            Text(warning.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(warning.advice, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CategorySelector(selected: DecisionCategory?, onSelect: (DecisionCategory?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") }) }
        items(DecisionCategory.entries.filter { c -> DecisionType.entries.any { it != DecisionType.CUSTOM && it.category == c } }) {
            FilterChip(selected = selected == it, onClick = { onSelect(it) }, label = { Text(it.displayName) })
        }
    }
}

@Composable
fun DecisionChip(type: DecisionType, onClick: () -> Unit, onInfo: () -> Unit, modifier: Modifier = Modifier) {
    val isPos = type.defaultImpact > 0
    val color = if (isPos) Good else Bad
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f), onClick = onClick) {
        Row(modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isPos) Icons.Default.Add else Icons.Default.Remove, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                type.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onInfo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Info, contentDescription = "What this leads to", tint = color.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun DecisionItem(decision: Decision, onDelete: () -> Unit, onInfo: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (decision.isPositive) Good else Bad
    val time = remember(decision.createdAt) { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(decision.createdAt)) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (decision.isPositive) Icons.Default.ThumbUp else Icons.Default.ThumbDown, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(decision.displayTitle(), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(time, decision.category.displayName, decision.notes).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (decision.decisionType != DecisionType.CUSTOM) {
                IconButton(onClick = onInfo) { Icon(Icons.Default.Info, contentDescription = "Consequences", modifier = Modifier.size(18.dp)) }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun UndoWindowBanner(undoWindow: UndoWindow, onRecover: () -> Unit, onDismiss: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(undoWindow.expiresAt) {
        while (now < undoWindow.expiresAt) {
            delay(15_000)
            now = System.currentTimeMillis()
        }
    }
    val minutesLeft = ((undoWindow.expiresAt - now) / 60_000).coerceAtLeast(0) + 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Warn.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = Warn, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recovery window", fontWeight = FontWeight.Bold, color = Warn)
                    Text("About $minutesLeft min left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Dismiss") }
            }
            Text(undoWindow.recoveryAction, style = MaterialTheme.typography.bodyMedium)
            if (undoWindow.recoveryAction2.isNotBlank()) {
                Text("or: ${undoWindow.recoveryAction2}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRecover, colors = ButtonDefaults.buttonColors(containerColor = Warn, contentColor = Color.White)) {
                Text("I did it")
            }
        }
    }
}

@Composable
fun ImmediateFeedbackCard(feedback: ImmediateFeedback, onDismiss: () -> Unit) {
    val txtColor = if (feedback.isPositive) Good else Bad
    AnimatedVisibility(visible = true) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = txtColor.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (feedback.isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = txtColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(feedback.title, fontWeight = FontWeight.Bold, color = txtColor, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(feedback.immediateConsequence, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (feedback.timesThisWeek > 1) {
                    Text("${feedback.timesThisWeek}× in the last 7 days", style = MaterialTheme.typography.bodySmall)
                }
                if (feedback.estimatedWeightChange != 0f) {
                    Text(
                        "At this rate: ${if (feedback.estimatedWeightChange > 0) "+" else ""}${"%.1f".format(feedback.estimatedWeightChange * 4)} kg/month",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Warn
                    )
                }
            }
        }
    }
}

@Composable
fun ConsequenceDialog(type: DecisionType, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = if (type.defaultImpact > 0) Good else Bad) },
        title = { Text(type.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ConsequenceLine("Right now", type.immediateConsequence)
                ConsequenceLine("In 7 days", type.shortTerm7Day)
                ConsequenceLine("In 30 days", type.longTerm30Day)
                if (type.defaultImpact < 0 && type.recoveryAction.isNotBlank()) {
                    ConsequenceLine("Recovery", type.recoveryAction)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

@Composable
private fun ConsequenceLine(label: String, text: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun IdentitySelectionDialog(current: FutureIdentity, onSelect: (FutureIdentity) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Who do you want to become?") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FutureIdentity.entries.toList()) { identity ->
                    val isSel = current == identity
                    Card(
                        onClick = { onSelect(identity) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (identity) {
                                    FutureIdentity.FIT_SELF -> "💪"
                                    FutureIdentity.RICH_SELF -> "💰"
                                    FutureIdentity.DISCIPLINED_SELF -> "🎯"
                                    FutureIdentity.HAPPY_SELF -> "😊"
                                    FutureIdentity.GROWING_SELF -> "📚"
                                    FutureIdentity.NO_IDENTITY -> "⭕"
                                },
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (identity == FutureIdentity.NO_IDENTITY) "No identity" else identity.displayName,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(identity.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSel) Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
