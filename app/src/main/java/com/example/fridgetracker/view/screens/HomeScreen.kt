package com.example.fridgetracker.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.view_model.ProductViewModel

@Composable
fun HomeScreen(onAdd: () -> Unit, onOpen: (Long) -> Unit, vm: ProductViewModel = viewModel()) {
    val products by vm.products.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Products") }, backgroundColor = Color(0xFF6A1B9A), contentColor = Color.White)
        },
        floatingActionButton = {
        FloatingActionButton(onClick = onAdd, backgroundColor = Color(0xFFFFC107)) { Text("+") }
    }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            // Grouping by location/category - quick naive grouping by product.location
            val groups = products.groupBy { it.location ?: "Not stored" }
            groups.forEach { (groupName, list) ->
                item {
                    GroupHeader(title = groupName, count = list.size)
                }
                items(list) { p ->
                    ProductCard(p, onClick = { onOpen(p.id) })
                }
            }
        }
    }
}
@Composable
fun ProductRow(p: Product, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(p.name); Text("Qty: ${p.quantity} ${p.unit}") }
            val daysLeft = p.bestBeforeEpochDay - java.time.LocalDate.now().toEpochDay()
            Text(if (daysLeft < 0) "Expired" else "${daysLeft}d")
        }
    }
}
@Composable
fun GroupHeader(title: String, count: Int) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFFFFC107)),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f).padding(12.dp), fontWeight = FontWeight.Bold)
        Text(count.toString(), modifier = Modifier.padding(end = 12.dp))
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = 2.dp) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
                // placeholder for image or initials
                Text(product.name.take(3).uppercase(), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Medium)
                Text(product.category ?: "", style = MaterialTheme.typography.caption)
            }
            // days left
            val daysLeft = product.bestBeforeEpochDay - java.time.LocalDate.now().toEpochDay()
            Text(if (daysLeft < 0) "Expired" else "${daysLeft}d", modifier = Modifier.padding(start = 8.dp))
        }
    }
}