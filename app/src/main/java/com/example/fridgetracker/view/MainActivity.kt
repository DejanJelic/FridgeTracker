package com.example.fridgetracker.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fridgetracker.view.screens.AppNavHost
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fridgetracker.data.AppDatabase
import com.example.fridgetracker.repository.ProductRepository
import com.example.fridgetracker.view_model.ProductViewModel
import com.example.fridgetracker.view_model.ProductViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getInstance(application).productDao()
        val repo = ProductRepository(dao)
        val factory = ProductViewModelFactory(repo)
        setContent {
            RequestNotificationsWithMemory()
            val vm: ProductViewModel = viewModel(factory = factory)
            AppNavHost(productViewModel = vm)
        }
    }
}
private const val PREFS_NAME = "app_prefs"
private const val KEY_NOTIF_REQUESTED = "notif_requested_once"

@Composable
fun RequestNotificationsWithMemory() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val permission = Manifest.permission.POST_NOTIFICATIONS
    val activity = LocalContext.current as? Activity

    // SharedPreferences to track if we have already searched
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var askedOnce by remember { mutableStateOf(prefs.getBoolean(KEY_NOTIF_REQUESTED, false)) }
    val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    var showDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        prefs.edit().putBoolean(KEY_NOTIF_REQUESTED, true).apply()
        askedOnce = true
        if (granted) {
            Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications denied", Toast.LENGTH_SHORT).show()
        }
    }

    // If we don't have permission and haven't asked yet -> show dialog for explanation
    LaunchedEffect(hasPermission, askedOnce) {
        if (!hasPermission && !askedOnce) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Allow reminders?") },
            text = { Text("We need permission to post notifications for expiry reminders. You can always change this later in app settings.") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    val shouldShowRationale = activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                    } ?: false
                    launcher.launch(permission)
                }) { Text("Allow") }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog = false
                    prefs.edit().putBoolean(KEY_NOTIF_REQUESTED, true).apply()
                    askedOnce = true
                }) { Text("No thanks") }
            }
        )
    }

    // If we have a permanent rejection (askedOnce true && !hasPermission && !shouldShowRationale) — we offer to open Settings
    val shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
    } ?: false

    if (askedOnce && !hasPermission && !shouldShowRationale) {
        Button(onClick = {
            // Open application settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }) {
            Text("Enable notifications in Settings")
        }
    }
}