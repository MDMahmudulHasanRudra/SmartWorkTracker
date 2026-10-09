package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.rudra.smartworktracker.model.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insertFocusSession(focusSession: FocusSession)

    @Query("SELECT * FROM focus_sessions")
    fun getAllFocusSessions(): Flow<List<FocusSession>>

    @Upsert
    suspend fun upsertFocusSession(focusSession: FocusSession)
}
