package com.example.fridgetracker.worker

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fridgetracker.data.AppDatabase
import com.example.fridgetracker.view.notifications.NotificationHelper
import java.time.LocalDate

class ExpiryCheckWorker(context: android.content.Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext

        val hasPermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            return Result.success()
        }

        val dao = AppDatabase.getInstance(ctx).productDao()

        val products = try {
            dao.getAllSync()
        } catch (t: Throwable) {
            t.printStackTrace()
            return Result.failure()
        }

        val todayEpoch = LocalDate.now().toEpochDay()
        val toNotify = mutableListOf<com.example.fridgetracker.model.Product>()

        for (p in products) {
            if (p.notifyExpiry) {
                val remDays = p.expiryDaysBefore
                val notifyDay = p.bestBeforeEpochDay - remDays
                if (notifyDay == todayEpoch) {
                    toNotify.add(p)
                    continue
                }
            }

            if (p.notifyAfterOpening && p.openedAtEpochDay != null) {
                val afterDays = p.afterOpeningDays
                val notifyDay = p.openedAtEpochDay + afterDays
                if (notifyDay == todayEpoch) {
                    toNotify.add(p)
                    continue
                }
            }
        }

        if (toNotify.isNotEmpty()) {
            NotificationHelper.showExpiryNotification(ctx, toNotify)
        }

        return Result.success()
    }
}
