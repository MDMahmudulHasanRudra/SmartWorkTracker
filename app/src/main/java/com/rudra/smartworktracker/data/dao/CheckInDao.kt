package com.rudra.smartworktracker.data.dao

import androidx.room.*
import com.rudra.smartworktracker.model.ConsequenceDebt
import com.rudra.smartworktracker.model.DailyCheckIn
import com.rudra.smartworktracker.model.DecisionCategory
import com.rudra.smartworktracker.model.UserHistory
import com.rudra.smartworktracker.model.WeeklyReport
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM daily_checkins ORDER BY date DESC LIMIT 30")
    fun getAllCheckIns(): Flow<List<DailyCheckIn>>

    @Query("SELECT * FROM daily_checkins WHERE date >= :startOfDay AND date < :endOfDay LIMIT 1")
    fun getCheckInForDay(startOfDay: Long, endOfDay: Long): Flow<DailyCheckIn?>

    @Query("SELECT * FROM daily_checkins WHERE checkInType = :type AND date >= :startOfDay AND date < :endOfDay LIMIT 1")
    fun getCheckInByType(type: String, startOfDay: Long, endOfDay: Long): Flow<DailyCheckIn?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: DailyCheckIn)

    @Update
    suspend fun updateCheckIn(checkIn: DailyCheckIn)

    @Query("DELETE FROM daily_checkins WHERE id = :id")
    suspend fun deleteCheckIn(id: String)
}

@Dao
interface ConsequenceDebtDao {
    @Query("SELECT * FROM consequence_debt")
    fun getAllDebts(): Flow<List<ConsequenceDebt>>

    @Query("SELECT * FROM consequence_debt WHERE category = :category LIMIT 1")
    fun getDebtByCategory(category: String): Flow<ConsequenceDebt?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: ConsequenceDebt)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDebtIfAbsent(debt: ConsequenceDebt)

    @Update
    suspend fun updateDebt(debt: ConsequenceDebt)

    @Query("UPDATE consequence_debt SET debtAmount = debtAmount + :amount, lastUpdated = :timestamp WHERE category = :category")
    suspend fun addToDebt(category: String, amount: Float, timestamp: Long)

    @Query("SELECT SUM(debtAmount) FROM consequence_debt")
    fun getTotalDebt(): Flow<Float?>
}

@Dao
interface WeeklyReportDao {
    @Query("SELECT * FROM weekly_reports ORDER BY weekStartDate DESC")
    fun getAllReports(): Flow<List<WeeklyReport>>

    @Query("SELECT * FROM weekly_reports WHERE weekStartDate = :weekStart LIMIT 1")
    fun getReportForWeek(weekStart: Long): Flow<WeeklyReport?>

    @Query("SELECT * FROM weekly_reports WHERE weekStartDate = :weekStart LIMIT 1")
    suspend fun getReportForWeekOnce(weekStart: Long): WeeklyReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: WeeklyReport)

    @Query("SELECT * FROM weekly_reports ORDER BY createdAt DESC LIMIT 1")
    fun getLatestReport(): Flow<WeeklyReport?>
}

@Dao
interface UserHistoryDao {
    @Query("SELECT * FROM user_history WHERE id = 'user_stats' LIMIT 1")
    fun getUserHistory(): Flow<UserHistory?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: UserHistory)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistoryIfAbsent(history: UserHistory)

    @Update
    suspend fun updateHistory(history: UserHistory)

    @Query("UPDATE user_history SET totalDaysActive = totalDaysActive + 1, lastActiveDate = :date WHERE id = 'user_stats'")
    suspend fun incrementDaysActive(date: Long)

    @Query("UPDATE user_history SET totalDecisions = totalDecisions + 1, totalPositiveDecisions = totalPositiveDecisions + :positive, totalNegativeDecisions = totalNegativeDecisions + :negative WHERE id = 'user_stats'")
    suspend fun addDecision(positive: Int, negative: Int)

    @Query("UPDATE user_history SET totalDecisions = MAX(totalDecisions - 1, 0), totalPositiveDecisions = MAX(totalPositiveDecisions - :positive, 0), totalNegativeDecisions = MAX(totalNegativeDecisions - :negative, 0) WHERE id = 'user_stats'")
    suspend fun removeDecision(positive: Int, negative: Int)
}
