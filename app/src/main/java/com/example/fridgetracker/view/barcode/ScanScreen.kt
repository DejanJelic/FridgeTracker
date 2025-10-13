package com.example.fridgetracker.view.barcode

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fridgetracker.model.ProductDraft
import com.example.fridgetracker.view_model.ProductViewModel
import kotlinx.coroutines.launch

@Composable
fun ScanScreen(navController: NavController, vm: ProductViewModel) {
    var handled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        BarcodeScannerScreen { code ->
            if (handled || code.isBlank() || isLoading) return@BarcodeScannerScreen
            handled = true
            isLoading = true

            scope.launch {
                Toast.makeText(context, "Looking up product...", Toast.LENGTH_SHORT).show()

                try {
                    // Use callback-based API instead of suspend
                    vm.lookupBarcodeOnline(code) { productInfo ->
                        if (productInfo != null) {
                            val category = productInfo.categories
                                ?.split(",")
                                ?.firstOrNull()
                                ?.trim()
                                ?: "No category"

                            vm.setPrefill(ProductDraft(
                                barcode = code,
                                name = productInfo.productName ?: "Unknown Product",
                                quantity = 1.0,
                                unit = "pcs",
                                daysUntilExpiry = 30,
                                location = "Fridge",
                                imageUrl = productInfo.imageUrl,
                                category = category
                            ))

                            Toast.makeText(
                                context,
                                "Product found: ${productInfo.productName}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Product not found online
                            vm.setPrefill(ProductDraft(
                                barcode = code,
                                name = "",
                                quantity = 1.0,
                                unit = "pcs",
                                daysUntilExpiry = 30
                            ))

                            Toast.makeText(
                                context,
                                "Product not found. Please enter details manually.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // Navigate to add screen
                        navController.navigate("add") {
                            popUpTo("home") { inclusive = false }
                        }

                        isLoading = false
                    }
                } catch (e: Exception) {
                    // Better error handling
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    navController.popBackStack()
                    isLoading = false
                }
            }
        }

        // Loading indicator overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Looking up product...",
                        color = Color.White
                    )
                }
            }
        }
    }
}

