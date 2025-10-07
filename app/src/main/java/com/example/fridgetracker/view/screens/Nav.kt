package com.example.fridgetracker.view.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fridgetracker.view.barcode.ScanScreen
import com.example.fridgetracker.view_model.ProductViewModel

@Composable
fun AppNavHost(productViewModel: ProductViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(onAdd = { navController.navigate("add") }, onOpen = { id -> navController.navigate("edit/$id") },
            vm = productViewModel) }
        composable("add") {
            EditProductScreen(
            navController = navController,
            vm = productViewModel,
            productId = null
            )
        }
        composable("scan") {
            ScanScreen(navController = navController, vm = productViewModel)
        }
        composable("edit/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            EditProductScreen(
                navController = navController,
                vm = productViewModel,
                productId = id // Edit mode
            )
        }
    }
}