package com.rudra.smartworktracker.ui.screens.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.backup.BackupManager
import com.rudra.smartworktracker.data.repository.*
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val userProfileRepository: UserProfileRepository,
    private val workLogRepository: WorkLogRepository,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val savingsRepository: SavingsRepository,
    private val mealOvertimeRepository: MealOvertimeRepository
) : AndroidViewModel(application) {

    private val backupManager = BackupManager(application)

    private val _backupResult = MutableSharedFlow<String>()
    val backupResult: SharedFlow<String> = _backupResult

    private val _restoreResult = MutableSharedFlow<Result<Unit>>()
    val restoreResult: SharedFlow<Result<Unit>> = _restoreResult

    val userProfile = userProfileRepository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val mealRate = settingsRepository.mealRate.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 60.0
    )
    val isDarkTheme = settingsRepository.darkTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val notificationsEnabled = settingsRepository.notifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    val vibrationEnabled = settingsRepository.vibration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    val autoBackupEnabled = settingsRepository.autoBackup.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val biometricEnabled = settingsRepository.biometric.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val currency = settingsRepository.currency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "BDT"
    )
    val overtimeRate = settingsRepository.overtimeRate.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 100.0
    )
    val dailyWorkHours = settingsRepository.dailyWorkHours.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 9.0
    )
    val workingDaysPerWeek = settingsRepository.workingDaysPerWeek.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 5
    )

    fun setMealRate(rate: Double) {
        viewModelScope.launch {
            settingsRepository.setMealRate(rate)
            syncRoomSettings()
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(isDark)
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifications(enabled)
        }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibration(enabled)
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBackup(enabled)
        }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometric(enabled)
        }
    }

    fun setCurrency(currencyCode: String) {
        viewModelScope.launch {
            settingsRepository.setCurrency(currencyCode)
            CurrencyManager.setCurrency(currencyCode)
        }
    }

    fun setOvertimeRate(rate: Double) {
        viewModelScope.launch {
            settingsRepository.setOvertimeRate(rate)
            syncRoomSettings()
        }
    }

    fun setDailyWorkHours(hours: Double) {
        viewModelScope.launch {
            settingsRepository.setDailyWorkHours(hours)
            syncRoomSettings()
        }
    }

    fun setWorkingDaysPerWeek(days: Int) {
        viewModelScope.launch {
            settingsRepository.setWorkingDaysPerWeek(days)
            syncRoomSettings()
        }
    }

    private suspend fun syncRoomSettings() {
        val mealRate = settingsRepository.mealRate.first()
        val overtimeRate = settingsRepository.overtimeRate.first()
        val dailyWorkHours = settingsRepository.dailyWorkHours.first()
        val workingDaysPerWeek = settingsRepository.workingDaysPerWeek.first()
        mealOvertimeRepository.saveSettings(
            com.rudra.smartworktracker.data.entity.Settings(
                mealRate = mealRate,
                overtimeRate = overtimeRate,
                dailyWorkHours = dailyWorkHours,
                workingDaysPerWeek = workingDaysPerWeek
            )
        )
    }

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val success = backupManager.exportToJson(outputStream)
                    if (success) {
                        _backupResult.emit("Backup successful")
                    } else {
                        _backupResult.emit("Backup failed")
                    }
                }
            } catch (e: Exception) {
                _backupResult.emit("Error: ${e.message}")
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val result = backupManager.importFromJson(inputStream)
                    _restoreResult.emit(result)
                }
            } catch (e: Exception) {
                _restoreResult.emit(Result.failure(e))
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            userProfileRepository.clearAll()
            workLogRepository.clearAll()
            incomeRepository.clearAll()
            expenseRepository.clearAll()
            settingsRepository.clearAll()
            savingsRepository.clearAll()
        }
    }
}
