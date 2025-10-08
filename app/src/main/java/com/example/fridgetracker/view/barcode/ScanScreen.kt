package com.example.fridgetracker.view.barcode

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.example.fridgetracker.model.ProductDraft
import com.example.fridgetracker.view_model.ProductViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
@Composable
fun ScanScreen(navController: NavController, vm: ProductViewModel) {
    var handled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BarcodeScannerScreen { code ->
        if (handled || code.isBlank()) return@BarcodeScannerScreen
        handled = true

        scope.launch {
            Toast.makeText(context, "Looking up product...", Toast.LENGTH_SHORT).show()

            val productInfo = vm.lookupBarcodeOnline(code)

            if (productInfo != null) {
                val category = productInfo.categories?.split(",")?.firstOrNull()?.trim() ?: "No category"

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

                Toast.makeText(context, "Product found: ${productInfo.productName}", Toast.LENGTH_SHORT).show()
            } else {
                // Proizvod nije pronađen online
                vm.setPrefill(ProductDraft(
                    barcode = code,
                    name = "",
                    quantity = 1.0,
                    unit = "pcs",
                    daysUntilExpiry = 30
                ))

                Toast.makeText(context, "Product not found. Please enter details manually.", Toast.LENGTH_LONG).show()
            }

            navController.navigate("add") {
                popUpTo("home") { inclusive = false }
            }
        }
    }
}

