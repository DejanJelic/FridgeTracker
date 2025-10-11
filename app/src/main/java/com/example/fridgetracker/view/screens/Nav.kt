package com.example.fridgetracker.view.screens

import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fridgetracker.view.barcode.ScanScreen
import com.example.fridgetracker.view_model.ProductViewModel
import com.example.fridgetracker.view_model.ShoppingListViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(productViewModel: ProductViewModel,shoppingViewModel: ShoppingListViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController,onAdd = { navController.navigate("add") }, onOpen = { id -> navController.navigate("edit/$id") },
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
        composable("suggestions") {
            val scope = rememberCoroutineScope()
            val scaffoldState = rememberScaffoldState()
            SuggestionsScreen(
                navController = navController,
                vm = productViewModel,
                onMenuClick = {
                    scope.launch {
                        scaffoldState.drawerState.open()
                    }
                }
            )
        }
        composable("category") {
            val scope = rememberCoroutineScope()
            val scaffoldState = rememberScaffoldState()

            CategoryScreen(
                navController = navController,
                vm = productViewModel,
                onMenuClick = {
                    scope.launch {
                        scaffoldState.drawerState.open()
                    }
                }
            )
        }
        composable("location") {
            val scope = rememberCoroutineScope()
            val scaffoldState = rememberScaffoldState()
            LocationScreen(
                navController = navController,
                vm = productViewModel,
                onMenuClick = {
                    scope.launch {
                        scaffoldState.drawerState.open()
                    }
                }
            )
        }
        composable("shopping") {
            val scope = rememberCoroutineScope()
            val scaffoldState = rememberScaffoldState()

            ShoppingListScreen(
                navController = navController,
                vm = shoppingViewModel,
                onMenuClick = {
                    scope.launch {
                        scaffoldState.drawerState.open()
                    }
                }
            )
        }
    }
}