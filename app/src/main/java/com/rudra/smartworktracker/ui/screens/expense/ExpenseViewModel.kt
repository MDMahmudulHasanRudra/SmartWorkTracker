package com.rudra.smartworktracker.ui.screens.expense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.repository.AccountRepository
import com.rudra.smartworktracker.data.repository.ExpenseRepository
import com.rudra.smartworktracker.model.Expense
import com.rudra.smartworktracker.model.ExpenseCategory
import com.rudra.smartworktracker.utils.CurrencyManager
import com.rudra.smartworktracker.utils.DateTimeUtils
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val accountRepository = AccountRepository(db.accountDao())
    private val expenseRepository = ExpenseRepository(db.expenseDao(), db.accountDao())

    val recentExpenses: StateFlow<List<Expense>> = expenseRepository.getExpenses(page = 1, pageSize = RECENT_COUNT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthTotal: StateFlow<Double> = DateTimeUtils.monthRange().let { (start, end) ->
        expenseRepository.getTotalExpensesBetween(start, end - 1).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch { accountRepository.initializeDefaultAccounts() }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            _events.tryEmit("Expense deleted")
        }
    }

    /**
     * Saves the expense and, when an account is chosen, deducts it from that account. The
     * account id is stored on the expense so deleting it later refunds the account.
     */
    fun saveExpense(
        amount: Double,
        category: ExpenseCategory,
        merchant: String?,
        notes: String?,
        timestamp: Long,
        accountId: Long?
    ) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                currency = CurrencyManager.getCurrencyCode(),
                category = category,
                merchant = merchant,
                notes = notes,
                timestamp = timestamp,
                accountId = accountId
            )
            expenseRepository.insertExpense(expense)
            accountId?.let { accountRepository.adjustBalance(it, -amount) }
            _events.tryEmit("Expense of ${CurrencyManager.format(amount)} saved")
        }
    }

    fun hasSufficientBalance(accountId: Long, amount: Double): Boolean {
        val account = accounts.value.find { it.id == accountId }
        return account != null && account.balance >= amount
    }

    private companion object {
        const val RECENT_COUNT = 10
    }
}
