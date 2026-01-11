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
import com.example.fridgetracker.view_model.DeleteState
import com.example.fridgetracker.view_model.SaveState
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

    val saveState by vm.saveState.collectAsState()
    val deleteState by vm.deleteState.collectAsState()

    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveState.Success -> {
                val successMsg = (saveState as SaveState.Success).message
                state.showSnackbar(successMsg, SnackbarType.SUCCESS)
                vm.resetSaveState()
            }
            is SaveState.Error -> {
                val errorMsg = (saveState as SaveState.Error).message
                state.showSnackbar(errorMsg, SnackbarType.ERROR)
                vm.resetSaveState()
            }
            is SaveState.Loading -> {
                state.showSnackbar("Saving...", SnackbarType.INFO)
            }
            SaveState.Idle -> {}
        }
    }

    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                val successMsg = (deleteState as DeleteState.Success).message
                state.showSnackbar(successMsg, SnackbarType.SUCCESS)
                vm.resetDeleteState()
            }
            is DeleteState.Error -> {
                val errorMsg = (deleteState as DeleteState.Error).message
                state.showSnackbar(errorMsg, SnackbarType.ERROR)
                vm.resetDeleteState()
            }
            is DeleteState.Loading -> {
                state.showSnackbar("Deleting...", SnackbarType.INFO)
            }
            DeleteState.Idle -> {}
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            ShoppingListTopBar(
                state = state,
                onMenuClick = { coroutineScope.launch { scaffoldState.drawerState.open() } },
                onSaveClick = {
                    keyboardController?.hide()
                    if (state.selectedListName != null) {
                        state.updateCurrentList()
                        state.showSnackbar("List '${state.selectedListName}' updated", SnackbarType.SUCCESS)
                    } else {
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