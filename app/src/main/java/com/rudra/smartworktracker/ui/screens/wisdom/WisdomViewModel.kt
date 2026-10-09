package com.rudra.smartworktracker.ui.screens.wisdom

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.UserStatsEntity
import com.rudra.smartworktracker.data.repository.WisdomRepository
import com.rudra.smartworktracker.model.Goal
import com.rudra.smartworktracker.model.Target
import com.rudra.smartworktracker.model.Wisdom
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.roundToInt

enum class BoosterType(val label: String, val multiplier: Float) {
    XP_2X("2x XP", 2.0f),
    XP_1_5X("1.5x XP", 1.5f),
    TARGET_SPEED("Target Speed", 1.0f)
}

data class Booster(
    val id: String = UUID.randomUUID().toString(),
    val type: BoosterType,
    val durationHours: Int,
    val activatedAt: LocalDateTime? = null
) {
    val expiresAt: LocalDateTime? get() = activatedAt?.plusHours(durationHours.toLong())
}

data class UserStats(
    val experiencePoints: Int = 0,
    val level: Int = 1,
    val streak: Int = 1,
    val totalGoalsCompleted: Int = 0,
    val lastActiveDate: LocalDate = LocalDate.now(),
    val streakProtectionAvailable: Int = 0,
    val xpMultiplier: Float = 1.0f
) {
    val xpForNextLevel: Int get() = WisdomViewModel.xpForLevel(level)
}

