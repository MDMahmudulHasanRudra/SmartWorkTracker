package com.rudra.smartworktracker.ui.screens.income

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.Income
import com.rudra.smartworktracker.data.repository.AccountRepository
import com.rudra.smartworktracker.data.repository.IncomeRepository
import com.rudra.smartworktracker.utils.CurrencyManager
import com.rudra.smartworktracker.utils.DateTimeUtils
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IncomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val accountRepository = AccountRepository(db.accountDao())
    private val incomeRepository = IncomeRepository(db.incomeDao(), db.accountDao())

    /** Income received this calendar month. */
    val income: StateFlow<Double> = DateTimeUtils.monthRange().let { (start, end) ->
        incomeRepository.getTotalIncomeBetween(start, end - 1).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val recentIncomes: StateFlow<List<Income>> = incomeRepository.getIncomes(page = 1, pageSize = RECENT_COUNT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch { accountRepository.initializeDefaultAccounts() }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            incomeRepository.deleteIncome(income)
            _events.tryEmit("Income deleted")
        }
    }

    /**
     * Saves the income and credits the chosen account. The account id is stored on the
     * income so deleting it later takes the money back out of that account.
     */
    fun saveIncome(
        amount: Double,
        description: String,
        category: String,
        source: String,
        timestamp: Long,
        accountId: Long?
    ) {
        viewModelScope.launch {
            val newIncome = Income(
                amount = amount,
                description = description,
                category = category,
                timestamp = timestamp,
                source = source,
                accountId = accountId
            )
            incomeRepository.insertIncome(newIncome)
            accountId?.let { accountRepository.adjustBalance(it, amount) }
            _events.tryEmit("Income of ${CurrencyManager.format(amount)} saved")
        }
    }

    private companion object {
        const val RECENT_COUNT = 10
    }
}
