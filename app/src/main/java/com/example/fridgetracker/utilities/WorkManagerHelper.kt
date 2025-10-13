package com.example.fridgetracker.utilities

import android.content.Context
import androidx.work.*
import com.example.fridgetracker.worker.ExpiryCheckWorker
import java.util.concurrent.TimeUnit

/**
 * Helper object to manage WorkManager tasks
 * Extracted from MainActivity for better separation of concerns
 */
object WorkManagerHelper {

    private const val EXPIRY_CHECK_WORK_NAME = "expiry_check_unique"
    private const val EXPIRY_CHECK_TAG = "expiry_check_tag"

    /**
     * Schedule periodic expiry check worker
     * Runs once per day
     */
    fun scheduleExpiryCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiresStorageNotLow(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .addTag(EXPIRY_CHECK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EXPIRY_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            periodicWork
        )
    }

    /**
     * Cancel expiry check worker
     */
    fun cancelExpiryCheck(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(EXPIRY_CHECK_WORK_NAME)
    }

    /**
     * Force run expiry check immediately (for testing)
     */
    fun runExpiryCheckNow(context: Context) {
        val oneTimeWork = OneTimeWorkRequestBuilder<ExpiryCheckWorker>()
            .addTag(EXPIRY_CHECK_TAG)
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeWork)
    }
}