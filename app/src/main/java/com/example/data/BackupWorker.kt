package com.example.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.KikiApplication
import com.example.domain.BackupFrequency
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = (applicationContext as KikiApplication).database
            BackupManager.createLocalBackup(
                context = applicationContext,
                db = db,
                actor = null,
                isAutomatic = true
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "kiki_auto_backup_work"

        fun schedule(context: Context, frequency: BackupFrequency) {
            val wm = WorkManager.getInstance(context)
            when (frequency) {
                BackupFrequency.OFF -> wm.cancelUniqueWork(WORK_NAME)
                BackupFrequency.DAILY -> {
                    val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS).build()
                    wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
                }
                BackupFrequency.WEEKLY -> {
                    val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build()
                    wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
                }
            }
        }
    }
}
