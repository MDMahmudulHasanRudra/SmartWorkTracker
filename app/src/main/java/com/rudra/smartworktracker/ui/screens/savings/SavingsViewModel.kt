package com.rudra.smartworktracker.ui.screens.savings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.Savings
import com.rudra.smartworktracker.data.repository.AccountRepository
import com.rudra.smartworktracker.data.repository.SavingsRepository
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimeRange {
    ALL, TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

data class SavingsStats(
    val totalDeposits: Double = 0.0,
    val totalWithdrawals: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTransaction: Double = 0.0
)

data class EnhancedSavingsUiState(
    val savings: Double = 0.0,
    val savingsHistory: List<Savings> = emptyList(),
    val filteredHistory: List<Savings> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedTimeRange: TimeRange = TimeRange.ALL,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val stats: SavingsStats = SavingsStats(),
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val accountSavings: Map<Long, Double> = emptyMap()
)

class SavingsViewModel(
    private val savingsRepository: SavingsRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnhancedSavingsUiState())
    val uiState: StateFlow<EnhancedSavingsUiState> = _uiState.asStateFlow()

    init {
        loadSavingsData()
    }

    private fun loadSavingsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            combine(
                savingsRepository.getSavings(),
                savingsRepository.getSavingsHistory(),
                accountRepository.getAllAccounts()
            ) { savings, history, accounts ->
                val filtered = filterAndSortHistory(history, TimeRange.ALL, SortOrder.DESCENDING)
                val stats = calculateStats(history)
                val accountSavings = mutableMapOf<Long, Double>()
                accounts.forEach { account ->
                    val total = history.filter { it.accountId == account.id }.sumOf { it.amount }
                    if (total != 0.0) accountSavings[account.id] = total
                }
                EnhancedSavingsUiState(
                    savings = savings ?: 0.0,
                    savingsHistory = history,
                    filteredHistory = filtered,
                    isLoading = false,
                    stats = stats,
                    selectedTimeRange = TimeRange.ALL,
                    sortOrder = SortOrder.DESCENDING,
                    accounts = accounts,
                    accountSavings = accountSavings
                )
            }.catch { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    private fun calculateStats(history: List<Savings>): SavingsStats {
        if (history.isEmpty()) return SavingsStats()

        val deposits = history.filter { it.amount > 0 }
        val withdrawals = history.filter { it.amount < 0 }

        val totalDeposits = deposits.sumOf { it.amount }
        val totalWithdrawals = -withdrawals.sumOf { it.amount }
        val transactionCount = history.size
        val averageTransaction = if (transactionCount > 0) (totalDeposits - totalWithdrawals) / transactionCount else 0.0

        return SavingsStats(
            totalDeposits = totalDeposits,
            totalWithdrawals = totalWithdrawals,
            transactionCount = transactionCount,
            averageTransaction = averageTransaction
        )
    }

    private fun filterAndSortHistory(
        history: List<Savings>,
        timeRange: TimeRange,
        sortOrder: SortOrder
    ): List<Savings> {
        val selectedAccountId = _uiState.value.selectedAccountId
        val filtered = history.filter { savings ->
            val matchesAccount = selectedAccountId == null || savings.accountId == selectedAccountId
            val matchesTime = when (timeRange) {
                TimeRange.ALL -> true
                TimeRange.TODAY -> {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    savings.timestamp >= calendar.timeInMillis
                }
                TimeRange.THIS_WEEK -> {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    savings.timestamp >= calendar.timeInMillis
                }
                TimeRange.THIS_MONTH -> {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    savings.timestamp >= calendar.timeInMillis
                }
                TimeRange.THIS_YEAR -> {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    savings.timestamp >= calendar.timeInMillis
                }
            }
            matchesAccount && matchesTime
        }.filter { savings ->
            _uiState.value.searchQuery.isEmpty() ||
                    savings.note?.contains(_uiState.value.searchQuery, ignoreCase = true) == true
        }

        return when (sortOrder) {
            SortOrder.DESCENDING -> filtered.sortedByDescending { it.timestamp }
            SortOrder.ASCENDING -> filtered.sortedBy { it.timestamp }
        }
    }

    fun filterByTimeRange(range: TimeRange) {
        val filtered = filterAndSortHistory(_uiState.value.savingsHistory, range, _uiState.value.sortOrder)
        _uiState.value = _uiState.value.copy(
            selectedTimeRange = range,
            filteredHistory = filtered
        )
    }

    fun selectAccount(accountId: Long?) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId)
        val filtered = filterAndSortHistory(_uiState.value.savingsHistory, _uiState.value.selectedTimeRange, _uiState.value.sortOrder)
        _uiState.value = _uiState.value.copy(filteredHistory = filtered)
    }

    fun searchTransactions(query: String) {
        val filtered = filterAndSortHistory(_uiState.value.savingsHistory, _uiState.value.selectedTimeRange, _uiState.value.sortOrder)
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredHistory = filtered
        )
    }

    fun toggleSortOrder() {
        val newOrder = if (_uiState.value.sortOrder == SortOrder.DESCENDING) SortOrder.ASCENDING else SortOrder.DESCENDING
        val filtered = filterAndSortHistory(_uiState.value.savingsHistory, _uiState.value.selectedTimeRange, newOrder)
        _uiState.value = _uiState.value.copy(
            sortOrder = newOrder,
            filteredHistory = filtered
        )
    }

    fun addToSavings(amount: Double, note: String = "", accountId: Long? = null) {
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Amount must be greater than 0")
            return
        }

        viewModelScope.launch {
            try {
                savingsRepository.addToSavings(amount, note, accountId = accountId)
                // Update account balance if linked
                if (accountId != null) {
                    accountRepository.addIncomeToAccount(accountId, amount)
                }
                _uiState.value = _uiState.value.copy(successMessage = "Successfully added ${CurrencyManager.format(amount)}")
                clearMessages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to add savings: ${e.message}")
            }
        }
    }

    fun withdrawFromSavings(amount: Double, note: String = "", accountId: Long? = null) {
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Amount must be greater than 0")
            return
        }

        if (amount > _uiState.value.savings) {
            _uiState.value = _uiState.value.copy(errorMessage = "Insufficient savings balance")
            return
        }

        viewModelScope.launch {
            try {
                savingsRepository.withdrawFromSavings(amount, note, accountId = accountId)
                // Deduct from account balance if linked
                if (accountId != null) {
                    accountRepository.deductExpenseFromAccount(accountId, amount)
                }
                _uiState.value = _uiState.value.copy(successMessage = "Successfully withdrew ${CurrencyManager.format(amount)}")
                clearMessages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to withdraw: ${e.message}")
            }
        }
    }

    fun deleteTransaction(savings: Savings) {
        viewModelScope.launch {
            try {
                // Reverse balance impact if linked to account
                if (savings.accountId != null) {
                    if (savings.amount > 0) {
                        accountRepository.deductExpenseFromAccount(savings.accountId, savings.amount)
                    } else {
                        accountRepository.addIncomeToAccount(savings.accountId, kotlin.math.abs(savings.amount))
                    }
                }
                savingsRepository.deleteTransaction(savings)
                _uiState.value = _uiState.value.copy(successMessage = "Transaction deleted")
                clearMessages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to delete: ${e.message}")
            }
        }
    }

    private fun clearMessages() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
