package com.rudra.smartworktracker.ui.screens.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.alarm.AlarmScheduler
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.backup.BackupManager
import com.rudra.smartworktracker.data.repository.*
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** Emitted once a full reset finished; the screen restarts the app into onboarding. */
    private val _resetDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetDone: SharedFlow<Unit> = _resetDone

    val themeMode = settingsRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

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

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
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
                val stream = context.contentResolver.openOutputStream(uri)
                if (stream == null) {
                    _backupResult.emit("Couldn't open the selected file")
                    return@launch
                }
                stream.use { outputStream ->
                    val success = backupManager.exportToJson(outputStream)
                    _backupResult.emit(if (success) "Backup saved" else "Backup failed")
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
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    _restoreResult.emit(Result.failure(IllegalStateException("Couldn't open the selected file")))
                    return@launch
                }
                stream.use { inputStream ->
                    // BackupManager re-arms alarms and reloads the currency after a restore
                    _restoreResult.emit(backupManager.importFromJson(inputStream))
                }
            } catch (e: Exception) {
                _restoreResult.emit(Result.failure(e))
            }
        }
    }

    /**
     * Factory reset. Previously only six tables were cleared, leaving accounts, loans, EMIs,
     * schedules (with live alarms), habits, journals etc. behind.
     */
    fun clearAllData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val database = AppDatabase.getDatabase(context)
            // Cancel alarms while their schedules still exist
            runCatching {
                val scheduler = AlarmScheduler(context)
                database.scheduleDao().getAllSchedulesOnce().forEach(scheduler::cancel)
            }
            withContext(Dispatchers.IO) { database.clearAllTables() }
            settingsRepository.clearAll()
            RESET_PREFERENCE_FILES.forEach {
                context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
            }
            CurrencyManager.setCurrency(settingsRepository.currency.first())
            _resetDone.emit(Unit)
        }
    }

    companion object {
        /** Every SharedPreferences file the app writes, including the onboarding flag. */
        private val RESET_PREFERENCE_FILES = listOf(
            "main_prefs", "WorkLogs", "backup_prefs", "future_self_prefs", "recent_features",
            "recurring_prefs", "spend_advisor_prefs", "work_health", "recurring_notifications",
            "wisdom_boosters"
        )
    }
}
