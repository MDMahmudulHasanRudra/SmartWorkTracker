package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.rudra.smartworktracker.data.entity.RecurringRule
import com.rudra.smartworktracker.data.entity.RecurringFrequency
import com.rudra.smartworktracker.data.entity.RecurringPriority
import com.rudra.smartworktracker.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RecurringRule): Long
    
    @Update
    suspend fun updateRule(rule: RecurringRule)

    @Upsert
    suspend fun upsertRule(rule: RecurringRule)
    
    @Delete
    suspend fun deleteRule(rule: RecurringRule)
    
    @Query("DELETE FROM recurring_rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)
    
    @Query("SELECT * FROM recurring_rules WHERE id = :ruleId")
    suspend fun getRuleById(ruleId: Long): RecurringRule?
    
    @Query("SELECT * FROM recurring_rules WHERE id = :ruleId")
    fun getRuleByIdFlow(ruleId: Long): Flow<RecurringRule?>
    
    @Query("SELECT * FROM recurring_rules WHERE isDeleted = 0 ORDER BY priority ASC, nextExecutionDate ASC")
    fun getAllRules(): Flow<List<RecurringRule>>
    
    @Query("SELECT * FROM recurring_rules WHERE isActive = 1 AND isDeleted = 0 ORDER BY priority ASC, nextExecutionDate ASC")
    fun getActiveRules(): Flow<List<RecurringRule>>
    
    @Query("SELECT * FROM recurring_rules WHERE isActive = 1 AND isDeleted = 0 AND nextExecutionDate <= :timestamp ORDER BY priority ASC, nextExecutionDate ASC")
    suspend fun getRulesDueForExecution(timestamp: Long): List<RecurringRule>
    
    @Query("SELECT * FROM recurring_rules WHERE transactionType = :type AND isDeleted = 0")
    fun getRulesByType(type: TransactionType): Flow<List<RecurringRule>>
    
    @Query("SELECT * FROM recurring_rules WHERE frequency = :frequency AND isDeleted = 0")
    fun getRulesByFrequency(frequency: RecurringFrequency): Flow<List<RecurringRule>>
    
    @Query("SELECT * FROM recurring_rules WHERE priority = :priority AND isDeleted = 0")
    fun getRulesByPriority(priority: RecurringPriority): Flow<List<RecurringRule>>
    
    @Query("SELECT * FROM recurring_rules WHERE isDeleted = 0 AND nextExecutionDate BETWEEN :startDate AND :endDate ORDER BY nextExecutionDate ASC")
    fun getRulesBetweenDates(startDate: Long, endDate: Long): Flow<List<RecurringRule>>
    
    @Query("UPDATE recurring_rules SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :ruleId")
    suspend fun updateRuleActiveStatus(ruleId: Long, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE recurring_rules SET nextExecutionDate = :nextDate, updatedAt = :updatedAt WHERE id = :ruleId")
    suspend fun updateNextExecutionDate(ruleId: Long, nextDate: Long, updatedAt: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM recurring_rules WHERE isActive = 1 AND isDeleted = 0")
    fun getActiveRulesCount(): Flow<Int>
    
    @Query("SELECT SUM(amount) FROM recurring_rules WHERE transactionType = :type AND isActive = 1 AND isDeleted = 0")
    fun getTotalAmountByType(type: TransactionType): Flow<Double?>
    
    @Query("SELECT * FROM recurring_rules WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')")
    fun searchRules(query: String): Flow<List<RecurringRule>>
}
