package com.rudra.smartworktracker.ui.screens.habit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.AchievementRepository
import com.rudra.smartworktracker.engine.AchievementManager
import com.rudra.smartworktracker.model.Habit
import com.rudra.smartworktracker.model.HabitDifficulty
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val habitDao = db.habitDao()
    private val achievementRepository = AchievementRepository(db.achievementDao())
    private val achievementManager = AchievementManager(db.achievementDao(), habitDao, db.focusSessionDao())

    /** Habits with a streak of 0 once a day has been missed (the stored value only resets on the next completion). */
    val habits: StateFlow<List<Habit>> = habitDao.getAllHabits()
        .map { list ->
            list.map { habit -> if (isStreakBroken(habit.lastCompleted)) habit.copy(streak = 0) else habit }
                .sortedWith(compareBy<Habit> { isCompletedToday(it.lastCompleted) }.thenBy { it.name.lowercase() })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun addHabit(name: String, description: String, difficulty: HabitDifficulty) {
        viewModelScope.launch {
            val newHabit = Habit(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                description = description.trim(),
                streak = 0,
                difficulty = difficulty,
                triggerHabitId = null,
                createdAt = System.currentTimeMillis()
            )
            habitDao.insertHabit(newHabit)
            _messages.tryEmit("Habit \"${newHabit.name}\" added")
        }
    }

    fun completeHabit(habit: Habit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            when {
                isCompletedToday(habit.lastCompleted) -> {
                    _messages.tryEmit("\"${habit.name}\" is already done today")
                    return@launch
                }
                isEligibleForCompletion(habit.lastCompleted) ->
                    habitDao.updateHabit(habit.copy(streak = habit.streak + 1, lastCompleted = now, updatedAt = now))
                else -> // Streak broken: today's completion starts a new one
                    habitDao.updateHabit(habit.copy(streak = 1, lastCompleted = now, updatedAt = now))
            }
            _messages.tryEmit("\"${habit.name}\" done — keep the streak going!")
            // Streak achievements used to unlock only when the Achievements screen was opened
            achievementRepository.initializeAchievements()
            achievementManager.checkAndUnlockAchievements()
        }
    }

    fun heavyDeleteHabit(habit: Habit) {
        viewModelScope.launch {
            deleteHabitAndItsTriggers(habit)
        }
    }

    private suspend fun deleteHabitAndItsTriggers(habit: Habit) {
        val triggeredHabits = habitDao.getHabitsByTriggerId(habit.id)
        for (triggeredHabit in triggeredHabits) {
            deleteHabitAndItsTriggers(triggeredHabit)
        }
        habitDao.deleteHabit(habit)
    }

    private fun isCompletedToday(lastCompleted: Long?): Boolean {
        if (lastCompleted == null) return false
        val today = Calendar.getInstance()
        val last = Calendar.getInstance().apply { timeInMillis = lastCompleted }
        return today.get(Calendar.YEAR) == last.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == last.get(Calendar.DAY_OF_YEAR)
    }

    private fun isEligibleForCompletion(lastCompleted: Long?): Boolean {
        if (lastCompleted == null) return true // First time completion

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val lastCompletionDate = Calendar.getInstance().apply { timeInMillis = lastCompleted }
        return yesterday.get(Calendar.YEAR) == lastCompletionDate.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == lastCompletionDate.get(Calendar.DAY_OF_YEAR)
    }

    /** True when the last completion was before yesterday. */
    private fun isStreakBroken(lastCompleted: Long?): Boolean {
        if (lastCompleted == null) return false
        if (isCompletedToday(lastCompleted)) return false
        val startOfYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return lastCompleted < startOfYesterday.timeInMillis
    }
}
