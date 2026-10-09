package com.rudra.smartworktracker.ui.screens.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.model.BreakPeriod
import com.rudra.smartworktracker.model.SessionType
import com.rudra.smartworktracker.model.WorkSession
import com.rudra.smartworktracker.utils.DateTimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class TimerMode { RUNNING, PAUSED, STOPPED, ON_BREAK, ON_LUNCH }

data class TimerState(
    val mode: TimerMode = TimerMode.STOPPED,
    val totalSeconds: Int = 0,
    val workSeconds: Int = 0,
    val breakSeconds: Int = 0,
    val currentSessionId: String? = null,
    val currentBreakStartTime: Long? = null,
    val todayStats: TodayStats = TodayStats(),
    val sessionHistory: List<SessionItem> = emptyList()
)

data class TodayStats(
    val totalWorkTime: Int = 0,
    val totalBreaks: Int = 0,
    val productivityScore: Float = 0f,
    val sessionsCompleted: Int = 0
)

data class SessionItem(
    val id: String,
    val type: SessionType,
    val startTime: String,
    val duration: String,
    val productivityScore: Int? = null
)

/**
 * Work-session stopwatch backed by the work_sessions table. Pauses, breaks and lunch are all
 * stored as break periods, so for any session: wall-clock time = work time + break time.
 */
class WorkTimerViewModel(application: Application) : AndroidViewModel(application) {

    private val workSessionDao = AppDatabase.getDatabase(application).workSessionDao()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private val breaks = mutableListOf<BreakPeriod>()
    private var sessionStartTime = 0L
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    init {
        observeToday()
        restoreOpenSession()
    }

    /** Today's stats and history, kept live from the database. */
    private fun observeToday() {
        val (todayStart, _) = DateTimeUtils.dayRange()
        viewModelScope.launch {
            workSessionDao.getSessionsSince(todayStart).collect { sessions ->
                val completed = sessions.filter { it.endTime != null }
                val stats = TodayStats(
                    totalWorkTime = completed.sumOf { workSecondsOf(it) },
                    totalBreaks = sessions.sumOf { it.breaks.size },
                    productivityScore = completed.mapNotNull { it.productivityScore }.average()
                        .takeIf { !it.isNaN() }?.toFloat() ?: 0f,
                    sessionsCompleted = completed.size
                )
                val history = sessions.flatMap { session ->
                    val workItem = SessionItem(
                        id = session.id,
                        type = SessionType.WORK,
                        startTime = timeFormat.format(Date(session.startTime)),
                        duration = if (session.endTime == null) "In progress" else formatDuration(workSecondsOf(session)),
                        productivityScore = session.productivityScore
                    )
                    val breakItems = session.breaks.mapIndexed { index, period ->
                        SessionItem(
                            id = "${session.id}_break_$index",
                            type = SessionType.BREAK,
                            startTime = timeFormat.format(Date(period.startTime)),
                            duration = formatDuration(((period.endTime - period.startTime) / 1000).toInt())
                        )
                    }
                    listOf(workItem) + breakItems.reversed()
                }
                _timerState.value = _timerState.value.copy(todayStats = stats, sessionHistory = history)
            }
        }
    }

