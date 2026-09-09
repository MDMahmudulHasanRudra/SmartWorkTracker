package com.rudra.smartworktracker.ui

import com.rudra.smartworktracker.data.entity.Income
import com.rudra.smartworktracker.data.repository.SettingsRepository.Companion.DARK_THEME
import com.rudra.smartworktracker.data.repository.SettingsRepository.Companion.NOTIFICATIONS
import com.rudra.smartworktracker.data.repository.SettingsRepository.Companion.VIBRATION
import com.rudra.smartworktracker.model.Expense
import com.rudra.smartworktracker.model.ExpenseCategory
import com.rudra.smartworktracker.model.HealthMetricType
import com.rudra.smartworktracker.model.WorkLog
import com.rudra.smartworktracker.model.WorkType
import java.time.LocalDate

data class DashboardUiState(
    val userName: String? = null,
    val todayWorkType: WorkType? = null,
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val recentActivities: List<WorkLogUi> = emptyList(),
    val financialSummary: FinancialSummary = FinancialSummary(),
    val expensesByCategory: Map<ExpenseCategory, Double> = emptyMap(),
    val incomesByCategory: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val incomes: List<Income> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val workLogs: List<WorkLog> = emptyList(),
    val accounts: List<com.rudra.smartworktracker.data.entity.Account> = emptyList()
)

data class MonthlyStats(
    val officeDays: Int = 0,
    val homeOfficeDays: Int = 0,
    val offDays: Int = 0,
    val extraHours: Double = 0.0,
    val totalWorkDays: Int = 0
) {
    val totalDays: Int
        get() = officeDays + homeOfficeDays + offDays
}

data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalSavings: Double = 0.0,
    val netSavings: Double = 0.0,
    val monthlyNetSavings: Double = 0.0,
    val dailyIncome: Double = 0.0,
    val dailyExpense: Double = 0.0,
    val dailySavings: Double = 0.0,
    val totalMealCost: Double = 0.0,
    val expenseBreakdown: Map<String, Double> = emptyMap(),
    val totalLoan: Double = 0.0,
    val totalOfficeDays: Int = 0,
    val totalOffDays: Int = 0,
    val overtimeHours: Double = 0.0,
    val overtimeEarnings: Double = 0.0,
    val allTimeIncome: Double = 0.0,
    val allTimeExpense: Double = 0.0
) {
    val savingsPercentage: Double
        get() = if (totalIncome > 0) (monthlyNetSavings / totalIncome) * 100 else 0.0
}
data class HealthData(
    val currentWeight: Double? = null,
    val currentHeight: Double? = null,
    val currentBMI: Double? = null,
    val weightGoal: Double? = null,
    val weightProgress: List<Pair<LocalDate, Double>> = emptyList(),
    val recentEntries: List<HealthMetricEntry> = emptyList()
)

data class HealthMetricEntry(
    val type: HealthMetricType,
    val value: Double,
    val date: LocalDate = LocalDate.now()
)

data class WorkLogUi(
    val id: Long,
    val date: java.util.Date,
    val workType: com.rudra.smartworktracker.model.WorkType,
    val formattedDate: String,
    val duration: String,
    val startTime: String?,
    val endTime: String?,
    val notes: String? = null
) {
    val isCompleted: Boolean
        get() = startTime != null && endTime != null
}

data class SettingsUiState(
    val userName: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val currency: String = "BDT",
    val workDayHours: Double = 8.0
) {
    companion object {
        fun fromPreferences(preferences: Map<String, Boolean>): SettingsUiState {
            return SettingsUiState(
                notificationsEnabled = preferences[NOTIFICATIONS] ?: true,
                darkThemeEnabled = preferences[DARK_THEME] ?: false,
                vibrationEnabled = preferences[VIBRATION] ?: true
            )
        }
    }
}

data class AddEntryUiState(
    val expenseAmount: String = "",
    val expenseCategory: ExpenseCategory = ExpenseCategory.OTHER,
    val expenseNotes: String = "",
    val workType: WorkType = WorkType.OFFICE,
    val workStartTime: String = "",
    val workEndTime: String = "",
    val mealAmount: String = "",
    val mealNotes: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEntrySaved: Boolean = false,
    val selectedEntryType: EntryType = EntryType.WORK_TIME
)

data class StatisticsUiState(
    val period: TimePeriod = TimePeriod.MONTHLY,
    val workTypeDistribution: Map<WorkType, Int> = emptyMap(),
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val financialOverview: FinancialOverview = FinancialOverview(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class FinancialOverview(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val monthlySavings: List<MonthlySaving> = emptyList()
)

data class MonthlyTrend(
    val month: String,
    val officeDays: Int,
    val homeOfficeDays: Int,
    val totalHours: Double
)

data class MonthlySaving(
    val month: String,
    val income: Double,
    val expenses: Double,
    val savings: Double
)

enum class TimePeriod {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

enum class EntryType {
    EXPENSE, WORK_TIME, MEAL
}

data class WorkLogDetailUiState(
    val workLog: WorkLogUi? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditing: Boolean = false
)

data class ExpensesUiState(
    val expenses: List<ExpenseUi> = emptyList(),
    val selectedCategory: String? = null,
    val dateRange: ClosedRange<LocalDate>? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ExpenseUi(
    val id: Long,
    val amount: Double,
    val category: String,
    val description: String,
    val date: LocalDate,
    val formattedDate: String,
    val receiptUrl: String? = null
)

val DashboardUiState.hasData: Boolean
    get() = !isLoading && errorMessage == null && recentActivities.isNotEmpty()

val FinancialSummary.hasFinancialData: Boolean
    get() = totalIncome > 0 || totalExpense > 0

val MonthlyStats.hasWorkData: Boolean
    get() = totalDays > 0

fun defaultDashboardUiState(): DashboardUiState {
    return DashboardUiState(
        userName = "User",
        monthlyStats = MonthlyStats(),
        financialSummary = FinancialSummary(),
        recentActivities = emptyList(),
        isLoading = true
    )
}

fun defaultSettingsUiState(): SettingsUiState {
    return SettingsUiState(
        userName = "User",
        notificationsEnabled = true,
        darkThemeEnabled = false,
        vibrationEnabled = true
    )
}
