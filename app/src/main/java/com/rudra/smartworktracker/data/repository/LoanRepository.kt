package com.rudra.smartworktracker.data.repository

import com.rudra.smartworktracker.data.dao.FinancialTransactionDao
import com.rudra.smartworktracker.data.dao.LoanDao
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.entity.FinancialTransaction
import com.rudra.smartworktracker.data.entity.Loan
import com.rudra.smartworktracker.data.entity.LoanType
import com.rudra.smartworktracker.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LoanRepository(private val loanDao: LoanDao, private val transactionDao: FinancialTransactionDao) {

    fun getAllLoans(): Flow<List<Loan>> = loanDao.getAllLoans()

    fun getActiveLoans(): Flow<List<Loan>> = loanDao.getActiveLoans()

    fun getLoansByType(loanType: LoanType): Flow<List<Loan>> = loanDao.getLoansByType(loanType)

    fun getLoanById(loanId: Int): Flow<Loan?> = loanDao.getLoanById(loanId)

    fun searchLoans(query: String): Flow<List<Loan>> = loanDao.searchLoans(query)

    fun getLoansDueSoon(): Flow<List<Loan>> = loanDao.getLoansDueSoon()

    fun getOverdueLoans(): Flow<List<Loan>> = loanDao.getOverdueLoans(System.currentTimeMillis())

    fun getTotalBorrowed(): Flow<Double?> = loanDao.getTotalRemainingByType(LoanType.BORROWED)

    fun getTotalLent(): Flow<Double?> = loanDao.getTotalRemainingByType(LoanType.LENT)

    fun getActiveBorrowedCount(): Flow<Int> = loanDao.getActiveLoanCountByType(LoanType.BORROWED)

    fun getActiveLentCount(): Flow<Int> = loanDao.getActiveLoanCountByType(LoanType.LENT)

    suspend fun insertLoan(loan: Loan) {
        val loanId = loanDao.insertLoan(loan)
        val transactionType = if (loan.loanType == LoanType.BORROWED) {
            TransactionType.LOAN_BORROW
        } else {
            TransactionType.LOAN_LEND
        }
        
        val transaction = FinancialTransaction(
            type = transactionType,
            amount = loan.initialAmount,
            source = if (loan.loanType == LoanType.BORROWED) loan.destinationAccount else loan.sourceAccount,
            destination = if (loan.loanType == LoanType.BORROWED) loan.sourceAccount else loan.destinationAccount,
            note = buildString {
                append("Loan ${if (loan.loanType == LoanType.BORROWED) "from" else "to"}: ${loan.personName}")
                if (!loan.notes.isNullOrBlank()) append(" - ${loan.notes}")
            },
            date = loan.date,
            relatedLoanId = loanId.toInt()
        )
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateLoan(loan: Loan) {
        val updatedLoan = loan.copy(updatedAt = System.currentTimeMillis())
        loanDao.updateLoan(updatedLoan)
    }

    suspend fun deleteLoan(loan: Loan) {
        val deletedLoan = loan.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
        loanDao.updateLoan(deletedLoan)
        transactionDao.deleteTransactionsByLoanId(loan.id)
    }

    suspend fun repayLoan(loan: Loan, amount: Double) {
        // Never record more than what is still owed
        val applied = amount.coerceAtMost(loan.remainingAmount)
        if (applied <= 0.0) return
        val newRemaining = (loan.remainingAmount - applied).coerceAtLeast(0.0)
        val isFullyPaid = newRemaining <= 0.0
        
        val updatedLoan = loan.copy(
            remainingAmount = newRemaining,
            isFullyPaid = isFullyPaid,
            isActive = !isFullyPaid,
            paidEmis = loan.paidEmis + 1,
            updatedAt = System.currentTimeMillis()
        )
        loanDao.updateLoan(updatedLoan)
        
        val transaction = FinancialTransaction(
            type = if (loan.loanType == LoanType.BORROWED) TransactionType.LOAN_REPAY else TransactionType.LOAN_RECEIVE,
            amount = applied,
            source = if (loan.loanType == LoanType.BORROWED) loan.sourceAccount else loan.destinationAccount,
            destination = if (loan.loanType == LoanType.BORROWED) loan.destinationAccount else loan.sourceAccount,
            note = "Payment ${if (loan.loanType == LoanType.BORROWED) "to" else "from"} ${loan.personName}",
            date = System.currentTimeMillis(),
            relatedLoanId = loan.id
        )
        transactionDao.insertTransaction(transaction)
    }

    suspend fun receiveLoanRepayment(loan: Loan, amount: Double) {
        repayLoan(loan, amount)
    }

    suspend fun markLoanAsPaid(loan: Loan) {
        val remaining = loan.remainingAmount
        loanDao.updateLoanProgress(loan.id, 0.0, loan.paidEmis)
        loanDao.markLoanAsPaid(loan.id)
        if (remaining <= 0.0) return

        val transaction = FinancialTransaction(
            type = if (loan.loanType == LoanType.BORROWED) TransactionType.LOAN_REPAY else TransactionType.LOAN_RECEIVE,
            amount = remaining,
            source = if (loan.loanType == LoanType.BORROWED) loan.sourceAccount else loan.destinationAccount,
            destination = if (loan.loanType == LoanType.BORROWED) loan.destinationAccount else loan.sourceAccount,
            note = "Final settlement for loan ${if (loan.loanType == LoanType.BORROWED) "from" else "to"} ${loan.personName}",
            date = System.currentTimeMillis(),
            relatedLoanId = loan.id
        )
        transactionDao.insertTransaction(transaction)
    }

    suspend fun getLoanWithTransactions(loanId: Int): Pair<Loan?, List<FinancialTransaction>> {
        val loan = loanDao.getLoanById(loanId).first()
        val transactions = transactionDao.getAllTransactions().first()
            .filter { it.relatedLoanId == loanId }
        return Pair(loan, transactions)
    }
}
