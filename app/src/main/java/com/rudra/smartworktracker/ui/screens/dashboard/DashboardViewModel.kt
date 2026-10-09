package com.rudra.smartworktracker.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.repository.*
import com.rudra.smartworktracker.model.Expense
import com.rudra.smartworktracker.model.ExpenseByCategory
import com.rudra.smartworktracker.model.ExpenseCategory
import com.rudra.smartworktracker.model.IncomeByCategory
import com.rudra.smartworktracker.model.WorkLog
import com.rudra.smartworktracker.model.WorkType
import com.rudra.smartworktracker.ui.DashboardUiState
import com.rudra.smartworktracker.ui.FinancialSummary
import com.rudra.smartworktracker.ui.MonthlyStats
import com.rudra.smartworktracker.ui.WorkLogUi
import com.rudra.smartworktracker.utils.CurrencyManager
import com.rudra.smartworktracker.utils.DateTimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(
    private val workLogRepository: WorkLogRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val savingsRepository: SavingsRepository,
    private val settingsRepository: SettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val accountRepository: AccountRepository? = null
) : ViewModel() {

    private val _uiSate = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiSate.asStateFlow()

    private val _heroColor = MutableStateFlow(0)
    val heroColor: StateFlow<Int> = _heroColor.asStateFlow()

    private val _selectedAccountId = MutableStateFlow(-1L)
    val selectedAccountId: StateFlow<Long> = _selectedAccountId.asStateFlow()

    init {
        loadHeroColor()
        loadSelectedAccountId()
        loadDashboardData()
        loadAccounts()
    }

    private fun loadHeroColor() {
        viewModelScope.launch {
            settingsRepository.heroColor.collect { color ->
                _heroColor.value = color
            }
        }
    }

    private fun loadSelectedAccountId() {
        viewModelScope.launch {
            settingsRepository.selectedAccountId.collect { id ->
                _selectedAccountId.value = id
            }
        }
    }

    fun setHeroColor(color: Int) {
        viewModelScope.launch {
            settingsRepository.setHeroColor(color)
        }
    }

    fun setSelectedAccountId(id: Long) {
        viewModelScope.launch {
            settingsRepository.setSelectedAccountId(id)
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository?.getAllAccounts()?.collect { accounts ->
                _uiSate.value = _uiSate.value.copy(accounts = accounts)
            }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            
            // Monthly range
            val startTime = (calendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val endTime = (calendar.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.DATE, -1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // Today range
            val todayStart = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val todayEnd = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            val now = System.currentTimeMillis()

            val flows = listOf(
                workLogRepository.getTodayWorkLog(),
                expenseRepository.getTotalExpensesBetween(startTime, endTime),
                expenseRepository.getMealExpensesBetween(startTime, endTime),
                incomeRepository.getTotalIncomeBetween(startTime, endTime),
                savingsRepository.getSavingsBetween(startTime, endTime),
                settingsRepository.mealRate,
                workLogRepository.getRecentActivities(),
                expenseRepository.getExpensesByCategoryBetween(startTime, endTime),
                incomeRepository.getIncomesByCategoryBetween(startTime, endTime),
                workLogRepository.getMonthlyStats(),
                incomeRepository.getIncomes(1, 50),
                expenseRepository.getExpenses(1, 50),
                workLogRepository.getWorkLogs(1, 50),
                userProfileRepository.userProfile,
                incomeRepository.getTotalIncomeUpTo(now),
                expenseRepository.getTotalExpensesUpTo(now),
                incomeRepository.getTotalIncomeBetween(todayStart, todayEnd),
                expenseRepository.getTotalExpensesBetween(todayStart, todayEnd)
            )

            combine(flows) { array ->
                val todayWorkLog = array[0] as? WorkLog
                val totalExpense = array[1] as? Double ?: 0.0
                val monthlyMealExpenses = array[2] as? Double ?: 0.0
                val totalIncome = array[3] as? Double ?: 0.0
                val totalSavings = array[4] as? Double ?: 0.0
                val recentActivities = array[6] as? List<WorkLog> ?: emptyList()
                val expensesByCategory = array[7] as? List<ExpenseByCategory> ?: emptyList()
                val incomesByCategory = array[8] as? List<IncomeByCategory> ?: emptyList()
                val monthlyStats = array[9] as MonthlyStats
                val incomes = array[10] as? List<com.rudra.smartworktracker.data.entity.Income> ?: emptyList()
                val expenses = array[11] as? List<Expense> ?: emptyList()
                val workLogs = array[12] as? List<WorkLog> ?: emptyList()
                val userProfile = array[13] as? com.rudra.smartworktracker.data.entity.UserProfile

                val allTimeIncome = array[14] as? Double ?: 0.0
                val allTimeExpense = array[15] as? Double ?: 0.0
                val todayIncome = array[16] as? Double ?: 0.0
                val todayExpense = array[17] as? Double ?: 0.0

                val dailyIncome = todayIncome
                val dailyExpense = todayExpense
                val dailySavings = dailyIncome - dailyExpense

                // Overtime for the current month only, matching the other monthly figures
                val monthOvertimeLogs = workLogs.filter {
                    it.isOvertime && it.date.time in startTime..endTime
                }
                val overtimeHours = monthOvertimeLogs.sumOf { DateTimeUtils.hoursBetween(it.startTime, it.endTime) }
                val overtimeEarnings = monthOvertimeLogs.sumOf {
                    DateTimeUtils.hoursBetween(it.startTime, it.endTime) * (it.overtimeRate ?: 0.0)
                }

                val netSavings = totalIncome - totalExpense
                val allTimeNetSavings = allTimeIncome - allTimeExpense
                val expensesByCategoryMap = expensesByCategory.associate { it.category to it.total }
                val incomesByCategoryMap = incomesByCategory.associate { it.category to it.total }

                DashboardUiState(
                    userName = userProfile?.name?.takeIf { it.isNotBlank() },
                    workStreak = calculateWorkStreak(workLogs),
                    todayWorkType = todayWorkLog?.workType,
                    monthlyStats = monthlyStats,
                    recentActivities = recentActivities.map { it.toUiModel() },
                    financialSummary = FinancialSummary(
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        totalSavings = totalSavings,
                        netSavings = allTimeNetSavings,
                        monthlyNetSavings = netSavings,
                        dailyIncome = dailyIncome,
                        dailyExpense = dailyExpense,
                        dailySavings = dailySavings,
                        totalMealCost = monthlyMealExpenses,
                        overtimeHours = overtimeHours,
                        overtimeEarnings = overtimeEarnings,
                        allTimeIncome = allTimeIncome,
                        allTimeExpense = allTimeExpense
                    ),
                    expensesByCategory = expensesByCategoryMap,
                    incomesByCategory = incomesByCategoryMap,
                    incomes = incomes,
                    expenses = expenses,
                    workLogs = workLogs
                )
            }.collect { newState ->
                // Accounts come from a separate collector; keep them instead of resetting on every emission
                _uiSate.value = newState.copy(accounts = _uiSate.value.accounts)
            }
        }
    }

    /**
     * Sets today's work type. Updates today's existing log instead of inserting a new one on
     * every tap, and only adds the office meal expense when the day first becomes an office day.
     */
    fun updateTodayWorkType(workType: WorkType) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = workLogRepository.getWorkLogForDay(now)
            if (existing?.workType == workType) return@launch

            val isWorkingDay = workType != WorkType.OFF_DAY
            val dailyHours = settingsRepository.dailyWorkHours.first()
            val startTime = existing?.startTime?.takeIf { DateTimeUtils.isValidTime(it) } ?: DEFAULT_START_TIME
            val endTime = existing?.endTime?.takeIf { DateTimeUtils.isValidTime(it) } ?: endTimeAfter(startTime, dailyHours)
            val isOvertime = workType == WorkType.OVERTIME
            val base = existing ?: WorkLog(date = Date(now), workType = workType, startTime = null, endTime = null)

            val workLog = base.copy(
                workType = workType,
                startTime = if (isWorkingDay) startTime else null,
                endTime = if (isWorkingDay) endTime else null,
                isOvertime = isOvertime,
                overtimeRate = if (isOvertime) settingsRepository.overtimeRate.first() else base.overtimeRate,
                updatedAt = now
            )
            if (existing == null) workLogRepository.insertWorkLog(workLog) else workLogRepository.updateWorkLog(workLog)

            if (workType == WorkType.OFFICE) {
                val mealRate = settingsRepository.mealRate.first()
                val mealExpense = Expense(
                    amount = mealRate,
                    category = ExpenseCategory.MEAL,
                    timestamp = now,
                    currency = CurrencyManager.getCurrencyCode(),
                    merchant = "Office Canteen",
                    notes = "Auto-generated meal expense for office day",
                    imageUri = null
                )
                expenseRepository.insertExpense(mealExpense)
            }
        }
    }

    private fun endTimeAfter(start: String, hours: Double): String {
        val startMinutes = DateTimeUtils.minutesOfDay(start) ?: (9 * 60)
        val end = (startMinutes + (hours * 60).toInt()).mod(24 * 60)
        return String.format(Locale.US, "%02d:%02d", end / 60, end % 60)
    }

    /** Consecutive working days (anything but an off day) ending today, or yesterday when today has no log yet. */
    private fun calculateWorkStreak(logs: List<WorkLog>): Int {
        val zone = java.time.ZoneId.systemDefault()
        val workedDays = logs
            .filter { it.workType != WorkType.OFF_DAY }
            .map { java.time.Instant.ofEpochMilli(it.date.time).atZone(zone).toLocalDate() }
            .toSet()
        var day = java.time.LocalDate.now(zone)
        if (day !in workedDays) day = day.minusDays(1)
        var streak = 0
        while (day in workedDays) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    private fun WorkLog.toUiModel(): WorkLogUi {
        return WorkLogUi(
            id = this.id,
            date = this.date,
            workType = this.workType,
            formattedDate = formatDate(this.date),
            duration = calculateDuration(this.startTime, this.endTime),
            startTime = this.startTime,
            endTime = this.endTime
        )
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }

    private fun calculateDuration(startTime: String?, endTime: String?): String =
        DateTimeUtils.formatDuration(startTime, endTime)

    companion object {
        private const val DEFAULT_START_TIME = "09:00"

        fun factory(appDatabase: AppDatabase, context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                        val workLogRepository = WorkLogRepository(appDatabase.workLogDao())
                        val expenseRepository = ExpenseRepository(appDatabase.expenseDao())
                        val incomeRepository = IncomeRepository(appDatabase.incomeDao())
                        val savingsRepository = SavingsRepository(appDatabase.savingsDao())
                        val settingsRepository = SettingsRepository(context)
                        val userProfileRepository = UserProfileRepository(appDatabase.userProfileDao())
                        val accountRepository = AccountRepository(appDatabase.accountDao())
                        return DashboardViewModel(workLogRepository, expenseRepository, incomeRepository, savingsRepository, settingsRepository, userProfileRepository, accountRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}
