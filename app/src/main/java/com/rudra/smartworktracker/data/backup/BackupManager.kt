package com.rudra.smartworktracker.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rudra.smartworktracker.alarm.AlarmScheduler
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.SettingsRepository
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.*

/**
 * Manager for handling database backups and restores in JSON format.
 * Rule 3.1: Primary backup = JSON export (SAFE).
 * Rule 3.2: Restore logic via Room transactions.
 */
class BackupManager(private val context: Context) {
    companion object {
        const val BACKUP_FORMAT_VERSION = 33
    }

    private val db = AppDatabase.getDatabase(context)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToJson(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = AppBackup(
                version = BACKUP_FORMAT_VERSION,
                appVersion = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: "unknown",
                timestamp = System.currentTimeMillis(),
                settings = db.settingsDao().getAllSettings().first(),
                userProfile = db.userProfileDao().getUserProfile().first()?.let { listOf(it) } ?: emptyList(),
                dailyJournals = db.dailyJournalDao().getAllJournals().first(),
                habits = db.habitDao().getAllHabits().first(),
                focusSessions = db.focusSessionDao().getAllFocusSessions().first(),
                workSessions = db.workSessionDao().getAllWorkSessions().first(),
                healthMetrics = db.healthMetricDao().getAllHealthMetrics().first(),
                workDays = db.workDayDao().getAllWorkDays().first(),
                workLogs = db.workLogDao().getAllWorkLogs().first(),
                achievements = db.achievementDao().getAllAchievements().first(),
                colleagues = db.colleagueDao().getAllColleagues().first(),
                expenses = db.expenseDao().getAllExpenses().first(),
                incomes = db.incomeDao().getAllIncomes().first(),
                monthlyInputs = db.monthlyInputDao().getAllMonthlyInputs().first(),
                calculations = db.calculationDao().getCalculations().first(),
                financialTransactions = db.financialTransactionDao().getAllTransactions().first(),
                loans = db.loanDao().getAllLoans().first(),
                emis = db.emiDao().getAllEmis().first(),
                creditCards = db.creditCardDao().getAllCreditCards().first(),
                creditCardTransactions = db.creditCardTransactionDao().getAllTransactions().first(),
                savings = db.savingsDao().getAllSavings().first(),
                schedules = db.scheduleDao().getAllSchedules().first(),
                meals = db.mealDao().getAllMeals().first(),
                travelExpenses = db.travelExpenseDao().getTravelExpense().first()?.let { listOf(it) } ?: emptyList(),
                accounts = db.accountDao().getAllAccountsIncludingInactive(),
                recurringRules = db.recurringRuleDao().getAllRules().first(),
                recurringTransactions = db.recurringTransactionDao().getAllTransactions().first(),
                billSplits = db.billSplitDao().getAll().first(),
                realityEntries = db.realityTrackerDao().getAllEntries().first(),
                decisions = db.decisionDao().getAllDecisions().first(),
                executionHistory = db.executionHistoryDao().getAll().first(),
                lifePlanGoals = db.lifePlanDao().getAllGoals().first(),
                lifePlanTargets = db.lifePlanDao().getAllTargets().first(),
                gamificationStats = db.lifePlanDao().getUserStats().first(),
                checkIns = db.checkInDao().getAllCheckIns().first(),
                consequenceDebts = db.consequenceDebtDao().getAllDebts().first(),
                weeklyReports = db.weeklyReportDao().getAllReports().first(),
                userHistory = db.userHistoryDao().getUserHistory().first()
            )

            val jsonString = gson.toJson(backup)
            BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { it.write(jsonString) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Restored schedules need live alarms again; currency may have changed with the settings. */
    private suspend fun afterRestore() {
        runCatching {
            AlarmScheduler(context).rescheduleAll(db.scheduleDao().getAllSchedulesOnce())
        }
        runCatching {
            CurrencyManager.setCurrency(SettingsRepository(context).currency.first())
        }
    }

    /** Parses a backup without restoring it; null when the file isn't a backup of this app. */
    suspend fun readSummary(inputStream: InputStream): BackupSummary? = withContext(Dispatchers.IO) {
        try {
            val backup = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use {
                gson.fromJson(it, AppBackup::class.java)
            } ?: return@withContext null
            if (backup.timestamp <= 0L) return@withContext null
            val counts = linkedMapOf(
                "Work logs" to backup.workLogs.size,
                "Incomes" to backup.incomes.size,
                "Expenses" to backup.expenses.size,
                "Accounts" to (backup.accounts?.size ?: 0),
                "Savings" to backup.savings.size,
                "Loans" to backup.loans.size,
                "EMIs" to backup.emis.size,
                "Credit cards" to backup.creditCards.size,
                "Recurring rules" to (backup.recurringRules?.size ?: 0),
                "Schedules" to backup.schedules.size,
                "Habits" to backup.habits.size,
                "Journal entries" to backup.dailyJournals.size,
                "Health entries" to backup.healthMetrics.size,
                "Goals" to (backup.lifePlanGoals?.size ?: 0),
                "Decisions" to (backup.decisions?.size ?: 0)
            ).filterValues { it > 0 }
            BackupSummary(
                timestamp = backup.timestamp,
                formatVersion = backup.version,
                appVersion = backup.appVersion,
                counts = counts
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun importFromJson(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backup = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use {
                gson.fromJson(it, AppBackup::class.java)
            } ?: return@withContext Result.failure(Exception("Failed to parse backup file"))

            // Rule 3.2: Safe import via transaction.
            // Restore uses upsert/replace everywhere so restoring over existing data does not abort
            // on a duplicate primary key.
            db.withTransaction {
                backup.accounts?.forEach { db.accountDao().insertAccount(it) }
                backup.settings.forEach { db.settingsDao().saveSettings(it) }
                backup.userProfile.forEach { db.userProfileDao().insertUserProfile(it) }
                backup.expenses.forEach { db.expenseDao().insertExpense(it) }
                backup.incomes.forEach { db.incomeDao().insertIncome(it) }
                backup.workLogs.forEach { db.workLogDao().insertWorkLog(it) }
                backup.habits.forEach { db.habitDao().upsertHabit(it) }
                backup.focusSessions.forEach { db.focusSessionDao().upsertFocusSession(it) }
                backup.workSessions.forEach { db.workSessionDao().upsertWorkSession(it) }
                backup.dailyJournals.forEach { db.dailyJournalDao().insertJournal(it) }
                backup.healthMetrics.forEach { db.healthMetricDao().insertHealthMetric(it) }
                backup.workDays.forEach { db.workDayDao().insertWorkDay(it) }
                backup.achievements.forEach { db.achievementDao().insertAchievement(it) }
                backup.colleagues.forEach { db.colleagueDao().insertColleague(it) }
                backup.monthlyInputs.forEach { db.monthlyInputDao().insertMonthlyInput(it) }
                backup.calculations.forEach { db.calculationDao().insert(it) }
                backup.loans.forEach { db.loanDao().upsertLoan(it) }
                backup.emis.forEach { db.emiDao().insertEmi(it) }
                backup.financialTransactions.forEach { db.financialTransactionDao().upsertTransaction(it) }
                backup.creditCards.forEach { db.creditCardDao().insertCard(it) }
                backup.creditCardTransactions.forEach { db.creditCardTransactionDao().insertTransaction(it) }
                backup.savings.forEach { db.savingsDao().insert(it) }
                backup.schedules.forEach { db.scheduleDao().insertSchedule(it) }
                backup.meals.forEach { db.mealDao().insertMeal(it) }
                backup.travelExpenses.forEach { db.travelExpenseDao().insert(it) }
                backup.recurringRules?.forEach { db.recurringRuleDao().upsertRule(it) }
                backup.recurringTransactions?.forEach { db.recurringTransactionDao().upsertTransaction(it) }
                backup.billSplits?.forEach { db.billSplitDao().upsert(it) }
                backup.realityEntries?.forEach { db.realityTrackerDao().insertEntry(it) }
                backup.decisions?.forEach { db.decisionDao().insertDecision(it) }
                backup.executionHistory?.forEach { db.executionHistoryDao().upsert(it) }
                // Goals before targets (foreign key)
                backup.lifePlanGoals?.forEach { db.lifePlanDao().insertGoal(it) }
                backup.lifePlanTargets?.forEach { db.lifePlanDao().insertTarget(it) }
                backup.gamificationStats?.let { db.lifePlanDao().saveUserStats(it) }
                backup.checkIns?.forEach { db.checkInDao().insertCheckIn(it) }
                backup.consequenceDebts?.forEach { db.consequenceDebtDao().insertDebt(it) }
                backup.weeklyReports?.forEach { db.weeklyReportDao().insertReport(it) }
                backup.userHistory?.let { db.userHistoryDao().insertHistory(it) }
            }
            afterRestore()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class BackupSummary(
    val timestamp: Long,
    val formatVersion: Int,
    val appVersion: String,
    val counts: Map<String, Int>
)
