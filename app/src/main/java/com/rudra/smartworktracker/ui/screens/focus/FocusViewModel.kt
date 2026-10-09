package com.rudra.smartworktracker.ui.screens.focus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.R
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.AchievementRepository
import com.rudra.smartworktracker.engine.AchievementManager
import com.rudra.smartworktracker.model.FocusSession
import com.rudra.smartworktracker.model.FocusType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val focusSessionDao = db.focusSessionDao()
    private val achievementRepository = AchievementRepository(db.achievementDao())
    private val achievementManager = AchievementManager(db.achievementDao(), db.habitDao(), focusSessionDao)
    private var timerJob: Job? = null
    private var interruptionsCount = 0
    private var startTime = 0L

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val notificationManager: NotificationManager
        get() = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        // Notifications posted to a channel that doesn't exist are silently dropped on Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Focus sessions", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Progress and completion of focus sessions"
                }
            )
        }
    }

    fun startFocusSession(type: FocusType, duration: Long) {
        timerJob?.cancel()
        startTime = System.currentTimeMillis()
        interruptionsCount = 0
        _timerState.value = TimerState.Running(type, duration, 0)
        _isPaused.value = false

        startTimer(duration)
        showOngoing(type, "${type.displayName} • ${duration / 60} minutes")
    }

    fun pauseResumeTimer() {
        val currentState = _timerState.value as? TimerState.Running ?: return
        _isPaused.value = !_isPaused.value
        if (_isPaused.value) {
            timerJob?.cancel()
            showOngoing(currentState.type, "Paused • ${formatRemaining(currentState.duration - currentState.elapsed)} left")
        } else {
            startTimer(currentState.duration, currentState.elapsed)
            showOngoing(currentState.type, "Resumed • ${formatRemaining(currentState.duration - currentState.elapsed)} left")
        }
    }

    /** Interruptions lower the session's focus score (they no longer move the timer). */
    fun recordInterruption() {
        if (_timerState.value is TimerState.Running) interruptionsCount++
    }

    fun stopFocusSession() {
        timerJob?.cancel()
        val currentState = _timerState.value
        if (currentState is TimerState.Running) {
            saveSession(currentState)
        }
        _timerState.value = TimerState.Idle
        _isPaused.value = false
        cancelOngoing()
    }

    private fun startTimer(totalDuration: Long, initialElapsed: Long = 0) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var elapsed = initialElapsed
            while (elapsed < totalDuration && !_isPaused.value) {
                delay(1000)
                elapsed++
                val currentState = _timerState.value
                if (currentState is TimerState.Running) {
                    _timerState.value = currentState.copy(elapsed = elapsed)

                    if (elapsed % 300 == 0L) { // Every 5 minutes
                        showOngoing(currentState.type, "${formatRemaining(totalDuration - elapsed)} left")
                    }

                    if (elapsed >= totalDuration) {
                        onTimerComplete(currentState.copy(elapsed = elapsed))
                        break
                    }
                }
            }
        }
    }

    private fun onTimerComplete(state: TimerState.Running) {
        viewModelScope.launch {
            val focusScore = calculateFocusScore(state.elapsed, interruptionsCount)
            persist(state, focusScore)
            cancelOngoing()
            sendCompletionNotification(state.type)
            _timerState.value = TimerState.Completed(state.type, focusScore)
        }
    }

    private fun saveSession(state: TimerState.Running) {
        if (state.elapsed < MIN_SAVED_SECONDS) return // Ignore accidental start/stop taps
        viewModelScope.launch {
            persist(state, calculateFocusScore(state.elapsed, interruptionsCount))
        }
    }

    private suspend fun persist(state: TimerState.Running, focusScore: Int) {
        focusSessionDao.insertFocusSession(
            FocusSession(
                id = UUID.randomUUID().toString(),
                type = state.type,
                duration = state.duration,
                elapsedTime = state.elapsed,
                interruptions = interruptionsCount,
                focusScore = focusScore,
                timestamp = startTime
            )
        )
        // Focus achievements used to unlock only when the Achievements screen was opened
        achievementRepository.initializeAchievements()
        achievementManager.checkAndUnlockAchievements()
    }

    private fun calculateFocusScore(elapsedTime: Long, interruptions: Int): Int {
        val baseScore = (elapsedTime.toDouble() / 3600 * 100).toInt() // Score based on hours
        val interruptionPenalty = interruptions * 5 // 5 points penalty per interruption
        return maxOf(0, baseScore - interruptionPenalty)
    }

    private fun formatRemaining(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0)
        return "%d:%02d".format(safe / 60, safe % 60)
    }

    private fun showOngoing(type: FocusType, text: String) {
        val notification = NotificationCompat.Builder(getApplication(), CHANNEL_ID)
            .setContentTitle("Focus: ${type.displayName}")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_alarm)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun sendCompletionNotification(type: FocusType) {
        val notification = NotificationCompat.Builder(getApplication(), CHANNEL_ID)
            .setContentTitle("Focus Session Completed!")
            .setContentText("Great job completing your ${type.displayName} session!")
            .setSmallIcon(R.drawable.ic_alarm)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(COMPLETED_NOTIFICATION_ID, notification)
    }

    private fun cancelOngoing() {
        notificationManager.cancel(ONGOING_NOTIFICATION_ID)
    }

    override fun onCleared() {
        // Leaving the screen ends the session; keep the time already focused
        val state = _timerState.value
        if (state is TimerState.Running && state.elapsed >= MIN_SAVED_SECONDS) {
            val score = calculateFocusScore(state.elapsed, interruptionsCount)
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                withContext(NonCancellable) { persist(state, score) }
            }
        }
        cancelOngoing()
        super.onCleared()
    }

    private companion object {
        const val CHANNEL_ID = "focus_channel"
        // Distinct from alarm notification ids, which are schedule ids (1, 2, …)
        const val ONGOING_NOTIFICATION_ID = 41_001
        const val COMPLETED_NOTIFICATION_ID = 41_002
        const val MIN_SAVED_SECONDS = 60L
    }
}

sealed class TimerState {
    object Idle : TimerState()
    data class Running(val type: FocusType, val duration: Long, val elapsed: Long) : TimerState()
    data class Completed(val type: FocusType, val score: Int) : TimerState()
}
