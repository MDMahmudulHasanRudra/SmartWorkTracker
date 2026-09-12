package com.rudra.smartworktracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val mealRateKey = doublePreferencesKey("meal_rate")
    private val overtimeRateKey = doublePreferencesKey("overtime_rate")
    private val dailyWorkHoursKey = doublePreferencesKey("daily_work_hours")
    private val workingDaysPerWeekKey = intPreferencesKey("working_days_per_week")
    private val darkThemeKey = booleanPreferencesKey(DARK_THEME)
    private val notificationsKey = booleanPreferencesKey(NOTIFICATIONS)
    private val vibrationKey = booleanPreferencesKey(VIBRATION)
    private val autoBackupKey = booleanPreferencesKey(AUTO_BACKUP)
    private val biometricKey = booleanPreferencesKey(BIOMETRIC)
    private val currencyKey = stringPreferencesKey(CURRENCY)
    private val fontSizeKey = doublePreferencesKey("font_size")
    private val accentColorKey = intPreferencesKey("accent_color")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val heroColorKey = intPreferencesKey("hero_color")
    private val selectedAccountIdKey = longPreferencesKey("selected_account_id")

    val heroColor: Flow<Int> = context.dataStore.data.map {
        it[heroColorKey] ?: 0
    }

    suspend fun setHeroColor(color: Int) {
        context.dataStore.edit {
            it[heroColorKey] = color
        }
    }

    val selectedAccountId: Flow<Long> = context.dataStore.data.map {
        it[selectedAccountIdKey] ?: -1L
    }

    suspend fun setSelectedAccountId(id: Long) {
        context.dataStore.edit {
            it[selectedAccountIdKey] = id
        }
    }

    val mealRate: Flow<Double> = context.dataStore.data.map {
        it[mealRateKey] ?: 60.0
    }

    suspend fun setMealRate(rate: Double) {
        context.dataStore.edit {
            it[mealRateKey] = rate
        }
    }

    val overtimeRate: Flow<Double> = context.dataStore.data.map {
        it[overtimeRateKey] ?: 100.0
    }

    suspend fun setOvertimeRate(rate: Double) {
        context.dataStore.edit {
            it[overtimeRateKey] = rate
        }
    }

    val dailyWorkHours: Flow<Double> = context.dataStore.data.map {
        it[dailyWorkHoursKey] ?: 9.0
    }

    suspend fun setDailyWorkHours(hours: Double) {
        context.dataStore.edit {
            it[dailyWorkHoursKey] = hours
        }
    }

    val workingDaysPerWeek: Flow<Int> = context.dataStore.data.map {
        it[workingDaysPerWeekKey] ?: 5
    }

    suspend fun setWorkingDaysPerWeek(days: Int) {
        context.dataStore.edit {
            it[workingDaysPerWeekKey] = days
        }
    }

    val darkTheme: Flow<Boolean> = context.dataStore.data.map {
        it[darkThemeKey] ?: false
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit {
            it[darkThemeKey] = isDark
        }
    }

    val notifications: Flow<Boolean> = context.dataStore.data.map {
        it[notificationsKey] ?: true
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit {
            it[notificationsKey] = enabled
        }
    }

    val vibration: Flow<Boolean> = context.dataStore.data.map {
        it[vibrationKey] ?: true
    }

    suspend fun setVibration(enabled: Boolean) {
        context.dataStore.edit {
            it[vibrationKey] = enabled
        }
    }

    val autoBackup: Flow<Boolean> = context.dataStore.data.map {
        it[autoBackupKey] ?: false
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        context.dataStore.edit {
            it[autoBackupKey] = enabled
        }
    }

    val biometric: Flow<Boolean> = context.dataStore.data.map {
        it[biometricKey] ?: false
    }

    suspend fun setBiometric(enabled: Boolean) {
        context.dataStore.edit {
            it[biometricKey] = enabled
        }
    }

    val currency: Flow<String> = context.dataStore.data.map {
        it[currencyKey] ?: "BDT"
    }

    suspend fun setCurrency(currencyCode: String) {
        context.dataStore.edit {
            it[currencyKey] = currencyCode
        }
    }

    val fontSize: Flow<Double> = context.dataStore.data.map {
        it[fontSizeKey] ?: 1.0
    }

    suspend fun setFontSize(size: Double) {
        context.dataStore.edit {
            it[fontSizeKey] = size
        }
    }

    val accentColor: Flow<Int> = context.dataStore.data.map {
        it[accentColorKey] ?: 0
    }

    suspend fun setAccentColor(index: Int) {
        context.dataStore.edit {
            it[accentColorKey] = index
        }
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map {
        it[dynamicColorKey] ?: true
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit {
            it[dynamicColorKey] = enabled
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit {
            it.clear()
        }
    }

    companion object {
        const val NOTIFICATIONS = "notifications"
        const val DARK_THEME = "dark_theme"
        const val VIBRATION = "vibration"
        const val AUTO_BACKUP = "auto_backup"
        const val BIOMETRIC = "biometric"
        const val CURRENCY = "currency"
        const val MEAL_RATE = "meal_rate"
        const val OVERTIME_RATE = "overtime_rate"
        const val DAILY_WORK_HOURS = "daily_work_hours"
        const val WORKING_DAYS_PER_WEEK = "working_days_per_week"
    }
}