    /** Resumes a session that was still open when the app was closed. */
    private fun restoreOpenSession() {
        viewModelScope.launch {
            val (todayStart, _) = DateTimeUtils.dayRange()
            val open = workSessionDao.getSessionsSince(todayStart).first().firstOrNull { it.endTime == null } ?: return@launch
            if (_timerState.value.currentSessionId != null) return@launch
            sessionStartTime = open.startTime
            breaks.clear()
            breaks.addAll(open.breaks)
            val breakSeconds = breaks.sumOf { ((it.endTime - it.startTime) / 1000).toInt() }
            val totalSeconds = ((System.currentTimeMillis() - open.startTime) / 1000).toInt()
            _timerState.value = _timerState.value.copy(
                mode = TimerMode.RUNNING,
                currentSessionId = open.id,
                totalSeconds = totalSeconds,
                breakSeconds = breakSeconds,
                workSeconds = (totalSeconds - breakSeconds).coerceAtLeast(0)
            )
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timerState.value = _timerState.value.let { state ->
                    when (state.mode) {
                        TimerMode.RUNNING -> state.copy(
                            totalSeconds = state.totalSeconds + 1,
                            workSeconds = state.workSeconds + 1
                        )
                        TimerMode.ON_BREAK, TimerMode.ON_LUNCH, TimerMode.PAUSED -> state.copy(
                            totalSeconds = state.totalSeconds + 1,
                            breakSeconds = state.breakSeconds + 1
                        )
                        TimerMode.STOPPED -> state
                    }
                }
            }
        }
    }

    fun startWorkSession() {
        if (_timerState.value.mode != TimerMode.STOPPED) return
        val newSessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val workSession = WorkSession(
            id = newSessionId,
            startTime = now,
            endTime = null,
            type = SessionType.WORK,
            breaks = emptyList(),
            productivityScore = null
        )
        sessionStartTime = now
        breaks.clear()
        viewModelScope.launch {
            workSessionDao.insertWorkSession(workSession)
            _timerState.value = _timerState.value.copy(
                mode = TimerMode.RUNNING,
                currentSessionId = newSessionId,
                currentBreakStartTime = null,
                workSeconds = 0,
                breakSeconds = 0,
                totalSeconds = 0
            )
            startTimer()
        }
    }

    fun pauseWorkSession() {
        if (_timerState.value.mode != TimerMode.RUNNING) return
        _timerState.value = _timerState.value.copy(
            mode = TimerMode.PAUSED,
            currentBreakStartTime = System.currentTimeMillis()
        )
    }

    fun resumeWorkSession() {
        closeOpenBreak()
        _timerState.value = _timerState.value.copy(mode = TimerMode.RUNNING, currentBreakStartTime = null)
    }

    /** Ends the session: records the end time, breaks and a work/total productivity score. */
    fun stopWorkSession() {
        val state = _timerState.value
        val sessionId = state.currentSessionId ?: return
        timerJob?.cancel()
        // The stop write below saves the breaks; a separate write here could race and drop endTime
        closeOpenBreak(persist = false)
        val total = state.workSeconds + state.breakSeconds
        val score = if (total > 0) (state.workSeconds * 100 / total) else null
        viewModelScope.launch {
            workSessionDao.getWorkSessionById(sessionId)?.let { session ->
                workSessionDao.upsertWorkSession(
                    session.copy(
                        endTime = System.currentTimeMillis(),
                        breaks = breaks.toList(),
                        productivityScore = score,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        breaks.clear()
        _timerState.value = state.copy(
            mode = TimerMode.STOPPED,
            currentSessionId = null,
            currentBreakStartTime = null,
            totalSeconds = 0,
            workSeconds = 0,
            breakSeconds = 0
        )
    }

    fun startBreak() {
        if (_timerState.value.mode != TimerMode.RUNNING) return
        _timerState.value = _timerState.value.copy(
            mode = TimerMode.ON_BREAK,
            currentBreakStartTime = System.currentTimeMillis()
        )
    }

    fun startLunch() {
        if (_timerState.value.mode != TimerMode.RUNNING) return
        _timerState.value = _timerState.value.copy(
            mode = TimerMode.ON_LUNCH,
            currentBreakStartTime = System.currentTimeMillis()
        )
    }

    fun endBreak() {
        closeOpenBreak()
        _timerState.value = _timerState.value.copy(mode = TimerMode.RUNNING, currentBreakStartTime = null)
    }

    /** Stores the running pause/break as a break period and (by default) saves it right away. */
    private fun closeOpenBreak(persist: Boolean = true) {
        val state = _timerState.value
        val breakStart = state.currentBreakStartTime ?: return
        val sessionId = state.currentSessionId ?: return
        breaks.add(BreakPeriod(startTime = breakStart, endTime = System.currentTimeMillis()))
        if (!persist) return
        val snapshot = breaks.toList()
        viewModelScope.launch {
            workSessionDao.getWorkSessionById(sessionId)?.let {
                workSessionDao.upsertWorkSession(it.copy(breaks = snapshot, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    private fun workSecondsOf(session: WorkSession): Int {
        val end = session.endTime ?: System.currentTimeMillis()
        val breakMillis = session.breaks.sumOf { it.endTime - it.startTime }
        return ((end - session.startTime - breakMillis) / 1000).toInt().coerceAtLeast(0)
    }

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${seconds % 60}s"
    }
}
