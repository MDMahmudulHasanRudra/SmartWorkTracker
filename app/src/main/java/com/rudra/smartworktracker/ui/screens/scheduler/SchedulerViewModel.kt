package com.rudra.smartworktracker.ui.screens.scheduler

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.alarm.AlarmScheduler
import com.rudra.smartworktracker.data.repository.ScheduleRepository
import com.rudra.smartworktracker.model.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalTime

data class SchedulerUiState(
    val schedules: List<Schedule> = emptyList(),
    val newScheduleTitle: String = "",
    val newScheduleDescription: String = "",
    val newScheduleHour: Int = LocalTime.now().hour,
    val newScheduleMinute: Int = LocalTime.now().minute,
    val newScheduleIsRepeating: Boolean = false,
    val selectedDays: List<Int> = emptyList(),
    val history: List<ScheduleHistory> = emptyList(),
    val selectedRingtoneUri: String? = null,
    val selectedRingtoneName: String = "Default",
    val vibrationPattern: String = "default",
    val volumeLevel: Int = 80,
    val selectedCategory: String = "General",
    val isImportant: Boolean = false,
    val snoozeDuration: Int = 5,
    val maxSnoozeCount: Int = 3,
    val isLoading: Boolean = false,
    val selectedColorTag: Int? = null,
    val showAdvancedOptions: Boolean = false
)

class SchedulerViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchedulerUiState())
    val uiState: StateFlow<SchedulerUiState> = _uiState.asStateFlow()

    private var alarmScheduler: AlarmScheduler? = null
    private var audioManager: AudioManager? = null

    init {
        context?.let {
            alarmScheduler = AlarmScheduler(it)
            audioManager = it.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }

        scheduleRepository.getAllSchedules()
            .onEach { schedules ->
                _uiState.value = _uiState.value.copy(schedules = schedules)
                // Pass every schedule so disabled ones are cancelled too
                alarmScheduler?.rescheduleAll(schedules)
            }
            .launchIn(viewModelScope)
    }

    fun addSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val time = LocalTime.of(
                _uiState.value.newScheduleHour,
                _uiState.value.newScheduleMinute
            )

            val newSchedule = Schedule(
                title = _uiState.value.newScheduleTitle,
                description = _uiState.value.newScheduleDescription,
                time = time,
                isRepeating = _uiState.value.newScheduleIsRepeating,
                repeatingDays = if (_uiState.value.newScheduleIsRepeating) {
                    _uiState.value.selectedDays.toSet()
                } else {
                    emptySet()
                },
                ringtoneUri = _uiState.value.selectedRingtoneUri,
                ringtoneName = _uiState.value.selectedRingtoneName,
                vibrationPattern = _uiState.value.vibrationPattern,
                volumeLevel = _uiState.value.volumeLevel,
                category = _uiState.value.selectedCategory,
                snoozeDuration = _uiState.value.snoozeDuration,
                maxSnoozeCount = _uiState.value.maxSnoozeCount,
                isImportant = _uiState.value.isImportant,
                colorTag = _uiState.value.selectedColorTag
            )

            try {
                // The alarm must use the generated row id, or it can never be cancelled/updated
                val savedSchedule = newSchedule.copy(id = scheduleRepository.insertSchedule(newSchedule))
                alarmScheduler?.schedule(savedSchedule)

                // Add to history
                addHistoryEntry(
                    scheduleId = savedSchedule.id,
                    type = HistoryType.CREATED,
                    details = "Created schedule '${newSchedule.title}'",
                    ringtoneName = newSchedule.ringtoneName
                )
                
                // Reset form
                resetForm()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(updatedAt = System.currentTimeMillis())
            scheduleRepository.updateSchedule(updatedSchedule)
            
            if (updatedSchedule.isEnabled) {
                alarmScheduler?.schedule(updatedSchedule)
            } else {
                alarmScheduler?.cancel(updatedSchedule)
            }
            
            addHistoryEntry(
                scheduleId = updatedSchedule.id,
                type = HistoryType.UPDATED,
                details = "Updated schedule '${updatedSchedule.title}'",
                ringtoneName = updatedSchedule.ringtoneName
            )
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            alarmScheduler?.cancel(schedule)
            scheduleRepository.deleteSchedule(schedule)
            
            addHistoryEntry(
                scheduleId = schedule.id,
                type = HistoryType.DELETED,
                details = "Deleted schedule '${schedule.title}'",
                ringtoneName = schedule.ringtoneName
            )
        }
    }

    fun toggleSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(isEnabled = !schedule.isEnabled)
            scheduleRepository.updateSchedule(updatedSchedule)
            
            if (updatedSchedule.isEnabled) {
                alarmScheduler?.schedule(updatedSchedule)
                addHistoryEntry(
                    scheduleId = updatedSchedule.id,
                    type = HistoryType.UPDATED,
                    details = "Enabled schedule '${updatedSchedule.title}'"
                )
            } else {
                alarmScheduler?.cancel(updatedSchedule)
                addHistoryEntry(
                    scheduleId = updatedSchedule.id,
                    type = HistoryType.UPDATED,
                    details = "Disabled schedule '${updatedSchedule.title}'"
                )
            }
        }
    }

    fun changeScheduleRingtone(schedule: Schedule, ringtoneUri: String?, ringtoneName: String) {
        viewModelScope.launch {
            val updatedSchedule = schedule.copy(
                ringtoneUri = ringtoneUri,
                ringtoneName = ringtoneName
            )
            scheduleRepository.updateSchedule(updatedSchedule)
            
            if (updatedSchedule.isEnabled) {
                alarmScheduler?.schedule(updatedSchedule)
            }
            
            addHistoryEntry(
                scheduleId = updatedSchedule.id,
                type = HistoryType.RINGTONE_CHANGED,
                details = "Changed ringtone to '$ringtoneName'",
                ringtoneName = ringtoneName
            )
        }
    }

    fun testAlarmNow(schedule: Schedule) {
        viewModelScope.launch {
            val testTime = LocalTime.now().plusSeconds(5)
            // Separate id so the test alarm does not replace the schedule's real alarm
            val testSchedule = schedule.copy(
                id = (schedule.id ?: 0L) + AlarmScheduler.TEST_ID_OFFSET,
                time = testTime,
                isRepeating = false,
                isEnabled = true
            )
            
            alarmScheduler?.schedule(testSchedule)
            
            addHistoryEntry(
                scheduleId = schedule.id,
                type = HistoryType.TRIGGERED,
                details = "Test alarm triggered",
                ringtoneName = schedule.ringtoneName
            )
        }
    }

    fun clearHistory() {
        _uiState.value = _uiState.value.copy(history = emptyList())
    }

    fun deleteHistory(history: ScheduleHistory) {
        val newHistory = _uiState.value.history.toMutableList()
        newHistory.remove(history)
        _uiState.value = _uiState.value.copy(history = newHistory)
    }

    fun selectRingtone(uri: String?, name: String) {
        _uiState.value = _uiState.value.copy(
            selectedRingtoneUri = uri,
            selectedRingtoneName = name
        )
    }

    fun setVolumeLevel(level: Int) {
        _uiState.value = _uiState.value.copy(volumeLevel = level)
    }

    fun setVibrationPattern(pattern: String) {
        _uiState.value = _uiState.value.copy(vibrationPattern = pattern)
    }

    fun setSnoozeDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(snoozeDuration = minutes)
    }

    fun setMaxSnoozeCount(count: Int) {
        _uiState.value = _uiState.value.copy(maxSnoozeCount = count)
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setIsImportant(isImportant: Boolean) {
        _uiState.value = _uiState.value.copy(isImportant = isImportant)
    }

    fun toggleAdvancedOptions() {
        _uiState.value = _uiState.value.copy(
            showAdvancedOptions = !_uiState.value.showAdvancedOptions
        )
    }

    fun setColorTag(color: Int?) {
        _uiState.value = _uiState.value.copy(selectedColorTag = color)
    }

    private fun addHistoryEntry(
        scheduleId: Long?,
        type: HistoryType,
        details: String? = null,
        ringtoneName: String? = null
    ) {
        val historyEntry = ScheduleHistory(
            scheduleId = scheduleId,
            type = type,
            details = details,
            ringtoneName = ringtoneName
        )
        
        val newHistory = listOf(historyEntry) + _uiState.value.history.take(99)
        _uiState.value = _uiState.value.copy(history = newHistory)
    }

    private fun resetForm() {
        _uiState.value = SchedulerUiState(
            schedules = _uiState.value.schedules,
            history = _uiState.value.history,
            newScheduleHour = LocalTime.now().hour,
            newScheduleMinute = LocalTime.now().minute
        )
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(newScheduleTitle = title)
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(newScheduleDescription = description)
    }

    fun onTimeChange(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(newScheduleHour = hour, newScheduleMinute = minute)
    }

    fun onIsRepeatingChange(isRepeating: Boolean) {
        _uiState.value = _uiState.value.copy(newScheduleIsRepeating = isRepeating)
    }

    fun onDaySelected(day: Int) {
        val currentDays = _uiState.value.selectedDays.toMutableList()
        if (currentDays.contains(day)) {
            currentDays.remove(day)
        } else {
            currentDays.add(day)
        }
        _uiState.value = _uiState.value.copy(selectedDays = currentDays)
    }
    
    fun getAvailableRingtoneNames(): List<String> {
        return listOf(
            "Default",
            "Alarm",
            "Notification",
            "Ringtone",
            "Melody",
            "Chime",
            "Beep",
            "Buzzer"
        )
    }
    
    fun getCategories(): List<String> {
        return listOf(
            "General",
            "Work",
            "Personal",
            "Meeting",
            "Exercise",
            "Medication",
            "Study",
            "Appointment"
        )
    }
    
    fun getColorTags(): List<Pair<Int, String>> {
        return listOf(
            Pair(0xFF4CAF50.toInt(), "Green"),
            Pair(0xFF2196F3.toInt(), "Blue"),
            Pair(0xFFFF9800.toInt(), "Orange"),
            Pair(0xFFF44336.toInt(), "Red"),
            Pair(0xFF9C27B0.toInt(), "Purple"),
            Pair(0xFF00BCD4.toInt(), "Cyan"),
            Pair(0xFFFFC107.toInt(), "Amber"),
            Pair(0xFF795548.toInt(), "Brown")
        )
    }
}
