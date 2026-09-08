package com.rudra.smartworktracker.engine

import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.entity.DayOfWeek
import com.rudra.smartworktracker.data.entity.FinancialTransaction
import com.rudra.smartworktracker.data.entity.Income
import com.rudra.smartworktracker.data.entity.RecurringFrequency
import com.rudra.smartworktracker.data.entity.RecurringPriority
import com.rudra.smartworktracker.data.entity.RecurringRule
import com.rudra.smartworktracker.data.entity.RecurringTransaction
import com.rudra.smartworktracker.data.entity.RecurringTransactionStatus
import com.rudra.smartworktracker.data.entity.SyncStatus
import com.rudra.smartworktracker.data.entity.TransactionType
import com.rudra.smartworktracker.data.repository.AccountRepository
import com.rudra.smartworktracker.data.repository.ExpenseRepository
import com.rudra.smartworktracker.data.repository.IncomeRepository
import com.rudra.smartworktracker.data.repository.RecurringRepository
import com.rudra.smartworktracker.data.repository.SavingsRepository
import com.rudra.smartworktracker.data.repository.TransactionRepository
import com.rudra.smartworktracker.model.Expense
import com.rudra.smartworktracker.model.ExpenseCategory
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID

class RecurringEngine(
    private val recurringRepository: RecurringRepository,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val transactionRepository: TransactionRepository? = null,
    private val savingsRepository: SavingsRepository? = null,
    private val accountRepository: AccountRepository? = null
) {
    companion object {
        private const val DEFAULT_MINIMUM_BALANCE = 0.0
    }

    fun calculateNextExecutionDate(
        currentDate: Long,
        frequency: RecurringFrequency,
        interval: Int = 1,
        preferredTime: com.rudra.smartworktracker.data.entity.PreferredTime = com.rudra.smartworktracker.data.entity.PreferredTime.MORNING
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate

        val hour = when (preferredTime) {
            com.rudra.smartworktracker.data.entity.PreferredTime.MORNING -> 9
            com.rudra.smartworktracker.data.entity.PreferredTime.AFTERNOON -> 14
            com.rudra.smartworktracker.data.entity.PreferredTime.EVENING -> 19
            com.rudra.smartworktracker.data.entity.PreferredTime.NIGHT -> 22
        }
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        when (frequency) {
            RecurringFrequency.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, interval)
            RecurringFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, interval)
            RecurringFrequency.BIWEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 2 * interval)
            RecurringFrequency.MONTHLY -> calendar.add(Calendar.MONTH, interval)
            RecurringFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3 * interval)
            RecurringFrequency.YEARLY -> calendar.add(Calendar.YEAR, interval)
            RecurringFrequency.CUSTOM -> calendar.add(Calendar.DAY_OF_MONTH, interval)
            RecurringFrequency.WEEKLY_SPECIFIC_DAYS -> calculateNextSpecificDay(calendar)
        }

        return calendar.timeInMillis
    }

    fun calculateNextExecutionDateWithDays(
        currentDate: Long,
        selectedDays: List<DayOfWeek>,
        preferredTime: com.rudra.smartworktracker.data.entity.PreferredTime = com.rudra.smartworktracker.data.entity.PreferredTime.MORNING
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate
        return calculateNextSpecificDay(calendar, selectedDays, preferredTime)
    }

    private fun calculateNextSpecificDay(
        calendar: Calendar,
        selectedDays: List<DayOfWeek>? = null,
        preferredTime: com.rudra.smartworktracker.data.entity.PreferredTime = com.rudra.smartworktracker.data.entity.PreferredTime.MORNING
    ): Long {
        val hour = when (preferredTime) {
            com.rudra.smartworktracker.data.entity.PreferredTime.MORNING -> 9
            com.rudra.smartworktracker.data.entity.PreferredTime.AFTERNOON -> 14
            com.rudra.smartworktracker.data.entity.PreferredTime.EVENING -> 19
            com.rudra.smartworktracker.data.entity.PreferredTime.NIGHT -> 22
        }

        if (selectedDays.isNullOrEmpty()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis
        }

        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val selectedCalendarDays = selectedDays.map { DayOfWeek.toCalendarDay(it) }.sorted()

        var nextDay: Int? = null
        for (day in selectedCalendarDays) {
            if (day > currentDayOfWeek) {
                nextDay = day
                break
            }
        }

        if (nextDay != null) {
            val daysToAdd = nextDay - currentDayOfWeek
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        } else {
            val daysToAdd = (7 - currentDayOfWeek) + selectedCalendarDays.first()
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        return calendar.timeInMillis
    }

    fun isExecutionDay(rule: RecurringRule, date: Long = System.currentTimeMillis()): Boolean {
        if (rule.frequency != RecurringFrequency.WEEKLY_SPECIFIC_DAYS) return true

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val today = DayOfWeek.fromCalendarDay(currentDayOfWeek)

        return rule.selectedDaysOfWeek?.contains(today) ?: false
    }

    suspend fun executeRule(rule: RecurringRule, currentBalance: Double = DEFAULT_MINIMUM_BALANCE): EngineExecutionResult {
        val effectiveBalance = if (rule.accountId != null && accountRepository != null) {
            accountRepository.getAccountById(rule.accountId)?.balance ?: currentBalance
        } else {
            currentBalance
        }

        val balanceCheck = checkBalanceProtection(rule, effectiveBalance)
        if (!balanceCheck.canExecute) {
            return EngineExecutionResult(
                success = false,
                reason = balanceCheck.reason ?: "Insufficient balance",
                shouldReschedule = balanceCheck.shouldReschedule,
                shouldSkip = balanceCheck.shouldSkip
            )
        }

        val transaction = createTransactionFromRule(rule)
        val transactionId = recurringRepository.insertTransaction(transaction)

        return try {
            val result = when (rule.transactionType) {
                TransactionType.INCOME -> executeIncome(rule, transactionId)
                TransactionType.EXPENSE -> executeExpense(rule, transactionId)
                TransactionType.SAVINGS_ADD -> executeSavings(rule, transactionId)
                TransactionType.SAVINGS_WITHDRAW -> executeSavingsWithdraw(rule, transactionId)
                TransactionType.TRANSFER -> executeTransfer(rule, transactionId)
                else -> EngineExecutionResult(success = false, reason = "Unsupported transaction type")
            }

            if (result.success) {
                recurringRepository.markTransactionExecuted(transactionId, RecurringTransactionStatus.EXECUTED)

                val nextDate = calculateNextExecutionDate(
                    rule.nextExecutionDate, rule.frequency, rule.interval, rule.preferredTime
                )

                if (rule.endDate != null && nextDate > rule.endDate) {
                    recurringRepository.updateRuleActiveStatus(rule.id, false)
                } else {
                    recurringRepository.updateNextExecutionDate(rule.id, nextDate)
                }
            } else {
                recurringRepository.markTransactionFailed(transactionId, result.reason)
            }

            result
        } catch (e: Exception) {
            recurringRepository.markTransactionFailed(transactionId, e.message)
            EngineExecutionResult(success = false, reason = e.message ?: "Unknown error", shouldReschedule = true)
        }
    }

    private fun checkBalanceProtection(rule: RecurringRule, currentBalance: Double): BalanceCheckResult {
        if (rule.transactionType == TransactionType.INCOME || rule.transactionType == TransactionType.SAVINGS_ADD) {
            return BalanceCheckResult(canExecute = true, shouldReschedule = false, shouldSkip = false)
        }

        val minimumRequired = rule.minimumBalanceRequired ?: (rule.amount + DEFAULT_MINIMUM_BALANCE)

        return when (rule.priority) {
            RecurringPriority.CRITICAL -> BalanceCheckResult(canExecute = true, shouldReschedule = false, shouldSkip = false)
            RecurringPriority.HIGH -> {
                if (currentBalance >= minimumRequired) {
                    BalanceCheckResult(canExecute = true, shouldReschedule = false, shouldSkip = false)
                } else {
                    BalanceCheckResult(canExecute = false, reason = "Insufficient balance for high priority", shouldReschedule = true, shouldSkip = false)
                }
            }
            RecurringPriority.MEDIUM, RecurringPriority.LOW, RecurringPriority.OPTIONAL -> {
                if (currentBalance >= minimumRequired) {
                    BalanceCheckResult(canExecute = true, shouldReschedule = false, shouldSkip = false)
                } else {
                    BalanceCheckResult(
                        canExecute = false,
                        reason = "Insufficient balance",
                        shouldReschedule = rule.priority != RecurringPriority.OPTIONAL,
                        shouldSkip = rule.priority == RecurringPriority.OPTIONAL
                    )
                }
            }
        }
    }

    private fun createTransactionFromRule(rule: RecurringRule): RecurringTransaction {
        return RecurringTransaction(
            ruleId = rule.id,
            uuid = UUID.randomUUID().toString(),
            name = rule.name,
            description = rule.description,
            transactionType = rule.transactionType,
            amount = rule.amount,
            category = rule.category,
            sourceAccount = rule.sourceAccount,
            destinationAccount = rule.destinationAccount,
            accountId = rule.accountId,
            destinationAccountId = rule.destinationAccountId,
            scheduledDate = rule.nextExecutionDate,
            status = if (rule.autoExecute) RecurringTransactionStatus.PENDING else RecurringTransactionStatus.CONFIRMED
        )
    }

    private suspend fun executeIncome(rule: RecurringRule, transactionId: Long): EngineExecutionResult {
        return try {
            val income = Income(
                amount = rule.amount,
                description = rule.name,
                category = rule.category ?: "Salary",
                timestamp = System.currentTimeMillis(),
                source = rule.sourceAccount.name,
                syncStatus = SyncStatus.LOCAL_ONLY
            )
            incomeRepository.insertIncome(income)

            rule.accountId?.let { accountId ->
                accountRepository?.addIncomeToAccount(accountId, rule.amount)
            }

            EngineExecutionResult(success = true, reason = "Income added successfully")
        } catch (e: Exception) {
            EngineExecutionResult(success = false, reason = e.message)
        }
    }

    private suspend fun executeExpense(rule: RecurringRule, transactionId: Long): EngineExecutionResult {
        return try {
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                amount = rule.amount,
                category = mapCategory(rule.category),
                merchant = rule.name,
                notes = rule.description,
                timestamp = System.currentTimeMillis(),
                syncStatus = SyncStatus.LOCAL_ONLY
            )
            expenseRepository.insertExpense(expense)

            rule.accountId?.let { accountId ->
                accountRepository?.deductExpenseFromAccount(accountId, rule.amount)
            }

            EngineExecutionResult(success = true, reason = "Expense added successfully")
        } catch (e: Exception) {
            EngineExecutionResult(success = false, reason = e.message)
        }
    }

    private suspend fun executeSavings(rule: RecurringRule, transactionId: Long): EngineExecutionResult {
        return try {
            savingsRepository?.addToSavings(
                amount = rule.amount,
                note = rule.name,
                category = rule.category ?: "Recurring Savings"
            )
            EngineExecutionResult(success = true, reason = "Savings added successfully")
        } catch (e: Exception) {
            EngineExecutionResult(success = false, reason = e.message)
        }
    }

    private suspend fun executeSavingsWithdraw(rule: RecurringRule, transactionId: Long): EngineExecutionResult {
        return try {
            savingsRepository?.withdrawFromSavings(
                amount = rule.amount,
                note = rule.name,
                category = rule.category ?: "Recurring Withdrawal"
            )
            EngineExecutionResult(success = true, reason = "Savings withdrawal successful")
        } catch (e: Exception) {
            EngineExecutionResult(success = false, reason = e.message)
        }
    }

    private suspend fun executeTransfer(rule: RecurringRule, transactionId: Long): EngineExecutionResult {
        return try {
            if (rule.accountId != null && rule.destinationAccountId != null && accountRepository != null) {
                val transferResult = accountRepository.transferBetweenAccounts(
                    fromAccountId = rule.accountId,
                    toAccountId = rule.destinationAccountId,
                    amount = rule.amount
                )
                when (transferResult) {
                    is com.rudra.smartworktracker.data.repository.TransferResult.Success -> {
                        EngineExecutionResult(success = true, reason = "Transfer executed successfully")
                    }
                    is com.rudra.smartworktracker.data.repository.TransferResult.Error -> {
                        EngineExecutionResult(success = false, reason = transferResult.message)
                    }
                }
            } else {
                val transaction = FinancialTransaction(
                    type = TransactionType.TRANSFER,
                    amount = rule.amount,
                    source = rule.sourceAccount,
                    destination = rule.destinationAccount ?: AccountType.BALANCE,
                    note = rule.name,
                    category = rule.category,
                    date = System.currentTimeMillis(),
                    syncStatus = SyncStatus.LOCAL_ONLY
                )
                transactionRepository?.insertTransaction(transaction)
                EngineExecutionResult(success = true, reason = "Transfer executed successfully")
            }
        } catch (e: Exception) {
            EngineExecutionResult(success = false, reason = e.message)
        }
    }

    suspend fun getUpcomingTransactions(days: Int = 30): List<RecurringTransaction> {
        val now = System.currentTimeMillis()
        val endDate = now + (days.toLong() * 24 * 60 * 60 * 1000)
        return recurringRepository.getPendingTransactionsDue(endDate)
    }

    suspend fun processDueRules(currentBalance: Double = DEFAULT_MINIMUM_BALANCE): List<EngineExecutionResult> {
        val results = mutableListOf<EngineExecutionResult>()
        val now = System.currentTimeMillis()
        val allActiveRules = recurringRepository.getRulesDueForExecution(now)

        for (rule in allActiveRules) {
            if (rule.isActive) {
                if (rule.frequency == RecurringFrequency.WEEKLY_SPECIFIC_DAYS) {
                    if (isExecutionDay(rule, now)) {
                        val result = executeRule(rule, currentBalance)
                        results.add(result)
                    }
                } else {
                    if (rule.nextExecutionDate <= now) {
                        val result = executeRule(rule, currentBalance)
                        results.add(result)
                    }
                }
            }
        }

        return results
    }

    suspend fun smartReschedule(rule: RecurringRule, failureReason: String?): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = rule.nextExecutionDate

        val gracePeriodHours = when (rule.priority) {
            RecurringPriority.CRITICAL -> 2
            RecurringPriority.HIGH -> 6
            RecurringPriority.MEDIUM -> 24
            RecurringPriority.LOW -> 48
            RecurringPriority.OPTIONAL -> 72
        }

        calendar.add(Calendar.HOUR_OF_DAY, gracePeriodHours)
        val newDate = calendar.timeInMillis
        recurringRepository.updateNextExecutionDate(rule.id, newDate)
        return newDate
    }

    fun calculateMonthlyEquivalent(rule: RecurringRule): Double {
        return when (rule.frequency) {
            RecurringFrequency.DAILY -> rule.amount * 30
            RecurringFrequency.WEEKLY -> rule.amount * 4.33
            RecurringFrequency.BIWEEKLY -> rule.amount * 2.17
            RecurringFrequency.MONTHLY -> rule.amount
            RecurringFrequency.QUARTERLY -> rule.amount / 3.0
            RecurringFrequency.YEARLY -> rule.amount / 12.0
            RecurringFrequency.CUSTOM -> rule.amount * (30.0 / rule.interval.coerceAtLeast(1))
            RecurringFrequency.WEEKLY_SPECIFIC_DAYS -> {
                val daysPerWeek = rule.selectedDaysOfWeek?.size?.toDouble() ?: 1.0
                rule.amount * daysPerWeek * 4.33
            }
        }
    }

    fun calculateYearlyProjection(rules: List<RecurringRule>): YearlyProjection {
        var totalYearlyIncome = 0.0
        var totalYearlyExpenses = 0.0
        val categoryBreakdown = mutableMapOf<String, Double>()

        rules.filter { it.isActive }.forEach { rule ->
            val yearlyAmount = when (rule.frequency) {
                RecurringFrequency.DAILY -> rule.amount * 365
                RecurringFrequency.WEEKLY -> rule.amount * 52
                RecurringFrequency.BIWEEKLY -> rule.amount * 26
                RecurringFrequency.MONTHLY -> rule.amount * 12
                RecurringFrequency.QUARTERLY -> rule.amount * 4
                RecurringFrequency.YEARLY -> rule.amount
                RecurringFrequency.CUSTOM -> rule.amount * (365.0 / rule.interval.coerceAtLeast(1))
                RecurringFrequency.WEEKLY_SPECIFIC_DAYS -> {
                    val daysPerWeek = rule.selectedDaysOfWeek?.size?.toDouble() ?: 1.0
                    rule.amount * daysPerWeek * 52
                }
            }

            when (rule.transactionType) {
                TransactionType.INCOME -> totalYearlyIncome += yearlyAmount
                TransactionType.EXPENSE -> {
                    totalYearlyExpenses += yearlyAmount
                    val category = rule.category ?: "Other"
                    categoryBreakdown[category] = (categoryBreakdown[category] ?: 0.0) + yearlyAmount
                }
                else -> {}
            }
        }

        return YearlyProjection(
            totalYearlyIncome = totalYearlyIncome,
            totalYearlyExpenses = totalYearlyExpenses,
            netYearly = totalYearlyIncome - totalYearlyExpenses,
            categoryBreakdown = categoryBreakdown
        )
    }

    suspend fun detectPatterns(): List<PatternSuggestion> {
        val suggestions = mutableListOf<PatternSuggestion>()
        val allTransactions = recurringRepository.getAllTransactions().first()

        val groupedByName = allTransactions.groupBy { it.name.lowercase() }
        groupedByName.forEach { (name, transactions) ->
            if (transactions.size >= 3) {
                val amounts = transactions.map { it.amount }
                val avgAmount = amounts.average()
                val allSameAmount = amounts.all { it == avgAmount }

                if (allSameAmount && transactions.size >= 3) {
                    val sortedDates = transactions.map { it.scheduledDate }.sorted()
                    val intervals = sortedDates.zipWithNext().map { it.second - it.first }
                    val avgInterval = intervals.average()

                    val frequency = when {
                        avgInterval < 2 * 24 * 60 * 60 * 1000 -> RecurringFrequency.DAILY
                        avgInterval < 10 * 24 * 60 * 60 * 1000 -> RecurringFrequency.WEEKLY
                        avgInterval < 20 * 24 * 60 * 60 * 1000 -> RecurringFrequency.BIWEEKLY
                        avgInterval < 45 * 24 * 60 * 60 * 1000 -> RecurringFrequency.MONTHLY
                        avgInterval < 120 * 24 * 60 * 60 * 1000 -> RecurringFrequency.QUARTERLY
                        else -> RecurringFrequency.YEARLY
                    }

                    suggestions.add(
                        PatternSuggestion(
                            name = transactions.first().name,
                            amount = avgAmount,
                            frequency = frequency,
                            category = transactions.first().category ?: "Other",
                            confidence = (transactions.size.toFloat() / 10f).coerceAtMost(1f)
                        )
                    )
                }
            }
        }

        return suggestions.sortedByDescending { it.confidence }
    }

    private fun mapCategory(category: String?): ExpenseCategory {
        return when (category?.uppercase()) {
            "MEAL", "FOOD & DINING" -> ExpenseCategory.MEAL
            "TRANSPORT", "TRANSPORTATION" -> ExpenseCategory.TRANSPORT
            "ENTERTAINMENT" -> ExpenseCategory.ENTERTAINMENT
            "BILLS", "BILLS & UTILITIES" -> ExpenseCategory.BILLS
            "SHOPPING" -> ExpenseCategory.SHOPPING
            "HEALTHCARE" -> ExpenseCategory.OTHER
            "EDUCATION" -> ExpenseCategory.OTHER
            "TRAVEL" -> ExpenseCategory.TRANSPORT
            "SUBSCRIPTIONS" -> ExpenseCategory.BILLS
            else -> ExpenseCategory.OTHER
        }
    }
}

data class EngineExecutionResult(
    val success: Boolean,
    val reason: String? = null,
    val shouldReschedule: Boolean = false,
    val shouldSkip: Boolean = false,
    val relatedIncomeId: Long? = null,
    val relatedExpenseId: Long? = null,
    val relatedFinancialTransactionId: Int? = null
)

data class BalanceCheckResult(
    val canExecute: Boolean,
    val reason: String? = null,
    val shouldReschedule: Boolean = false,
    val shouldSkip: Boolean = false
)

data class PatternSuggestion(
    val name: String,
    val amount: Double,
    val frequency: RecurringFrequency,
    val category: String,
    val confidence: Float
)

data class YearlyProjection(
    val totalYearlyIncome: Double,
    val totalYearlyExpenses: Double,
    val netYearly: Double,
    val categoryBreakdown: Map<String, Double>
)
