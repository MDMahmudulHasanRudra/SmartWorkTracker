package com.rudra.smartworktracker.ui.screens.accounts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.AccountCategory
import com.rudra.smartworktracker.data.entity.AccountProvider
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.repository.AccountRepository
import com.rudra.smartworktracker.engine.FusionEngine
import com.rudra.smartworktracker.engine.SmartAlert
import com.rudra.smartworktracker.data.entity.FinancialTransaction
import com.rudra.smartworktracker.data.entity.TransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AccountsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val accountRepository = AccountRepository(db.accountDao())
    private val fusionEngine = FusionEngine(db.accountDao(), db.financialTransactionDao())

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadAccounts()
    }

    // Cancels the previous collector so "Refresh" doesn't stack a new one each tap
    private fun loadAccounts() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            accountRepository.initializeDefaultAccounts()

            accountRepository.getAllAccounts().collect { accounts ->
                val wallets = accounts.filter { it.type == AccountCategory.WALLET }
                val banks = accounts.filter { it.type == AccountCategory.BANK }
                val mobileBanking = accounts.filter { it.type == AccountCategory.MOBILE_BANKING }

                val netWorth = accounts.sumOf { it.balance }
                val walletTotal = wallets.sumOf { it.balance }
                val bankTotal = banks.sumOf { it.balance }
                val mobileTotal = mobileBanking.sumOf { it.balance }

                val alerts = try {
                    fusionEngine.getSmartAlerts()
                } catch (e: Exception) {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        accounts = accounts,
                        wallets = wallets,
                        bankAccounts = banks,
                        mobileBankingAccounts = mobileBanking,
                        totalNetWorth = netWorth,
                        walletTotal = walletTotal,
                        bankTotal = bankTotal,
                        mobileBankingTotal = mobileTotal,
                        smartAlerts = alerts,
                        error = null
                    )
                }
            }
        }
    }

    fun selectAccount(account: Account) {
        _selectedAccount.value = account
    }

    fun clearSelectedAccount() {
        _selectedAccount.value = null
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.deleteAccountById(accountId)
        }
    }

    fun deleteAccountWithTransfer(accountId: Long, targetAccountId: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // The dialog passes -1 for an empty account: there is nothing to move, just delete it
            if (targetAccountId <= 0) {
                val account = accountRepository.getAccountById(accountId)
                if (account != null && account.balance != 0.0) {
                    onResult(false, "Choose an account to receive the remaining balance")
                } else {
                    accountRepository.deleteAccountById(accountId)
                    onResult(true, "Account deleted successfully")
                }
                return@launch
            }
            val result = accountRepository.deleteAccountWithTransfer(accountId, targetAccountId)
            when (result) {
                is com.rudra.smartworktracker.data.repository.DeleteResult.Success -> onResult(true, "Account deleted successfully")
                is com.rudra.smartworktracker.data.repository.DeleteResult.Error -> onResult(false, result.message)
            }
        }
    }

    fun createAccount(
        name: String,
        type: AccountCategory,
        provider: AccountProvider,
        accountNumber: String,
        nickname: String?,
        balance: Double,
        maxBalance: Double? = null,
        hasLimit: Boolean = false,
        dailyLimit: Double? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                accountRepository.createAccount(name, type, provider, accountNumber, nickname, balance, maxBalance, hasLimit, dailyLimit)
                onResult(true, "Account created successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to create account")
            }
        }
    }

    fun updateAccount(account: Account, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                accountRepository.updateAccountDetails(account)
                onResult(true, "Account updated successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update account")
            }
        }
    }

    fun refreshData() {
        loadAccounts()
    }
}

class AccountDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val accountRepository = AccountRepository(db.accountDao())
    private val financialTransactionDao = db.financialTransactionDao()

    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadedAccountId: Long? = null

    fun loadAccountDetails(accountId: Long) {
        // LaunchedEffect re-runs after configuration changes; keep the existing collector
        if (loadedAccountId == accountId && loadJob?.isActive == true) return
        loadedAccountId = accountId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                accountRepository.getAccountByIdFlow(accountId),
                db.incomeDao().getAllIncomes(),
                db.expenseDao().getAllExpenses(),
                db.savingsDao().getSavingsByAccount(accountId),
                financialTransactionDao.getAllTransactions()
            ) { account, incomes, expenses, savings, transactions ->
                if (account == null) {
                    return@combine AccountDetailUiState(isLoading = false, error = "Account not found")
                }

                // Entries recorded against this account (stored since DB v13)
                val linked = buildList {
                    incomes.filter { it.accountId == accountId }.forEach {
                        add(LinkedEntry(it.timestamp, it.amount, FinancialTransaction(
                            type = TransactionType.INCOME, amount = it.amount, source = AccountType.BALANCE,
                            destination = null, note = it.description.ifBlank { it.category },
                            category = it.category, date = it.timestamp
                        )))
                    }
                    expenses.filter { it.accountId == accountId }.forEach {
                        add(LinkedEntry(it.timestamp, -it.amount, FinancialTransaction(
                            type = TransactionType.EXPENSE, amount = it.amount, source = AccountType.BALANCE,
                            destination = null, note = it.merchant ?: it.notes ?: it.category.displayName,
                            category = it.category.displayName, date = it.timestamp
                        )))
                    }
                    savings.forEach {
                        add(LinkedEntry(it.timestamp, it.amount, FinancialTransaction(
                            type = if (it.amount >= 0) TransactionType.SAVINGS_ADD else TransactionType.SAVINGS_WITHDRAW,
                            amount = kotlin.math.abs(it.amount), source = AccountType.SAVINGS,
                            destination = null, note = it.note.ifBlank { it.category },
                            category = it.category, date = it.timestamp
                        )))
                    }
                }

                // Transfers/loans are stored per account type, not per account
                val legacyType = account.type.toLegacyAccountType()
                val typeMatched = transactions.filter { tx ->
                    tx.type != TransactionType.INCOME && tx.type != TransactionType.EXPENSE &&
                        (tx.source in legacyType || tx.destination in legacyType)
                }

                val recent = (linked.map { it.transaction } + typeMatched)
                    .sortedByDescending { it.date }
                    .take(15)

                AccountDetailUiState(
                    isLoading = false,
                    account = account,
                    recentTransactions = recent,
                    balanceHistory = buildBalanceHistory(account.balance, linked),
                    error = null
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun addMoneyToAccount(accountId: Long, amount: Double, onResult: (Boolean, String) -> Unit) {
        if (amount <= 0) {
            onResult(false, "Enter an amount greater than zero")
            return
        }
        viewModelScope.launch {
            try {
                accountRepository.adjustBalance(accountId, amount)
                onResult(true, "Money added successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to add money")
            }
        }
    }

    fun cashOutFromAccount(accountId: Long, amount: Double, onResult: (Boolean, String) -> Unit) {
        if (amount <= 0) {
            onResult(false, "Enter an amount greater than zero")
            return
        }
        viewModelScope.launch {
            try {
                val account = accountRepository.getAccountById(accountId)
                if (account != null && account.balance >= amount) {
                    accountRepository.adjustBalance(accountId, -amount)
                    onResult(true, "Cash out successful")
                } else {
                    onResult(false, "Insufficient balance")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to cash out")
            }
        }
    }

    /**
     * End-of-day balances for the last 7 days, reconstructed backwards from the current
     * balance using the entries linked to this account.
     */
    private fun buildBalanceHistory(currentBalance: Double, entries: List<LinkedEntry>): List<BalanceHistoryItem> {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        return (6 downTo 0).map { daysAgo ->
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endOfDay = calendar.timeInMillis
            val changesAfter = entries.filter { it.timestamp > endOfDay }.sumOf { it.delta }
            BalanceHistoryItem(
                date = endOfDay,
                balance = currentBalance - changesAfter,
                dayLabel = dayFormat.format(Date(endOfDay))
            )
        }
    }

    private data class LinkedEntry(val timestamp: Long, val delta: Double, val transaction: FinancialTransaction)

    private fun AccountCategory.toLegacyAccountType(): Set<AccountType> = when (this) {
        AccountCategory.WALLET -> setOf(AccountType.CASH)
        AccountCategory.BANK -> setOf(AccountType.BANK, AccountType.SAVINGS)
        AccountCategory.MOBILE_BANKING -> setOf(AccountType.BALANCE)
    }
}
