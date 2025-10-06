package com.example.fridgetracker.view.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.view_model.ProductViewModel
import java.time.LocalDate

@Composable
fun AddProductScreen(
    navController: NavController,
    vm: ProductViewModel
) {
    val context = LocalContext.current

    val prefill by vm.prefill.collectAsState()

    var name by remember { mutableStateOf(prefill?.name ?: "") }
    var barcode by remember { mutableStateOf(prefill?.barcode ?: "") }
    var quantity by remember { mutableDoubleStateOf(prefill?.quantity ?: 1.0) }
    var unit by remember { mutableStateOf(prefill?.unit ?: "pcs") }
    var daysUntilExpiryStr by remember { mutableStateOf(prefill?.daysUntilExpiry?.toString() ?: "30") }
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

    val unitOptions = listOf("pcs", "kg", "g", "L")
    var unitExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(prefill) {
        prefill?.let {
            name = it.name
            barcode = it.barcode ?: ""
            quantity = it.quantity
            unit = it.unit
            daysUntilExpiryStr = it.daysUntilExpiry.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add product") },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.setPrefill(null)
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
                            LocalDate.now().plusDays(days).toEpochDay()
                        } else {
                            // choose some sentinel or use added date as best-before
                            LocalDate.now().plusYears(100).toEpochDay()
                        }

                        val product = Product(
                            name = name.ifBlank { "Unnamed" },
                            quantity = quantity,
                            unit = unit,
                            addedAtEpochDay = LocalDate.now().toEpochDay(),
                            bestBeforeEpochDay = bestBeforeEpochDay,
                            openedAtEpochDay = if (isOpened) LocalDate.now().toEpochDay() else null,
                            location = location,
                            photoUri = null,
                            barcode = barcode.ifBlank { null }
                        )

                        vm.upsert(product)
                        vm.setPrefill(null)
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        },
        floatingActionButton = {
            // small FAB for camera/photo inside Add screen (the orange camera in screenshot)
            FloatingActionButton(onClick = {
                // placeholder: open camera or image picker (implementation depends on your image flow)
                Toast.makeText(context, "Camera / attach image (TODO)", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Attach photo")
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Product", style = MaterialTheme.typography.h6)
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(onClick = {
                                navController.navigate("scan")
                            }) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan barcode")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Quantity", modifier = Modifier.weight(1f))
                            IconButton(onClick = { if (quantity > 0.0) quantity -= 1.0 }) { Text("-") }
                            Text(quantity.toInt().toString(), modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { quantity += 1.0 }) { Text("+") }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box {
                                Text(unit, modifier = Modifier
                                    .clickable { unitExpanded = true }
                                    .padding(8.dp))
                                DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                    unitOptions.forEach { u ->
                                        DropdownMenuItem(onClick = {
                                            unit = u
                                            unitExpanded = false
                                        }) {
                                            Text(u)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isOpened, onCheckedChange = { isOpened = it })
                            Text("Open individually")
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Category")
                                Text(category)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Location")
                                Text(location)
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
