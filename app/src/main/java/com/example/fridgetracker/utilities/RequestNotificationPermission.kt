package com.example.fridgetracker.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val PREFS_NAME = "app_prefs"
private const val KEY_NOTIF_REQUESTED = "notif_requested_once"

@Composable
fun RequestNotificationPermission() {
    // Only relevant for Android 13+
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val permission = Manifest.permission.POST_NOTIFICATIONS

    // SharedPreferences to track if permission was requested
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var askedOnce by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_NOTIF_REQUESTED, false))
    }

    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED

    var showDialog by remember { mutableStateOf(false) }

    // Permission launcher
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        prefs.edit().putBoolean(KEY_NOTIF_REQUESTED, true).apply()
        askedOnce = true

        val message = if (granted) {
            "Notifications enabled"
        } else {
            "Notifications denied"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Show dialog if permission needed and not asked yet
    LaunchedEffect(hasPermission, askedOnce) {
        if (!hasPermission && !askedOnce) {
            showDialog = true
        }
    }

    // Permission request dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Allow reminders?") },
            text = {
                Text("We need permission to send notifications for expiry reminders. You can change this later in settings.")
            },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    launcher.launch(permission)
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog = false
                    prefs.edit().putBoolean(KEY_NOTIF_REQUESTED, true).apply()
                    askedOnce = true
                }) {
                    Text("No thanks")
                }
            }
        )
    }
}