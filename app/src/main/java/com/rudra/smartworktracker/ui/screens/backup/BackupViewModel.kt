package com.rudra.smartworktracker.ui.screens.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rudra.smartworktracker.data.backup.AutoBackupWorker
import com.rudra.smartworktracker.data.backup.BackupManager
import com.rudra.smartworktracker.data.backup.BackupSummary
import com.rudra.smartworktracker.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingRestore(val uri: Uri, val summary: BackupSummary)

/**
 * Backup screen state. The auto-backup switch is the same DataStore setting the worker and the
 * Settings screen use (this screen used to keep its own SharedPreferences flag that the worker
 * never read, so turning it on here did nothing).
 */
class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val backupManager = BackupManager(application)
    private val settingsRepository = SettingsRepository(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _backupResult = MutableSharedFlow<BackupResult>(extraBufferCapacity = 4)
    val backupResult: SharedFlow<BackupResult> = _backupResult.asSharedFlow()

    private val _lastBackupTime = MutableStateFlow(0L)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _lastAutoBackupTime = MutableStateFlow(0L)
    val lastAutoBackupTime: StateFlow<Long> = _lastAutoBackupTime.asStateFlow()

    private val _pendingRestore = MutableStateFlow<PendingRestore?>(null)
    val pendingRestore: StateFlow<PendingRestore?> = _pendingRestore.asStateFlow()

    val isAutoBackupEnabled: StateFlow<Boolean> = settingsRepository.autoBackup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Next run of the periodic worker, straight from WorkManager. */
    val nextBackupTime: StateFlow<Long> = workManager.getWorkInfosForUniqueWorkFlow(AutoBackupWorker.UNIQUE_WORK_NAME)
        .map { infos ->
            infos.firstOrNull { it.state == WorkInfo.State.ENQUEUED }?.nextScheduleTimeMillis
                ?.takeIf { it != Long.MAX_VALUE } ?: 0L
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        loadBackupStatus()
        // Refresh "last backup" when a "back up now" run finishes
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(AutoBackupWorker.MANUAL_WORK_NAME).collect { infos ->
                val info = infos.firstOrNull() ?: return@collect
                if (info.state.isFinished) {
                    loadBackupStatus()
                    if (_isLoading.value) {
                        _isLoading.value = false
                        _backupResult.emit(
                            if (info.state == WorkInfo.State.SUCCEEDED) BackupResult.Success("Backup saved to Downloads")
                            else BackupResult.Error("Backup to Downloads failed")
                        )
                    }
                }
            }
        }
    }

    fun loadBackupStatus() {
        val auto = prefs.getLong(AutoBackupWorker.KEY_LAST_AUTO_BACKUP, 0L)
        val manual = prefs.getLong(KEY_LAST_MANUAL_BACKUP, 0L)
        _lastAutoBackupTime.value = auto
        _lastBackupTime.value = maxOf(auto, manual)
    }

    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBackup(enabled)
            // The periodic worker is always scheduled (see SmartWorkTrackerApplication);
            // make sure it exists in case it was cancelled by an older version
            if (enabled) AutoBackupWorker.schedule(context, replace = false)
        }
    }

    /** Writes a backup to Downloads right away, whether or not daily backups are on. */
    fun backupNowToDownloads() {
        if (_isLoading.value) return
        _isLoading.value = true
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInputData(workDataOf(AutoBackupWorker.KEY_FORCE to true))
            .build()
        workManager.enqueueUniqueWork(
            AutoBackupWorker.MANUAL_WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stream = context.contentResolver.openOutputStream(uri)
                if (stream == null) {
                    _backupResult.emit(BackupResult.Error("Couldn't open the selected file"))
                    return@launch
                }
                val success = stream.use { backupManager.exportToJson(it) }
                if (success) {
                    prefs.edit().putLong(KEY_LAST_MANUAL_BACKUP, System.currentTimeMillis()).apply()
                    loadBackupStatus()
                    _backupResult.emit(BackupResult.Success("Backup file saved"))
                } else {
                    _backupResult.emit(BackupResult.Error("Failed to create backup"))
                }
            } catch (e: Exception) {
                _backupResult.emit(BackupResult.Error("Backup failed: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Reads the picked file first so the confirmation can say what's inside. */
    fun inspectBackup(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val summary = context.contentResolver.openInputStream(uri)?.use { backupManager.readSummary(it) }
                if (summary == null) {
                    _backupResult.emit(BackupResult.Error("That file isn't a Smart Work Tracker backup"))
                } else {
                    _pendingRestore.value = PendingRestore(uri, summary)
                }
            } catch (e: Exception) {
                _backupResult.emit(BackupResult.Error("Couldn't read the file: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelRestore() {
        _pendingRestore.value = null
    }

    fun confirmRestore() {
        val pending = _pendingRestore.value ?: return
        _pendingRestore.value = null
        restoreBackup(pending.uri)
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    _backupResult.emit(BackupResult.Error("Couldn't open the selected file"))
                    return@launch
                }
                val result = stream.use { backupManager.importFromJson(it) }
                if (result.isSuccess) {
                    _backupResult.emit(BackupResult.Success("Data restored successfully"))
                } else {
                    _backupResult.emit(BackupResult.Error("Restore failed: ${result.exceptionOrNull()?.message}"))
                }
            } catch (e: Exception) {
                _backupResult.emit(BackupResult.Error("Restore failed: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        const val PREFS_NAME = "backup_prefs"
        private const val KEY_LAST_MANUAL_BACKUP = "last_manual_backup_time"
    }
}

sealed class BackupResult {
    data class Success(val message: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}
