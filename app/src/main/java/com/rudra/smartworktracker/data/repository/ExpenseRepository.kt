package com.rudra.smartworktracker.data.repository

import com.rudra.smartworktracker.data.dao.AccountDao
import com.rudra.smartworktracker.data.dao.ExpenseDao
import com.rudra.smartworktracker.model.Expense
import com.rudra.smartworktracker.model.ExpenseByCategory
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val accountDao: AccountDao? = null
) {

    fun getAllExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses()
    }

    fun getExpenses(page: Int, pageSize: Int): Flow<List<Expense>> {
        val offset = (page - 1) * pageSize
        return expenseDao.getPaginatedExpenses(offset, pageSize)
    }

    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesBetween(startTime, endTime)
    }

    fun getMealExpensesBetween(startTime: Long, endTime: Long): Flow<Double?> {
        return expenseDao.getMealExpensesBetween(startTime, endTime)
    }

    fun getTotalExpensesBetween(startTime: Long, endTime: Long): Flow<Double?> {
        return expenseDao.getTotalExpensesBetween(startTime, endTime)
    }

    fun getTotalExpensesBefore(endTime: Long): Flow<Double?> {
        return expenseDao.getTotalExpensesBefore(endTime)
    }

    fun getTotalExpensesUpTo(endTime: Long): Flow<Double?> {
        return expenseDao.getTotalExpensesUpTo(endTime)
    }

    fun getExpensesByCategoryBetween(startTime: Long, endTime: Long): Flow<List<ExpenseByCategory>> {
        return expenseDao.getExpensesByCategoryBetween(startTime, endTime)
    }

    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
        reverseAccountEffect(expense)
    }

    suspend fun deleteExpenseById(expenseId: String) {
        val expense = expenseDao.getExpenseById(expenseId)
        expenseDao.deleteExpenseById(expenseId)
        expense?.let { reverseAccountEffect(it) }
    }

    /** Refunds a deleted expense to the account it was paid from. */
    private suspend fun reverseAccountEffect(expense: Expense) {
        val accountId = expense.accountId ?: return
        val account = accountDao?.getAccountById(accountId) ?: return
        accountDao.updateBalance(accountId, account.balance + expense.amount)
    }

    suspend fun clearAll() {
        expenseDao.deleteAll()
    }
}
