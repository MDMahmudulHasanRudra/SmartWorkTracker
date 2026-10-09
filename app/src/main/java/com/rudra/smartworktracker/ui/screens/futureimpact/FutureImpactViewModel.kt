package com.rudra.smartworktracker.ui.screens.futureimpact

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.BehaviorEngineRepository
import com.rudra.smartworktracker.model.CheckInType
import com.rudra.smartworktracker.model.ConsequenceDebt
import com.rudra.smartworktracker.model.DailyCheckIn
import com.rudra.smartworktracker.model.Decision
import com.rudra.smartworktracker.model.DecisionCategory
import com.rudra.smartworktracker.model.DecisionType
import com.rudra.smartworktracker.model.FutureIdentity
import com.rudra.smartworktracker.model.UserHistory
import com.rudra.smartworktracker.model.WeeklyReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.concurrent.TimeUnit

data class FutureImpactUiState(
    val todayDecisions: List<Decision> = emptyList(),
    val impactStats: ImpactStats = ImpactStats(),
    val patternWarnings: List<PatternWarning> = emptyList(),
    val streaks: StreakInfo = StreakInfo(),
    val dailyScore: Int = 50,
    val identity: FutureIdentity = FutureIdentity.NO_IDENTITY,
    val identityMessage: String = "",
    val identityAligned: Boolean = true,
    val debts: List<ConsequenceDebt> = emptyList(),
    val totalDebt: Float = 0f,
    val userHistory: UserHistory? = null,
    val latestReport: WeeklyReport? = null,
    val projection: FutureProjection = FutureProjection(),
    val morningCheckIn: DailyCheckIn? = null,
    val nightCheckIn: DailyCheckIn? = null,
    val isLoading: Boolean = true
)

/**
 * Decision journal with consequence projections. Everything is derived from one rolling query
 * of recent decisions, with day boundaries computed on each emission (the old version fixed the
 * query's end time when the screen opened, so newly logged decisions never showed up).
 */
