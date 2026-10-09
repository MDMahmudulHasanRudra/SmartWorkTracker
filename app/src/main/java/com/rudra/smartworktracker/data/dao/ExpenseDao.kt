package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rudra.smartworktracker.model.Expense
import com.rudra.smartworktracker.model.ExpenseByCategory
import com.rudra.smartworktracker.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT 5")
    fun getLatest5Expenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :pageSize OFFSET :offset")
    fun getPaginatedExpenses(offset: Int, pageSize: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE category = 'MEAL' AND timestamp BETWEEN :startTime AND :endTime")
    fun getMealExpensesBetween(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE timestamp BETWEEN :startTime AND :endTime GROUP BY category")
    fun getExpensesByCategoryBetween(startTime: Long, endTime: Long): Flow<List<ExpenseByCategory>>

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp BETWEEN :startTime AND :endTime")
    fun getTotalExpensesBetween(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenses(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp < :endTime")
    fun getTotalExpensesBefore(endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp <= :endTime")
    fun getTotalExpensesUpTo(endTime: Long): Flow<Double?>

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: String)

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: String): Expense?

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
