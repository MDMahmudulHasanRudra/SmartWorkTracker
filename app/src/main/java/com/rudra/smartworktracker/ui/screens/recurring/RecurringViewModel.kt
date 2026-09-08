package com.rudra.smartworktracker.ui.screens.recurring

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.RecurringFrequency
import com.rudra.smartworktracker.data.entity.RecurringRule
import com.rudra.smartworktracker.data.entity.RecurringTransaction
import com.rudra.smartworktracker.data.entity.RecurringTransactionStatus
import com.rudra.smartworktracker.data.entity.TransactionType
import com.rudra.smartworktracker.data.entity.ExecutionHistoryEntity
import com.rudra.smartworktracker.data.repository.ExpenseRepository
import com.rudra.smartworktracker.data.repository.ExecutionHistoryRepository
import com.rudra.smartworktracker.data.repository.IncomeRepository
import com.rudra.smartworktracker.data.repository.RecurringRepository
import com.rudra.smartworktracker.data.repository.SavingsRepository
import com.rudra.smartworktracker.data.repository.AccountRepository
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.engine.RecurringEngine
import com.rudra.smartworktracker.engine.YearlyProjection
import com.rudra.smartworktracker.engine.PatternSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

private const val DEFAULT_SPENDING_LIMIT = 10000.0

