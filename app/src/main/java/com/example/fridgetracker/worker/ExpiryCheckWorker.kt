package com.example.fridgetracker.worker

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fridgetracker.data.AppDatabase
import com.example.fridgetracker.view.notifications.NotificationHelper
import java.time.LocalDate

class ExpiryCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).productDao()
        val today = LocalDate.now().toEpochDay()
        val threshold = today + 2 // products expiring in 2 days
        val expiring = dao.getExpiringBefore(threshold)
        if (expiring.isNotEmpty()) {
            NotificationHelper.showExpiryNotification(applicationContext, expiring)
        }
        return Result.success()
    }
}