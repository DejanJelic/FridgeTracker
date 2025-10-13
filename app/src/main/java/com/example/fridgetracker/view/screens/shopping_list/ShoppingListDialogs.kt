package com.example.fridgetracker.view.screens.shopping_list

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
import androidx.compose.ui.unit.sp
import com.example.fridgetracker.view_model.ShoppingListViewModel

@Composable
fun ShoppingListDialogs(
    state: ShoppingListState,
    vm: ShoppingListViewModel
) {
    // Save Dialog
    if (state.showSaveDialog) {
        SaveListDialog(
            state = state,
            onSave = { name ->
                state.saveList(name)
                state.showSaveDialog = false
            },
            onDismiss = { state.showSaveDialog = false }
        )
    }

    // Create List Dialog
    if (state.showCreateListDialog) {
        CreateListDialog(
            state = state,
            onCreate = { name ->
                state.createList(name)
                state.showCreateListDialog = false
            },
            onDismiss = { state.showCreateListDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (state.showDeleteDialog && state.listToDeleteId != null && state.listToDeleteName != null) {
        DeleteListDialog(
            listName = state.listToDeleteName!!,
            onConfirm = {
                state.deleteList(state.listToDeleteId!!, state.listToDeleteName!!)
                state.showDeleteDialog = false
                state.listToDeleteId = null
                state.listToDeleteName = null
            },
            onDismiss = {
                state.showDeleteDialog = false
                state.listToDeleteId = null
                state.listToDeleteName = null
            }
        )
    }
}

@Composable
fun SaveListDialog(
    state: ShoppingListState,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = Color(0xFF6A1B9A),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Save Shopping List",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    "Enter a name for this list:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.saveNameText,
                    onValueChange = { state.saveNameText = it },
                    label = { Text("List name") },
                    placeholder = { Text("e.g., Weekly Shopping") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF6A1B9A),
                        cursorColor = Color(0xFF6A1B9A)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = state.saveNameText.ifBlank {
                        "List ${state.savedLists.size + 1}"
                    }
                    onSave(name)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF6A1B9A)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SAVE", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}

@Composable
fun CreateListDialog(
    state: ShoppingListState,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Create New List",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    "Enter a name for the new shopping list:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.createNameText,
                    onValueChange = { state.createNameText = it },
                    label = { Text("List name") },
                    placeholder = { Text("e.g., Weekly Shopping") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFFA726),
                        cursorColor = Color(0xFFFFA726)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = state.createNameText.ifBlank {
                        "New List ${state.savedLists.size + 1}"
                    }
                    onCreate(name)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFFFA726)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CREATE", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}

@Composable
fun DeleteListDialog(
    listName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Delete List?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Text(
                "Are you sure you want to delete '$listName'? This action cannot be undone.",
                fontSize = 16.sp,
                color = Color.DarkGray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFEF5350)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("DELETE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}