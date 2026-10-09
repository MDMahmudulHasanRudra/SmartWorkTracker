package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rudra.smartworktracker.data.entity.Income
import com.rudra.smartworktracker.model.IncomeByCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income)

    @Query("SELECT * FROM incomes ORDER BY timestamp DESC LIMIT 1")
    fun getLatestIncome(): Flow<Income?>

    @Query("SELECT * FROM incomes ORDER BY timestamp DESC LIMIT 5")
    fun getLatest5Incomes(): Flow<List<Income>>

    @Query("SELECT * FROM incomes WHERE timestamp BETWEEN :startTime AND :endTime")
    fun getIncomesBetween(startTime: Long, endTime: Long): Flow<List<Income>>

    @Query("SELECT * FROM incomes ORDER BY timestamp DESC")
    fun getAllIncomes(): Flow<List<Income>>

    @Query("SELECT * FROM incomes ORDER BY timestamp DESC LIMIT :pageSize OFFSET :offset")
    fun getPaginatedIncomes(offset: Int, pageSize: Int): Flow<List<Income>>

    @Query("SELECT SUM(amount) FROM incomes WHERE timestamp BETWEEN :startTime AND :endTime")
    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM incomes")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM incomes WHERE timestamp < :endTime")
    fun getTotalIncomeBefore(endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM incomes WHERE timestamp <= :endTime")
    fun getTotalIncomeUpTo(endTime: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM incomes WHERE timestamp BETWEEN :startTime AND :endTime GROUP BY category")
    fun getIncomesByCategoryBetween(startTime: Long, endTime: Long): Flow<List<IncomeByCategory>>

    @Update
    suspend fun updateIncome(income: Income)

    @Delete
    suspend fun deleteIncome(income: Income)

    @Query("DELETE FROM incomes WHERE id = :incomeId")
    suspend fun deleteIncomeById(incomeId: Long)

    @Query("SELECT * FROM incomes WHERE id = :incomeId")
    suspend fun getIncomeById(incomeId: Long): Income?

    @Query("DELETE FROM incomes")
    suspend fun deleteAll()
}
