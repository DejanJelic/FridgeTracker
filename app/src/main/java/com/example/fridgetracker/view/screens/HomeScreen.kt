package com.example.fridgetracker.view.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.view_model.ProductViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

//@Composable
//fun ProductCard(product: Product, onClick: () -> Unit) {
//    Card(modifier = Modifier
//        .padding(8.dp)
//        .fillMaxWidth()
//        .clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = 2.dp) {
//        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
//            Box(modifier = Modifier
//                .size(56.dp)
//                .clip(RoundedCornerShape(8.dp))
//                .background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
//                // placeholder for image or initials
//                Text(product.name.take(3).uppercase(), fontWeight = FontWeight.Bold)
//            }
//            Spacer(modifier = Modifier.width(12.dp))
//            Column(modifier = Modifier.weight(1f)) {
//                Text(product.name, fontWeight = FontWeight.Medium)
//                Text(product.category ?: "", style = MaterialTheme.typography.caption)
//            }
//            // days left
//            val daysLeft = product.bestBeforeEpochDay - java.time.LocalDate.now().toEpochDay()
//            Text(if (daysLeft < 0) "Expired" else "${daysLeft}d", modifier = Modifier.padding(start = 8.dp))
//        }
//    }
//}
// ProductCard.kt
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, Color(0xFF6A1B9A)),
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // IMAGE
            val imageModifier = Modifier
                .size(width = 64.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp))

            Surface(modifier = imageModifier, color = Color(0xFFEFEFEF)) {
                if (!product.photoUri.isNullOrBlank()) {
                    // AsyncImage with placeholder and error
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(product.photoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Product image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        fallback = null,
                        error = null,
                        placeholder = null
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "placeholder",
                            tint = Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Left middle column: name + category/location
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.h6.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!product.category.isNullOrBlank()) {
                        Text(
                            text = product.category!!,
                            color = Color(0xFFFB8C00),
                            maxLines = 1,
                            style = MaterialTheme.typography.body2
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!product.location.isNullOrBlank()) {
                        Text(
                            text = product.location!!,
                            color = Color(0xFF90A4AE),
                            maxLines = 1,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }

            // Right column: quantity/unit + days until expiry + opened
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatQuantity(product),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                val days = daysUntil(product.bestBeforeEpochDay)
                Text(
                    text = "${days}d",
                    style = MaterialTheme.typography.h6.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.Bold
                )

                product.openedAtEpochDay?.let { openedEpoch ->
                    val openedDays = daysSince(openedEpoch)
                    Text(
                        text = "Opened ${openedDays}d",
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// Helpers
@Composable
private fun formatQuantity(product: Product): String {
    // If you keep track of consumed vs total (not in default Product), adapt here.
    // For now show "<int quantity> <unit>" with 'piece' handling.
    val qtyInt = product.quantity.toInt()
    val unitLabel = when (product.unit) {
        "pcs", "piece" -> "piece"
        else -> product.unit
    }
    return "$qtyInt $unitLabel"
}

private fun daysUntil(epochDay: Long): Long {
    val today = LocalDate.now()
    val best = LocalDate.ofEpochDay(epochDay)
    // ChronoUnit.DAYS.between returns Long; coerceAtLeast expects Long, so use 0L literal
    return ChronoUnit.DAYS.between(today, best).coerceAtLeast(0L)
}

private fun daysSince(epochDay: Long): Long {
    val today = LocalDate.now()
    val opened = LocalDate.ofEpochDay(epochDay)
    return ChronoUnit.DAYS.between(opened, today).coerceAtLeast(0L)
}