class FutureImpactViewModel(
    private val behaviorRepo: BehaviorEngineRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val _selectedIdentity = MutableStateFlow(loadIdentity())
    val selectedIdentity: StateFlow<FutureIdentity> = _selectedIdentity.asStateFlow()

    private val recentDecisions: StateFlow<List<Decision>> = behaviorRepo
        .getDecisionsInRange(LocalDate.now().minusDays(HISTORY_DAYS).atStartOfDay(zone).toInstant().toEpochMilli(), Long.MAX_VALUE)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val checkIns = combine(
        behaviorRepo.getTodayCheckIn(CheckInType.MORNING.name),
        behaviorRepo.getTodayCheckIn(CheckInType.NIGHT.name)
    ) { morning, night -> morning to night }

    private val stored = combine(
        behaviorRepo.getAllDebts(),
        behaviorRepo.getTotalDebt(),
        behaviorRepo.getUserHistory(),
        behaviorRepo.getLatestReport()
    ) { debts, total, history, report -> StoredData(debts, total ?: 0f, history, report) }

    val uiState: StateFlow<FutureImpactUiState> = combine(
        recentDecisions, _selectedIdentity, stored, checkIns
    ) { decisions, identity, data, (morning, night) ->
        val today = LocalDate.now(zone)
        val todayList = decisions.filter { dayOf(it) == today }
        val week = decisions.filter { !dayOf(it).isBefore(today.minusDays(6)) }
        val streaks = calculateStreaks(decisions, today)
        val stats = calculateImpact(week, decisions)
        val (alignment, aligned, message) = identityAlignment(identity, todayList)
        FutureImpactUiState(
            todayDecisions = todayList,
            impactStats = stats,
            patternWarnings = detectPatterns(week, streaks),
            streaks = streaks,
            dailyScore = calculateDailyScore(todayList, streaks, stats, alignment),
            identity = identity,
            identityMessage = message,
            identityAligned = aligned,
            debts = data.debts,
            totalDebt = data.totalDebt,
            userHistory = data.history,
            latestReport = data.report,
            projection = calculateFutureProjection(week),
            morningCheckIn = morning,
            nightCheckIn = night,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FutureImpactUiState())

    private val _immediateFeedback = MutableStateFlow<ImmediateFeedback?>(null)
    val immediateFeedback: StateFlow<ImmediateFeedback?> = _immediateFeedback.asStateFlow()
    private var feedbackJob: Job? = null

    private val _undoWindow = MutableStateFlow<UndoWindow?>(null)
    val undoWindow: StateFlow<UndoWindow?> = _undoWindow.asStateFlow()
    private var undoJob: Job? = null

    private val _showCheckInPrompt = MutableStateFlow(false)
    val showCheckInPrompt: StateFlow<Boolean> = _showCheckInPrompt.asStateFlow()

    private val _events = MutableSharedFlow<FutureImpactEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<FutureImpactEvent> = _events.asSharedFlow()

    private var lastDeleted: Decision? = null

    init {
        viewModelScope.launch {
            behaviorRepo.initUserHistory()
            maybePromptCheckIn()
            maybeGenerateLastWeekReport()
        }
    }

    // ---------------------------------------------------------------- check-ins

    /** Prompts once per window per day, and never when that check-in is already done. */
    private suspend fun maybePromptCheckIn() {
        val type = getCheckInType() ?: return
        val today = LocalDate.now(zone).toString()
        if (prefs.getString(KEY_PROMPT_DISMISSED + type.name, null) == today) return
        val done = behaviorRepo.getTodayCheckIn(type.name).first() != null
        _showCheckInPrompt.value = !done
    }

    fun dismissCheckInPrompt() {
        getCheckInType()?.let {
            prefs.edit().putString(KEY_PROMPT_DISMISSED + it.name, LocalDate.now(zone).toString()).apply()
        }
        _showCheckInPrompt.value = false
    }

    fun saveCheckIn(type: CheckInType, mood: Int, answer: String) {
        viewModelScope.launch {
            val existing = behaviorRepo.getTodayCheckIn(type.name).first()
            val checkIn = DailyCheckIn(
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                checkInType = type,
                morningMood = if (type == CheckInType.MORNING) mood else 0,
                morningAnswer = if (type == CheckInType.MORNING) answer.trim() else "",
                nightMood = if (type == CheckInType.NIGHT) mood else 0,
                nightAnswer = if (type == CheckInType.NIGHT) answer.trim() else "",
                completed = true
            )
            behaviorRepo.saveCheckIn(checkIn)
            _showCheckInPrompt.value = false
            _events.emit(FutureImpactEvent.Message("${if (type == CheckInType.MORNING) "Morning" else "Night"} check-in saved"))
        }
    }

    fun saveMorningCheckIn(mood: Int, answer: String) = saveCheckIn(CheckInType.MORNING, mood, answer)

    fun saveNightCheckIn(mood: Int, answer: String) = saveCheckIn(CheckInType.NIGHT, mood, answer)

    /** Morning window 05:00-11:59, night window 19:00-23:59. */
    fun getCheckInType(): CheckInType? = when (LocalTime.now(zone).hour) {
        in 5..11 -> CheckInType.MORNING
        in 19..23 -> CheckInType.NIGHT
        else -> null
    }

    // ---------------------------------------------------------------- identity

    fun setIdentity(identity: FutureIdentity) {
        _selectedIdentity.value = identity
        prefs.edit().putString(KEY_IDENTITY, identity.name).apply()
    }

    private fun loadIdentity(): FutureIdentity {
        val name = prefs.getString(KEY_IDENTITY, null) ?: return FutureIdentity.NO_IDENTITY
        return runCatching { FutureIdentity.valueOf(name) }.getOrDefault(FutureIdentity.NO_IDENTITY)
    }

    /** Returns (score bonus -10..10, aligned?, message) for today's decisions. */
    private fun identityAlignment(identity: FutureIdentity, today: List<Decision>): Triple<Int, Boolean, String> {
        if (identity == FutureIdentity.NO_IDENTITY) return Triple(0, true, "")
        val relevant = today.filter { it.category in identity.targetCategories }
        if (relevant.isEmpty()) return Triple(0, true, "")
        val good = relevant.count { it.isPositive }
        val bad = relevant.size - good
        val bonus = ((good - bad).toFloat() / relevant.size * 10).toInt()
        val aligned = good >= bad
        return Triple(bonus, aligned, if (aligned) identity.positivePhrase else identity.negativePhrase)
    }

    fun getIdentityMessage(): String = uiState.value.identityMessage

    // ---------------------------------------------------------------- decisions

    fun addDecision(
        decisionType: DecisionType,
        customTitle: String = "",
        notes: String = "",
        isPositive: Boolean? = null,
        category: DecisionCategory? = null
    ) {
        viewModelScope.launch {
            val decision = Decision(
                decisionType = decisionType,
                category = category ?: decisionType.category,
                customTitle = customTitle.trim(),
                notes = notes.trim(),
                isPositive = isPositive ?: (decisionType.defaultImpact > 0)
            )
            // Count from the snapshot before the insert lands, plus this one
            val weekStart = LocalDate.now(zone).minusDays(6)
            val timesThisWeek = recentDecisions.value.count {
                it.decisionType == decisionType && !dayOf(it).isBefore(weekStart)
            } + 1
            behaviorRepo.addDecision(decision)
            showImmediateFeedback(decision, timesThisWeek)
            if (!decision.isPositive && decisionType.recoveryAction.isNotBlank()) startUndoWindow(decision)
        }
    }

    private fun showImmediateFeedback(decision: Decision, timesThisWeek: Int) {
        val type = decision.decisionType
        feedbackJob?.cancel()
        _immediateFeedback.value = ImmediateFeedback(
            decisionType = type,
            isPositive = decision.isPositive,
            title = decision.displayTitle(),
            timesThisWeek = timesThisWeek,
            immediateConsequence = if (type == DecisionType.CUSTOM) {
                if (decision.isPositive) "A step toward your future self." else "Noted. Small slips add up — plan a recovery."
            } else type.immediateConsequence,
            estimatedWeightChange = type.estimatedWeightChange * timesThisWeek,
            energyImpact = type.energyImpact
        )
        feedbackJob = viewModelScope.launch {
            delay(6000)
            _immediateFeedback.value = null
        }
    }

    fun dismissFeedback() {
        feedbackJob?.cancel()
        _immediateFeedback.value = null
    }

    private fun startUndoWindow(decision: Decision) {
        val type = decision.decisionType
        val window = UndoWindow(
            decision = decision,
            recoveryAction = type.recoveryAction,
            recoveryAction2 = type.recoveryAction2,
            expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(RECOVERY_MINUTES)
        )
        _undoWindow.value = window
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            delay(window.expiresAt - System.currentTimeMillis())
            if (_undoWindow.value?.decision?.id == decision.id) _undoWindow.value = null
        }
    }

    fun executeRecovery() {
        val undo = _undoWindow.value ?: return
        undoJob?.cancel()
        _undoWindow.value = null
        viewModelScope.launch {
            behaviorRepo.addDecision(
                Decision(
                    decisionType = getRecoveryType(undo.recoveryAction),
                    notes = "Recovery: ${undo.recoveryAction}"
                )
            )
            behaviorRepo.reduceDebt(undo.decision.category, RECOVERY_BONUS)
            _events.emit(FutureImpactEvent.Message("Recovery logged — debt reduced"))
        }
    }

    private fun getRecoveryType(action: String): DecisionType = when {
        listOf("pushup", "walk", "exercise", "stretch").any { action.contains(it, ignoreCase = true) } -> DecisionType.EXERCISE
        listOf("water", "protein", "fruit", "vegetable").any { action.contains(it, ignoreCase = true) } -> DecisionType.EAT_HEALTHY
        listOf("nap", "sleep").any { action.contains(it, ignoreCase = true) } -> DecisionType.SLEEP_EARLY
        listOf("transfer", "save").any { action.contains(it, ignoreCase = true) } -> DecisionType.SAVE_MONEY
        listOf("task", "start").any { action.contains(it, ignoreCase = true) } -> DecisionType.WORK_FOCUS
        listOf("breath", "grateful", "screen", "phone").any { action.contains(it, ignoreCase = true) } -> DecisionType.MEDITATE
        listOf("text", "call").any { action.contains(it, ignoreCase = true) } -> DecisionType.MEET_FRIENDS
        listOf("plan", "read").any { action.contains(it, ignoreCase = true) } -> DecisionType.SET_GOALS
        else -> DecisionType.WORK_FOCUS
    }

    fun dismissUndoWindow() {
        undoJob?.cancel()
        _undoWindow.value = null
    }

    fun deleteDecision(decision: Decision) {
        viewModelScope.launch {
            behaviorRepo.deleteDecision(decision)
            if (_undoWindow.value?.decision?.id == decision.id) dismissUndoWindow()
            lastDeleted = decision
            _events.emit(FutureImpactEvent.Deleted(decision.displayTitle()))
        }
    }

    fun deleteDecision(id: String) {
        recentDecisions.value.firstOrNull { it.id == id }?.let(::deleteDecision)
    }

    fun undoDelete() {
        val decision = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { behaviorRepo.addDecision(decision) }
    }

    // ---------------------------------------------------------------- weekly report

    fun generateWeeklyReport() {
        viewModelScope.launch {
            val start = weekStart(LocalDate.now(zone))
            generateReportFor(start, start.plusWeeks(1))
            _events.emit(FutureImpactEvent.Message("Weekly report updated"))
        }
    }

    /** Writes last week's report once, the first time the screen is opened in a new week. */
    private suspend fun maybeGenerateLastWeekReport() {
        val lastWeekStart = weekStart(LocalDate.now(zone)).minusWeeks(1)
        val latest = behaviorRepo.getLatestReport().first()
        val lastWeekMillis = lastWeekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        if (latest != null && latest.weekStartDate >= lastWeekMillis) return
        generateReportFor(lastWeekStart, lastWeekStart.plusWeeks(1), skipIfEmpty = true)
    }

    private suspend fun generateReportFor(start: LocalDate, endExclusive: LocalDate, skipIfEmpty: Boolean = false) {
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val decisions = behaviorRepo.getDecisionsInRange(startMillis, endMillis).first()
        if (skipIfEmpty && decisions.isEmpty()) return
        val dailyScores = decisions.groupBy(::dayOf).values.map { dayDecisions ->
            (dayDecisions.count { it.isPositive } * 100 / dayDecisions.size)
        }
        val identity = _selectedIdentity.value
        val alignment = if (identity == FutureIdentity.NO_IDENTITY) 0 else {
            val relevant = decisions.filter { it.category in identity.targetCategories }
            if (relevant.isEmpty()) 0 else relevant.count { it.isPositive } * 100 / relevant.size
        }
        val streaks = calculateStreaks(decisions, endExclusive.minusDays(1))
        behaviorRepo.generateWeeklyReport(
            weekStart = startMillis,
            weekEnd = endMillis,
            decisions = decisions,
            avgScore = if (dailyScores.isEmpty()) 0 else dailyScores.average().toInt(),
            identityAlignment = alignment,
            disciplineStreak = streaks.maxDisciplineStreak,
            damageStreak = streaks.maxDamageStreak
        )
    }

    // ---------------------------------------------------------------- calculations

    private fun calculateImpact(weekDecisions: List<Decision>, monthDecisions: List<Decision>): ImpactStats =
        ImpactStats(
            weekPositive = weekDecisions.count { it.isPositive },
            weekNegative = weekDecisions.count { !it.isPositive },
            weekTotalImpact = weekDecisions.sumOf { it.decisionType.defaultImpact.toInt() },
            monthPositive = monthDecisions.count { it.isPositive },
            monthNegative = monthDecisions.count { !it.isPositive },
            monthTotalImpact = monthDecisions.sumOf { it.decisionType.defaultImpact.toInt() },
            weekDecisionsByCategory = DecisionCategory.entries.associateWith { c -> weekDecisions.count { it.category == c } },
            monthDecisionsByCategory = DecisionCategory.entries.associateWith { c -> monthDecisions.count { it.category == c } }
        )

    /**
     * Day-based streaks: a "discipline day" has more positive than negative decisions, a
     * "damage day" the reverse. The current streak runs back from today (or yesterday when
     * nothing is logged yet today).
     */
    private fun calculateStreaks(decisions: List<Decision>, today: LocalDate): StreakInfo {
        if (decisions.isEmpty()) return StreakInfo()
        val balanceByDay = decisions.groupBy(::dayOf).mapValues { (_, list) ->
            list.count { it.isPositive } - list.count { !it.isPositive }
        }
        fun sign(day: LocalDate): Int = balanceByDay[day]?.let { Integer.signum(it) } ?: 0

        var day = if (balanceByDay.containsKey(today)) today else today.minusDays(1)
        val currentSign = sign(day)
        var current = 0
        while (currentSign != 0 && sign(day) == currentSign) {
            current++
            day = day.minusDays(1)
        }

        var maxDiscipline = 0
        var maxDamage = 0
        var run = 0
        var runSign = 0
        val first = balanceByDay.keys.min()
        var cursor = first
        while (!cursor.isAfter(today)) {
            val s = sign(cursor)
            run = if (s != 0 && s == runSign) run + 1 else if (s != 0) 1 else 0
            runSign = s
            if (s > 0) maxDiscipline = maxOf(maxDiscipline, run)
            if (s < 0) maxDamage = maxOf(maxDamage, run)
            cursor = cursor.plusDays(1)
        }

        val discipline = if (currentSign > 0) current else 0
        val damage = if (currentSign < 0) current else 0
        return StreakInfo(
            currentDisciplineStreak = discipline,
            currentDamageStreak = damage,
            maxDisciplineStreak = maxDiscipline,
            maxDamageStreak = maxDamage,
            isOnDamageStreak = damage >= 2,
            isOnDisciplineStreak = discipline >= 2
        )
    }

    private fun calculateDailyScore(today: List<Decision>, streaks: StreakInfo, stats: ImpactStats, identityBonus: Int): Int {
        if (today.isEmpty()) return 50
        var score = 50
        val positiveRatio = today.count { it.isPositive }.toFloat() / today.size
        score += ((positiveRatio - 0.5f) * 40).toInt()
        score += streaks.currentDisciplineStreak.coerceAtMost(5) * 3
        score -= streaks.currentDamageStreak.coerceAtMost(5) * 3
        score += ((stats.weekBalance - 50) * 0.3f).toInt()
        score += identityBonus
        return score.coerceIn(0, 100)
    }

    private fun detectPatterns(week: List<Decision>, streak: StreakInfo): List<PatternWarning> {
        val warnings = mutableListOf<PatternWarning>()
        val negativeCount = week.count { !it.isPositive }
        val positiveCount = week.count { it.isPositive }

        if (streak.currentDamageStreak >= 3) {
            warnings += PatternWarning(
                title = "${streak.currentDamageStreak}-day damage streak",
                description = "Negative choices have outweighed positive ones ${streak.currentDamageStreak} days in a row.",
                severity = PatternSeverity.HIGH,
                advice = "Habits harden the longer a streak runs. One deliberate positive choice today breaks it."
            )
        }

        if (negativeCount >= 3 && negativeCount > positiveCount * 2) {
            warnings += PatternWarning(
                title = "Negative pattern this week",
                description = "$negativeCount negative vs $positiveCount positive decisions in the last 7 days.",
                severity = PatternSeverity.HIGH,
                advice = "Aim for at least one positive decision every day."
            )
        }

        // Area with the most negative decisions (positives used to trigger this too)
        val worst = week.filter { !it.isPositive }.groupBy { it.category }.maxByOrNull { it.value.size }
        if (worst != null && worst.value.size >= 3) {
            val impact = when (worst.key) {
                DecisionCategory.HEALTH -> "Your health is taking the hit."
                DecisionCategory.FINANCE -> "These spending choices are costing your future."
                DecisionCategory.PRODUCTIVITY -> "Focus is slipping."
                DecisionCategory.SOCIAL -> "Relationships need attention."
                DecisionCategory.MENTAL_HEALTH -> "Stress is accumulating."
                DecisionCategory.PERSONAL -> "Personal growth is stalled."
                DecisionCategory.OTHER -> "A negative pattern is forming."
            }
            warnings += PatternWarning(
                title = "${worst.key.displayName} needs attention",
                description = "${worst.value.size} negative ${worst.key.displayName.lowercase()} decisions this week.",
                severity = if (worst.value.size >= 5) PatternSeverity.HIGH else PatternSeverity.MEDIUM,
                advice = impact
            )
        }

        if (streak.currentDisciplineStreak >= 3) {
            warnings += PatternWarning(
                title = "${streak.currentDisciplineStreak}-day discipline streak",
                description = "Positive choices have won ${streak.currentDisciplineStreak} days running.",
                severity = PatternSeverity.LOW,
                advice = "Keep it going — consistency is what compounds."
            )
        }
        return warnings
    }

    private fun calculateFutureProjection(decisions: List<Decision>): FutureProjection {
        if (decisions.isEmpty()) {
            return FutureProjection(message = "No data yet. Log a few decisions to see where you're heading.")
        }
        val positiveRatio = decisions.count { it.isPositive }.toFloat() / decisions.size
        val currentScore = (positiveRatio * 100).toInt()

        // Decisions arrive newest first, so take(10) is the most recent behaviour
        val recent = decisions.take(10)
        val recentRatio = recent.count { it.isPositive }.toFloat() / recent.size
        val trend = when {
            recentRatio > 0.6f -> Trend.IMPROVING
            recentRatio < 0.4f -> Trend.DECLINING
            else -> Trend.STABLE
        }

        val step = (recentRatio - 0.5f) * 20
        val week1 = (currentScore + step).coerceIn(0f, 100f)
        val week2 = (week1 + step * 0.8f).coerceIn(0f, 100f)
        val week3 = (week2 + step * 0.6f).coerceIn(0f, 100f)
        val week4 = (week3 + step * 0.4f).coerceIn(0f, 100f)

        val message = when (trend) {
            Trend.IMPROVING -> "Great trajectory. Keep this up and your future self will thank you."
            Trend.DECLINING -> "Concerning path. Without a change, the next weeks trend down."
            Trend.STABLE -> "Stable but not growing. Small improvements compound over time."
        }
        return FutureProjection(currentScore, week1.toInt(), week2.toInt(), week3.toInt(), week4.toInt(), trend, message)
    }

    fun predictFuture(decisions: List<Decision>): String {
        if (decisions.isEmpty()) return "No data to predict future."
        val positiveRatio = decisions.count { it.isPositive }.toFloat() / decisions.size
        return when {
            positiveRatio >= 0.7f -> "Excellent! Building habits that compound."
            positiveRatio >= 0.5f -> "Good direction! Steady progress ahead."
            positiveRatio >= 0.3f -> "Concerning: negative outweighs positive."
            else -> "Critical: destructive patterns. Time for a reset."
        }
    }

    private fun dayOf(decision: Decision): LocalDate =
        Instant.ofEpochMilli(decision.createdAt).atZone(zone).toLocalDate()

    private fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))

    private data class StoredData(
        val debts: List<ConsequenceDebt>,
        val totalDebt: Float,
        val history: UserHistory?,
        val report: WeeklyReport?
    )

    companion object {
        private const val KEY_IDENTITY = "future_identity"
        private const val KEY_PROMPT_DISMISSED = "checkin_prompt_dismissed_"
        private const val HISTORY_DAYS = 35L
        private const val RECOVERY_MINUTES = 30L
        private const val RECOVERY_BONUS = 5f
    }
}

