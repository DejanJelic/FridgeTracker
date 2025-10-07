package com.example.fridgetracker.view.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.view_model.ProductViewModel
import java.time.LocalDate
import androidx.core.net.toUri

@Composable
fun EditProductScreen(
    navController: NavController,
    vm: ProductViewModel,
    productId: Long? = null
) {
    val context = LocalContext.current
    val isEditMode = productId != null

    val existingProduct by vm.getProductFlow(productId ?: 0L).collectAsState(initial = null)

    val prefill by vm.prefill.collectAsState()

    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var quantity by remember { mutableDoubleStateOf(1.0) }
    var unit by remember { mutableStateOf("pcs") }
    var daysUntilExpiryStr by remember { mutableStateOf("30") }
    var purchaseDate by remember { mutableStateOf(LocalDate.now()) }
    var hasBestBefore by remember { mutableStateOf(true) }
    var isOpened by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("No category") }
    var location by remember { mutableStateOf("Not stored") }
    var comment by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var notifyExpiry by remember { mutableStateOf(true) }
    var expiryDaysBefore by remember { mutableStateOf("4") }
    var notifyAfterOpening by remember { mutableStateOf(true) }
    var afterOpeningDays by remember { mutableStateOf("2") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrlFromApi by remember { mutableStateOf<String?>(null) }

    val unitOptions = listOf("pcs", "kg", "g", "L")
    var unitExpanded by remember { mutableStateOf(false) }

    var showConsumeDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }

    LaunchedEffect(existingProduct) {
        existingProduct?.let { product ->
            name = product.name
            barcode = product.barcode ?: ""
            quantity = product.quantity
            unit = product.unit
            purchaseDate = LocalDate.ofEpochDay(product.addedAtEpochDay)
            val bestBefore = LocalDate.ofEpochDay(product.bestBeforeEpochDay)
            daysUntilExpiryStr = (bestBefore.toEpochDay() - purchaseDate.toEpochDay()).toString()
            isOpened = product.openedAtEpochDay != null
            category = product.category ?: "No category"
            location = product.location ?: "Not stored"
            if (!product.photoUri.isNullOrBlank()) {
                imageUri = product.photoUri.toUri()
            }
        }
    }

    // Inicijalizacija za Add mode sa prefill
    LaunchedEffect(prefill) {
        if (!isEditMode) {
            prefill?.let {
                name = it.name
                barcode = it.barcode ?: ""
                quantity = it.quantity
                unit = it.unit
                daysUntilExpiryStr = it.daysUntilExpiry.toString()
                category = it.category ?: "No category"
                location = it.location ?: "Not stored"
                imageUrlFromApi = it.imageUrl
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit product" else "Add product") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isEditMode) vm.setPrefill(null)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Save action
                        val days = daysUntilExpiryStr.toLongOrNull() ?: 30L
                        val bestBeforeEpochDay = if (hasBestBefore) {
                            purchaseDate.plusDays(days).toEpochDay()
                        } else {
                            purchaseDate.plusYears(100).toEpochDay()
                        }

                        val product = Product(
                            id = if (isEditMode) existingProduct?.id ?: 0L else 0L,
                            name = name.ifBlank { "Unnamed" },
                            quantity = quantity,
                            unit = unit,
                            addedAtEpochDay = purchaseDate.toEpochDay(),
                            bestBeforeEpochDay = bestBeforeEpochDay,
                            openedAtEpochDay = if (isOpened) LocalDate.now().toEpochDay() else null,
                            location = location,
                            photoUri = imageUri?.toString(),
                            barcode = barcode.ifBlank { null },
                            category = category
                        )

                        vm.upsert(product)
                        if (!isEditMode) vm.setPrefill(null)
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        },
        bottomBar = {
            if (isEditMode && existingProduct != null) {
                BottomAppBar(
                    backgroundColor = Color(0xFF6A1B9A),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { showConsumeDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent
                            ),
                            elevation = ButtonDefaults.elevation(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CONSUME", color = Color.White)
                        }

                        Divider(
                            modifier = Modifier
                                .width(1.dp)
                                .height(48.dp),
                            color = Color.White.copy(alpha = 0.3f)
                        )

                        Button(
                            onClick = { showTrashDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent
                            ),
                            elevation = ButtonDefaults.elevation(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TRASH", color = Color.White)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isEditMode) {
                val imagePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    imageUri = uri
                    Toast.makeText(context, "Image selected", Toast.LENGTH_SHORT).show()
                }

                FloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    backgroundColor = Color(0xFFFFA726)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Attach photo")
                }
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Product", style = MaterialTheme.typography.h6)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Barcode scanner button
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                                    ) {
                                        IconButton(
                                            onClick = { navController.navigate("scan") }
                                        ) {
                                            Icon(
                                                Icons.Filled.QrCodeScanner,
                                                contentDescription = "Scan barcode",
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    // Image placeholder/picker
                                    var imageUri by remember { mutableStateOf<Uri?>(null) }
                                    val imagePickerLauncher = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.GetContent()
                                    ) { uri: Uri? ->
                                        imageUri = uri
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .size(120.dp, 100.dp)
                                            .clickable { imagePickerLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFDD835)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            when {
                                                imageUri != null -> {
                                                    AsyncImage(
                                                        model = imageUri,
                                                        contentDescription = "Product image",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }

                                                !imageUrlFromApi.isNullOrBlank() -> {
                                                    AsyncImage(
                                                        model = imageUrlFromApi,
                                                        contentDescription = "Product image",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }

                                                else -> {
                                                    Icon(
                                                        Icons.Default.Image,
                                                        contentDescription = "Add image",
                                                        modifier = Modifier.size(48.dp),
                                                        tint = Color.White.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quantity section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Quantity",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.body1
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 0.0) quantity -= 1.0 },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("-", style = MaterialTheme.typography.h5)
                                }

                                Surface(
                                    modifier = Modifier.width(60.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                                ) {
                                    Text(
                                        text = quantity.toInt().toString(),
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.body1
                                    )
                                }

                                IconButton(
                                    onClick = { quantity += 1.0 },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("+", style = MaterialTheme.typography.h5)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Unit dropdown
                                Box {
                                    OutlinedButton(
                                        onClick = { unitExpanded = true },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (unit == "pcs") "No unit" else unit)
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = unitExpanded,
                                        onDismissRequest = { unitExpanded = false }
                                    ) {
                                        unitOptions.forEach { u ->
                                            DropdownMenuItem(onClick = {
                                                unit = u
                                                unitExpanded = false
                                            }) {
                                                Text(if (u == "pcs") "No unit" else u)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isOpened,
                                onCheckedChange = { isOpened = it }
                            )
                            Text("Open individually")
                            IconButton(onClick = {
                                Toast.makeText(
                                    context,
                                    "Track each item separately",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Date", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Purchase date picker
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Purchase date", modifier = Modifier.weight(1f))
                            Text(purchaseDate.toString())
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                showDatePicker(context = context, initial = purchaseDate) { d ->
                                    purchaseDate = d
                                }
                            }) { Text("Pick") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Best before")
                                OutlinedTextField(
                                    value = daysUntilExpiryStr,
                                    onValueChange = { daysUntilExpiryStr = it.filter { c -> c.isDigit() } },
                                    label = { Text("Days") },
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                            val daysVal = daysUntilExpiryStr.toLongOrNull() ?: 30L
                            Text("=> " + purchaseDate.plusDays(daysVal).toString(), modifier = Modifier.padding(start = 8.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Checkbox(checked = !hasBestBefore, onCheckedChange = { hasBestBefore = !it })
                            Text("No best before")
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Classification", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(6.dp))

                        var categoryExpanded by remember { mutableStateOf(false) }
                        var locationExpanded by remember { mutableStateOf(false) }

                        val categoryOptions = listOf("No category", "Dairy", "Meat", "Vegetables", "Fruits")
                        val locationOptions = listOf("Not stored", "Fridge", "Freezer", "Pantry")

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Category", style = MaterialTheme.typography.caption)
                                Box {
                                    OutlinedButton(
                                        onClick = { categoryExpanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(category)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = categoryExpanded,
                                        onDismissRequest = { categoryExpanded = false }
                                    ) {
                                        categoryOptions.forEach { opt ->
                                            DropdownMenuItem(onClick = {
                                                category = opt
                                                categoryExpanded = false
                                            }) {
                                                Text(opt)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Location", style = MaterialTheme.typography.caption)
                                Box {
                                    OutlinedButton(
                                        onClick = { locationExpanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(location)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = locationExpanded,
                                        onDismissRequest = { locationExpanded = false }
                                    ) {
                                        locationOptions.forEach { opt ->
                                            DropdownMenuItem(onClick = {
                                                location = opt
                                                locationExpanded = false
                                            }) {
                                                Text(opt)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Misc.", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isOpened, onCheckedChange = { isOpened = it })
                            Text("Already opened")
                            IconButton(onClick = {
                                Toast.makeText(context, "Info about already opened", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Comment")
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Price")
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("€") },
                            trailingIcon = { Text("€", modifier = Modifier.padding(end = 8.dp)) }
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Notifications", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = notifyExpiry,
                                onCheckedChange = { notifyExpiry = it }
                            )
                            Text("Expiry", modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = expiryDaysBefore,
                                onValueChange = { expiryDaysBefore = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.width(80.dp),
                                singleLine = true
                            )
                            Text("days before", modifier = Modifier.padding(start = 8.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = notifyAfterOpening,
                                onCheckedChange = { notifyAfterOpening = it }
                            )
                            Text("After opening", modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = afterOpeningDays,
                                onValueChange = { afterOpeningDays = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.width(80.dp),
                                singleLine = true
                            )
                            Text("days", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    )

    // Consume Dialog
    if (showConsumeDialog && existingProduct != null) {
        ConsumeDialog(
            product = existingProduct!!,
            onDismiss = { showConsumeDialog = false },
            onConfirm = { consumedQuantity ->
                val remaining = existingProduct!!.quantity - consumedQuantity
                if (remaining <= 0) {
                    vm.delete(existingProduct!!)
                    Toast.makeText(context, "Product consumed completely", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    val updated = existingProduct!!.copy(quantity = remaining)
                    vm.upsert(updated)
                    Toast.makeText(context, "Consumed $consumedQuantity", Toast.LENGTH_SHORT).show()
                    showConsumeDialog = false
                }
            }
        )
    }

    // Trash Dialog
    if (showTrashDialog && existingProduct != null) {
        TrashDialog(
            product = existingProduct!!,
            onDismiss = { showTrashDialog = false },
            onConfirm = {
                vm.delete(existingProduct!!)
                Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun ConsumeDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var consumeQuantity by remember { mutableDoubleStateOf(1.0) }
    val maxQuantity = product.quantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consume ${product.name}") },
        text = {
            Column {
                Text("How much do you want to consume?")
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (consumeQuantity > 1.0) consumeQuantity -= 1.0 }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = "${consumeQuantity.toInt()} / ${maxQuantity.toInt()} ${product.unit}",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    IconButton(
                        onClick = { if (consumeQuantity < maxQuantity) consumeQuantity += 1.0 }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = consumeQuantity.toFloat(),
                    onValueChange = { consumeQuantity = it.toDouble() },
                    valueRange = 1f..maxQuantity.toFloat(),
                    steps = (maxQuantity - 2).toInt().coerceAtLeast(0)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(consumeQuantity) }) {
                Text("CONSUME")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun TrashDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${product.name}?") },
        text = { Text("Are you sure you want to permanently delete this product?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            ) {
                Text("DELETE", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
private fun showDatePicker(context: android.content.Context, initial: LocalDate = LocalDate.now(), onDateSelected: (LocalDate) -> Unit) {
    val year = initial.year
    val month = initial.monthValue - 1
    val day = initial.dayOfMonth
    val dpd = DatePickerDialog(context, { _, y, m, d ->
        onDateSelected(LocalDate.of(y, m + 1, d))
    }, year, month, day)
    dpd.show()
}
