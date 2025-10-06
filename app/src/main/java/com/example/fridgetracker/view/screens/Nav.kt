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
        composable("home") { HomeScreen(onAdd = { navController.navigate("add") }, onOpen = { id -> navController.navigate("detail/$id") },
            vm = productViewModel) }
        composable("add") { AddProductScreen(navController = navController, vm = productViewModel) }
        composable("scan") {
            ScanScreen(navController = navController, vm = productViewModel)
        }
        composable("detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            ProductDetailScreen(productId = id ?: 0L, onBack = { navController.popBackStack() }, vm = productViewModel)
        }
    }
}