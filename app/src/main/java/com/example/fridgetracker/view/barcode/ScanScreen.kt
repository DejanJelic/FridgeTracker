package com.example.fridgetracker.view.barcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.example.fridgetracker.model.ProductDraft
import com.example.fridgetracker.view_model.ProductViewModel

@Composable
fun ScanScreen(navController: NavController, vm: ProductViewModel) {
    var handled by remember { mutableStateOf(false) }
    var detectedCode by remember { mutableStateOf<String?>(null) }

    BarcodeScannerScreen { code ->
        if (handled) return@BarcodeScannerScreen
        handled = true
        detectedCode = code
    }

    LaunchedEffect(detectedCode) {
        val code = detectedCode
        if (code.isNullOrBlank()) {
            navController.popBackStack()
            return@LaunchedEffect
        }
        val existing = vm.findByBarcode(code)
        if (existing != null) {
            navController.navigate("detail/${existing.id}") {
                popUpTo("scan") { inclusive = true }
            }
        } else {
            vm.setPrefill(ProductDraft(barcode = code))
            navController.navigate("add") {
                popUpTo("scan") { inclusive = true }
            }
        }
    }
}

