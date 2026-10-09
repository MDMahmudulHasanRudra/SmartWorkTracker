package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.rudra.smartworktracker.data.entity.ExecutionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionHistoryDao {
    @Insert
    suspend fun insert(entry: ExecutionHistoryEntity): Long

    @Query("SELECT * FROM execution_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ExecutionHistoryEntity>>

    @Query("SELECT * FROM execution_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ExecutionHistoryEntity>>

    @Query("SELECT * FROM execution_history WHERE ruleId = :ruleId ORDER BY timestamp DESC")
    fun getByRuleId(ruleId: Long): Flow<List<ExecutionHistoryEntity>>

    @Query("DELETE FROM execution_history")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsert(entry: ExecutionHistoryEntity)
}
