package com.rudra.smartworktracker.engine

import com.rudra.smartworktracker.data.dao.AccountDao
import com.rudra.smartworktracker.data.dao.FinancialTransactionDao
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.AccountCategory
import com.rudra.smartworktracker.data.entity.FinancialTransaction
import com.rudra.smartworktracker.data.entity.TransactionType
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

private const val LOW_BALANCE_THRESHOLD = 500.0

class FusionEngine(
    private val accountDao: AccountDao,
    private val financialTransactionDao: FinancialTransactionDao
) {
    suspend fun processTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String?
    ): FusionResult {
        if (amount <= 0) return FusionResult.Error("Amount must be greater than zero")
        if (fromAccountId == toAccountId) return FusionResult.Error("Cannot transfer to the same account")

        val fromAccount = accountDao.getAccountById(fromAccountId)
            ?: return FusionResult.Error("Source account not found")

        val toAccount = accountDao.getAccountById(toAccountId)
            ?: return FusionResult.Error("Destination account not found")

        if (fromAccount.balance < amount) {
            return FusionResult.Error("Insufficient balance in ${fromAccount.name}")
        }

        val effectiveLimit = fromAccount.getEffectiveLimit()
        if (effectiveLimit != null) {
            val todayTotal = getTodayTransferTotal(fromAccountId)
            if (todayTotal + amount > effectiveLimit) {
                return FusionResult.Error("Daily limit exceeded (${CurrencyManager.format(effectiveLimit)})")
            }
        }

        val timestamp = System.currentTimeMillis()
        accountDao.updateBalance(fromAccountId, fromAccount.balance - amount, timestamp)
        accountDao.updateBalance(toAccountId, toAccount.balance + amount, timestamp)

        val transaction = FinancialTransaction(
            type = TransactionType.TRANSFER,
            amount = amount,
            source = fromAccount.type.toOldAccountType(),
            destination = toAccount.type.toOldAccountType(),
            note = note ?: "Transfer from ${fromAccount.name} to ${toAccount.name}",
            date = timestamp
        )
        financialTransactionDao.insertTransaction(transaction)

        return FusionResult.Success(
            fromAccount = fromAccount.copy(balance = fromAccount.balance - amount),
            toAccount = toAccount.copy(balance = toAccount.balance + amount),
            transaction = transaction
        )
    }

    suspend fun getTodayTransferTotal(accountId: Long): Double {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // FinancialTransaction stores the legacy AccountType, so compare against the account's
        // mapped type (comparing AccountCategory names like WALLET to CASH never matched)
        val sourceType = accountDao.getAccountById(accountId)?.type?.toOldAccountType() ?: return 0.0
        val transactions = financialTransactionDao.getAllTransactions().first()
        return transactions
            .filter { it.date >= today && it.type == TransactionType.TRANSFER && it.source == sourceType }
            .sumOf { it.amount }
    }

    fun getNetWorth(): Flow<Double?> {
        return accountDao.getTotalBalance()
    }

    fun getNetWorthByCategory(): Flow<Map<AccountCategory, Double>> {
        return accountDao.getTotalBalance().let { balanceFlow ->
            kotlinx.coroutines.flow.flow {
                val allAccounts = accountDao.getAllAccountsList()
                val categoryTotals = AccountCategory.entries.associateWith { category ->
                    allAccounts.filter { it.type == category }.sumOf { it.balance }
                }
                emit(categoryTotals)
            }
        }
    }

    suspend fun getSmartAlerts(): List<SmartAlert> {
        val alerts = mutableListOf<SmartAlert>()
        val accounts = accountDao.getAllAccountsList()
        val transactions = financialTransactionDao.getAllTransactions().first()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        accounts.forEach { account ->
            if (account.balance < LOW_BALANCE_THRESHOLD && account.balance > 0) {
                alerts.add(
                    SmartAlert.LowBalance(
                        accountName = account.name,
                        balance = account.balance,
                        message = "Your ${account.name} balance is low (${CurrencyManager.format(account.balance)})"
                    )
                )
            }

            if (account.hasLimit && account.dailyTransferLimit != null) {
                val todayTransferred = getTodayTransferTotal(account.id)
                if (todayTransferred > account.dailyTransferLimit * 0.8) {
                    alerts.add(
                        SmartAlert.ApproachingLimit(
                            accountName = account.name,
                            used = todayTransferred,
                            limit = account.dailyTransferLimit,
                            message = "Approaching daily limit: ${CurrencyManager.format(todayTransferred)}/${CurrencyManager.format(account.dailyTransferLimit!!)}"
                        )
                    )
                }
            }
        }

        val todayExpenses = transactions.filter {
            it.date >= today && it.type == TransactionType.EXPENSE
        }
        val totalTodaySpent = todayExpenses.sumOf { it.amount }
        if (totalTodaySpent > 5000) {
            alerts.add(
                SmartAlert.HighSpending(
                    accountName = "Overall",
                    message = "High spending today: ${CurrencyManager.format(totalTodaySpent)}"
                )
            )
        }

        val thisMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val monthlyTransfers = transactions.filter { it.date >= thisMonth && it.type == TransactionType.TRANSFER }
        if (monthlyTransfers.size > 20) {
            alerts.add(
                SmartAlert.TransferHabit(
                    message = "Frequent transfers this month: ${monthlyTransfers.size} transfers"
                )
            )
        }

        return alerts
    }

    suspend fun getTransferInsights(): TransferInsights {
        val transactions = financialTransactionDao.getAllTransactions().first()
        val accounts = accountDao.getAllAccountsList()

        val thisMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val monthlyTransfers = transactions.filter { it.date >= thisMonth && it.type == TransactionType.TRANSFER }
        val totalTransferred = monthlyTransfers.sumOf { it.amount }
        val transferCount = monthlyTransfers.size
        val avgTransferAmount = if (transferCount > 0) totalTransferred / transferCount else 0.0

        return TransferInsights(
            totalTransferredThisMonth = totalTransferred,
            transferCountThisMonth = transferCount,
            averageTransferAmount = avgTransferAmount,
            mostUsedAccount = accounts.maxByOrNull { it.lastUpdated }?.name ?: "None"
        )
    }

    private fun com.rudra.smartworktracker.data.entity.AccountCategory.toOldAccountType(): com.rudra.smartworktracker.data.entity.AccountType {
        return when (this) {
            AccountCategory.WALLET -> com.rudra.smartworktracker.data.entity.AccountType.CASH
            AccountCategory.BANK -> com.rudra.smartworktracker.data.entity.AccountType.BANK
            AccountCategory.MOBILE_BANKING -> com.rudra.smartworktracker.data.entity.AccountType.BALANCE
        }
    }
}

sealed class FusionResult {
    data class Success(
        val fromAccount: Account,
        val toAccount: Account,
        val transaction: FinancialTransaction
    ) : FusionResult()
    
    data class Error(val message: String) : FusionResult()
}

sealed class SmartAlert {
    data class LowBalance(
        val accountName: String,
        val balance: Double,
        val message: String
    ) : SmartAlert()

    data class ApproachingLimit(
        val accountName: String,
        val used: Double,
        val limit: Double,
        val message: String
    ) : SmartAlert()

    data class HighSpending(
        val accountName: String,
        val message: String
    ) : SmartAlert()

    data class TransferHabit(
        val message: String
    ) : SmartAlert()
}

data class TransferInsights(
    val totalTransferredThisMonth: Double,
    val transferCountThisMonth: Int,
    val averageTransferAmount: Double,
    val mostUsedAccount: String
)