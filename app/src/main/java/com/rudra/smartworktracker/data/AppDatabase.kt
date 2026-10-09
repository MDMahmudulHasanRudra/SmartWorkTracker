package com.rudra.smartworktracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        BillSplit::class,
        Goal::class,
        com.rudra.smartworktracker.model.Target::class,
        UserStatsEntity::class
    ],
    views = [
        MonthlySummary::class
    ],
    version = 14, // v14: life-plan goals/targets + gamification stats (Wisdom screen persistence)
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
    abstract fun lifePlanDao(): LifePlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Adds the nullable account link columns without touching existing rows. */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `incomes` ADD COLUMN `accountId` INTEGER")
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `accountId` INTEGER")
            }
        }

        /** Creates the life-plan tables; SQL mirrors Room's generated schema so validation passes. */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `life_plan_goals` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `targetDate` INTEGER, `createdAt` INTEGER NOT NULL, `totalTargets` INTEGER NOT NULL, `completedTargets` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `life_plan_targets` (`id` TEXT NOT NULL, `goalId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `completedAt` INTEGER, `order` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`goalId`) REFERENCES `life_plan_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_life_plan_targets_goalId` ON `life_plan_targets` (`goalId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_gamification_stats` (`id` INTEGER NOT NULL, `experiencePoints` INTEGER NOT NULL, `level` INTEGER NOT NULL, `streak` INTEGER NOT NULL, `totalGoalsCompleted` INTEGER NOT NULL, `lastActiveDate` TEXT NOT NULL, `streakProtectionAvailable` INTEGER NOT NULL, `xpMultiplier` REAL NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        /**
         * Returns the single instance of AppDatabase.
         * Uses a unique database name 'smart_work_tracker_v2' to avoid conflicts with 
         * corrupted legacy versions and ensure a clean, migration-free schema.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return it }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_work_tracker_v2"
                )
                .addMigrations(MIGRATION_12_13, MIGRATION_13_14)
                // Only used when no migration path exists (e.g. very old pre-release schemas)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
