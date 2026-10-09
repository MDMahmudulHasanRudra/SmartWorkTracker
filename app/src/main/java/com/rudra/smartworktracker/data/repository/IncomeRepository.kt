package com.rudra.smartworktracker.data.repository

import com.rudra.smartworktracker.data.dao.AccountDao
import com.rudra.smartworktracker.data.dao.IncomeDao
import com.rudra.smartworktracker.data.entity.Income
import com.rudra.smartworktracker.model.IncomeByCategory
import kotlinx.coroutines.flow.Flow

class IncomeRepository(
    private val incomeDao: IncomeDao,
    private val accountDao: AccountDao? = null
) {

    fun getIncomes(page: Int, pageSize: Int): Flow<List<Income>> {
        val offset = (page - 1) * pageSize
        return incomeDao.getPaginatedIncomes(offset, pageSize)
    }

    suspend fun insertIncome(income: Income) {
        incomeDao.insertIncome(income)
    }

    suspend fun deleteIncome(income: Income) {
        incomeDao.deleteIncome(income)
        reverseAccountEffect(income)
    }

    suspend fun deleteIncomeById(incomeId: Long) {
        val income = incomeDao.getIncomeById(incomeId)
        incomeDao.deleteIncomeById(incomeId)
        income?.let { reverseAccountEffect(it) }
    }

    /** Takes a deleted income back out of the account it was credited to. */
    private suspend fun reverseAccountEffect(income: Income) {
        val accountId = income.accountId ?: return
        val account = accountDao?.getAccountById(accountId) ?: return
        accountDao.updateBalance(accountId, account.balance - income.amount)
    }

    fun getIncomesBetween(startTime: Long, endTime: Long): Flow<List<Income>> {
        return incomeDao.getIncomesBetween(startTime, endTime)
    }

    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Double?> {
        return incomeDao.getTotalIncomeBetween(startTime, endTime)
    }

    fun getTotalIncomeBefore(endTime: Long): Flow<Double?> {
        return incomeDao.getTotalIncomeBefore(endTime)
    }

    fun getTotalIncomeUpTo(endTime: Long): Flow<Double?> {
        return incomeDao.getTotalIncomeUpTo(endTime)
    }

    fun getIncomesByCategoryBetween(startTime: Long, endTime: Long): Flow<List<IncomeByCategory>> {
        return incomeDao.getIncomesByCategoryBetween(startTime, endTime)
    }
    
    fun getAllIncomes(): Flow<List<Income>> {
        return incomeDao.getAllIncomes()
    }

    suspend fun clearAll() {
        incomeDao.deleteAll()
    }
}
