package com.example.fridgetracker.view.screens.edit_product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.view_model.ProductViewModel
import kotlinx.coroutines.launch

@Composable
fun EditProductDialogs(
    state: EditProductState,
    scaffoldState: ScaffoldState,
    navController: NavController,
    vm: ProductViewModel
) {
    val coroutineScope = rememberCoroutineScope()

    // Consume Dialog
    if (state.showConsumeDialog && state.existingProduct != null) {
        ConsumeDialog(
            product = state.existingProduct!!,
            onDismiss = { state.showConsumeDialog = false },
            onConfirm = { consumedQuantity ->
                state.consumeProduct(
                    consumedQuantity = consumedQuantity,
                    onSuccess = { message ->
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar(message)
                            if (state.existingProduct!!.quantity - consumedQuantity <= 0.0) {
                                navController.popBackStack()
                            }
                        }
                    },
                    onError = { message ->
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar(message)
                        }
                    }
                )
            }
        )
    }

    // Trash Dialog
    if (state.showTrashDialog && state.existingProduct != null) {
        TrashDialog(
            product = state.existingProduct!!,
            onDismiss = { state.showTrashDialog = false },
            onConfirm = {
                state.deleteProduct(
                    onSuccess = {
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar("✓ Product deleted")
                            navController.popBackStack()
                        }
                    },
                    onError = { message ->
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar(message)
                        }
                    }
                )
            }
        )
    }

    // Already Opened Info Dialog
    if (state.showAlreadyOpenedDialog) {
        InfoDialog(
            title = "Already opened",
            message = "Use this option to add a product that is already opened.\n\n" +
                    "For example, when you enter your current inventory in the app. For new products purchased this option is not useful.\n\n" +
                    "This option is available only when adding a product.",
            onDismiss = { state.showAlreadyOpenedDialog = false },
            onConfirm = {
                state.alreadyOpened = true
                state.openIndividually = false
                state.showAlreadyOpenedDialog = false
            }
        )
    }

    // Open Individually Info Dialog
    if (state.showOpenIndividuallyDialog) {
        InfoDialog(
            title = "Open individually",
            message = "Use this option for foods that you can eat one by one without affecting the consumption time.\n\n" +
                    "For example, apples, bananas, eggs.\n\n" +
                    "Don't apply for fresh products to be consumed quickly after opening, such as a tray of steaks, a bottle of milk, mayonnaise.\n\n" +
                    "With this option the opened product will not be displayed in the Opened tab. Notifications after opening the product will not be available.",
            onDismiss = { state.showOpenIndividuallyDialog = false },
            onConfirm = {
                state.openIndividually = true
                state.alreadyOpened = false
                state.notifyAfterOpening = false
                state.showOpenIndividuallyDialog = false
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
        title = { Text("Consume ${product.name}", fontWeight = FontWeight.Bold) },
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
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFA726),
                        activeTrackColor = Color.Green,
                        inactiveTrackColor = Color.Red,
                        activeTickColor = Color.Blue,
                    ),
                    value = consumeQuantity.toFloat(),
                    onValueChange = { v: Float -> consumeQuantity = v.toDouble() },
                    valueRange = 1f..maxQuantity.toFloat(),
                    steps = (maxQuantity.toInt() - 2).coerceAtLeast(0)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(consumeQuantity) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFA726))
            ) {
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

@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.h5)
                Spacer(Modifier.height(12.dp))
                Text(message, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) {
                        Text("OK", color = Color(0xFF6A1B9A))
                    }
                }
            }
        }
    }
}