data class WisdomUiState(
    val goals: List<Goal> = emptyList(),
    val targetsMap: Map<String, List<Target>> = emptyMap(),
    val selectedGoal: Goal? = null,
    val showGoalCelebration: Boolean = false,
    val showTargetCelebration: Boolean = false,
    val lastAchievedItemName: String = "",
    val lastXpGained: Int = 0,
    val userStats: UserStats = UserStats(),
    val inventoryBoosters: List<Booster> = emptyList(),
    val activeBoosters: List<Booster> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

/** State that lives only in memory (or prefs), merged with the Room-backed goals and stats. */
private data class LocalState(
    val selectedGoalId: String? = null,
    val showGoalCelebration: Boolean = false,
    val showTargetCelebration: Boolean = false,
    val lastAchievedItemName: String = "",
    val lastXpGained: Int = 0,
    val inventoryBoosters: List<Booster> = emptyList(),
    val activeBoosters: List<Booster> = emptyList(),
    val message: String? = null
)

/**
 * Life plan + gamification. Goals, targets and XP stats are stored in Room (they used to be
 * hard-coded demo data that vanished on restart); boosters are kept in SharedPreferences.
 * All writes run under one mutex so quick taps can't interleave read-modify-write cycles.
 */
class WisdomViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).lifePlanDao()
    private val repository = WisdomRepository()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val local = MutableStateFlow(LocalState())
    private val boosterJobs = mutableMapOf<String, Job>()
    private var targetCelebrationJob: Job? = null
    private var goalCelebrationJob: Job? = null

    val uiState: StateFlow<WisdomUiState> = combine(
        dao.getAllGoals(),
        dao.getAllTargets(),
        dao.getUserStats(),
        local
    ) { goals, targets, stats, state ->
        WisdomUiState(
            // Open goals first, newest first within each group
            goals = goals.sortedWith(compareBy<Goal> { it.isCompleted }.thenByDescending { it.createdAt }),
            targetsMap = targets.groupBy { it.goalId },
            selectedGoal = goals.firstOrNull { it.id == state.selectedGoalId },
            showGoalCelebration = state.showGoalCelebration,
            showTargetCelebration = state.showTargetCelebration,
            lastAchievedItemName = state.lastAchievedItemName,
            lastXpGained = state.lastXpGained,
            userStats = (stats?.toUserStats() ?: UserStats()).copy(xpMultiplier = multiplierOf(state.activeBoosters)),
            inventoryBoosters = state.inventoryBoosters,
            activeBoosters = state.activeBoosters,
            isLoading = false,
            message = state.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WisdomUiState())

    init {
        loadBoosters()
        launchLocked { checkStreak() }
    }

    fun getWisdom(): List<Wisdom> = repository.getWisdom()

    fun quoteOfTheDay(): Wisdom = repository.quoteOfTheDay()

    fun clearMessage() = local.update { it.copy(message = null) }

    fun selectGoal(goal: Goal?) = local.update { it.copy(selectedGoalId = goal?.id) }

    // ---------------------------------------------------------------- goals & targets

    fun addGoal(goal: Goal) = launchLocked {
        if (goal.title.isBlank()) return@launchLocked
        dao.insertGoal(
            goal.copy(
                title = goal.title.trim(),
                description = goal.description.trim(),
                totalTargets = 0,
                completedTargets = 0,
                isCompleted = false
            )
        )
        local.update { it.copy(selectedGoalId = goal.id, message = "Goal added. Break it into targets below.") }
    }

    fun deleteGoal(goalId: String) = launchLocked {
        // Targets first, so nothing is left behind even if foreign keys are off
        dao.deleteTargetsByGoal(goalId)
        dao.deleteGoal(goalId)
        local.update {
            it.copy(
                selectedGoalId = if (it.selectedGoalId == goalId) null else it.selectedGoalId,
                message = "Goal deleted"
            )
        }
    }

    fun addTargetToGoal(goalId: String, title: String) = launchLocked {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return@launchLocked
        val existing = dao.getTargetsForGoal(goalId).first()
        dao.insertTarget(
            Target(
                id = UUID.randomUUID().toString(),
                goalId = goalId,
                title = trimmed,
                description = "",
                order = (existing.maxOfOrNull { it.order } ?: 0) + 1
            )
        )
        syncGoalProgress(goalId)
    }

    fun deleteTarget(goalId: String, targetId: String) = launchLocked {
        dao.deleteTarget(targetId)
        syncGoalProgress(goalId)
    }

    fun completeTarget(goalId: String, targetId: String) = launchLocked {
        val target = dao.getTargetsForGoal(goalId).first().firstOrNull { it.id == targetId } ?: return@launchLocked
        if (target.isCompleted) return@launchLocked
        dao.insertTarget(target.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        val (before, after) = syncGoalProgress(goalId) ?: return@launchLocked

        var stats = statsOrSeed()
        val levelBefore = stats.level
        val xpBefore = totalXp(stats)
        stats = withXp(stats, TARGET_XP)
        val goalCompleted = after.isCompleted && !before.isCompleted
        if (goalCompleted) {
            stats = withXp(stats, GOAL_XP).copy(totalGoalsCompleted = stats.totalGoalsCompleted + 1)
        }
        stats = stats.copy(lastActiveDate = LocalDate.now().toString())
        dao.saveUserStats(stats)

        val gained = totalXp(stats) - xpBefore
        if (goalCompleted) celebrateGoal(after.title, gained) else celebrateTarget(target.title, gained)
        if (stats.level > levelBefore) {
            local.update { it.copy(message = "Level up! You reached level ${stats.level}") }
        }
    }

    /** Recounts a goal's targets; returns the goal before and after the update. */
    private suspend fun syncGoalProgress(goalId: String): Pair<Goal, Goal>? {
        val goal = dao.getGoalById(goalId) ?: return null
        val targets = dao.getTargetsForGoal(goalId).first()
        val completed = targets.count { it.isCompleted }
        val updated = goal.copy(
            totalTargets = targets.size,
            completedTargets = completed,
            isCompleted = targets.isNotEmpty() && completed == targets.size
        )
        // updateGoal (not insert/REPLACE), which would cascade-delete the goal's targets
        if (updated != goal) dao.updateGoal(updated)
        return goal to updated
    }

    private fun celebrateTarget(name: String, xp: Int) {
        targetCelebrationJob?.cancel()
        targetCelebrationJob = viewModelScope.launch {
            local.update { it.copy(showTargetCelebration = true, lastAchievedItemName = name, lastXpGained = xp) }
            delay(2500)
            local.update { it.copy(showTargetCelebration = false) }
        }
    }

    private fun celebrateGoal(name: String, xp: Int) {
        goalCelebrationJob?.cancel()
        goalCelebrationJob = viewModelScope.launch {
            local.update { it.copy(showGoalCelebration = true, lastAchievedItemName = name, lastXpGained = xp) }
            delay(4000)
            local.update { it.copy(showGoalCelebration = false) }
        }
    }

    fun dismissGoalCelebration() {
        goalCelebrationJob?.cancel()
        local.update { it.copy(showGoalCelebration = false) }
    }

    // ---------------------------------------------------------------- streak & XP

    /**
     * Daily visit streak. A missed day can be covered by a streak protection (one per missed
     * day); otherwise the streak restarts at 1.
     */
    private suspend fun checkStreak() {
        val today = LocalDate.now()
        val stats = dao.getUserStats().first()
        if (stats == null) {
            dao.saveUserStats(UserStatsEntity(lastActiveDate = today.toString()))
            return
        }
        val last = parseDate(stats.lastActiveDate)
        if (last == null || !last.isBefore(today)) {
            if (last == null) dao.saveUserStats(stats.copy(lastActiveDate = today.toString()))
            return
        }
        val missedDays = (ChronoUnit.DAYS.between(last, today) - 1).toInt()
        val continued = when {
            missedDays == 0 -> stats
            stats.streakProtectionAvailable >= missedDays -> {
                local.update { it.copy(message = "Streak saved with $missedDays protection${if (missedDays == 1) "" else "s"}") }
                stats.copy(streakProtectionAvailable = stats.streakProtectionAvailable - missedDays)
            }
            else -> null
        }
        val updated = if (continued == null) {
            stats.copy(streak = 1, lastActiveDate = today.toString())
        } else {
            val streak = continued.streak + 1
            var next = withXp(continued.copy(streak = streak, lastActiveDate = today.toString()), STREAK_XP)
            when (streak) {
                7 -> next = withXp(next, 100)
                30 -> next = withXp(next, 500).copy(streakProtectionAvailable = next.streakProtectionAvailable + 1)
            }
            next
        }
        dao.saveUserStats(updated)
    }

    /** Adds XP (scaled by active boosters) and carries over into as many levels as it covers. */
    private fun withXp(stats: UserStatsEntity, baseXp: Int): UserStatsEntity {
        val gained = (baseXp * multiplierOf(activeBoostersNow())).roundToInt()
        var xp = stats.experiencePoints + gained
        var level = stats.level.coerceAtLeast(1)
        while (xp >= xpForLevel(level)) {
            xp -= xpForLevel(level)
            level++
        }
        return stats.copy(experiencePoints = xp, level = level)
    }

    private fun totalXp(stats: UserStatsEntity): Int =
        (1 until stats.level).sumOf { xpForLevel(it) } + stats.experiencePoints

    private suspend fun statsOrSeed(): UserStatsEntity =
        dao.getUserStats().first() ?: UserStatsEntity(lastActiveDate = LocalDate.now().toString())

    // ---------------------------------------------------------------- shop & boosters

    fun purchaseStreakProtection(amount: Int, cost: Int) = launchLocked {
        val stats = statsOrSeed()
        if (stats.experiencePoints < cost) {
            local.update { it.copy(message = "Not enough XP — complete targets to earn more") }
            return@launchLocked
        }
        dao.saveUserStats(
            stats.copy(
                experiencePoints = stats.experiencePoints - cost,
                streakProtectionAvailable = stats.streakProtectionAvailable + amount
            )
        )
        local.update { it.copy(message = "Added $amount streak protection${if (amount == 1) "" else "s"}") }
    }

    fun purchaseBooster(type: BoosterType, cost: Int) = launchLocked {
        val stats = statsOrSeed()
        if (stats.experiencePoints < cost) {
            local.update { it.copy(message = "Not enough XP — complete targets to earn more") }
            return@launchLocked
        }
        dao.saveUserStats(stats.copy(experiencePoints = stats.experiencePoints - cost))
        local.update {
            it.copy(
                inventoryBoosters = it.inventoryBoosters + Booster(type = type, durationHours = BOOSTER_HOURS),
                message = "${type.label} booster added to your inventory"
            )
        }
        saveBoosters()
    }

    fun activateBooster(booster: Booster) {
        val state = local.value
        if (state.inventoryBoosters.none { it.id == booster.id }) return
        if (activeBoostersNow().any { it.type == booster.type }) {
            local.update { it.copy(message = "A ${booster.type.label} booster is already running") }
            return
        }
        val activated = booster.copy(activatedAt = LocalDateTime.now())
        local.update {
            it.copy(
                inventoryBoosters = it.inventoryBoosters.filter { b -> b.id != booster.id },
                activeBoosters = it.activeBoosters + activated,
                message = "${booster.type.label} active for ${booster.durationHours}h"
            )
        }
        saveBoosters()
        scheduleExpiry(activated)
    }

    private fun scheduleExpiry(booster: Booster) {
        val expiresAt = booster.expiresAt ?: return
        boosterJobs.remove(booster.id)?.cancel()
        boosterJobs[booster.id] = viewModelScope.launch {
            val remaining = Duration.between(LocalDateTime.now(), expiresAt).toMillis()
            if (remaining > 0) delay(remaining)
            local.update { it.copy(activeBoosters = it.activeBoosters.filter { b -> b.id != booster.id }) }
            saveBoosters()
            boosterJobs.remove(booster.id)
        }
    }

    private fun activeBoostersNow(): List<Booster> {
        val now = LocalDateTime.now()
        return local.value.activeBoosters.filter { it.expiresAt?.isAfter(now) == true }
    }

    private fun loadBoosters() {
        val all = prefs.getString(KEY_BOOSTERS, null).orEmpty()
            .split(';')
            .mapNotNull(::decodeBooster)
        val now = LocalDateTime.now()
        val active = all.filter { it.expiresAt?.isAfter(now) == true }
        local.update {
            it.copy(inventoryBoosters = all.filter { b -> b.activatedAt == null }, activeBoosters = active)
        }
        active.forEach(::scheduleExpiry)
        saveBoosters() // drops anything that expired while the app was closed
    }

    private fun saveBoosters() {
        val state = local.value
        val encoded = (state.inventoryBoosters + state.activeBoosters).joinToString(";") { booster ->
            val activated = booster.activatedAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: -1L
            "${booster.id},${booster.type.name},${booster.durationHours},$activated"
        }
        prefs.edit().putString(KEY_BOOSTERS, encoded).apply()
    }

    private fun decodeBooster(raw: String): Booster? {
        val parts = raw.split(',')
        if (parts.size != 4) return null
        val type = runCatching { BoosterType.valueOf(parts[1]) }.getOrNull() ?: return null
        val hours = parts[2].toIntOrNull() ?: return null
        val activatedMillis = parts[3].toLongOrNull() ?: return null
        val activatedAt = if (activatedMillis < 0) null else
            LocalDateTime.ofInstant(Instant.ofEpochMilli(activatedMillis), ZoneId.systemDefault())
        return Booster(id = parts[0], type = type, durationHours = hours, activatedAt = activatedAt)
    }

    // ---------------------------------------------------------------- helpers

    private fun launchLocked(block: suspend () -> Unit) {
        viewModelScope.launch { mutex.withLock { block() } }
    }

    private fun multiplierOf(boosters: List<Booster>): Float =
        boosters.fold(1.0f) { acc, booster -> acc * booster.type.multiplier }

    private fun UserStatsEntity.toUserStats() = UserStats(
        experiencePoints = experiencePoints,
        level = level,
        streak = streak,
        totalGoalsCompleted = totalGoalsCompleted,
        lastActiveDate = parseDate(lastActiveDate) ?: LocalDate.now(),
        streakProtectionAvailable = streakProtectionAvailable,
        xpMultiplier = xpMultiplier
    )

    private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

    override fun onCleared() {
        boosterJobs.values.forEach { it.cancel() }
        super.onCleared()
    }

    companion object {
        private const val PREFS_NAME = "wisdom_boosters"
        private const val KEY_BOOSTERS = "boosters"
        const val TARGET_XP = 50
        const val GOAL_XP = 200
        const val STREAK_XP = 20
        const val BOOSTER_HOURS = 1

        fun xpForLevel(level: Int): Int = level.coerceAtLeast(1) * 500
    }
}
