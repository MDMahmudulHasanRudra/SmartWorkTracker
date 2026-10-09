package com.rudra.smartworktracker.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.model.WorkLog
import com.rudra.smartworktracker.model.WorkType
import com.rudra.smartworktracker.data.repository.WorkLogRepository
import com.rudra.smartworktracker.ui.WorkLogUi
import com.rudra.smartworktracker.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class CalendarViewModel(private val repository: WorkLogRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    private val _workLogs = MutableStateFlow<List<WorkLogUi>>(emptyList())
    private val _selectedWorkLog = MutableStateFlow<WorkLogUi?>(null)
    private val _multiSelectMode = MutableStateFlow(false)
    private val _multiSelectedDates = MutableStateFlow<List<LocalDate>>(emptyList())
    private val _activeFilters = MutableStateFlow<List<WorkType>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _monthlyStats = MutableStateFlow(MonthlyStats())
    // Month shown in the calendar grid; stats follow it rather than the selected day
    private var displayedMonth: YearMonth = YearMonth.now()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDate,
        _workLogs,
        _selectedWorkLog,
        _multiSelectMode,
        _multiSelectedDates,
        _activeFilters,
        _searchQuery,
        _monthlyStats
    ) { values ->
        val selectedDate = values[0] as LocalDate
        val workLogs = values[1] as List<WorkLogUi>
        val selectedWorkLog = values[2] as WorkLogUi?
        val multiSelectMode = values[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val multiSelectedDates = values[4] as List<LocalDate>
        @Suppress("UNCHECKED_CAST")
        val activeFilters = values[5] as List<WorkType>
        val searchQuery = values[6] as String
        val monthlyStats = values[7] as MonthlyStats

        val filteredWorkLogs = workLogs.filter { workLog ->
            val matchesFilter = activeFilters.isEmpty() ||
                    workLog.workType in activeFilters
            val matchesSearch = searchQuery.isEmpty() ||
                    workLog.formattedDate.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }

        CalendarUiState(
            selectedDate = selectedDate,
            workLogs = workLogs,
            filteredWorkLogs = filteredWorkLogs,
            selectedWorkLog = selectedWorkLog,
            isMultiSelectMode = multiSelectMode,
            multiSelectedDates = multiSelectedDates,
            activeFilters = activeFilters,
            searchQuery = searchQuery,
            monthlyStats = monthlyStats,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    init {
        loadWorkLogs()
        observeSelectedDate()
    }

    fun onDateSelected(date: LocalDate) {
        if (_multiSelectMode.value) {
            val currentDates = _multiSelectedDates.value.toMutableList()
            if (currentDates.contains(date)) {
                currentDates.remove(date)
            } else {
                currentDates.add(date)
            }
            _multiSelectedDates.value = currentDates
        } else {
            _selectedDate.value = date
        }
    }

    fun onDateLongPress(date: LocalDate) {
        if (!_multiSelectMode.value) {
            toggleMultiSelectMode()
        }
        onDateSelected(date)
    }

    fun onQuickMonthSelect(yearMonth: YearMonth) {
        displayedMonth = yearMonth
        updateMonthlyStats(yearMonth)
    }

    fun toggleMultiSelectMode() {
        _multiSelectMode.value = !_multiSelectMode.value
        _multiSelectedDates.value = emptyList()
    }

    fun selectAllDatesInMonth(yearMonth: YearMonth) {
        val daysInMonth = yearMonth.lengthOfMonth()
        val allDates = (1..daysInMonth).map { yearMonth.atDay(it) }
        _multiSelectedDates.value = allDates
    }

    fun markSelectedDates(workType: WorkType) {
        viewModelScope.launch {
            _multiSelectedDates.value.forEach { date ->
                updateWorkLog(date, workType, isMultiSelect = true)
            }
            toggleMultiSelectMode()
        }
    }

    fun toggleFilter() {
        if (_activeFilters.value.isEmpty()) {
            _activeFilters.value = WorkType.entries.toList()
        } else {
            _activeFilters.value = emptyList()
        }
    }

    fun addFilter(workType: WorkType) {
        if (workType !in _activeFilters.value) {
            _activeFilters.value = _activeFilters.value + workType
        }
    }

    fun removeFilter(workType: WorkType) {
        _activeFilters.value = _activeFilters.value - workType
    }

    fun clearFilters() {
        _activeFilters.value = emptyList()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Copies the entry to the following day (it used to create a duplicate on the same day).
     * An existing entry on that day is updated instead of adding a second one.
     */
    fun copyWorkLog(workLog: WorkLogUi) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val source = repository.getWorkLogByIdOnce(workLog.id) ?: return@launch
            val targetDate = workLog.date.toInstant().atZone(zone).toLocalDate().plusDays(1)
            val existing = repository.getWorkLogForDay(targetDate.atStartOfDay(zone).toInstant().toEpochMilli())
            if (existing != null) {
                repository.updateWorkLog(
                    existing.copy(
                        workType = source.workType,
                        startTime = source.startTime,
                        endTime = source.endTime,
                        isOvertime = source.isOvertime,
                        overtimeRate = source.overtimeRate,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                repository.insertWorkLog(
                    source.copy(
                        id = 0,
                        uuid = null,
                        date = Date.from(targetDate.atStartOfDay(zone).toInstant()),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            _messages.tryEmit("Copied to ${targetDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))}")
        }
    }

    fun shareWorkLog(workLog: WorkLogUi) {
        viewModelScope.launch {
            _shareWorkLog.emit(workLog)
        }
    }

    fun saveAsTemplate(workLog: WorkLogUi) {
        viewModelScope.launch {
            _templateWorkLog.emit(workLog)
        }
    }

    private val _shareWorkLog = MutableSharedFlow<WorkLogUi>()
    val shareWorkLog: SharedFlow<WorkLogUi> = _shareWorkLog.asSharedFlow()

    private val _templateWorkLog = MutableSharedFlow<WorkLogUi>()
    val templateWorkLog: SharedFlow<WorkLogUi> = _templateWorkLog.asSharedFlow()

    private fun observeSelectedDate() {
        viewModelScope.launch {
            _selectedDate.collect { date ->
                _selectedWorkLog.value = _workLogs.value.find {
                    it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == date
                }
                if (!_multiSelectMode.value && YearMonth.from(date) != displayedMonth) {
                    displayedMonth = YearMonth.from(date)
                    updateMonthlyStats(displayedMonth)
                }
            }
        }
    }

    fun updateWorkLog(date: LocalDate, workType: WorkType, isMultiSelect: Boolean = false) {
        viewModelScope.launch {
            val existingWorkLog = _workLogs.value.find {
                it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == date
            }

            val isWorkingDay = workType != WorkType.OFF_DAY
            val stored = existingWorkLog?.let { repository.getWorkLogByIdOnce(it.id) }
            if (stored != null) {
                // Copy the stored row so overtime flags/rates and audit fields aren't lost
                repository.updateWorkLog(
                    stored.copy(
                        workType = workType,
                        startTime = if (isWorkingDay) stored.startTime ?: "09:00" else null,
                        endTime = if (isWorkingDay) stored.endTime ?: "17:00" else null,
                        isOvertime = workType == WorkType.OVERTIME || (stored.isOvertime && workType == stored.workType),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                val workLog = WorkLog(
                    date = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                    workType = workType,
                    startTime = if (isWorkingDay) "09:00" else null,
                    endTime = if (isWorkingDay) "17:00" else null,
                    isOvertime = workType == WorkType.OVERTIME
                )
                repository.insertWorkLog(workLog)
            }
        }
    }

    fun deleteWorkLog(id: Long) {
        viewModelScope.launch {
            repository.deleteWorkLogById(id)
        }
    }

    private fun updateMonthlyStats(yearMonth: YearMonth) {
        viewModelScope.launch {
            val monthWorkLogs = _workLogs.value.filter { workLog ->
                val date = workLog.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                YearMonth.from(date) == yearMonth
            }

            val stats = MonthlyStats(
                officeDays = monthWorkLogs.count { it.workType == WorkType.OFFICE },
                homeDays = monthWorkLogs.count { it.workType == WorkType.HOME_OFFICE },
                offDays = monthWorkLogs.count { it.workType == WorkType.OFF_DAY },
                extraDays = monthWorkLogs.count { it.workType == WorkType.EXTRA_WORK },
                totalHours = calculateTotalHours(monthWorkLogs)
            )

            _monthlyStats.value = stats
        }
    }

    private fun calculateTotalHours(workLogs: List<WorkLogUi>): String {
        var totalMinutes = 0
        workLogs.forEach { workLog ->
            totalMinutes += when {
                // Off days used to add a default 8h to the monthly total
                workLog.workType == WorkType.OFF_DAY -> 0
                DateTimeUtils.isValidTime(workLog.startTime) && DateTimeUtils.isValidTime(workLog.endTime) ->
                    (DateTimeUtils.hoursBetween(workLog.startTime, workLog.endTime) * 60).toInt()
                else -> 8 * 60
            }
        }

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
    }

    private fun loadWorkLogs() {
        viewModelScope.launch {
            repository.getAllWorkLogs().collect { workLogs ->
                _workLogs.value = workLogs.map { workLog ->
                    WorkLogUi(
                        id = workLog.id,
                        date = workLog.date,
                        workType = workLog.workType,
                        formattedDate = formatDate(workLog.date),
                        duration = calculateDuration(workLog.startTime, workLog.endTime),
                        startTime = workLog.startTime,
                        endTime = workLog.endTime
                    )
                }
                _selectedWorkLog.value = _workLogs.value.find {
                    it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == _selectedDate.value
                }
                updateMonthlyStats(displayedMonth)
            }
        }
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }

    private fun calculateDuration(startTime: String?, endTime: String?): String =
        DateTimeUtils.formatDuration(startTime, endTime)

    companion object {
        fun factory(appDatabase: AppDatabase): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                        val workLogRepository = WorkLogRepository(appDatabase.workLogDao())
                        return CalendarViewModel(workLogRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val workLogs: List<WorkLogUi> = emptyList(),
    val filteredWorkLogs: List<WorkLogUi> = emptyList(),
    val selectedWorkLog: WorkLogUi? = null,
    val isMultiSelectMode: Boolean = false,
    val multiSelectedDates: List<LocalDate> = emptyList(),
    val activeFilters: List<WorkType> = emptyList(),
    val searchQuery: String = "",
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val isLoading: Boolean = true
)
