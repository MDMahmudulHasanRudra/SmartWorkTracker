package com.rudra.smartworktracker.ui.screens.wisdom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.repository.WisdomRepository
import com.rudra.smartworktracker.model.Goal
import com.rudra.smartworktracker.model.GoalCategory
import com.rudra.smartworktracker.model.Target
import com.rudra.smartworktracker.model.Wisdom
import com.rudra.smartworktracker.model.WisdomCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

enum class BoosterType {
    XP_2X, XP_1_5X, TARGET_SPEED
}

data class Booster(
    val id: String = UUID.randomUUID().toString(),
    val type: BoosterType,
    val durationHours: Int,
    var activatedAt: LocalDateTime? = null
)

data class UserStats(
    val experiencePoints: Int = 0,
    val level: Int = 1,
    val streak: Int = 1,
    val totalGoalsCompleted: Int = 0,
    val lastActiveDate: LocalDate = LocalDate.now(),
    val streakProtectionAvailable: Int = 0,
    val xpMultiplier: Float = 1.0f
)

data class WisdomUiState(
    val goals: List<Goal> = emptyList(),
    val targetsMap: Map<String, List<Target>> = emptyMap(),
    val selectedGoal: Goal? = null,
    val showGoalCelebration: Boolean = false,
    val showTargetCelebration: Boolean = false,
    val lastAchievedItemName: String = "",
    val userStats: UserStats = UserStats(),
    val inventoryBoosters: List<Booster> = emptyList(),
    val activeBoosters: List<Booster> = emptyList()
)

class WisdomViewModel : ViewModel() {
    private val repository = WisdomRepository()

    private val _uiState = MutableStateFlow(WisdomUiState())
    val uiState: StateFlow<WisdomUiState> = _uiState.asStateFlow()

    private var boosterJobs = mutableMapOf<String, Job>()

    init {
        loadGoals()
        checkStreak()
    }

