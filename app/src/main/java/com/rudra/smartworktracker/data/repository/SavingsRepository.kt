package com.rudra.smartworktracker.data.repository

import com.rudra.smartworktracker.data.dao.AccountSavingsTotal
import com.rudra.smartworktracker.data.dao.SavingsDao
import com.rudra.smartworktracker.data.entity.Savings
import kotlinx.coroutines.flow.Flow

class SavingsRepository(private val savingsDao: SavingsDao) {

    fun getSavings(): Flow<Double> = savingsDao.getTotalSavings()

    fun getSavingsHistory(): Flow<List<Savings>> = savingsDao.getSavingsHistory()

    suspend fun addToSavings(amount: Double, note: String = "", category: String = "Deposit", accountId: Long? = null) {
        val savings = Savings(
            amount = amount,
            note = note,
            category = category,
            timestamp = System.currentTimeMillis(),
            accountId = accountId
        )
        savingsDao.insert(savings)
    }

    suspend fun withdrawFromSavings(amount: Double, note: String = "", category: String = "Withdrawal", accountId: Long? = null) {
        val savings = Savings(
            amount = -amount,
            note = note,
            category = category,
            timestamp = System.currentTimeMillis(),
            accountId = accountId
        )
        savingsDao.insert(savings)
    }

    suspend fun deleteTransaction(savings: Savings) {
        savingsDao.delete(savings)
    }

    fun getSavingsBetween(startTime: Long, endTime: Long): Flow<Double?> {
        return savingsDao.getSavingsBetween(startTime, endTime)
    }

    fun getSavingsByCategory(category: String): Flow<List<Savings>> {
        return savingsDao.getSavingsByCategory(category)
    }

    fun searchSavings(query: String): Flow<List<Savings>> {
        return savingsDao.searchSavings(query)
    }

    fun getSavingsSince(startTime: Long): Flow<List<Savings>> {
        return savingsDao.getSavingsSince(startTime)
    }

    suspend fun clearAll() {
        savingsDao.deleteAll()
    }

    // Account-linked methods
    fun getSavingsByAccount(accountId: Long): Flow<List<Savings>> =
        savingsDao.getSavingsByAccount(accountId)

    fun getTotalSavingsByAccount(accountId: Long): Flow<Double?> =
        savingsDao.getTotalSavingsByAccount(accountId)

    fun getSavingsByAccountBetween(accountId: Long, startTime: Long, endTime: Long): Flow<Double?> =
        savingsDao.getSavingsByAccountBetween(accountId, startTime, endTime)

    fun getSavingsByAllAccounts(): Flow<List<AccountSavingsTotal>> =
        savingsDao.getSavingsByAllAccounts()

    fun getSavingsWithoutAccount(): Flow<List<Savings>> =
        savingsDao.getSavingsWithoutAccount()
}
