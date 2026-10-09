package com.rudra.smartworktracker.data.dao

import androidx.room.*
import com.rudra.smartworktracker.data.entity.UserStatsEntity
import com.rudra.smartworktracker.model.Goal
import com.rudra.smartworktracker.model.Target
import kotlinx.coroutines.flow.Flow

@Dao
interface LifePlanDao {
    @Query("SELECT * FROM life_plan_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Query("DELETE FROM life_plan_goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: String)

    // @Update, not REPLACE: replacing a goal row would cascade-delete its targets
    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("SELECT * FROM life_plan_goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: String): Goal?

    @Query("SELECT * FROM life_plan_targets ORDER BY `order` ASC")
    fun getAllTargets(): Flow<List<Target>>

    @Query("SELECT * FROM life_plan_targets WHERE goalId = :goalId ORDER BY `order` ASC")
    fun getTargetsForGoal(goalId: String): Flow<List<Target>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: Target)

    @Query("DELETE FROM life_plan_targets WHERE id = :targetId")
    suspend fun deleteTarget(targetId: String)

    @Query("DELETE FROM life_plan_targets WHERE goalId = :goalId")
    suspend fun deleteTargetsByGoal(goalId: String)

    @Query("SELECT * FROM user_gamification_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserStats(stats: UserStatsEntity)
}