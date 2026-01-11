package com.example.fridgetracker.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fridgetracker.view.screens.AppNavHost
import com.example.fridgetracker.ui.theme.FridgeTrackerTheme
import com.example.fridgetracker.utilities.RequestNotificationPermission
import com.example.fridgetracker.utilities.WorkManagerHelper
import com.example.fridgetracker.view_model.ProductViewModel
import com.example.fridgetracker.view_model.ShoppingListViewModel
import com.example.fridgetracker.view_model.UserProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Schedule background worker
        WorkManagerHelper.scheduleExpiryCheck(this)

        setContent {
            // Request notification permission
            RequestNotificationPermission()

            // Create ViewModels using Hilt
            val productViewModel: ProductViewModel = hiltViewModel()
            val shoppingViewModel: ShoppingListViewModel = hiltViewModel()
            val userProfileViewModel: UserProfileViewModel = hiltViewModel()

            // App navigation
            FridgeTrackerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavHost(
                        productViewModel = productViewModel,
                        shoppingViewModel = shoppingViewModel,
                        userProfileViewModel = userProfileViewModel
                    )
                }
            }
        }
    }
}

