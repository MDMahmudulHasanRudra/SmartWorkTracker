package com.rudra.smartworktracker.ui.screens.overtime

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.SettingsRepository
import com.rudra.smartworktracker.data.repository.WorkLogRepository
import com.rudra.smartworktracker.model.WorkLog
import com.rudra.smartworktracker.model.WorkType
import com.rudra.smartworktracker.utils.DateTimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OvertimeViewModel(application: Application) : AndroidViewModel(application) {

    private val workLogRepository: WorkLogRepository

    private val _monthlyOvertimeLogs = MutableStateFlow<List<WorkLog>>(emptyList())
    val monthlyOvertimeLogs: StateFlow<List<WorkLog>> = _monthlyOvertimeLogs.asStateFlow()

    private val _yearlyOvertimeLogs = MutableStateFlow<List<WorkLog>>(emptyList())
    val yearlyOvertimeLogs: StateFlow<List<WorkLog>> = _yearlyOvertimeLogs.asStateFlow()

    val monthlySummary: Flow<OvertimeSummary> = monthlyOvertimeLogs.map { logs -> calculateSummary(logs) }
    val yearlySummary: Flow<OvertimeSummary> = yearlyOvertimeLogs.map { logs -> calculateSummary(logs) }

    /** Overtime rate from Settings, used to pre-fill new entries. */
    val defaultOvertimeRate: StateFlow<Double> = SettingsRepository(application).overtimeRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    init {
        val workLogDao = AppDatabase.getDatabase(application).workLogDao()
        workLogRepository = WorkLogRepository(workLogDao)
        loadOvertimeData()
    }

    private fun loadOvertimeData() {
        val calendar = Calendar.getInstance()
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.time)

        viewModelScope.launch {
            workLogRepository.getOvertimeLogsByMonth(monthYear).collect { _monthlyOvertimeLogs.value = it }
        }
        viewModelScope.launch {
            workLogRepository.getOvertimeLogsByYear(year).collect { _yearlyOvertimeLogs.value = it }
        }
    }

    fun saveOvertime(date: Date, startTime: String, endTime: String, overtimeRate: Double): Boolean {
        if (!isValidTime(startTime) || !isValidTime(endTime)) return false
        
        viewModelScope.launch {
            val workLog = WorkLog(
                date = date,
                workType = WorkType.OVERTIME,
                startTime = startTime,
                endTime = endTime,
                isOvertime = true,
                overtimeRate = overtimeRate
            )
            workLogRepository.insertWorkLog(workLog)
        }
        return true
    }

    private fun isValidTime(time: String): Boolean {
        val regex = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
        return regex.matches(time)
    }

    fun deleteOvertime(workLog: WorkLog) {
        viewModelScope.launch {
            workLogRepository.deleteWorkLog(workLog)
        }
    }

    // Handles cross-midnight shifts (e.g. 22:00 to 02:00)
    fun calculateDuration(startTime: String?, endTime: String?): Double =
        DateTimeUtils.hoursBetween(startTime, endTime)

    private fun calculateSummary(logs: List<WorkLog>): OvertimeSummary {
        var totalHours = 0.0
        var totalEarnings = 0.0
        logs.forEach { log ->
            val duration = calculateDuration(log.startTime, log.endTime)
            totalHours += duration
            totalEarnings += duration * (log.overtimeRate ?: 0.0)
        }
        return OvertimeSummary(totalHours, totalEarnings)
    }

    fun getMonthName() = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    fun getYearString() = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
}

data class OvertimeSummary(val totalHours: Double = 0.0, val totalEarnings: Double = 0.0)
