package com.rudra.smartworktracker.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.repository.WorkLogRepository
import com.rudra.smartworktracker.model.WorkLog
import com.rudra.smartworktracker.model.WorkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class MonthlyReportUiState(
    val selectedMonth: String = "",
    val workLogs: List<WorkLog> = emptyList(),
    val officeCount: Int = 0,
    val homeCount: Int = 0,
    val offCount: Int = 0,
    val extraCount: Int = 0
)

class MonthlyReportViewModel(private val workLogRepository: WorkLogRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyReportUiState())
    val uiState: StateFlow<MonthlyReportUiState> = _uiState.asStateFlow()

    // java.time avoids the lenient-Calendar bug where setting MONTH on the 29th-31st
    // rolled into the next month (e.g. "March" listed twice, "February" missing)
    val months: List<String> = java.time.Month.entries.map {
        it.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
    }

    private var collectJob: Job? = null

    init {
        val currentMonth = months[Calendar.getInstance().get(Calendar.MONTH)]
        onMonthSelected(currentMonth)
    }

    /** Shows the selected month of the current year (logs from other years are excluded). */
    fun onMonthSelected(month: String) {
        val monthIndex = months.indexOf(month).takeIf { it >= 0 } ?: return
        val year = Calendar.getInstance().get(Calendar.YEAR)
        // One collector at a time; older ones kept overwriting the state with their month
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            workLogRepository.getAllWorkLogs().collect { allLogs ->
                val filteredLogs = allLogs.filter {
                    val logCalendar = Calendar.getInstance()
                    logCalendar.time = it.date
                    logCalendar.get(Calendar.MONTH) == monthIndex && logCalendar.get(Calendar.YEAR) == year
                }
                _uiState.value = MonthlyReportUiState(
                    selectedMonth = month,
                    workLogs = filteredLogs,
                    officeCount = filteredLogs.count { it.workType == WorkType.OFFICE },
                    homeCount = filteredLogs.count { it.workType == WorkType.HOME_OFFICE },
                    offCount = filteredLogs.count { it.workType == WorkType.OFF_DAY },
                    extraCount = filteredLogs.count { it.workType == WorkType.EXTRA_WORK || it.workType == WorkType.OVERTIME }
                )
            }
        }
    }
}
