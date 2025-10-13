package com.example.fridgetracker.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fridgetracker.view.screens.edit_product.*
import com.example.fridgetracker.view_model.ProductViewModel
import kotlinx.coroutines.launch

@Composable
fun EditProductScreenNew(
    navController: NavController,
    vm: ProductViewModel,
    productId: Long? = null
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    // State management
    val state = rememberEditProductState(
        productId = productId,
        vm = vm,
        context = context
    )

    // Loading indicator
    if (state.isLoading) {
        LoadingScreen(message = "Loading...")
        return
    }

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            CustomSnackbarHost(hostState = it)
        },
        topBar = {
            EditProductTopBar(
                isEditMode = state.isEditMode,
                onBackClick = {
                    if (!state.isEditMode) vm.setPrefill(null)
                    navController.popBackStack()
                },
                onSaveClick = {
                    state.validateAndSave(
                        onSuccess = {
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar("✓ Product saved successfully")
                                if (!state.isEditMode) vm.setPrefill(null)
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
        },
        bottomBar = {
            if (state.isEditMode && state.existingProduct != null) {
                EditProductBottomBar(
                    onConsumeClick = { state.showConsumeDialog = true },
                    onTrashClick = { state.showTrashDialog = true }
                )
            }
        },
        floatingActionButton = {
            EditProductFAB(
                showMenu = state.showFabMenu,
                onMenuToggle = { state.showFabMenu = !state.showFabMenu },
                onTakePhoto = { state.takePictureLauncher.launch(null) },
                onScanBarcode = { navController.navigate("scan") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            ProductCard(state = state)
            DateCard(state = state, context = context)
            ClassificationCard(state = state)
            MiscCard(state = state)
            NotificationsCard(state = state)
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Loading overlay for save/delete operations
        if (state.isSaving || state.isDeleting) {
            LoadingScreen(
                message = when {
                    state.isSaving -> "Saving..."
                    state.isDeleting -> "Deleting..."
                    else -> "Working..."
                }
            )
        }
    }

    // Dialogs
    EditProductDialogs(
        state = state,
        scaffoldState = scaffoldState,
        navController = navController,
        vm = vm
    )
}