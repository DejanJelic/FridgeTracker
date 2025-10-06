package com.example.fridgetracker.view.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fridgetracker.view_model.ProductViewModel

@Composable
fun ProductDetailScreen(productId: Long, onBack: () -> Unit, vm: ProductViewModel = viewModel()) {
    val product by vm.getProductFlow(productId).collectAsState(initial = null)

    Column(modifier = Modifier.padding(16.dp)) {
        product?.let { p ->
            Text("Name: ${p.name}")
            Text("Best before: ${java.time.LocalDate.ofEpochDay(p.bestBeforeEpochDay)}")
            Button(onClick = { vm.delete(p); onBack() }) { Text("Delete") }
        } ?: Text("Not found")
    }
}