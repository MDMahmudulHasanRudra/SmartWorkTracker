package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rudra.smartworktracker.data.entity.Savings
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savings: Savings)

    @Update
    suspend fun update(savings: Savings)

    @Delete
    suspend fun delete(savings: Savings)

    @Query("SELECT SUM(amount) FROM savings")
    fun getTotalSavings(): Flow<Double>

    @Query("SELECT * FROM savings ORDER BY timestamp DESC")
    fun getSavingsHistory(): Flow<List<Savings>>

    @Query("SELECT * FROM savings")
    fun getAllSavings(): Flow<List<Savings>>

    @Query("SELECT SUM(amount) FROM savings WHERE timestamp BETWEEN :startTime AND :endTime")
    fun getSavingsBetween(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT * FROM savings WHERE category = :category ORDER BY timestamp DESC")
    fun getSavingsByCategory(category: String): Flow<List<Savings>>

    @Query("SELECT * FROM savings WHERE note LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchSavings(query: String): Flow<List<Savings>>

    @Query("SELECT * FROM savings WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getSavingsSince(startTime: Long): Flow<List<Savings>>

    @Query("DELETE FROM savings")
    suspend fun deleteAll()

    // Account-linked queries
    @Query("SELECT * FROM savings WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getSavingsByAccount(accountId: Long): Flow<List<Savings>>

    @Query("SELECT SUM(amount) FROM savings WHERE accountId = :accountId")
    fun getTotalSavingsByAccount(accountId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM savings WHERE accountId = :accountId AND timestamp BETWEEN :startTime AND :endTime")
    fun getSavingsByAccountBetween(accountId: Long, startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT accountId, SUM(amount) as total FROM savings GROUP BY accountId")
    fun getSavingsByAllAccounts(): Flow<List<AccountSavingsTotal>>

    @Query("SELECT * FROM savings WHERE accountId IS NULL ORDER BY timestamp DESC")
    fun getSavingsWithoutAccount(): Flow<List<Savings>>
}

data class AccountSavingsTotal(
    val accountId: Long?,
    val total: Double
)