/** Custom decisions show the user's own title. */
fun Decision.displayTitle(): String = customTitle.ifBlank { decisionType.displayName }

sealed interface FutureImpactEvent {
    data class Message(val text: String) : FutureImpactEvent
    data class Deleted(val title: String) : FutureImpactEvent
}

data class ImpactStats(
    val weekPositive: Int = 0,
    val weekNegative: Int = 0,
    val weekTotalImpact: Int = 0,
    val monthPositive: Int = 0,
    val monthNegative: Int = 0,
    val monthTotalImpact: Int = 0,
    val weekDecisionsByCategory: Map<DecisionCategory, Int> = emptyMap(),
    val monthDecisionsByCategory: Map<DecisionCategory, Int> = emptyMap()
) {
    val weekNetImpact: Int get() = weekPositive - weekNegative
    val monthNetImpact: Int get() = monthPositive - monthNegative
    val weekBalance: Float get() = if (weekPositive + weekNegative > 0)
        (weekPositive.toFloat() / (weekPositive + weekNegative)) * 100 else 50f
    val monthBalance: Float get() = if (monthPositive + monthNegative > 0)
        (monthPositive.toFloat() / (monthPositive + monthNegative)) * 100 else 50f
}

data class StreakInfo(
    val currentDisciplineStreak: Int = 0,
    val currentDamageStreak: Int = 0,
    val maxDisciplineStreak: Int = 0,
    val maxDamageStreak: Int = 0,
    val isOnDamageStreak: Boolean = false,
    val isOnDisciplineStreak: Boolean = false
)

