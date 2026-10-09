package com.rudra.smartworktracker

import android.app.Application
import androidx.work.*
import com.rudra.smartworktracker.data.backup.AutoBackupWorker
import com.rudra.smartworktracker.alarm.RecurringNotificationWorker
import com.rudra.smartworktracker.data.repository.SettingsRepository
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SmartWorkTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initCurrency()
        scheduleDailyBackup()
        scheduleRecurringNotifications()
    }

    private fun initCurrency() {
        runBlocking(Dispatchers.IO) {
            val settingsRepository = SettingsRepository(applicationContext)
            val savedCurrency = settingsRepository.currency.first()
            CurrencyManager.init(savedCurrency)
        }
    }

    private fun scheduleDailyBackup() {
        // 12:05 AM daily; KEEP so app starts don't push the next run back
        AutoBackupWorker.schedule(this, replace = false)
    }

    private fun scheduleRecurringNotifications() {
        // Schedule the recurring notification worker to check for due transactions
        RecurringNotificationWorker.schedule(this)
    }
}
