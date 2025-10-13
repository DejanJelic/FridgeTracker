package com.example.fridgetracker.view.screens.edit_product

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Product Card
@Composable
fun ProductCard(state: EditProductState) {
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
                        value = state.name,
                        onValueChange = { state.name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Spacer(modifier = Modifier.width(12.dp))
                    ProductImagePicker(state = state)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            QuantityRow(state = state)

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFFC107),
                        uncheckedColor = Color.Gray
                    ),
                    checked = state.alreadyOpened,
                    onCheckedChange = { checked ->
                        state.alreadyOpened = checked
                        if (checked) {
                            state.openIndividually = false
                        }
                    }
                )
                Text("Already opened")
                IconButton(onClick = { state.showAlreadyOpenedDialog = true }) {
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
}

// Date Card
@Composable
fun DateCard(state: EditProductState, context: Context) {
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Purchase date", modifier = Modifier.weight(1f))
                Text(state.purchaseDate.toString())
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    showDatePicker(
                        context = context,
                        initial = state.purchaseDate
                    ) { d -> state.purchaseDate = d }
                }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Pick purchase date")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Best before")
                    OutlinedTextField(
                        value = state.daysUntilExpiryStr,
                        onValueChange = {
                            state.daysUntilExpiryStr = it.filter { c -> c.isDigit() }
                        },
                        label = { Text("Days") },
                        modifier = Modifier.width(140.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                val daysVal = state.daysUntilExpiryStr.toLongOrNull() ?: 30L
                Text(
                    state.purchaseDate.plusDays(daysVal).toString(),
                    modifier = Modifier.padding(start = 12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    showDatePicker(
                        context = context,
                        initial = state.purchaseDate.plusDays(daysVal)
                    ) { selectedDate ->
                        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                            state.purchaseDate,
                            selectedDate
                        )
                        state.daysUntilExpiryStr = daysBetween.coerceAtLeast(0).toString()
                    }
                }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Pick best before")
                }
            }
        }
    }
}

// Classification Card
@Composable
fun ClassificationCard(state: EditProductState) {
    val categoryOptions = listOf(
        "No category",
        "Fruits",
        "Vegetables",
        "Legumes",
        "Meat",
        "Fish",
        "Seafood",
        "Bread and cereals",
        "Dairy products",
        "Desserts and sugary foods",
        "Prepared foods and snack foods",
        "Spices and condiments",
        "Drinks",
        "Alcohol",
        "Household and cleaning"
    )

    val locationOptions = listOf(
        "Not stored",
        "Fridge",
        "Freezer",
        "Pantry",
        "Larder"
    )

    var categoryExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }

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
                            Text(state.category)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categoryOptions.forEach { opt ->
                                DropdownMenuItem(onClick = {
                                    state.category = opt
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
                            Text(state.location)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = locationExpanded,
                            onDismissRequest = { locationExpanded = false }
                        ) {
                            locationOptions.forEach { opt ->
                                DropdownMenuItem(onClick = {
                                    state.location = opt
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
}

// Misc Card
@Composable
fun MiscCard(state: EditProductState) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.openIndividually,
                    onCheckedChange = { checked ->
                        state.openIndividually = checked
                        if (checked) {
                            state.alreadyOpened = false
                            state.notifyAfterOpening = false
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFFC107),
                        uncheckedColor = Color.Gray
                    )
                )
                Text("Open individually")
                IconButton(onClick = { state.showOpenIndividuallyDialog = true }) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Comment")
            OutlinedTextField(
                value = state.comment,
                onValueChange = { state.comment = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("") },
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Price")
            OutlinedTextField(
                value = state.price,
                onValueChange = { state.price = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("") },
                trailingIcon = { Text("€", modifier = Modifier.padding(end = 8.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }
    }
}

// Notifications Card
@Composable
fun NotificationsCard(state: EditProductState) {
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.notifyExpiry,
                        onCheckedChange = { state.notifyExpiry = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFFFC107),
                            uncheckedColor = Color.Gray
                        )
                    )
                    Text("Expiry")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedTextField(
                        value = state.expiryDaysBefore,
                        onValueChange = {
                            state.expiryDaysBefore = it.filter { c -> c.isDigit() }
                        },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "days before",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val enabled = !state.openIndividually
                    Checkbox(
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFFFC107),
                            uncheckedColor = Color.Gray
                        ),
                        checked = state.notifyAfterOpening,
                        onCheckedChange = { if (enabled) state.notifyAfterOpening = it },
                        enabled = enabled
                    )
                    Text("After opening")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedTextField(
                        value = state.afterOpeningDays,
                        onValueChange = {
                            state.afterOpeningDays = it.filter { c -> c.isDigit() }
                        },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !state.openIndividually
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "days",
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(end = 42.dp)
                    )
                }
            }

            if (state.openIndividually) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Note: 'Open individually' disables notifications after opening and the product will not appear in the Opened tab.",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}