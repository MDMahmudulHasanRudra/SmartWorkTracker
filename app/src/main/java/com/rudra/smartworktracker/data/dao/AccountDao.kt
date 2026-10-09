package com.rudra.smartworktracker.data.dao

import androidx.room.*
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.AccountCategory
import com.rudra.smartworktracker.data.entity.AccountProvider
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getAllAccountsList(): List<Account>

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    suspend fun getAllAccountsIncludingInactive(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): Account?

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountByIdFlow(accountId: Long): Flow<Account?>

    @Query("SELECT * FROM accounts WHERE type = :category AND isActive = 1 ORDER BY createdAt DESC")
    fun getAccountsByCategory(category: AccountCategory): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE provider = :provider AND isActive = 1 ORDER BY createdAt DESC")
    fun getAccountsByProvider(provider: AccountProvider): Flow<List<Account>>

    @Query("SELECT SUM(balance) FROM accounts WHERE isActive = 1")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(balance) FROM accounts WHERE type = :category AND isActive = 1")
    fun getTotalBalanceByCategory(category: AccountCategory): Flow<Double?>

    @Query("SELECT SUM(balance) FROM accounts WHERE isActive = 1 AND date(lastUpdated/1000, 'unixepoch', 'localtime') = date('now', 'localtime')")
    fun getTodayTransferredAmount(): Flow<Double?>

    @Query("SELECT * FROM accounts WHERE linkedGoalId = :goalId AND isActive = 1")
    fun getAccountsLinkedToGoal(goalId: String): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Update
    suspend fun updateAccount(account: Account)

    @Query("UPDATE accounts SET balance = :newBalance, lastUpdated = :timestamp WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, newBalance: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET linkedGoalId = :goalId WHERE id = :accountId")
    suspend fun linkToGoal(accountId: Long, goalId: String?)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: Long)

    @Query("UPDATE accounts SET isActive = 0 WHERE id = :accountId")
    suspend fun deactivateAccount(accountId: Long)

    @Query("SELECT COUNT(*) FROM accounts WHERE isActive = 1")
    fun getActiveAccountCount(): Flow<Int>
}