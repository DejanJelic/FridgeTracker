package com.example.fridgetracker.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fridgetracker.view.screens.AppNavHost
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fridgetracker.data.AppDatabase
import com.example.fridgetracker.repository.ProductRepository
import com.example.fridgetracker.repository.ShoppingListRepository
import com.example.fridgetracker.repository.UserProfileRepository
import com.example.fridgetracker.utilities.RequestNotificationPermission
import com.example.fridgetracker.utilities.WorkManagerHelper
import com.example.fridgetracker.view_model.ProductViewModel
import com.example.fridgetracker.view_model.ProductViewModelFactory
import com.example.fridgetracker.view_model.ShoppingListViewModel
import com.example.fridgetracker.view_model.ShoppingListViewModelFactory
import com.example.fridgetracker.view_model.UserProfileViewModel
import com.example.fridgetracker.view_model.UserProfileViewModelFactory

class MainActivity : ComponentActivity() {
    // Lazy initialization of repositories
    private val productRepository by lazy {
        val dao = AppDatabase.getInstance(application).productDao()
        ProductRepository(dao, applicationContext)
    }

    private val shoppingRepository by lazy {
        val dao = AppDatabase.getInstance(application).shoppingListDao()
        ShoppingListRepository(dao)
    }

    private val userProfileRepository by lazy {
        val dao = AppDatabase.getInstance(application).userProfileDao()
        UserProfileRepository(dao)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // ✅ Schedule background worker
            WorkManagerHelper.scheduleExpiryCheck(this)

            setContent {
                // ✅ Request notification permission
                RequestNotificationPermission()

                // ✅ Create ViewModels with factories
                val productViewModel: ProductViewModel = viewModel(
                    factory = ProductViewModelFactory(productRepository)
                )

                val shoppingViewModel: ShoppingListViewModel = viewModel(
                    factory = ShoppingListViewModelFactory(shoppingRepository)
                )

                val userProfileViewModel: UserProfileViewModel = viewModel(
                    factory = UserProfileViewModelFactory(userProfileRepository)
                )

                // ✅ App navigation
                AppNavHost(
                    productViewModel = productViewModel,
                    shoppingViewModel = shoppingViewModel,
                    userProfileViewModel = userProfileViewModel
                )
            }
        }
    }

