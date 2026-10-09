package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.rudra.smartworktracker.data.entity.RecurringTransaction
import com.rudra.smartworktracker.data.entity.RecurringTransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: RecurringTransaction): Long
    
    @Update
    suspend fun updateTransaction(transaction: RecurringTransaction)

    @Upsert
    suspend fun upsertTransaction(transaction: RecurringTransaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: RecurringTransaction)
    
    @Query("DELETE FROM recurring_transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)
    
    @Query("SELECT * FROM recurring_transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): RecurringTransaction?
    
    @Query("SELECT * FROM recurring_transactions WHERE id = :transactionId")
    fun getTransactionByIdFlow(transactionId: Long): Flow<RecurringTransaction?>
    
    @Query("SELECT * FROM recurring_transactions WHERE ruleId = :ruleId ORDER BY scheduledDate DESC")
    fun getTransactionsByRuleId(ruleId: Long): Flow<List<RecurringTransaction>>
    
    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0 ORDER BY scheduledDate ASC")
    fun getAllTransactions(): Flow<List<RecurringTransaction>>
    
    @Query("SELECT * FROM recurring_transactions WHERE status = :status AND isDeleted = 0 ORDER BY scheduledDate ASC")
    fun getTransactionsByStatus(status: RecurringTransactionStatus): Flow<List<RecurringTransaction>>
    
    @Query("SELECT * FROM recurring_transactions WHERE status = :status AND scheduledDate <= :timestamp AND isDeleted = 0")
    suspend fun getTransactionsDueForExecution(status: RecurringTransactionStatus, timestamp: Long): List<RecurringTransaction>
    
    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0 AND scheduledDate BETWEEN :startDate AND :endDate ORDER BY scheduledDate ASC")
    fun getTransactionsBetweenDates(startDate: Long, endDate: Long): Flow<List<RecurringTransaction>>
    
    @Query("SELECT * FROM recurring_transactions WHERE scheduledDate <= :timestamp AND status IN ('PENDING', 'CONFIRMED') AND isDeleted = 0 ORDER BY scheduledDate ASC")
    suspend fun getPendingTransactionsDue(timestamp: Long): List<RecurringTransaction>
    
    @Query("UPDATE recurring_transactions SET status = :status, updatedAt = :updatedAt WHERE id = :transactionId")
    suspend fun updateTransactionStatus(transactionId: Long, status: RecurringTransactionStatus, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE recurring_transactions SET status = :status, executedDate = :executedDate, updatedAt = :updatedAt WHERE id = :transactionId")
    suspend fun markTransactionExecuted(transactionId: Long, status: RecurringTransactionStatus, executedDate: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE recurring_transactions SET status = :status, failureReason = :reason, retryCount = retryCount + 1, updatedAt = :updatedAt WHERE id = :transactionId")
    suspend fun markTransactionFailed(transactionId: Long, status: RecurringTransactionStatus = RecurringTransactionStatus.FAILED, reason: String?, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE recurring_transactions SET isSkipped = 1, status = :status, skipReason = :reason, updatedAt = :updatedAt WHERE id = :transactionId")
    suspend fun skipTransaction(transactionId: Long, status: RecurringTransactionStatus = RecurringTransactionStatus.SKIPPED, reason: String?, updatedAt: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE status = :status AND isDeleted = 0")
    fun getTransactionCountByStatus(status: RecurringTransactionStatus): Flow<Int>
    
    @Query("SELECT SUM(amount) FROM recurring_transactions WHERE status = 'EXECUTED' AND transactionType = :type AND isDeleted = 0 AND executedDate BETWEEN :startDate AND :endDate")
    fun getTotalExecutedAmountByType(type: String, startDate: Long, endDate: Long): Flow<Double?>
    
    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')")
    fun searchTransactions(query: String): Flow<List<RecurringTransaction>>
    
    @Query("DELETE FROM recurring_transactions WHERE ruleId = :ruleId")
    suspend fun deleteTransactionsByRuleId(ruleId: Long)
    
    @Query("SELECT * FROM recurring_transactions WHERE relatedIncomeId = :incomeId OR relatedExpenseId = :expenseId OR relatedFinancialTransactionId = :transactionId")
    suspend fun getTransactionByRelatedIds(incomeId: Long?, expenseId: Long?, transactionId: Int?): RecurringTransaction?
    
    // CONFIRMED (not PENDING) so the retry is picked up by RecurringEngine.processDueRules once due
    @Query("UPDATE recurring_transactions SET scheduledDate = :newScheduledDate, status = 'CONFIRMED', isConfirmed = 1, updatedAt = :updatedAt WHERE id = :transactionId")
    suspend fun snoozeTransaction(transactionId: Long, newScheduledDate: Long, updatedAt: Long = System.currentTimeMillis())
}
