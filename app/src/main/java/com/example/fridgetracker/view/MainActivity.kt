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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fridgetracker.data.AppDatabase
import com.example.fridgetracker.repository.ProductRepository
import com.example.fridgetracker.repository.ShoppingListRepository
import com.example.fridgetracker.repository.UserProfileRepository
import com.example.fridgetracker.view_model.ProductViewModel
import com.example.fridgetracker.view_model.ProductViewModelFactory
import com.example.fridgetracker.view_model.ShoppingListViewModel
import com.example.fridgetracker.view_model.ShoppingListViewModelFactory
import com.example.fridgetracker.view_model.UserProfileViewModel
import com.example.fridgetracker.view_model.UserProfileViewModelFactory
import com.example.fridgetracker.worker.ExpiryCheckWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getInstance(application).productDao()
        val repo = ProductRepository(dao,application.applicationContext)
        val factory = ProductViewModelFactory(repo)
        val shoppingDao = AppDatabase.getInstance(application).shoppingListDao()
        val shoppingRepository = ShoppingListRepository(shoppingDao)
        val shoppingFactory = ShoppingListViewModelFactory(shoppingRepository)


        val userProfileDao = AppDatabase.getInstance(application).userProfileDao()
        val userProfileRepository = UserProfileRepository(userProfileDao)
        val userProfileViewModelFactory = UserProfileViewModelFactory(userProfileRepository)
        val userProfileViewModel = ViewModelProvider(this, userProfileViewModelFactory)
            .get(UserProfileViewModel::class.java)
        // Schedule periodic worker once (keep existing work if already scheduled)
        val periodic = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .setRequiresStorageNotLow(false)
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .addTag("expiry_check_tag")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "expiry_check_unique",
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
        setContent {
            RequestNotificationsWithMemory()
            val vm: ProductViewModel = viewModel(factory = factory)
            val shoppingViewModel: ShoppingListViewModel = viewModel(factory = shoppingFactory)
            val userProfileViewModel: UserProfileViewModel = viewModel(factory = userProfileViewModelFactory)
            AppNavHost(productViewModel = vm, shoppingViewModel = shoppingViewModel, userProfileViewModel = userProfileViewModel)
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
    var askedOnce by rememberSaveable { mutableStateOf(prefs.getBoolean(KEY_NOTIF_REQUESTED, false)) }
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