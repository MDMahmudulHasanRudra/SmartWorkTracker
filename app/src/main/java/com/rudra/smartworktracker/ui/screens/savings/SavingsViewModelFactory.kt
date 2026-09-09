package com.rudra.smartworktracker.ui.screens.savings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.AccountRepository
import com.rudra.smartworktracker.data.repository.SavingsRepository

class SavingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavingsViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            val savingsRepository = SavingsRepository(db.savingsDao())
            val accountRepository = AccountRepository(db.accountDao())
            @Suppress("UNCHECKED_CAST")
            return SavingsViewModel(savingsRepository, accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
