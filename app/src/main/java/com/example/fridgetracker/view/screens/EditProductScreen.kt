package com.example.fridgetracker.view.screens

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.view_model.ProductViewModel
import java.time.LocalDate
import androidx.core.net.toUri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@Composable
fun EditProductScreen(
    navController: NavController,
    vm: ProductViewModel,
    productId: Long? = null
) {
    val context = LocalContext.current
    val isEditMode = productId != null

    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    // REACTIVE product observation (this fixes "consume not persisting" and live updates)
    val existingProductState = if (productId != null) {
        vm.getProductFlow(productId).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<Product?>(null) }
    }
    val existingProduct by existingProductState

    // Loading / saving / deleting states for PageLoader
    var isLoading by remember { mutableStateOf(productId != null && existingProduct == null) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Prefill (Add mode)
    val prefill by vm.prefill.collectAsState()

    // States
    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1.0) }               // Double
    var unit by remember { mutableStateOf("pcs") }
    // put "pcs" (No unit) first
    val unitOptions = listOf("pcs","piece", "kg", "g", "L")
    var unitExpanded by remember { mutableStateOf(false) }

    var daysUntilExpiryStr by remember { mutableStateOf("30") }
    var purchaseDate by remember { mutableStateOf(LocalDate.now()) }
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

    var showConsumeDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }

    // Quantity UI helpers (inline numeric field similar to expiry)
    var editingQuantity by remember { mutableStateOf(false) }
    var manualQuantityText by remember { mutableStateOf(quantity.toInt().toString()) }

    // FAB menu (scan vs camera)
    var showFabMenu by remember { mutableStateOf(false) }
    val fabSpacing by animateDpAsState(targetValue = if (showFabMenu) 72.dp else 0.dp)

    // --- Image pickers ---
    // 1) GetContent (gallery) -> copy to internal file (copyUriToInternalFile)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val saved = copyUriToInternalFile(context, uri)
                withContext(Dispatchers.Main) {
                    imageUri = saved?.toUri() ?: uri
                }
            }
        }
    }

    // 2) Camera preview -> returns Bitmap, save to internal file
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch {
                val saved = saveBitmapToInternalFile(context, bitmap)
                withContext(Dispatchers.Main) {
                    imageUri = saved?.toUri()
                }
            }
        }
    }

    // --- Fill fields from DB or prefill ---
    LaunchedEffect(existingProduct) {
        isLoading = productId != null && existingProduct == null
        existingProduct?.let { product ->
            name = product.name
            barcode = product.barcode ?: ""
            quantity = product.quantity
            unit = product.unit.ifBlank { "pcs" }
            purchaseDate = LocalDate.ofEpochDay(product.addedAtEpochDay)
            val bestBefore = LocalDate.ofEpochDay(product.bestBeforeEpochDay)
            daysUntilExpiryStr = (bestBefore.toEpochDay() - purchaseDate.toEpochDay()).toString()
            isOpened = product.openedAtEpochDay != null
            category = product.category ?: "No category"
            location = product.location ?: "Not stored"
            expiryDaysBefore = product.expiryDaysBefore.toString()
            notifyAfterOpening = product.notifyAfterOpening
            afterOpeningDays = product.afterOpeningDays.toString()
            imageUri = product.photoUri?.let { Uri.parse(it) }
            comment = product.comment ?: ""
            price = product.price ?: ""
            notifyExpiry = product.notifyExpiry
            isLoading = false
        }
        if (productId != null && existingProduct == null) {
            // if still null after attempt, keep loading flag short time
            isLoading = false
        }
    }

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

    // --- Scaffold + TopBar + TabRow etc. (kept similar) ---
    Scaffold(
        scaffoldState = scaffoldState,
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
                backgroundColor = Color(0xFF6A1B9A),
                contentColor = Color.White,
                actions = {
                    IconButton(onClick = {
                        // VALIDATION + SAVE with loader (await using suspend vm method)
                        val daysNum = daysUntilExpiryStr.toLongOrNull()
                        val expiryDaysNum = expiryDaysBefore.toIntOrNull()
                        val afterOpeningNum = afterOpeningDays.toIntOrNull()
                        val priceNum = price.ifBlank { null }?.toDoubleOrNull()

                        when {
                            name.isBlank() -> {
                                coroutineScope.launch { scaffoldState.snackbarHostState.showSnackbar("Please enter product name") }
                                return@IconButton
                            }
                            quantity <= 0.0 -> {
                                coroutineScope.launch { scaffoldState.snackbarHostState.showSnackbar("Quantity must be greater than 0") }
                                return@IconButton
                            }
                            daysNum == null -> {
                                coroutineScope.launch { scaffoldState.snackbarHostState.showSnackbar("Invalid 'days until expiry' value") }
                                return@IconButton
                            }
                            expiryDaysNum == null -> {
                                coroutineScope.launch { scaffoldState.snackbarHostState.showSnackbar("Invalid expiry reminder days") }
                                return@IconButton
                            }
                            afterOpeningNum == null -> {
                                coroutineScope.launch { scaffoldState.snackbarHostState.showSnackbar("Invalid 'after opening' days") }
                                return@IconButton
                            }
                            price.isNotBlank() && priceNum == null -> {
                                coroutineScope.launch { scaffoldState.snackbarHostState.showSnackbar("Invalid price") }
                                return@IconButton
                            }
                        }

                        val days = daysNum ?: 30L
                        val bestBeforeEpochDay = purchaseDate.plusDays(days).toEpochDay()
                        val photoUriString = when {
                            imageUri != null -> imageUri.toString()
                            !imageUrlFromApi.isNullOrBlank() -> imageUrlFromApi
                            else -> null
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
                            photoUri = photoUriString,
                            barcode = barcode.ifBlank { null },
                            category = category,
                            comment = comment.ifBlank { null },
                            price = price.ifBlank { null },
                            notifyExpiry = notifyExpiry,
                            expiryDaysBefore = expiryDaysNum ?: 4,
                            notifyAfterOpening = notifyAfterOpening,
                            afterOpeningDays = afterOpeningNum ?: 2
                        )

                        coroutineScope.launch {
                            try {
                                isSaving = true
                                vm.upsert(product) // suspend -> repo.upsert completes before we continue
                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                if (!isEditMode) vm.setPrefill(null)
                                navController.popBackStack()
                            } catch (t: Throwable) {
                                Log.w("EditProduct", "save failed", t)
                                scaffoldState.snackbarHostState.showSnackbar("Save failed: ${t.message ?: "error"}")
                            } finally {
                                isSaving = false
                            }
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            if (isEditMode && existingProduct != null) {
                BottomAppBar(backgroundColor = Color(0xFF6A1B9A), contentColor = Color.White) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(
                            onClick = { showConsumeDialog = true },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                            elevation = ButtonDefaults.elevation(0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CONSUME", color = Color.White)
                        }

                        Divider(modifier = Modifier.width(1.dp).height(48.dp), color = Color.White.copy(alpha = 0.3f))

                        Button(
                            onClick = { showTrashDialog = true },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
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
            // FAB menu: main FAB toggles menu; show two mini FABs for camera and scan when expanded
            Box {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showFabMenu) {
                        SmallFloatingActionButton(onClick = { takePictureLauncher.launch(null); showFabMenu = false }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Take photo")
                        }
                        SmallFloatingActionButton(onClick = { navController.navigate("scan"); showFabMenu = false }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                        }
                    }
                    FloatingActionButton(onClick = { showFabMenu = !showFabMenu }, backgroundColor = Color(0xFFFFA726)) {
                        Icon(Icons.Default.Add, contentDescription = "Add / Scan / Photo")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                // PRODUCT CARD
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), elevation = 2.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
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

                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // image picker surface: clicking opens gallery; camera is from FAB
                                    Surface(modifier = Modifier.size(120.dp, 100.dp).clickable { imagePickerLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(8.dp), color = Color(0xFFFDD835)) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            when {
                                                imageUri != null -> {
                                                    AsyncImage(model = imageUri, contentDescription = "Product image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                }
                                                !imageUrlFromApi.isNullOrBlank() -> {
                                                    AsyncImage(model = imageUrlFromApi, contentDescription = "Product image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                }
                                                else -> {
                                                    Icon(Icons.Default.Image, contentDescription = "Add image", modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.7f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quantity: label fixed width so not clipped; inline numeric edit field styled as expiry
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Quantity", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.body1, maxLines = 1)
                            IconButton(onClick = { if (quantity > 1.0) quantity -= 1.0 }, modifier = Modifier.size(40.dp)) { Text("-", style = MaterialTheme.typography.h5) }

                            if (editingQuantity) {
                                OutlinedTextField(
                                    value = manualQuantityText,
                                    onValueChange = { manualQuantityText = it.filter { c -> c.isDigit() } },
                                    singleLine = true,
                                    modifier = Modifier.width(90.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val v = manualQuantityText.toDoubleOrNull() ?: quantity
                                            quantity = if (v <= 0.0) 1.0 else v
                                            editingQuantity = false
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = "Done")
                                        }
                                    }
                                )
//                                OutlinedTextField(
//                                    value = manualQuantityText,
//                                    onValueChange = { manualQuantityText = it.filter { c -> c.isDigit() } },
//                                    modifier = Modifier.width(100.dp),
//                                    singleLine = true,
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                                )
                            } else {
                                Surface(modifier = Modifier.width(80.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))) {
                                    Text(text = quantity.toInt().toString(), modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .clickable {
                                            manualQuantityText = quantity.toInt().toString()
                                            editingQuantity = true
                                        }, textAlign = TextAlign.Center)
                                }
                            }

                            IconButton(onClick = { quantity += 1.0 }, modifier = Modifier.size(40.dp)) { Text("+", style = MaterialTheme.typography.h5) }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Unit dropdown: "No unit" (pcs) first
                            Box {
                                OutlinedButton(onClick = { unitExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                                    Text(if (unit == "pcs") "No unit" else unit)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                                }
                                DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isOpened, onCheckedChange = { isOpened = it })
                            Text("Open individually")
                            IconButton(onClick = { Toast.makeText(context, "Track each item separately", Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(20.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                // DATE card (same)
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), elevation = 2.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Date", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Purchase date", modifier = Modifier.weight(1f))
                            Text(purchaseDate.toString())
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { showDatePicker(context = context, initial = purchaseDate) { d -> purchaseDate = d } }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pick purchase date")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Best before")
                                OutlinedTextField(value = daysUntilExpiryStr, onValueChange = { daysUntilExpiryStr = it.filter { c -> c.isDigit() } },
                                    label = { Text("Days") }, modifier = Modifier.width(140.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                            }
                            val daysVal = daysUntilExpiryStr.toLongOrNull() ?: 30L
                            Text( purchaseDate.plusDays(daysVal).toString(), modifier = Modifier.padding(start = 12.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { showDatePicker(context = context, initial = purchaseDate) { d -> purchaseDate = d } }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pick best before")
                            }
                        }
                    }
                }

                // Classification (kept same)
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), elevation = 2.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Classification", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(6.dp))

                        var categoryExpanded by remember { mutableStateOf(false) }
                        var locationExpanded by remember { mutableStateOf(false) }

                        val categoryOptions = listOf("No category", "Dairy", "Meat", "Vegetables", "Fruits")
                        val locationOptions = listOf("Not stored", "Fridge", "Freezer", "Pantry")

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Category", style = MaterialTheme.typography.caption)
                                Box {
                                    OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text(category)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                        categoryOptions.forEach { opt ->
                                            DropdownMenuItem(onClick = { category = opt; categoryExpanded = false }) { Text(opt) }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Location", style = MaterialTheme.typography.caption)
                                Box {
                                    OutlinedButton(onClick = { locationExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text(location)
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(expanded = locationExpanded, onDismissRequest = { locationExpanded = false }) {
                                        locationOptions.forEach { opt ->
                                            DropdownMenuItem(onClick = { location = opt; locationExpanded = false }) { Text(opt) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // MISC
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), elevation = 2.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Misc.", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isOpened, onCheckedChange = { isOpened = it })
                            Text("Already opened")
                            IconButton(onClick = { Toast.makeText(context, "Info about already opened", Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(20.dp), tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Comment")
                        OutlinedTextField(value = comment, onValueChange = { comment = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("") }, singleLine = false, maxLines = 3)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Price")
                        OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier.fillMaxWidth(), placeholder = { Text("") }, trailingIcon = { Text("€", modifier = Modifier.padding(end = 8.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    }
                }

                // NOTIFICATIONS - aligned left/right
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(12.dp), elevation = 2.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Notifications", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(6.dp))

                        // expiry row: left label, right field
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = notifyExpiry, onCheckedChange = { notifyExpiry = it })
                                Text("Expiry")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                OutlinedTextField(value = expiryDaysBefore, onValueChange = { expiryDaysBefore = it.filter { c -> c.isDigit() } },
                                    modifier = Modifier.width(100.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("days before", modifier = Modifier.align(Alignment.CenterVertically))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // after opening row: left label, right field
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = notifyAfterOpening, onCheckedChange = { notifyAfterOpening = it })
                                Text("After opening")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                OutlinedTextField(value = afterOpeningDays, onValueChange = { afterOpeningDays = it.filter { c -> c.isDigit() } },
                                    modifier = Modifier.width(100.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("days", modifier = Modifier.align(Alignment.CenterVertically).padding(end = 42.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // PageLoader overlay: show progress while loading/saving/deleting
            if (isLoading || isSaving || isDeleting) {
                Box(modifier = Modifier.fillMaxSize().alpha(0.85f).background(Color.White), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = when {
                            isLoading -> "Loading..."
                            isSaving -> "Saving..."
                            isDeleting -> "Deleting..."
                            else -> "Working..."
                        })
                    }
                }
            }
        }
    }

//     Consume Dialog: now uses vm.upsertSuspend to persist change and UI updates via collectAsState
    if (showConsumeDialog && existingProduct != null) {
        ConsumeDialog(product = existingProduct!!, onDismiss = { showConsumeDialog = false }, onConfirm = { consumedQuantity: Double ->
            coroutineScope.launch {
                try {
                    isSaving = true
                    val remaining = existingProduct!!.quantity - consumedQuantity
                    if (remaining <= 0.0) {
                        // delete product
                        vm.delete(existingProduct!!)
                        Toast.makeText(context, "Product consumed completely", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        val updated = existingProduct!!.copy(quantity = remaining)
                        vm.upsert(updated)
                        Toast.makeText(context, "Consumed ${consumedQuantity.toInt()}", Toast.LENGTH_SHORT).show()
                        // UI will auto-update because existingProduct is a collected flow
                    }
                } catch (t: Throwable) {
                    scaffoldState.snackbarHostState.showSnackbar("Error: ${t.message ?: "unknown"}")
                } finally {
                    isSaving = false
                    showConsumeDialog = false
                }
            }
        })
    }
    // Trash Dialog
    if (showTrashDialog && existingProduct != null) {
        TrashDialog(product = existingProduct!!, onDismiss = { showTrashDialog = false }, onConfirm = {
            coroutineScope.launch {
                try {
                    isDeleting = true
                    vm.delete(existingProduct!!)
                    Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } catch (t: Throwable) {
                    scaffoldState.snackbarHostState.showSnackbar("Delete failed: ${t.message ?: "error"}")
                } finally {
                    isDeleting = false
                }
            }
        })
    }

}

// --- Helpers ---
@Composable
fun ConsumeDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var consumeQuantity by remember { mutableDoubleStateOf(1.0) } // Double
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
                    IconButton(onClick = { if (consumeQuantity > 1.0) consumeQuantity -= 1.0 }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = "${consumeQuantity.toInt()} / ${maxQuantity.toInt()} ${product.unit}",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    IconButton(onClick = { if (consumeQuantity < maxQuantity) consumeQuantity += 1.0 }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Slider: eksplicitno napišemo tip parametra u lambda
                Slider(
                    value = consumeQuantity.toFloat(),
                    onValueChange = { v: Float -> consumeQuantity = v.toDouble() },
                    valueRange = 1f..maxQuantity.toFloat(),
                    steps = (maxQuantity.toInt() - 2).coerceAtLeast(0)
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
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

private fun showDatePicker(context: android.content.Context, initial: LocalDate = LocalDate.now(), onDateSelected: (LocalDate) -> Unit) {
    val year = initial.year
    val month = initial.monthValue - 1
    val day = initial.dayOfMonth
    val dpd = DatePickerDialog(context, { _, y, m, d -> onDateSelected(LocalDate.of(y, m + 1, d)) }, year, month, day)
    dpd.show()
}

suspend fun copyUriToInternalFile(context: android.content.Context, sourceUri: Uri): File? {
    return withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val outFile = File(context.filesDir, fileName)
            FileOutputStream(outFile).use { output -> input.use { inp -> inp.copyTo(output) } }
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

suspend fun saveBitmapToInternalFile(context: android.content.Context, bmp: Bitmap): File? {
    return withContext(Dispatchers.IO) {
        try {
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val outFile = File(context.filesDir, fileName)
            FileOutputStream(outFile).use { fos: OutputStream ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
