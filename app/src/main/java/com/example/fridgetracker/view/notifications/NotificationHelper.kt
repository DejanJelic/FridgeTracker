package com.example.fridgetracker.view.notifications

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.fridgetracker.model.Product

object NotificationHelper {
    private const val CHANNEL_ID = "expiry_channel"
    private const val NOTIF_ID = 1001

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showExpiryNotification(context: Context, products: List<Product>) {
        if (!hasNotificationPermission(context)) {
            Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted — skipping notification")
            return
        }

        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Expiry alerts", NotificationManager.IMPORTANCE_DEFAULT)
        mgr.createNotificationChannel(channel)

        val title = "${products.size} product(s) expiring soon"
        val text = products.joinToString(", ") { it.name }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())
    }
}