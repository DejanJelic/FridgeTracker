package com.example.fridgetracker.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fridgetracker.view.screens.shopping_list.*
import com.example.fridgetracker.view_model.ShoppingListViewModel
import com.example.fridgetracker.utilities.SnackbarType
import kotlinx.coroutines.launch

@Composable
fun ShoppingListScreen(
    navController: NavController,
    vm: ShoppingListViewModel,
    onMenuClick: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // State management
    val state = rememberShoppingListState(vm = vm)

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            ShoppingListTopBar(
                state = state,
                onMenuClick = { coroutineScope.launch { scaffoldState.drawerState.open() } },
                onSaveClick = {
                    keyboardController?.hide()
                    if (state.selectedListName != null) {
                        // Update postojeće liste
                        state.updateCurrentList()
                        state.showSnackbar("List '${state.selectedListName}' updated", SnackbarType.SUCCESS)
                    } else {
                        // Nova lista - otvori dialog
                        state.showSaveDialog = true
                        state.saveNameText = ""
                    }
                }
            )
        },
        drawerContent = {
            AppDrawer(
                navController = navController,
                currentRoute = "shopping",
                closeDrawer = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (state.searchQuery.isNotBlank()) {
                        state.addItem(state.searchQuery)
                        state.searchQuery = ""
                        state.showSuggestions = false
                    } else {
                        state.showCreateListDialog = true
                        state.createNameText = ""
                    }
                },
                backgroundColor = Color(0xFFFFC107)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.Black
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SearchBar(state = state)

            if (state.showSuggestions && state.filteredSuggestions.isNotEmpty()) {
                SuggestionsCard(state = state)
                Spacer(Modifier.height(12.dp))
            }

            ShoppingItemsList(
                state = state,
                scaffoldState = scaffoldState
            )
        }

        if (state.snackbarVisible) {
            CustomSnackbarOverlay(
                message = state.snackbarMessage,
                type = state.snackbarType,
                onDismiss = { state.snackbarVisible = false }
            )
        }
    }

    // Dialogs
    ShoppingListDialogs(state = state, vm = vm)
}