package com.rudra.smartworktracker.ui.screens.reports

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.ExpenseRepository
import com.rudra.smartworktracker.data.repository.IncomeRepository
import com.rudra.smartworktracker.data.repository.WorkLogRepository

class ReportsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val workLogRepository = WorkLogRepository(database.workLogDao())
            val expenseRepository = ExpenseRepository(database.expenseDao(), database.accountDao())
            val incomeRepository = IncomeRepository(database.incomeDao(), database.accountDao())
            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(workLogRepository, expenseRepository, incomeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
