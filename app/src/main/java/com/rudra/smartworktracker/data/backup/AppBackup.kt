package com.rudra.smartworktracker.data.backup

import com.rudra.smartworktracker.data.entity.*
import com.rudra.smartworktracker.model.*

/**
 * Data class representing the entire database for backup/restore purposes.
 * Rule 3.1: Export all tables to JSON, including schema and app version.
 */
data class AppBackup(
    val version: Int,
    val appVersion: String,
    val timestamp: Long,
    val settings: List<Settings> = emptyList(),
    val userProfile: List<UserProfile> = emptyList(),
    val dailyJournals: List<DailyJournal> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val focusSessions: List<FocusSession> = emptyList(),
    val workSessions: List<WorkSession> = emptyList(),
    val healthMetrics: List<HealthMetric> = emptyList(),
    val workDays: List<WorkDay> = emptyList(),
    val workLogs: List<WorkLog> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val colleagues: List<Colleague> = emptyList(),
    val travelExpenses: List<TravelAndExpense> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val monthlyInputs: List<MonthlyInput> = emptyList(),
    val calculations: List<Calculation> = emptyList(),
    val financialTransactions: List<FinancialTransaction> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val emis: List<Emi> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val creditCardTransactions: List<CreditCardTransaction> = emptyList(),
    val savings: List<Savings> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val meals: List<Meal> = emptyList(),
    // Added later; nullable because Gson leaves fields missing from older backup files as null
    val accounts: List<Account>? = null,
    val recurringRules: List<RecurringRule>? = null,
    val recurringTransactions: List<RecurringTransaction>? = null,
    val billSplits: List<BillSplit>? = null,
    val realityEntries: List<RealityEntry>? = null,
    val decisions: List<Decision>? = null,
    val executionHistory: List<ExecutionHistoryEntity>? = null,
    // Life plan (Wisdom) and Future Self data
    val lifePlanGoals: List<Goal>? = null,
    val lifePlanTargets: List<com.rudra.smartworktracker.model.Target>? = null,
    val gamificationStats: UserStatsEntity? = null,
    val checkIns: List<DailyCheckIn>? = null,
    val consequenceDebts: List<ConsequenceDebt>? = null,
    val weeklyReports: List<WeeklyReport>? = null,
    val userHistory: UserHistory? = null
)