    private val wisdomQuotes = listOf(
        Wisdom(text = "The only way to do great work is to love what you do.", author = "Steve Jobs", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Success is not final, failure is not fatal: it is the courage to continue that counts.", author = "Winston Churchill", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Innovation distinguishes between a leader and a follower.", author = "Steve Jobs", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Don't watch the clock; do what it does. Keep going.", author = "Sam Levenson", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "The future depends on what you do today.", author = "Mahatma Gandhi", category = WisdomCategory.HABITS),
        Wisdom(text = "It does not matter how slowly you go as long as you do not stop.", author = "Confucius", category = WisdomCategory.HABITS),
        Wisdom(text = "Hard work beats talent when talent doesn't work hard.", author = "Tim Notke", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Success usually comes to those who are too busy to be looking for it.", author = "Henry David Thoreau", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "The secret of getting ahead is getting started.", author = "Mark Twain", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Quality is not an act, it is a habit.", author = "Aristotle", category = WisdomCategory.HABITS)
    )

    fun getWisdom(): List<Wisdom> {
        return wisdomQuotes.shuffled().take(3)
    }

    private fun loadGoals() {
        val g1 = Goal(
            id = "1",
            title = "Become Senior Developer",
            description = "Reach senior developer position within 2 years",
            category = GoalCategory.CAREER,
            totalTargets = 3,
            completedTargets = 1
        )
        val g2 = Goal(
            id = "2",
            title = "Learn Modern Android",
            description = "Master Jetpack Compose and advanced Kotlin features",
            category = GoalCategory.LEARNING,
            totalTargets = 2,
            completedTargets = 0
        )

        val targetsMap = mapOf(
            "1" to listOf(
                Target(id = "t1", goalId = "1", title = "Complete React Course", description = "", order = 1, isCompleted = true),
                Target(id = "t2", goalId = "1", title = "Lead a Project", description = "", order = 2, isCompleted = false),
                Target(id = "t3", goalId = "1", title = "Master CI/CD", description = "", order = 3, isCompleted = false)
            ),
            "2" to listOf(
                Target(id = "t4", goalId = "2", title = "Finish Compose Basics", description = "", order = 1, isCompleted = false),
                Target(id = "t5", goalId = "2", title = "Implement Navigation", description = "", order = 2, isCompleted = false)
            )
        )

        _uiState.update { it.copy(goals = listOf(g1, g2), targetsMap = targetsMap) }
    }

    private fun checkStreak() {
        val today = LocalDate.now()
        val lastActive = _uiState.value.userStats.lastActiveDate

        if (lastActive.isBefore(today)) {
            if (lastActive.plusDays(1) == today) {
                _uiState.update { it.copy(userStats = it.userStats.copy(streak = it.userStats.streak + 1, lastActiveDate = today)) }
                awardXP(20)
                checkStreakMilestones(_uiState.value.userStats.streak)
            } else if (lastActive.plusDays(1).isBefore(today)) {
                if (!useStreakProtection()) {
                    _uiState.update { it.copy(userStats = it.userStats.copy(streak = 1, lastActiveDate = today)) }
                }
            }
        }
    }

    private fun checkStreakMilestones(streak: Int) {
        when (streak) {
            7 -> awardXP(100)
            30 -> {
                awardXP(500)
                _uiState.update { it.copy(userStats = it.userStats.copy(streakProtectionAvailable = it.userStats.streakProtectionAvailable + 1)) }
            }
        }
    }

    fun addGoal(goal: Goal) {
        _uiState.update { it.copy(goals = it.goals + goal, targetsMap = it.targetsMap + (goal.id to emptyList())) }
    }

    fun deleteGoal(goalId: String) {
        _uiState.update { state ->
            state.copy(
                goals = state.goals.filter { it.id != goalId },
                targetsMap = state.targetsMap - goalId,
                selectedGoal = if (state.selectedGoal?.id == goalId) null else state.selectedGoal
            )
        }
    }

    fun addTargetToGoal(goalId: String, title: String) {
        _uiState.update { state ->
            val currentTargets = state.targetsMap[goalId] ?: emptyList()
            val newTarget = Target(
                id = UUID.randomUUID().toString(),
                goalId = goalId,
                title = title,
                description = "",
                order = currentTargets.size + 1
            )
            state.copy(
                targetsMap = state.targetsMap + (goalId to (currentTargets + newTarget)),
                goals = state.goals.map { if (it.id == goalId) it.copy(totalTargets = it.totalTargets + 1) else it }
            )
        }
    }

    fun deleteTarget(goalId: String, targetId: String) {
        _uiState.update { state ->
            val currentTargets = state.targetsMap[goalId] ?: return@update state
            val updatedTargets = currentTargets.filter { it.id != targetId }
            val completedCount = updatedTargets.count { it.isCompleted }
            state.copy(
                targetsMap = state.targetsMap + (goalId to updatedTargets),
                goals = state.goals.map { goal ->
                    if (goal.id == goalId) {
                        val total = updatedTargets.size
                        val isGoalComplete = total > 0 && completedCount >= total
                        goal.copy(totalTargets = total, completedTargets = completedCount, isCompleted = isGoalComplete)
                    } else goal
                }
            )
        }
    }

    fun completeTarget(goalId: String, targetId: String) {
        _uiState.update { state ->
            val currentTargets = state.targetsMap[goalId] ?: return@update state
            var targetTitle = ""
            var newlyCompleted = false

            val updatedTargets = currentTargets.map { target ->
                if (target.id == targetId && !target.isCompleted) {
                    newlyCompleted = true
                    targetTitle = target.title
                    target.copy(isCompleted = true, completedAt = System.currentTimeMillis())
                } else target
            }

            if (!newlyCompleted) return@update state

            awardXP(50)

            val completedCount = updatedTargets.count { it.isCompleted }
            val newGoals = state.goals.map { goal ->
                if (goal.id == goalId) {
                    val isGoalComplete = completedCount >= goal.totalTargets
                    if (isGoalComplete && !goal.isCompleted) {
                        awardXP(200)
                        _uiState.update { s -> s.copy(userStats = s.userStats.copy(totalGoalsCompleted = s.userStats.totalGoalsCompleted + 1)) }
                        triggerGoalCelebration()
                    }
                    goal.copy(completedTargets = completedCount, isCompleted = isGoalComplete)
                } else goal
            }

            triggerTargetCelebration()
            state.copy(
                targetsMap = state.targetsMap + (goalId to updatedTargets),
                goals = newGoals,
                lastAchievedItemName = targetTitle
            )
        }
    }

    private fun awardXP(amount: Int) {
        _uiState.update { state ->
            val actualAmount = (amount * state.userStats.xpMultiplier).toInt()
            val newXP = state.userStats.experiencePoints + actualAmount
            val xpForNextLevel = state.userStats.level * 500

            if (newXP >= xpForNextLevel) {
                state.copy(userStats = state.userStats.copy(level = state.userStats.level + 1, experiencePoints = newXP - xpForNextLevel))
            } else {
                state.copy(userStats = state.userStats.copy(experiencePoints = newXP))
            }
        }
    }

    fun selectGoal(goal: Goal?) {
        _uiState.update { it.copy(selectedGoal = goal) }
    }

    private fun triggerGoalCelebration() {
        viewModelScope.launch {
            _uiState.update { it.copy(showGoalCelebration = true, lastAchievedItemName = _uiState.value.lastAchievedItemName) }
            delay(4000)
            _uiState.update { it.copy(showGoalCelebration = false) }
        }
    }

    private fun triggerTargetCelebration() {
        viewModelScope.launch {
            _uiState.update { it.copy(showTargetCelebration = true) }
            delay(2000)
            _uiState.update { it.copy(showTargetCelebration = false) }
        }
    }

    fun purchaseStreakProtection(amount: Int, cost: Int) {
        _uiState.update { state ->
            if (state.userStats.experiencePoints >= cost) {
                state.copy(userStats = state.userStats.copy(
                    experiencePoints = state.userStats.experiencePoints - cost,
                    streakProtectionAvailable = state.userStats.streakProtectionAvailable + amount
                ))
            } else state
        }
    }

    fun purchaseBooster(type: BoosterType, cost: Int) {
        _uiState.update { state ->
            if (state.userStats.experiencePoints >= cost) {
                val newBooster = Booster(type = type, durationHours = 1)
                state.copy(
                    userStats = state.userStats.copy(experiencePoints = state.userStats.experiencePoints - cost),
                    inventoryBoosters = state.inventoryBoosters + newBooster
                )
            } else state
        }
    }

    fun activateBooster(booster: Booster) {
        val activatedBooster = booster.copy(activatedAt = LocalDateTime.now())
        _uiState.update { state ->
            state.copy(
                inventoryBoosters = state.inventoryBoosters.filter { it.id != booster.id },
                activeBoosters = state.activeBoosters + activatedBooster
            )
        }
        recalculateMultipliers()
        val job = viewModelScope.launch {
            delay(activatedBooster.durationHours * 3600L * 1000L)
            deactivateBooster(activatedBooster)
        }
        boosterJobs[activatedBooster.id] = job
    }

    private fun deactivateBooster(booster: Booster) {
        _uiState.update { state ->
            state.copy(activeBoosters = state.activeBoosters.filter { it.id != booster.id })
        }
        boosterJobs.remove(booster.id)?.cancel()
        recalculateMultipliers()
    }

    private fun recalculateMultipliers() {
        _uiState.update { state ->
            var multiplier = 1.0f
            state.activeBoosters.forEach {
                when (it.type) {
                    BoosterType.XP_2X -> multiplier *= 2.0f
                    BoosterType.XP_1_5X -> multiplier *= 1.5f
                    else -> {}
                }
            }
            state.copy(userStats = state.userStats.copy(xpMultiplier = multiplier))
        }
    }

    private fun useStreakProtection(): Boolean {
        val stats = _uiState.value.userStats
        if (stats.streakProtectionAvailable > 0) {
            _uiState.update { it.copy(userStats = it.userStats.copy(streakProtectionAvailable = it.userStats.streakProtectionAvailable - 1, lastActiveDate = LocalDate.now())) }
            return true
        }
        return false
    }
}
