package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.rudra.smartworktracker.model.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE triggerHabitId = :triggerId")
    suspend fun getHabitsByTriggerId(triggerId: String): List<Habit>

    @Delete
    suspend fun deleteHabits(habits: List<Habit>)

    @Upsert
    suspend fun upsertHabit(habit: Habit)
}