class RecurringViewModel(
    private val recurringRepository: RecurringRepository,
    private val recurringEngine: RecurringEngine,
    private val incomeRepository: IncomeRepository? = null,
    private val expenseRepository: ExpenseRepository? = null,
    private val executionHistoryRepository: ExecutionHistoryRepository? = null,
    private val accountRepository: AccountRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    private val _executionHistory = MutableStateFlow<List<ExecutionHistoryItem>>(emptyList())
    val executionHistory: StateFlow<List<ExecutionHistoryItem>> = _executionHistory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow<RuleFilter>(RuleFilter.ALL)
    val selectedFilter: StateFlow<RuleFilter> = _selectedFilter.asStateFlow()

    init {
        loadData()
        loadExecutionHistory()
    }

    private fun loadData() {
        viewModelScope.launch {
            recurringRepository.getAllRules().collect { rules ->
                _uiState.value = _uiState.value.copy(rules = rules)
                updateActiveRulesCount(rules)
                calculateMonthlyTotals(rules)
                calculateCategoryBreakdown(rules)
                checkSpendingAlerts(rules)
            }
        }

        viewModelScope.launch {
            recurringRepository.getActiveRules().collect { activeRules ->
                _uiState.value = _uiState.value.copy(activeRules = activeRules)
            }
        }

        viewModelScope.launch {
            recurringRepository.getAllTransactions().collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    allTransactions = transactions,
                    pendingConfirmations = transactions.filter {
                        it.status == RecurringTransactionStatus.PENDING && !it.isConfirmed
                    }
                )
            }
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val thirtyDaysLater = now + (30L * 24 * 60 * 60 * 1000)
            recurringRepository.getTransactionsBetweenDates(now, thirtyDaysLater).collect { transactions ->
                _uiState.value = _uiState.value.copy(upcomingTransactions = transactions)
            }
        }

        viewModelScope.launch {
            loadYearlyProjection()
        }

        viewModelScope.launch {
            loadPatternSuggestions()
        }

        viewModelScope.launch {
            accountRepository?.getAllAccounts()?.collect { accounts ->
                _uiState.value = _uiState.value.copy(accounts = accounts)
            }
        }
    }

    private fun calculateCategoryBreakdown(rules: List<RecurringRule>) {
        val breakdown = mutableMapOf<String, Double>()
        rules.filter { it.isActive && it.transactionType == TransactionType.EXPENSE }.forEach { rule ->
            val category = rule.category ?: "Other"
            val monthlyAmount = recurringEngine.calculateMonthlyEquivalent(rule)
            breakdown[category] = (breakdown[category] ?: 0.0) + monthlyAmount
        }
        _uiState.value = _uiState.value.copy(categoryBreakdown = breakdown)
    }

    private fun checkSpendingAlerts(rules: List<RecurringRule>) {
        val categoryMonthly = mutableMapOf<String, Double>()
        rules.filter { it.isActive && it.transactionType == TransactionType.EXPENSE }.forEach { rule ->
            val category = rule.category ?: "Other"
            val monthly = recurringEngine.calculateMonthlyEquivalent(rule)
            categoryMonthly[category] = (categoryMonthly[category] ?: 0.0) + monthly
        }

        val alerts = mutableListOf<SpendingAlert>()
        categoryMonthly.forEach { (category, spent) ->
            val percentage = (spent / DEFAULT_SPENDING_LIMIT * 100).toFloat().coerceIn(0f, 100f)
            if (percentage > 70f) {
                alerts.add(SpendingAlert(category, spent, DEFAULT_SPENDING_LIMIT, percentage))
            }
        }
        _uiState.value = _uiState.value.copy(spendingAlerts = alerts)
    }

    private fun loadExecutionHistory() {
        executionHistoryRepository?.let { repo ->
            viewModelScope.launch {
                repo.getRecent(50).collect { entities ->
                    _executionHistory.value = entities.map { entity ->
                        ExecutionHistoryItem(
                            id = entity.id,
                            timestamp = entity.timestamp,
                            success = entity.success,
                            successCount = if (entity.success) 1 else 0,
                            totalCount = 1,
                            totalAmount = entity.amount,
                            failedRules = if (!entity.success) mapOf(entity.ruleName to (entity.failureReason ?: "Unknown")) else emptyMap()
                        )
                    }
                }
            }
        }
    }

    private fun updateActiveRulesCount(rules: List<RecurringRule>) {
        val activeCount = rules.count { it.isActive }
        _uiState.value = _uiState.value.copy(activeRulesCount = activeCount)
    }

    private fun calculateMonthlyTotals(rules: List<RecurringRule>) {
        var totalIncome = 0.0
        var totalExpenses = 0.0

        rules.filter { it.isActive }.forEach { rule ->
            val monthlyAmount = recurringEngine.calculateMonthlyEquivalent(rule)
            when (rule.transactionType) {
                TransactionType.INCOME -> totalIncome += monthlyAmount
                TransactionType.EXPENSE -> totalExpenses += monthlyAmount
                else -> {}
            }
        }

        _uiState.value = _uiState.value.copy(
            totalIncomeThisMonth = totalIncome,
            totalExpensesThisMonth = totalExpenses
        )
    }

    private suspend fun loadYearlyProjection() {
        val rules = recurringRepository.getActiveRules().first()
        val projection = recurringEngine.calculateYearlyProjection(rules)
        _uiState.value = _uiState.value.copy(yearlyProjection = projection)
    }

    private suspend fun loadPatternSuggestions() {
        val suggestions = recurringEngine.detectPatterns()
        _uiState.value = _uiState.value.copy(patternSuggestions = suggestions)
    }

    fun addRule(rule: RecurringRule) {
        viewModelScope.launch {
            recurringRepository.insertRule(rule)
        }
    }

    fun updateRule(rule: RecurringRule) {
        viewModelScope.launch {
            recurringRepository.updateRule(rule)
        }
    }

    fun deleteRule(rule: RecurringRule) {
        viewModelScope.launch {
            recurringRepository.deleteRule(rule)
        }
    }

    fun toggleRuleActive(rule: RecurringRule) {
        viewModelScope.launch {
            recurringRepository.updateRuleActiveStatus(rule.id, !rule.isActive)
        }
    }

    fun executeRuleNow(rule: RecurringRule) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val currentBalance = calculateCurrentBalance()
            val result = recurringEngine.executeRule(rule, currentBalance)
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (!result.success) {
                _uiState.value = _uiState.value.copy(lastExecutionError = result.reason)
            }
        }
    }

    fun checkAndExecuteDueRules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val currentBalance = calculateCurrentBalance()
            val results = recurringEngine.processDueRules(currentBalance)
            _uiState.value = _uiState.value.copy(isLoading = false)

            val failedCount = results.count { !it.success }
            if (failedCount > 0) {
                _uiState.value = _uiState.value.copy(lastExecutionError = "$failedCount transaction(s) failed")
            }
        }
    }

    private suspend fun calculateCurrentBalance(): Double {
        return try {
            val now = System.currentTimeMillis()
            val startOfMonth = getStartOfMonth()
            val totalIncome = incomeRepository?.getTotalIncomeBetween(startOfMonth, now)?.first() ?: 0.0
            val totalExpenses = expenseRepository?.getTotalExpensesBetween(startOfMonth, now)?.first() ?: 0.0
            totalIncome - totalExpenses
        } catch (e: Exception) {
            0.0
        }
    }

    private fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun skipTransaction(transaction: RecurringTransaction, reason: String? = "Skipped by user") {
        viewModelScope.launch {
            recurringRepository.skipTransaction(transaction.id, reason)
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun manualExecuteRules(rules: List<RecurringRule>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            var totalIncome = 0.0
            var totalExpenses = 0.0
            val failedRules = mutableMapOf<String, String>()
            var successCount = 0

            rules.forEach { rule ->
                val currentBalance = calculateCurrentBalance()
                val result = recurringEngine.executeRule(rule, currentBalance)

                if (result.success) {
                    successCount++
                    val monthlyAmount = recurringEngine.calculateMonthlyEquivalent(rule)
                    when (rule.transactionType) {
                        TransactionType.INCOME -> totalIncome += monthlyAmount
                        TransactionType.EXPENSE -> totalExpenses += monthlyAmount
                        else -> {}
                    }
                } else {
                    failedRules[rule.name] = result.reason ?: "Unknown error"
                }

                // Persist to database
                executionHistoryRepository?.insert(
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
            val totalAmount = totalIncome + totalExpenses

            val executionResult = ManualExecutionResult(
                success = successCount == rules.size,
                successCount = successCount,
                failureCount = rules.size - successCount,
                totalAmount = totalAmount,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                failedRules = failedRules,
                timestamp = System.currentTimeMillis()
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                lastExecutionResult = executionResult
            )
        }
    }

    fun clearExecutionResult() {
        _uiState.value = _uiState.value.copy(lastExecutionResult = null)
    }

    // Multi-select
    fun toggleMultiSelect() {
        _uiState.value = _uiState.value.copy(
            isMultiSelectMode = !_uiState.value.isMultiSelectMode,
            selectedRuleIds = emptySet()
        )
    }

    fun toggleRuleSelection(ruleId: Long) {
        val current = _uiState.value.selectedRuleIds.toMutableSet()
        if (current.contains(ruleId)) current.remove(ruleId) else current.add(ruleId)
        _uiState.value = _uiState.value.copy(selectedRuleIds = current)
    }

    fun selectAllRules() {
        val allIds = _uiState.value.filteredRules.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedRuleIds = allIds)
    }

    fun deselectAllRules() {
        _uiState.value = _uiState.value.copy(selectedRuleIds = emptySet())
    }

    fun deleteSelectedRules() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedRuleIds
            val rules = _uiState.value.rules.filter { it.id in ids }
            rules.forEach { recurringRepository.deleteRule(it) }
            _uiState.value = _uiState.value.copy(
                selectedRuleIds = emptySet(),
                isMultiSelectMode = false
            )
        }
    }

    fun toggleSelectedRulesActive() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedRuleIds
            val rules = _uiState.value.rules.filter { it.id in ids }
            rules.forEach { rule ->
                recurringRepository.updateRuleActiveStatus(rule.id, !rule.isActive)
            }
            _uiState.value = _uiState.value.copy(
                selectedRuleIds = emptySet(),
                isMultiSelectMode = false
            )
        }
    }

    // Transaction confirmation
    fun confirmTransaction(transaction: RecurringTransaction) {
        viewModelScope.launch {
            recurringRepository.updateTransaction(
                transaction.copy(isConfirmed = true, status = RecurringTransactionStatus.CONFIRMED)
            )
        }
    }

    fun confirmAllPending() {
        viewModelScope.launch {
            _uiState.value.pendingConfirmations.forEach { transaction ->
                recurringRepository.updateTransaction(
                    transaction.copy(isConfirmed = true, status = RecurringTransactionStatus.CONFIRMED)
                )
            }
        }
    }

    // Snooze/defer
    fun snoozeTransaction(transaction: RecurringTransaction, days: Int = 1) {
        viewModelScope.launch {
            val newScheduledDate = transaction.scheduledDate + (days.toLong() * 24 * 60 * 60 * 1000)
            recurringRepository.snoozeTransaction(transaction.id, newScheduledDate)
        }
    }

    fun snoozeAllFailed() {
        viewModelScope.launch {
            val failed = _uiState.value.allTransactions.filter {
                it.status == RecurringTransactionStatus.FAILED
            }
            failed.forEach { transaction ->
                val newScheduledDate = transaction.scheduledDate + (24L * 60 * 60 * 1000)
                recurringRepository.snoozeTransaction(transaction.id, newScheduledDate)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterRules()
    }

    fun updateFilter(filter: RuleFilter) {
        _selectedFilter.value = filter
        filterRules()
    }

    private fun filterRules() {
        viewModelScope.launch {
            val query = _searchQuery.value
            val filter = _selectedFilter.value

            val allRules = if (query.isBlank()) {
                recurringRepository.getAllRules().first()
            } else {
                recurringRepository.searchRules(query).first()
            }

            val filteredRules = when (filter) {
                RuleFilter.ALL -> allRules
                RuleFilter.ACTIVE -> allRules.filter { it.isActive }
                RuleFilter.INACTIVE -> allRules.filter { !it.isActive }
                RuleFilter.INCOME -> allRules.filter { it.transactionType == TransactionType.INCOME }
                RuleFilter.EXPENSE -> allRules.filter { it.transactionType == TransactionType.EXPENSE }
                RuleFilter.SAVINGS -> allRules.filter { it.transactionType == TransactionType.SAVINGS_ADD || it.transactionType == TransactionType.SAVINGS_WITHDRAW }
                RuleFilter.TRANSFER -> allRules.filter { it.transactionType == TransactionType.TRANSFER }
            }

            _uiState.value = _uiState.value.copy(filteredRules = filteredRules)
        }
    }

    fun getRuleTemplates(): List<RuleTemplate> {
        return listOf(
            RuleTemplate("Monthly Rent", "Housing", 15000.0, TransactionType.EXPENSE, RecurringFrequency.MONTHLY, "Regular monthly rent payment"),
            RuleTemplate("Salary Credit", "Salary", 50000.0, TransactionType.INCOME, RecurringFrequency.MONTHLY, "Monthly salary"),
            RuleTemplate("Electricity Bill", "Bills & Utilities", 2000.0, TransactionType.EXPENSE, RecurringFrequency.MONTHLY, "Monthly electricity bill"),
            RuleTemplate("Internet Bill", "Subscriptions", 1000.0, TransactionType.EXPENSE, RecurringFrequency.MONTHLY, "Monthly internet subscription"),
            RuleTemplate("Savings Transfer", "Savings", 5000.0, TransactionType.SAVINGS_ADD, RecurringFrequency.MONTHLY, "Monthly savings deposit"),
            RuleTemplate("EMI Payment", "Other", 8000.0, TransactionType.EXPENSE, RecurringFrequency.MONTHLY, "Monthly EMI"),
            RuleTemplate("Gym Membership", "Personal Care", 2000.0, TransactionType.EXPENSE, RecurringFrequency.MONTHLY, "Monthly gym fee"),
            RuleTemplate("Freelance Income", "Freelance", 10000.0, TransactionType.INCOME, RecurringFrequency.MONTHLY, "Freelance project payment")
        )
    }

    fun addRuleFromTemplate(template: RuleTemplate) {
        val calendar = Calendar.getInstance()
        val startDate = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val nextExecution = calendar.timeInMillis

        val rule = RecurringRule(
            name = template.name,
            description = template.description,
            transactionType = template.transactionType,
            amount = template.amount,
            category = template.category,
            sourceAccount = com.rudra.smartworktracker.data.entity.AccountType.BALANCE,
            frequency = template.frequency,
            startDate = startDate,
            nextExecutionDate = nextExecution,
            preferredTime = com.rudra.smartworktracker.data.entity.PreferredTime.MORNING,
            priority = com.rudra.smartworktracker.data.entity.RecurringPriority.MEDIUM,
            autoExecute = true,
            isActive = true
        )
        addRule(rule)
    }

    data class ManualExecutionResult(
        val success: Boolean,
        val successCount: Int,
        val failureCount: Int,
        val totalAmount: Double,
        val totalIncome: Double,
        val totalExpenses: Double,
        val failedRules: Map<String, String>,
        val timestamp: Long,
        val errorMessage: String? = null
    )

    data class ExecutionHistoryItem(
        val id: Long,
        val timestamp: Long,
        val success: Boolean,
        val successCount: Int,
        val totalCount: Int,
        val totalAmount: Double,
        val failedRules: Map<String, String>
    )

    data class RecurringUiState(
        val rules: List<RecurringRule> = emptyList(),
        val filteredRules: List<RecurringRule> = emptyList(),
        val activeRules: List<RecurringRule> = emptyList(),
        val allTransactions: List<RecurringTransaction> = emptyList(),
        val upcomingTransactions: List<RecurringTransaction> = emptyList(),
        val activeRulesCount: Int = 0,
        val totalIncomeThisMonth: Double = 0.0,
        val totalExpensesThisMonth: Double = 0.0,
        val lastExecutionError: String? = null,
        val lastExecutionResult: ManualExecutionResult? = null,
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val yearlyProjection: YearlyProjection? = null,
        val patternSuggestions: List<PatternSuggestion> = emptyList(),
        val spendingAlerts: List<SpendingAlert> = emptyList(),
        val selectedRuleIds: Set<Long> = emptySet(),
        val isMultiSelectMode: Boolean = false,
        val pendingConfirmations: List<RecurringTransaction> = emptyList(),
        val categoryBreakdown: Map<String, Double> = emptyMap(),
        val accounts: List<Account> = emptyList()
    )
}

data class RuleTemplate(
    val name: String,
    val category: String,
    val amount: Double,
    val transactionType: TransactionType,
    val frequency: RecurringFrequency,
    val description: String
)

data class SpendingAlert(
    val category: String,
    val spent: Double,
    val limit: Double,
    val percentage: Float
)

enum class RuleFilter {
    ALL, ACTIVE, INACTIVE, INCOME, EXPENSE, SAVINGS, TRANSFER
}

class RecurringViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecurringViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = RecurringRepository(
                database.recurringRuleDao(),
                database.recurringTransactionDao()
            )
            val incomeRepository = IncomeRepository(database.incomeDao())
            val expenseRepository = ExpenseRepository(database.expenseDao())
            val savingsRepository = SavingsRepository(database.savingsDao())
            val executionHistoryRepository = ExecutionHistoryRepository(database.executionHistoryDao())
            val accountRepository = AccountRepository(database.accountDao())
            val engine = RecurringEngine(
                repository, incomeRepository, expenseRepository,
                savingsRepository = savingsRepository,
                accountRepository = accountRepository
            )
            @Suppress("UNCHECKED_CAST")
            return RecurringViewModel(repository, engine, incomeRepository, expenseRepository, executionHistoryRepository, accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
