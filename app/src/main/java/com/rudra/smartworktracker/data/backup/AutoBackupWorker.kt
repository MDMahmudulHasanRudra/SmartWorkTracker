package com.rudra.smartworktracker.data.backup

import android.content.ContentValues
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rudra.smartworktracker.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "daily_backup_work"
        const val MANUAL_WORK_NAME = "manual_backup_work"
        /** Input flag for "back up now": write even when daily backups are switched off. */
        const val KEY_FORCE = "force"
        const val KEY_LAST_AUTO_BACKUP = "last_auto_backup_time"

        /**
         * Daily run shortly after midnight. The worker is always scheduled and the Settings
         * toggle decides whether a run writes anything.
         */
        fun schedule(context: Context, replace: Boolean) {
            val now = Calendar.getInstance()
            val due = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
                .setInitialDelay(due.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .addTag("daily_backup")
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val forced = inputData.getBoolean(KEY_FORCE, false)
        if (!forced && !SettingsRepository(applicationContext).autoBackup.first()) return Result.success()

        val backupManager = BackupManager(applicationContext)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "SmartWork_Auto_$timestamp.json"

        return try {
            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToDownloadsMediaStore(fileName, backupManager)
            } else {
                saveToDownloadsLegacy(fileName, backupManager)
            }

            if (success) {
                // Store last backup time in SharedPreferences
                val prefs = applicationContext.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_AUTO_BACKUP, System.currentTimeMillis()).apply()
                Result.success()
            } else if (forced) {
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveToDownloadsMediaStore(fileName: String, backupManager: BackupManager): Boolean {
        val resolver = applicationContext.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return false
        val success = resolver.openOutputStream(uri)?.use { outputStream ->
            backupManager.exportToJson(outputStream)
        } ?: false
        // Don't leave an empty/partial file behind in Downloads
        if (!success) resolver.delete(uri, null, null)
        return success
    }

    private suspend fun saveToDownloadsLegacy(fileName: String, backupManager: BackupManager): Boolean {
        // Android 9: public Downloads needs the storage permission, which a background worker
        // can't request; fall back to the app's own external folder instead of failing.
        val canWritePublic = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val downloadsDir = if (canWritePublic) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: applicationContext.filesDir
        }
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = File(downloadsDir, fileName)
        return FileOutputStream(file).use { outputStream ->
            backupManager.exportToJson(outputStream)
        }
    }
}
