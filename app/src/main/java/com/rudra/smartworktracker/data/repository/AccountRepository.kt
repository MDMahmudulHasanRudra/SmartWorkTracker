package com.rudra.smartworktracker.data.repository

import com.rudra.smartworktracker.data.dao.AccountDao
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.AccountCategory
import com.rudra.smartworktracker.data.entity.AccountProvider
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {

    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    suspend fun getAllAccountsList(): List<Account> = accountDao.getAllAccountsList()

    suspend fun getAccountById(accountId: Long): Account? = accountDao.getAccountById(accountId)

    fun getAccountByIdFlow(accountId: Long): Flow<Account?> = accountDao.getAccountByIdFlow(accountId)

    fun getAccountsByCategory(category: AccountCategory): Flow<List<Account>> =
        accountDao.getAccountsByCategory(category)

    fun getAccountsByProvider(provider: AccountProvider): Flow<List<Account>> =
        accountDao.getAccountsByProvider(provider)

    fun getTotalBalance(): Flow<Double?> = accountDao.getTotalBalance()

    fun getTotalBalanceByCategory(category: AccountCategory): Flow<Double?> =
        accountDao.getTotalBalanceByCategory(category)

    fun getTodayTransferredAmount(): Flow<Double?> = accountDao.getTodayTransferredAmount()

    fun getAccountsLinkedToGoal(goalId: String): Flow<List<Account>> =
        accountDao.getAccountsLinkedToGoal(goalId)

    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)

    suspend fun insertAccounts(accounts: List<Account>) = accountDao.insertAccounts(accounts)

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    suspend fun updateBalance(accountId: Long, newBalance: Double) =
        accountDao.updateBalance(accountId, newBalance)

    suspend fun linkToGoal(accountId: Long, goalId: String?) =
        accountDao.linkToGoal(accountId, goalId)

    suspend fun deleteAccount(account: Account) = accountDao.deleteAccount(account)

    suspend fun deleteAccountById(accountId: Long) = accountDao.deleteAccountById(accountId)

    suspend fun deactivateAccount(accountId: Long) = accountDao.deactivateAccount(accountId)

    fun getActiveAccountCount(): Flow<Int> = accountDao.getActiveAccountCount()

    suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double
    ): TransferResult {
        val fromAccount = accountDao.getAccountById(fromAccountId)
            ?: return TransferResult.Error("Source account not found")
        
        val toAccount = accountDao.getAccountById(toAccountId)
            ?: return TransferResult.Error("Destination account not found")

        if (fromAccount.balance < amount) {
            return TransferResult.Error("Insufficient balance")
        }

        val effectiveLimit = fromAccount.getEffectiveLimit()
        if (effectiveLimit != null && amount > effectiveLimit) {
            return TransferResult.Error("Amount exceeds daily transfer limit (${CurrencyManager.format(effectiveLimit)})")
        }

        val timestamp = System.currentTimeMillis()
        accountDao.updateBalance(fromAccountId, fromAccount.balance - amount, timestamp)
        accountDao.updateBalance(toAccountId, toAccount.balance + amount, timestamp)

        return TransferResult.Success(
            fromAccount = fromAccount.copy(balance = fromAccount.balance - amount),
            toAccount = toAccount.copy(balance = toAccount.balance + amount)
        )
    }

    suspend fun initializeDefaultAccounts() {
        val existingAccounts = accountDao.getAllAccountsList()
        if (existingAccounts.isEmpty()) {
            val defaultAccounts = listOf(
                Account(
                    name = "Cash",
                    type = AccountCategory.WALLET,
                    provider = AccountProvider.CASH,
                    accountNumber = "CASH001",
                    balance = 0.0,
                    hasLimit = false,
                    dailyTransferLimit = null,
                    nickname = "Cash"
                ),
                Account(
                    name = "Bank",
                    type = AccountCategory.BANK,
                    provider = AccountProvider.BANK,
                    accountNumber = "BANK001",
                    balance = 0.0,
                    hasLimit = false,
                    dailyTransferLimit = null,
                    nickname = "Bank Account"
                ),
                Account(
                    name = "Savings",
                    type = AccountCategory.BANK,
                    provider = AccountProvider.SAVINGS,
                    accountNumber = "SAV001",
                    balance = 0.0,
                    hasLimit = false,
                    dailyTransferLimit = null,
                    nickname = "Savings"
                ),
                Account(
                    name = "Credit Card",
                    type = AccountCategory.BANK,
                    provider = AccountProvider.CREDIT_CARD,
                    accountNumber = "CC001",
                    balance = 0.0,
                    hasLimit = false,
                    dailyTransferLimit = null,
                    nickname = "Credit Card"
                ),
                Account(
                    name = "Loan",
                    type = AccountCategory.BANK,
                    provider = AccountProvider.LOAN,
                    accountNumber = "LOAN001",
                    balance = 0.0,
                    hasLimit = false,
                    dailyTransferLimit = null,
                    nickname = "Loan"
                ),
                Account(
                    name = "bKash",
                    type = AccountCategory.MOBILE_BANKING,
                    provider = AccountProvider.BKASH,
                    accountNumber = "017XXXXXXXX",
                    balance = 0.0,
                    hasLimit = false,
                    dailyTransferLimit = null,
                    nickname = "bKash"
                ),
                Account(
                    name = "Nagad",
                    type = AccountCategory.MOBILE_BANKING,
                    provider = AccountProvider.NAGAD,
                    accountNumber = "018XXXXXXXX",
                    balance = 0.0,
                    hasLimit = false,
                    dailyTransferLimit = null,
                    nickname = "Nagad"
                ),
                Account(
                    name = "DBBL",
                    type = AccountCategory.BANK,
                    provider = AccountProvider.DBBL,
                    accountNumber = "1234567890",
                    balance = 0.0,
                    hasLimit = false,
                    dailyTransferLimit = null,
                    nickname = "DBBL"
                ),
                Account(
                    name = "City Bank",
                    type = AccountCategory.BANK,
                    provider = AccountProvider.CITY_BANK,
                    accountNumber = "9876543210",
                    balance = 0.0,
                    hasLimit = false,
                    dailyTransferLimit = null,
                    nickname = "City Bank"
                )
            )
            accountDao.insertAccounts(defaultAccounts)
        }
    }

    suspend fun createAccount(
        name: String,
        type: AccountCategory,
        provider: AccountProvider,
        accountNumber: String,
        nickname: String?,
        balance: Double,
        maxBalance: Double? = null,
        hasLimit: Boolean = false,
        dailyLimit: Double? = null
    ): Long {
        val account = Account(
            name = name,
            type = type,
            provider = provider,
            accountNumber = accountNumber,
            nickname = nickname,
            balance = balance,
            maxBalance = maxBalance,
            hasLimit = hasLimit,
            dailyTransferLimit = if (hasLimit && dailyLimit != null) dailyLimit else null
        )
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccountDetails(account: Account) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccountWithTransfer(
        accountId: Long,
        targetAccountId: Long
    ): DeleteResult {
        val sourceAccount = accountDao.getAccountById(accountId)
            ?: return DeleteResult.Error("Account not found")
        
        val targetAccount = accountDao.getAccountById(targetAccountId)
            ?: return DeleteResult.Error("Target account not found")

        if (sourceAccount.balance > 0) {
            val timestamp = System.currentTimeMillis()
            accountDao.updateBalance(targetAccountId, targetAccount.balance + sourceAccount.balance, timestamp)
            accountDao.updateBalance(accountId, 0.0, timestamp)
        }

        accountDao.deleteAccountById(accountId)
        return DeleteResult.Success
    }

    suspend fun addIncomeToAccount(accountId: Long, amount: Double) {
        val account = accountDao.getAccountById(accountId)
        account?.let {
            val newBalance = it.balance + amount
            accountDao.updateBalance(accountId, newBalance)
        }
    }

    suspend fun deductExpenseFromAccount(accountId: Long, amount: Double) {
        val account = accountDao.getAccountById(accountId)
        account?.let {
            val newBalance = it.balance - amount
            if (newBalance >= 0) {
                accountDao.updateBalance(accountId, newBalance)
            }
        }
    }

    suspend fun findAccountByType(accountType: AccountType): Account? {
        return when (accountType) {
            AccountType.CASH -> accountDao.getAllAccountsList().firstOrNull { it.type == AccountCategory.WALLET }
            AccountType.BANK -> accountDao.getAllAccountsList().firstOrNull { it.type == AccountCategory.BANK }
            AccountType.BALANCE -> accountDao.getAllAccountsList().firstOrNull { it.type == AccountCategory.MOBILE_BANKING }
            AccountType.SAVINGS -> accountDao.getAllAccountsList().firstOrNull { it.type == AccountCategory.BANK }
            AccountType.LOAN -> null
            AccountType.CREDIT_CARD -> null
        }
    }
}

sealed class TransferResult {
    data class Success(
        val fromAccount: Account,
        val toAccount: Account
    ) : TransferResult()
    
    data class Error(val message: String) : TransferResult()
}

sealed class DeleteResult {
    object Success : DeleteResult()
    data class Error(val message: String) : DeleteResult()
}