package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.rudra.smartworktracker.model.WorkSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkSessionDao {
    @Insert
    suspend fun insertWorkSession(workSession: WorkSession)

    @Query("SELECT * FROM work_sessions")
    fun getAllWorkSessions(): Flow<List<WorkSession>>

    @Upsert
    suspend fun upsertWorkSession(workSession: WorkSession)

    @Query("SELECT * FROM work_sessions WHERE startTime >= :since ORDER BY startTime DESC")
    fun getSessionsSince(since: Long): Flow<List<WorkSession>>

    @Query("SELECT * FROM work_sessions WHERE id = :id")
    suspend fun getWorkSessionById(id: String): WorkSession?
}
