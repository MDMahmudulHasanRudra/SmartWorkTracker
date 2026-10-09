package com.rudra.smartworktracker.data.repository

import com.rudra.smartworktracker.data.dao.EmiDao
import com.rudra.smartworktracker.data.dao.FinancialTransactionDao
import com.rudra.smartworktracker.data.dao.LoanDao
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.entity.Emi
import com.rudra.smartworktracker.data.entity.EmiStatus
import com.rudra.smartworktracker.data.entity.FinancialTransaction
import com.rudra.smartworktracker.data.entity.Loan
import com.rudra.smartworktracker.data.entity.LoanType
import com.rudra.smartworktracker.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

class EmiRepository(
    private val emiDao: EmiDao,
    private val loanDao: LoanDao,
    private val transactionDao: FinancialTransactionDao
) {
    fun getActiveEmis(): Flow<List<Emi>> = emiDao.getActiveEmis()

    fun getAllActiveEmis(): Flow<List<Emi>> = emiDao.getAllActiveEmis()

    fun getEmisForLoan(loanId: Int): Flow<List<Emi>> = emiDao.getEmisForLoan(loanId)

    fun getAllEmis(): Flow<List<Emi>> = emiDao.getAllEmis()

    fun getEmiById(emiId: Int): Flow<Emi?> = emiDao.getEmiById(emiId)

    fun getOverdueEmis(): Flow<List<Emi>> = emiDao.getOverdueEmis(System.currentTimeMillis())

    fun getEmisDueThisMonth(): Flow<List<Emi>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis
        
        return emiDao.getEmisDueInRange(startOfMonth, endOfMonth)
    }

    fun getPaidEmis(): Flow<List<Emi>> = emiDao.getPaidEmis()

    fun getTotalPendingAmount(): Flow<Double?> = emiDao.getTotalPendingEmiAmount()

    fun getPendingEmiCount(): Flow<Int> = emiDao.getPendingEmiCount()

    fun getOverdueEmiCount(): Flow<Int> = emiDao.getOverdueEmiCount(System.currentTimeMillis())

    fun getTotalPenaltyCollected(): Flow<Double?> = emiDao.getTotalPenaltyCollected()

    suspend fun insertEmi(emi: Emi): Long {
        return emiDao.insertEmi(emi)
    }

    suspend fun updateEmi(emi: Emi) {
        val updatedEmi = emi.copy(updatedAt = System.currentTimeMillis())
        emiDao.updateEmi(updatedEmi)
    }

    suspend fun deleteEmi(emi: Emi) {
        val deletedEmi = emi.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
        emiDao.updateEmi(deletedEmi)
    }

    fun getActiveLoans(): Flow<List<Loan>> = loanDao.getActiveLoans()

    /**
     * Pays the current installment: the row is kept as payment history (isPaid) and, while the
     * loan still has a balance, the next month's installment is created so the schedule continues.
     */
    suspend fun payEmi(emi: Emi) {
        val loan = loanDao.getLoanById(emi.loanId).first()
        if (loan == null) return

        val paymentDate = System.currentTimeMillis()
        // Without a principal split the whole installment reduces the loan
        val principalPaid = if (emi.principalAmount > 0) emi.principalAmount else emi.amount
        val newRemainingAmount = (loan.remainingAmount - principalPaid).coerceAtLeast(0.0)
        val isLoanFullyPaid = newRemainingAmount <= 0.0

        loanDao.updateLoan(
            loan.copy(
                remainingAmount = newRemainingAmount,
                isFullyPaid = isLoanFullyPaid,
                isActive = !isLoanFullyPaid,
                paidEmis = loan.paidEmis + 1,
                updatedAt = paymentDate
            )
        )

        emiDao.markEmiAsPaid(emi.id, paymentDate)

        if (!isLoanFullyPaid) {
            emiDao.insertEmi(nextInstallment(emi, paymentDate))
        }

        val transactionType = if (loan.loanType == LoanType.BORROWED) {
            TransactionType.LOAN_REPAY
        } else {
            TransactionType.LOAN_RECEIVE
        }

        val transaction = FinancialTransaction(
            type = transactionType,
            amount = emi.totalPayable,
            source = if (loan.loanType == LoanType.BORROWED) loan.sourceAccount else loan.destinationAccount,
            destination = if (loan.loanType == LoanType.BORROWED) loan.destinationAccount else loan.sourceAccount,
            note = buildString {
                append("EMI payment for ${loan.personName}")
                if (emi.interestAmount > 0) append(" (Principal: ${emi.principalAmount}, Interest: ${emi.interestAmount})")
                if (emi.penaltyAmount > 0) append(" (Includes penalty: ${emi.penaltyAmount})")
            },
            date = paymentDate,
            relatedLoanId = loan.id
        )
        transactionDao.insertTransaction(transaction)
    }

    /** Skips this month: the row is closed as skipped and the schedule moves to next month. */
    suspend fun skipEmi(emi: Emi) {
        val now = System.currentTimeMillis()
        emiDao.updateEmi(emi.copy(isSkipped = true, isActive = false, updatedAt = now))
        emiDao.insertEmi(nextInstallment(emi, now))
    }

    private fun nextInstallment(emi: Emi, now: Long): Emi {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = emi.nextDueDate
        calendar.add(Calendar.MONTH, 1)
        // Keep the configured day of month (e.g. 31st -> 30th/28th -> back to 31st)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, emi.dueDateOfMonth.coerceIn(1, maxDay))
        return emi.copy(
            id = 0,
            uuid = null,
            nextDueDate = calendar.timeInMillis,
            lastPaymentDate = null,
            isActive = true,
            isPaid = false,
            isSkipped = false,
            penaltyAmount = 0.0,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun getEmiWithLoan(emiId: Int): Pair<Emi?, Loan?> {
        val emi = emiDao.getEmiById(emiId).first()
        val loan = emi?.let { loanDao.getLoanById(it.loanId).first() }
        return Pair(emi, loan)
    }

    suspend fun getUpcomingEmisWithLoans(): List<Pair<Emi, Loan>> {
        val emis = emiDao.getActiveEmis().first()
        return emis.mapNotNull { emi ->
            val loan = loanDao.getLoanById(emi.loanId).first()
            if (loan != null) Pair(emi, loan) else null
        }
    }
}
