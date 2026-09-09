package com.rudra.smartworktracker.ui.screens.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.ExpenseRepository
import com.rudra.smartworktracker.data.repository.IncomeRepository
import com.rudra.smartworktracker.data.repository.MealOvertimeRepository
import com.rudra.smartworktracker.data.repository.SavingsRepository
import com.rudra.smartworktracker.data.repository.SettingsRepository
import com.rudra.smartworktracker.data.repository.UserProfileRepository
import com.rudra.smartworktracker.data.repository.WorkLogRepository

class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val userProfileRepository = UserProfileRepository(database.userProfileDao())
            val workLogRepository = WorkLogRepository(database.workLogDao())
            val incomeRepository = IncomeRepository(database.incomeDao())
            val expenseRepository = ExpenseRepository(database.expenseDao())
            val settingsRepository = SettingsRepository(application)
            val savingsRepository = SavingsRepository(database.savingsDao())
            val mealOvertimeRepository = MealOvertimeRepository(
                database.workDayDao(),
                database.settingsDao(),
                database.summaryDao(),
                database.monthlyInputDao()
            )
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                application,
                userProfileRepository,
                workLogRepository,
                incomeRepository,
                expenseRepository,
                settingsRepository,
                savingsRepository,
                mealOvertimeRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
