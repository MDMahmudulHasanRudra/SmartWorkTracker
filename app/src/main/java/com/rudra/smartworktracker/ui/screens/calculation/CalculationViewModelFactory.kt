package com.rudra.smartworktracker.ui.screens.calculation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.SettingsRepository

class CalculationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculationViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val settingsRepository = SettingsRepository(context)
            @Suppress("UNCHECKED_CAST")
            return CalculationViewModel(database, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
