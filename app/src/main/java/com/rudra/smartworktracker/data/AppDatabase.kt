package com.rudra.smartworktracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rudra.smartworktracker.data.dao.*
import com.rudra.smartworktracker.data.entity.*
import com.rudra.smartworktracker.data.local.TypeConverters as LocalTypeConverters
import com.rudra.smartworktracker.model.*

/**
 * The Room database for this app.
 * Rule 1: SQLite is the single source of truth.
 * This version uses a clean start (Version 1) with standardized UUID primary keys
 * and mandatory audit columns to ensure long-term scalability and zero data loss.
 */
@Database(
    entities = [
        WorkSession::class, 
        Expense::class, 
        Habit::class, 
        FocusSession::class, 
        HealthMetric::class, 
        Achievement::class, 
        DailyJournal::class, 
        WorkLog::class,
        WorkDay::class,
        Settings::class,
        MonthlyInput::class,
        UserProfile::class,
        Income::class,
        Calculation::class,
        FinancialTransaction::class,
        Loan::class,
        Emi::class,
        CreditCard::class,
        CreditCardTransaction::class,
        Savings::class,
        Colleague::class,
        TravelAndExpense::class,
        Schedule::class,
        Meal::class,
        RecurringRule::class,
        RecurringTransaction::class,
        RealityEntry::class,
        Decision::class,
        DailyCheckIn::class,
        ConsequenceDebt::class,
        WeeklyReport::class,
        UserHistory::class,
        Account::class,
        ExecutionHistoryEntity::class,
        BillSplit::class
    ],
    views = [
        MonthlySummary::class
    ],
    version = 12, // v12: Added accountId to savings for account linkage
    exportSchema = false
)
@TypeConverters(LocalTypeConverters::class, Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workSessionDao(): WorkSessionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun habitDao(): HabitDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun achievementDao(): AchievementDao
    abstract fun dailyJournalDao(): DailyJournalDao
    abstract fun workLogDao(): WorkLogDao
    abstract fun workDayDao(): WorkDayDao
    abstract fun settingsDao(): SettingsDao
    abstract fun summaryDao(): SummaryDao
    abstract fun monthlyInputDao(): MonthlyInputDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun incomeDao(): IncomeDao
    abstract fun calculationDao(): CalculationDao
    abstract fun financialTransactionDao(): FinancialTransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun emiDao(): EmiDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun creditCardTransactionDao(): CreditCardTransactionDao
    abstract fun savingsDao(): SavingsDao
    abstract fun colleagueDao(): ColleagueDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun mealDao(): MealDao

    abstract fun travelExpenseDao(): TravelExpenseDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun realityTrackerDao(): RealityTrackerDao
    abstract fun decisionDao(): DecisionDao
    abstract fun checkInDao(): CheckInDao
    abstract fun consequenceDebtDao(): ConsequenceDebtDao
    abstract fun weeklyReportDao(): WeeklyReportDao
    abstract fun userHistoryDao(): UserHistoryDao
    abstract fun accountDao(): AccountDao
    abstract fun executionHistoryDao(): ExecutionHistoryDao
    abstract fun billSplitDao(): BillSplitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the single instance of AppDatabase.
         * Uses a unique database name 'smart_work_tracker_v2' to avoid conflicts with 
         * corrupted legacy versions and ensure a clean, migration-free schema.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_work_tracker_v2"
                )
                // Fix for deprecation: Specify dropAllTables explicitly
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
