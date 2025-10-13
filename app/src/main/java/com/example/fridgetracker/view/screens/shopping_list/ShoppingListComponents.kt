package com.example.fridgetracker.view.screens.shopping_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fridgetracker.utilities.CustomSnackbar
import com.example.fridgetracker.utilities.SnackbarType

// Top Bar
@Composable
fun ShoppingListTopBar(
    state: ShoppingListState,
    onMenuClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    TopAppBar(
        backgroundColor = Color(0xFF6A1B9A),
        contentColor = Color.White,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, tint = Color.White, contentDescription = "Menu")
            }

            // List selector
            ListSelectorDropdown(state = state)

            IconButton(onClick = onSaveClick) {
                Icon(Icons.Default.Check, tint = Color.White, contentDescription = "Save list")
            }
        }
    }
}

// List Selector Dropdown
@Composable
fun RowScope.ListSelectorDropdown(state: ShoppingListState) {
    Box(modifier = Modifier.weight(1f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { state.dropdownExpanded = true }
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = state.selectedListName ?: "Shopping list",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Select list"
            )
        }

        DropdownMenu(
            expanded = state.dropdownExpanded,
            onDismissRequest = { state.dropdownExpanded = false }
        ) {
            if (state.savedLists.isEmpty()) {
                DropdownMenuItem(onClick = { state.dropdownExpanded = false }) {
                    Text("No saved lists", color = Color.Gray)
                }
            } else {
                state.savedLists.forEach { listWith ->
                    DropdownMenuItem(
                        onClick = {
                            state.loadList(listWith)
                            state.dropdownExpanded = false
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                listWith.list.name,
                                modifier = Modifier.weight(1f)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.selectedListName == listWith.list.name) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF6A1B9A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }

                                IconButton(
                                    onClick = {
                                        state.listToDeleteId = listWith.list.id
                                        state.listToDeleteName = listWith.list.name
                                        state.showDeleteDialog = true
                                        state.dropdownExpanded = false
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete list",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            DropdownMenuItem(
                onClick = {
                    state.dropdownExpanded = false
                    state.showCreateListDialog = true
                    state.createNameText = ""
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create new list")
            }
        }
    }
}

// Search Bar
@Composable
fun SearchBar(state: ShoppingListState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            BasicTextField(
                value = state.searchQuery,
                onValueChange = {
                    state.searchQuery = it
                    state.showSuggestions = it.isNotBlank()
                },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                decorationBox = { inner ->
                    if (state.searchQuery.isEmpty()) {
                        Text(
                            "What to buy?",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )

            if (state.searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        state.searchQuery = ""
                        state.showSuggestions = false
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

// Suggestions Card
@Composable
fun SuggestionsCard(state: ShoppingListState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(max = 250.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 6.dp
    ) {
        LazyColumn {
            items(state.filteredSuggestions) { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            state.addItem(suggestion)
                            state.searchQuery = ""
                            state.showSuggestions = false
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF6A1B9A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(suggestion, fontSize = 16.sp)
                }
                if (suggestion != state.filteredSuggestions.last()) {
                    Divider()
                }
            }
        }
    }
}

// Empty State
@Composable
fun EmptyShoppingListState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Your shopping list is empty",
            fontSize = 18.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Add items to get started",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

// Custom Snackbar Overlay
@Composable
fun CustomSnackbarOverlay(
    message: String,
    type: SnackbarType,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        CustomSnackbar(
            message = message,
            type = type,
            onDismiss = onDismiss
        )
    }
}