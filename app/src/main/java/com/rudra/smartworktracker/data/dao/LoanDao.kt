package com.rudra.smartworktracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.rudra.smartworktracker.data.entity.Loan
import com.rudra.smartworktracker.data.entity.LoanType
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Insert
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Upsert
    suspend fun upsertLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    @Query("SELECT * FROM loans WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 AND isActive = 1 ORDER BY date DESC")
    fun getActiveLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 AND loanType = :loanType AND isActive = 1 ORDER BY date DESC")
    fun getLoansByType(loanType: LoanType): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :loanId AND isDeleted = 0")
    fun getLoanById(loanId: Int): Flow<Loan?>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 AND personName LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchLoans(query: String): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 AND isActive = 1 AND isFullyPaid = 0 ORDER BY dueDate ASC")
    fun getLoansDueSoon(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 AND isActive = 1 AND dueDate < :currentTime AND isFullyPaid = 0 ORDER BY dueDate ASC")
    fun getOverdueLoans(currentTime: Long): Flow<List<Loan>>

    @Query("SELECT SUM(remainingAmount) FROM loans WHERE isDeleted = 0 AND isActive = 1 AND isFullyPaid = 0 AND loanType = :loanType")
    fun getTotalRemainingByType(loanType: LoanType): Flow<Double?>

    @Query("SELECT COUNT(*) FROM loans WHERE isDeleted = 0 AND isActive = 1 AND isFullyPaid = 0 AND loanType = :loanType")
    fun getActiveLoanCountByType(loanType: LoanType): Flow<Int>

    @Query("UPDATE loans SET isActive = 0, isFullyPaid = 1, updatedAt = :timestamp WHERE id = :loanId")
    suspend fun markLoanAsPaid(loanId: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE loans SET remainingAmount = :remainingAmount, paidEmis = :paidEmis, updatedAt = :timestamp WHERE id = :loanId")
    suspend fun updateLoanProgress(loanId: Int, remainingAmount: Double, paidEmis: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM loans WHERE id = :loanId")
    suspend fun deleteLoanById(loanId: Int)
}
