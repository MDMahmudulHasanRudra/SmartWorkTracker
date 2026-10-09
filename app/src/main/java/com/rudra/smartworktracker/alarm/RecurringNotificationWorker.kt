package com.rudra.smartworktracker.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.BackoffPolicy
import com.rudra.smartworktracker.MainActivity
import com.rudra.smartworktracker.R
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.ExecutionHistoryEntity
import com.rudra.smartworktracker.data.entity.RecurringRule
import com.rudra.smartworktracker.data.entity.RecurringTransactionStatus
import com.rudra.smartworktracker.data.repository.AccountRepository
import com.rudra.smartworktracker.data.repository.ExecutionHistoryRepository
import com.rudra.smartworktracker.data.repository.ExpenseRepository
import com.rudra.smartworktracker.data.repository.IncomeRepository
import com.rudra.smartworktracker.data.repository.RecurringRepository
import com.rudra.smartworktracker.data.repository.SavingsRepository
import com.rudra.smartworktracker.data.repository.SettingsRepository
import com.rudra.smartworktracker.data.repository.TransactionRepository
import com.rudra.smartworktracker.engine.EngineExecutionResult
import com.rudra.smartworktracker.engine.RecurringEngine
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Worker that processes recurring transactions in the background.
 * Runs periodically to check for due transactions and send notifications.
 */
class RecurringNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "recurring_notification_worker"
        const val CHANNEL_ID = "recurring_transactions"
        /** Same notifications without vibration, used when Settings > Vibration is off. */
        const val CHANNEL_ID_NO_VIBRATION = "recurring_transactions_no_vibration"
        const val NOTIFICATION_ID = 1001
        private const val PREFS_NAME = "recurring_notifications"
        private const val KEY_LAST_PENDING_NOTIFICATION = "last_pending_notification_day"
        private const val KEY_REMINDED_OCCURRENCES = "reminded_occurrences"

        /**
         * Schedule the recurring notification worker with constraints
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<RecurringNotificationWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    /** Channel picked from the user's vibration setting at the start of each run. */
    private var channelId = CHANNEL_ID

    override suspend fun doWork(): Result {
        return try {
            val settings = SettingsRepository(context)
            channelId = RecurringNotificationHelper.ensureChannel(context, settings.vibration.first())

            val database = AppDatabase.getDatabase(context)
            val repository = RecurringRepository(
                database.recurringRuleDao(),
                database.recurringTransactionDao()
            )
            val incomeRepository = IncomeRepository(database.incomeDao())
            val expenseRepository = ExpenseRepository(database.expenseDao())
            // Without these, background savings/transfers reported success but never wrote anything
            val engine = RecurringEngine(
                repository,
                incomeRepository,
                expenseRepository,
                transactionRepository = TransactionRepository(database.financialTransactionDao()),
                savingsRepository = SavingsRepository(database.savingsDao()),
                accountRepository = AccountRepository(database.accountDao())
            )
            val historyRepository = ExecutionHistoryRepository(database.executionHistoryDao())

            val currentBalance = calculateCurrentBalance(incomeRepository, expenseRepository)
            val results = engine.processDueRules(currentBalance)

            results.filter { !it.awaitingConfirmation }.forEach { result ->
                val rule = result.rule ?: return@forEach
                historyRepository.insert(
                    ExecutionHistoryEntity(
                        ruleId = rule.id,
                        ruleName = rule.name,
                        transactionType = rule.transactionType.name,
                        amount = rule.amount,
                        success = result.success,
                        failureReason = if (!result.success) result.reason else null
                    )
                )
            }

            if (settings.notifications.first()) {
                sendResultNotifications(results)
                sendUpcomingReminders(repository.getActiveRules().first())

                val awaiting = repository.getTransactionsByStatus(RecurringTransactionStatus.PENDING).first()
                    .count { !it.isConfirmed }
                if (awaiting > 0 && shouldNotifyToday(KEY_LAST_PENDING_NOTIFICATION)) {
                    sendUpcomingNotification(awaiting)
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun calculateCurrentBalance(
        incomeRepository: IncomeRepository,
        expenseRepository: ExpenseRepository
    ): Double {
        return try {
            // All-time net: month-to-date made early-month expenses fail as "insufficient balance"
            val now = System.currentTimeMillis()
            val totalIncome = incomeRepository.getTotalIncomeBetween(0L, now).first() ?: 0.0
            val totalExpenses = expenseRepository.getTotalExpensesBetween(0L, now).first() ?: 0.0

            totalIncome - totalExpenses
        } catch (e: Exception) {
            0.0
        }
    }

    /** Honours each rule's notifyOnExecution / notifyOnFailure preference. */
    private fun sendResultNotifications(results: List<EngineExecutionResult>) {
        var orphanFailures = 0
        results.forEach { result ->
            val rule = result.rule
            when {
                result.awaitingConfirmation -> Unit
                rule == null -> if (!result.success) orphanFailures++
                result.success && rule.notifyOnExecution ->
                    RecurringNotificationHelper.sendExecutionNotification(context, rule, true, channelId)
                !result.success && rule.notifyOnFailure ->
                    RecurringNotificationHelper.sendExecutionNotification(context, rule, false, channelId)
            }
        }
        if (orphanFailures > 0) sendFailureNotification(orphanFailures)
    }

    /**
     * One reminder per upcoming occurrence for rules with notifyBeforeExecution, sent once the
     * occurrence is within notifyDaysBefore days. Occurrences already announced are remembered so
     * the hourly worker does not repeat them.
     */
    private fun sendUpcomingReminders(rules: List<RecurringRule>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyReminded = prefs.getStringSet(KEY_REMINDED_OCCURRENCES, emptySet()).orEmpty()
        val inWindow = mutableSetOf<String>()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        rules.filter { it.isActive && it.notifyBeforeExecution }.forEach { rule ->
            val dueDay = Instant.ofEpochMilli(rule.nextExecutionDate).atZone(zone).toLocalDate()
            val daysUntil = ChronoUnit.DAYS.between(today, dueDay)
            if (daysUntil < 0 || daysUntil > rule.notifyDaysBefore.coerceAtLeast(0)) return@forEach

            val key = "${rule.id}:${rule.nextExecutionDate}"
            inWindow += key
            if (key !in alreadyReminded) {
                RecurringNotificationHelper.sendRuleNotification(context, rule, daysUntil.toInt(), channelId)
            }
        }
        prefs.edit().putStringSet(KEY_REMINDED_OCCURRENCES, inWindow).apply()
    }

    private fun shouldNotifyToday(key: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (prefs.getString(key, null) == today) return false
        prefs.edit().putString(key, today).apply()
        return true
    }

    private fun sendUpcomingNotification(count: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "recurring")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Confirmation Needed")
            .setContentText("$count recurring transaction(s) are waiting for your confirmation")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun sendFailureNotification(count: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "recurring")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Transaction Failed")
            .setContentText("$count recurring transaction(s) failed to execute")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }
}

/**
 * Notification helper for recurring transactions
 */
object RecurringNotificationHelper {

    /**
     * Creates (once) and returns the channel matching the vibration preference. Channel vibration
     * can't be changed after creation on Android 8+, hence two channels.
     */
    fun ensureChannel(context: Context, vibrate: Boolean): String {
        val id = if (vibrate) RecurringNotificationWorker.CHANNEL_ID else RecurringNotificationWorker.CHANNEL_ID_NO_VIBRATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                id,
                if (vibrate) "Recurring Transactions" else "Recurring Transactions (no vibration)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for recurring income and expenses"
                enableVibration(vibrate)
                if (!vibrate) vibrationPattern = longArrayOf(0L)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return id
    }

    /**
     * Send a notification for a specific upcoming recurring rule
     */
    fun sendRuleNotification(
        context: Context,
        rule: RecurringRule,
        daysUntil: Int,
        channelId: String = ensureChannel(context, vibrate = true)
    ) {

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "recurring")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            rule.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val amountText = CurrencyManager.format(rule.amount)
        val timeText = when (daysUntil) {
            0 -> "today"
            1 -> "tomorrow"
            else -> "in $daysUntil days"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Upcoming: ${rule.name}")
            .setContentText("$amountText scheduled $timeText")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(rule.id.toInt() + 10000, notification)
    }

    /**
     * Send a notification when a transaction is executed
     */
    fun sendExecutionNotification(
        context: Context,
        rule: RecurringRule,
        success: Boolean,
        channelId: String = ensureChannel(context, vibrate = true)
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "recurring")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            rule.id.toInt() + 1000,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val amountText = CurrencyManager.format(rule.amount)

        val (title, text) = if (success) {
            "Transaction Executed" to "$amountText ${rule.name} has been processed"
        } else {
            "Transaction Failed" to "$amountText ${rule.name} could not be processed"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(
                if (success) android.R.drawable.ic_popup_reminder
                else android.R.drawable.ic_dialog_alert
            )
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(
                if (success) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(rule.id.toInt() + 2000, notification)
    }
}
