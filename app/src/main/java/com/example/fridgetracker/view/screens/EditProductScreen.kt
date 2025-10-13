package com.example.fridgetracker.view.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fridgetracker.view.screens.edit_product.*
import com.example.fridgetracker.view_model.ProductViewModel
import kotlinx.coroutines.launch

@Composable
fun EditProductScreen(
    navController: NavController,
    vm: ProductViewModel,
    productId: Long? = null
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

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

    Box(modifier = Modifier.fillMaxSize()) {
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
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        state.validateAndSave(
                            onSuccess = {
                                // Navigate back immediately on success
                                if (!state.isEditMode){
                                    vm.setPrefill(null)
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
            },
            modifier = if (state.isSaving || state.isDeleting) {
                Modifier
                    .blur(radius = 4.dp)
                    .clickable(
                        onClick = { },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            } else {
                Modifier
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
        }

        // Loading overlay for save/delete operations
        if (state.isSaving || state.isDeleting || state.showSuccessSave || state.showSuccessDelete || state.showSuccessConsume) {
            LoadingScreen(
                message = when {
                    state.showSuccessSave -> "✓ Product saved successfully!"
                    state.showSuccessDelete -> "✓ Product deleted successfully!"
                    state.showSuccessConsume -> state.consumeSuccessMessage
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