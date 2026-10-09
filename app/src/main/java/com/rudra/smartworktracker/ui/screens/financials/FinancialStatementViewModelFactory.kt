package com.rudra.smartworktracker.ui.screens.financials

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.TransactionRepository
import com.rudra.smartworktracker.data.repository.IncomeRepository
import com.rudra.smartworktracker.data.repository.ExpenseRepository

class FinancialStatementViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinancialStatementViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val transactionRepository = TransactionRepository(database.financialTransactionDao())
            val incomeRepository = IncomeRepository(database.incomeDao(), database.accountDao())
            val expenseRepository = ExpenseRepository(database.expenseDao(), database.accountDao())
            @Suppress("UNCHECKED_CAST")
            return FinancialStatementViewModel(transactionRepository, incomeRepository, expenseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
