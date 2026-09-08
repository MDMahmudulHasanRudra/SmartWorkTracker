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

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
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
            val result = accountRepository.deleteAccountWithTransfer(accountId, targetAccountId)
            when (result) {
                is com.rudra.smartworktracker.data.repository.DeleteResult.Success -> onResult(true, "Account deleted successfully")
                is com.rudra.smartworktracker.data.repository.DeleteResult.Error -> onResult(false, result.message)
            }
        }
    }

    fun canDeleteAccount(accountId: Long): Boolean {
        val account = _uiState.value.accounts.find { it.id == accountId }
        return account?.balance == 0.0
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

    fun loadAccountDetails(accountId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            accountRepository.getAccountByIdFlow(accountId).collect { account ->
                if (account != null) {
                    val allTransactions = financialTransactionDao.getAllTransactions().first()
                    val recentTransactions = allTransactions
                        .filter { transaction ->
                            val sourceMatches = when (account.type) {
                                AccountCategory.WALLET -> transaction.source == AccountType.CASH
                                AccountCategory.BANK -> transaction.source == AccountType.BANK || transaction.source == AccountType.SAVINGS
                                AccountCategory.MOBILE_BANKING -> transaction.source == AccountType.BALANCE
                            }
                            val destMatches = account.type.let { category ->
                                transaction.destination?.let { dest ->
                                    when (category) {
                                        AccountCategory.WALLET -> dest == AccountType.CASH
                                        AccountCategory.BANK -> dest == AccountType.BANK || dest == AccountType.SAVINGS
                                        AccountCategory.MOBILE_BANKING -> dest == AccountType.BALANCE
                                    }
                                } ?: false
                            }
                            sourceMatches || destMatches
                        }
                        .take(10)

                    val history = generateBalanceHistory(account.balance)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            account = account,
                            recentTransactions = recentTransactions,
                            balanceHistory = history,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun addMoneyToAccount(accountId: Long, amount: Double, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                accountRepository.addIncomeToAccount(accountId, amount)
                onResult(true, "Money added successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to add money")
            }
        }
    }

    fun cashOutFromAccount(accountId: Long, amount: Double, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val account = accountRepository.getAccountById(accountId)
                if (account != null && account.balance >= amount) {
                    accountRepository.deductExpenseFromAccount(accountId, amount)
                    onResult(true, "Cash out successful")
                } else {
                    onResult(false, "Insufficient balance")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to cash out")
            }
        }
    }

    private fun generateBalanceHistory(currentBalance: Double): List<BalanceHistoryItem> {
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        return (6 downTo 0).map { daysAgo ->
            val date = calendar.apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }.timeInMillis

            val variance = if (daysAgo == 0) 0.0 else (currentBalance * 0.05 * (daysAgo % 3 - 1))
            BalanceHistoryItem(
                date = date,
                balance = (currentBalance + variance).coerceAtLeast(0.0),
                dayLabel = dayFormat.format(Date(date))
            )
        }
    }
}