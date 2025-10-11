package com.example.fridgetracker.view.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.fridgetracker.R
import com.example.fridgetracker.model.Product

object NotificationHelper {
    private const val CHANNEL_ID = "expiry_channel"
    private const val CHANNEL_NAME = "Expiry alerts"
    private const val NOTIF_ID = 1001
    private const val TAG = "NotificationHelper"

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val existing = mgr.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Notifications for products approaching expiry"
                    }
                    mgr.createNotificationChannel(channel)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to create notification channel: ${t.message}")
            }
        }
    }

    /**
     * Show a single notification summarizing expiring products.
     * This method performs a runtime permission check and returns silently if not allowed.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showExpiryNotification(context: Context, products: List<Product>) {
        if (products.isEmpty()) {
            Log.d(TAG, "No expiring products -> skipping notification")
            return
        }

        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted — skipping notification")
            return
        }

        ensureChannel(context)

        // Build a short title and a longer body (BigTextStyle)
        val title = when (products.size) {
            1 -> "${products[0].name} is expiring soon"
            else -> "${products.size} products expiring soon"
        }

        val maxShown = 6
        val names = products.map { it.name }
        val contentText = if (names.size <= maxShown) {
            names.joinToString(", ")
        } else {
            names.take(maxShown).joinToString(", ") + " and ${names.size - maxShown} more"
        }

        // PendingIntent to open your app (MainActivity or a specific screen)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, context.javaClass) // fallback
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon( R.drawable.ic_notification_small)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentText(contentText.take(60)) // short preview
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())
    }
}
