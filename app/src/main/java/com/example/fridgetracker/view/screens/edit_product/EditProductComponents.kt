package com.example.fridgetracker.view.screens.edit_product

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.time.LocalDate

// Top Bar
@Composable
fun EditProductTopBar(
    isEditMode: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    TopAppBar(
        title = { Text(if (isEditMode) "Edit product" else "Add product") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
        },
        backgroundColor = Color(0xFF6A1B9A),
        contentColor = Color.White,
        actions = {
            IconButton(onClick = onSaveClick) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
            }
        }
    )
}

// Bottom Bar
@Composable
fun EditProductBottomBar(
    onConsumeClick: () -> Unit,
    onTrashClick: () -> Unit
) {
    BottomAppBar(backgroundColor = Color(0xFF6A1B9A), contentColor = Color.White) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onConsumeClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                elevation = ButtonDefaults.elevation(0.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Restaurant, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CONSUME", color = Color.White)
            }

            Divider(
                modifier = Modifier.width(1.dp).height(48.dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            Button(
                onClick = onTrashClick,
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

// FAB Menu
@Composable
fun EditProductFAB(
    showMenu: Boolean,
    onMenuToggle: () -> Unit,
    onTakePhoto: () -> Unit,
    onScanBarcode: () -> Unit
) {
    Box {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showMenu) {
                SmallFloatingActionButton(
                    onClick = {
                        onTakePhoto()
                        onMenuToggle()
                    }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take photo")
                }
                SmallFloatingActionButton(
                    onClick = {
                        onScanBarcode()
                        onMenuToggle()
                    }
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                }
            }
            FloatingActionButton(
                onClick = onMenuToggle,
                backgroundColor = Color(0xFFFFC107)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add / Scan / Photo")
            }
        }
    }
}

// Loading Screen
@Composable
fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.85f)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message)
        }
    }
}

// Custom Snackbar Host
@Composable
fun CustomSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(
        hostState = hostState,
        snackbar = { data ->
            val (type, actualMessage) = when {
                data.message.startsWith("✓") -> SnackbarType.SUCCESS to data.message.substring(2)
                data.message.startsWith("✗") -> SnackbarType.ERROR to data.message.substring(2)
                data.message.startsWith("⚠") -> SnackbarType.WARNING to data.message.substring(2)
                else -> SnackbarType.INFO to data.message
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = when (type) {
                    SnackbarType.SUCCESS -> Color(0xFF4CAF50)
                    SnackbarType.ERROR -> Color(0xFFEF5350)
                    SnackbarType.INFO -> Color(0xFF2196F3)
                    SnackbarType.WARNING -> Color(0xFFFFA726)
                },
                elevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (type) {
                            SnackbarType.SUCCESS -> Icons.Default.CheckCircle
                            SnackbarType.ERROR -> Icons.Default.Error
                            SnackbarType.INFO -> Icons.Default.Info
                            SnackbarType.WARNING -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = actualMessage,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    )
}

enum class SnackbarType {
    SUCCESS, ERROR, INFO, WARNING
}

// Quantity Row Component
@Composable
fun QuantityRow(state: EditProductState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Quantity",
            modifier = Modifier.width(70.dp),
            style = MaterialTheme.typography.body1,
            maxLines = 1
        )

        IconButton(
            onClick = { if (state.quantity > 1.0) state.quantity -= 1.0 },
            modifier = Modifier.size(40.dp)
        ) {
            Text("-", style = MaterialTheme.typography.h5)
        }

        if (state.editingQuantity) {
            OutlinedTextField(
                value = state.manualQuantityText,
                onValueChange = {
                    state.manualQuantityText = it.filter { c -> c.isDigit() }
                },
                singleLine = true,
                modifier = Modifier.width(90.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = {
                        val v = state.manualQuantityText.toDoubleOrNull() ?: state.quantity
                        state.quantity = if (v <= 0.0) 1.0 else v
                        state.editingQuantity = false
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Done")
                    }
                }
            )
        } else {
            Surface(
                modifier = Modifier.width(80.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
            ) {
                Text(
                    text = state.quantity.toInt().toString(),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable {
                            state.manualQuantityText = state.quantity.toInt().toString()
                            state.editingQuantity = true
                        },
                    textAlign = TextAlign.Center
                )
            }
        }

        IconButton(
            onClick = { state.quantity += 1.0 },
            modifier = Modifier.size(40.dp)
        ) {
            Text("+", style = MaterialTheme.typography.h5)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Unit dropdown
        UnitDropdown(state = state)
    }
}

@Composable
fun UnitDropdown(state: EditProductState) {
    val unitOptions = listOf("pcs", "piece", "kg", "g", "L")

    Box {
        OutlinedButton(
            onClick = { state.unitExpanded = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (state.unit == "pcs") "No unit" else state.unit)
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        DropdownMenu(
            expanded = state.unitExpanded,
            onDismissRequest = { state.unitExpanded = false }
        ) {
            unitOptions.forEach { u ->
                DropdownMenuItem(onClick = {
                    state.unit = u
                    state.unitExpanded = false
                }) {
                    Text(if (u == "pcs") "No unit" else u)
                }
            }
        }
    }
}

// Image Picker Component
@Composable
fun ProductImagePicker(state: EditProductState) {
    Surface(
        modifier = Modifier
            .size(120.dp, 100.dp)
            .clickable { state.imagePickerLauncher.launch("image/*") },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFDD835)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.imageUri != null -> {
                    AsyncImage(
                        model = state.imageUri,
                        contentDescription = "Product image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                !state.imageUrlFromApi.isNullOrBlank() -> {
                    AsyncImage(
                        model = state.imageUrlFromApi,
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

// Date Picker Helper
fun showDatePicker(
    context: Context,
    initial: LocalDate = LocalDate.now(),
    onDateSelected: (LocalDate) -> Unit
) {
    val year = initial.year
    val month = initial.monthValue - 1
    val day = initial.dayOfMonth

    val dpd = DatePickerDialog(
        context,
        { _, y, m, d -> onDateSelected(LocalDate.of(y, m + 1, d)) },
        year,
        month,
        day
    )
    dpd.show()
}