data class ImmediateFeedback(
    val decisionType: DecisionType,
    val isPositive: Boolean = decisionType.defaultImpact > 0,
    val title: String = decisionType.displayName,
    val timesThisWeek: Int,
    val immediateConsequence: String,
    val estimatedWeightChange: Float,
    val energyImpact: Int
)

data class UndoWindow(
    val decision: Decision,
    val recoveryAction: String,
    val recoveryAction2: String,
    val expiresAt: Long
)

data class PatternWarning(
    val title: String,
    val description: String,
    val severity: PatternSeverity,
    val advice: String
)

data class FutureProjection(
    val currentScore: Int = 50,
    val week1Score: Int = 50,
    val week2Score: Int = 50,
    val week3Score: Int = 50,
    val week4Score: Int = 50,
    val trend: Trend = Trend.STABLE,
    val message: String = ""
)

enum class Trend { IMPROVING, DECLINING, STABLE }

enum class PatternSeverity { LOW, MEDIUM, HIGH }

class FutureImpactViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FutureImpactViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            val behaviorRepo = BehaviorEngineRepository(
                db.decisionDao(),
                db.checkInDao(),
                db.consequenceDebtDao(),
                db.weeklyReportDao(),
                db.userHistoryDao()
            )
            val prefs = application.getSharedPreferences("future_self_prefs", Context.MODE_PRIVATE)
            @Suppress("UNCHECKED_CAST")
            return FutureImpactViewModel(behaviorRepo, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
