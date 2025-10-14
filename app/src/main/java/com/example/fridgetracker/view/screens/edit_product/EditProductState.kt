package com.example.fridgetracker.view.screens.edit_product

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.net.toUri
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.view_model.ProductViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
@Stable
class EditProductState(
    val isEditMode: Boolean,
    val context: Context,
    private val vm: ProductViewModel,
    private val productId: Long?,
    private val coroutineScope: CoroutineScope
) {
    // Product data
    var existingProduct by mutableStateOf<Product?>(null)
    var name by mutableStateOf("")
    var barcode by mutableStateOf("")
    var quantity by mutableDoubleStateOf(1.0)
    var unit by mutableStateOf("pcs")
    var daysUntilExpiryStr by mutableStateOf("30")
    var purchaseDate: LocalDate by mutableStateOf(LocalDate.now())
    var alreadyOpened by mutableStateOf(false)
    var openIndividually by mutableStateOf(false)
    var category by mutableStateOf("No category")
    var location by mutableStateOf("Not stored")
    var comment by mutableStateOf("")
    var price by mutableStateOf("")
    var notifyExpiry by mutableStateOf(true)
    var expiryDaysBefore by mutableStateOf("4")
    var notifyAfterOpening by mutableStateOf(true)
    var afterOpeningDays by mutableStateOf("2")
    var imageUri by mutableStateOf<Uri?>(null)
    var imageUrlFromApi by mutableStateOf<String?>(null)

    // UI state
    var isLoading by mutableStateOf(productId != null)
    var isSaving by mutableStateOf(false)
    var isDeleting by mutableStateOf(false)
    var showSuccessSave by mutableStateOf(false)
    var showSuccessDelete by mutableStateOf(false)
    var showSuccessConsume by mutableStateOf(false)
    var consumeSuccessMessage by mutableStateOf("")
    var showConsumeDialog by mutableStateOf(false)
    var showTrashDialog by mutableStateOf(false)
    var showAlreadyOpenedDialog by mutableStateOf(false)
    var showOpenIndividuallyDialog by mutableStateOf(false)
    var editingQuantity by mutableStateOf(false)
    var manualQuantityText by mutableStateOf("1")
    var showFabMenu by mutableStateOf(false)
    var unitExpanded by mutableStateOf(false)

    // Launchers
    lateinit var imagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>
    lateinit var takePictureLauncher: ManagedActivityResultLauncher<Void?, Bitmap?>

    fun setupLaunchers(
        imagePicker: ManagedActivityResultLauncher<String, Uri?>,
        takePicture: ManagedActivityResultLauncher<Void?, Bitmap?>
    ) {
        imagePickerLauncher = imagePicker
        takePictureLauncher = takePicture
    }

    fun populateFromProduct(product: Product) {
        name = product.name
        barcode = product.barcode ?: ""
        quantity = product.quantity
        unit = product.unit.ifBlank { "pcs" }
        purchaseDate = LocalDate.ofEpochDay(product.addedAtEpochDay)

        val bestBefore = LocalDate.ofEpochDay(product.bestBeforeEpochDay)
        daysUntilExpiryStr = (bestBefore.toEpochDay() - purchaseDate.toEpochDay()).toString()

        alreadyOpened = product.openedAtEpochDay != null
        openIndividually = (product.openedAtEpochDay == null) && !product.notifyAfterOpening

        category = product.category ?: "No category"
        location = product.location ?: "Not stored"
        comment = product.comment ?: ""
        price = product.price ?: ""
        notifyExpiry = product.notifyExpiry
        expiryDaysBefore = product.expiryDaysBefore.toString()
        notifyAfterOpening = product.notifyAfterOpening
        afterOpeningDays = product.afterOpeningDays.toString()
        imageUri = product.photoUri?.let { Uri.parse(it) }
        manualQuantityText = quantity.toInt().toString()
    }

    fun validateAndSave(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val daysNum = daysUntilExpiryStr.toLongOrNull()
        val expiryDaysNum = expiryDaysBefore.toIntOrNull()
        val afterOpeningNum = afterOpeningDays.toIntOrNull()
        val priceNum = price.ifBlank { null }?.toDoubleOrNull()

        when {
            name.isBlank() -> {
                onError("⚠ Please enter product name")
                return
            }
            quantity <= 0.0 -> {
                onError("⚠ Quantity must be greater than 0")
                return
            }
            daysNum == null -> {
                onError("⚠ Invalid 'days until expiry' value")
                return
            }
            expiryDaysNum == null -> {
                onError("⚠ Invalid expiry reminder days")
                return
            }
            afterOpeningNum == null -> {
                onError("⚠ Invalid 'after opening' days")
                return
            }
            price.isNotBlank() && priceNum == null -> {
                onError("⚠ Invalid price")
                return
            }
        }

        val product = createProduct(
            daysNum = daysNum,
            expiryDaysNum = expiryDaysNum,
            afterOpeningNum = afterOpeningNum
        )

        coroutineScope.launch {
            try {
                // Set loading state on Main thread first
                withContext(Dispatchers.Main) {
                    isSaving = true
                }

                // Do IO work
                withContext(Dispatchers.IO) {
                    vm.upsert(product)
                }

                // Show success state for a moment while still loading
                withContext(Dispatchers.Main) {
                    // Change loading message to success but keep loading active
                    showSuccessSave = true
                }

                // Show success for a moment
                delay(1200)

                // Update UI on Main thread and navigate
                withContext(Dispatchers.Main) {
                    isSaving = false
                    showSuccessSave = false
                    onSuccess()
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    showSuccessSave = false
                    onError("✗ Save failed: ${t.message ?: "error"}")
                }
            }
        }
    }

    private fun createProduct(
        daysNum: Long,
        expiryDaysNum: Int,
        afterOpeningNum: Int
    ): Product {
        val bestBeforeEpochDay = purchaseDate.plusDays(daysNum).toEpochDay()
        val photoUriString = when {
            imageUri != null -> imageUri.toString()
            !imageUrlFromApi.isNullOrBlank() -> imageUrlFromApi
            else -> null
        }
        val openedEpoch = if (alreadyOpened) LocalDate.now().toEpochDay() else null
        val notifyAfterOpenFinal = if (openIndividually) false else notifyAfterOpening

        return Product(
            id = if (isEditMode) existingProduct?.id ?: 0L else 0L,
            name = name.ifBlank { "Unnamed" },
            quantity = quantity,
            unit = unit,
            addedAtEpochDay = purchaseDate.toEpochDay(),
            bestBeforeEpochDay = bestBeforeEpochDay,
            openedAtEpochDay = openedEpoch,
            location = location,
            photoUri = photoUriString,
            barcode = barcode.ifBlank { null },
            category = category,
            comment = comment.ifBlank { null },
            price = price.ifBlank { null },
            notifyExpiry = notifyExpiry,
            expiryDaysBefore = expiryDaysNum,
            notifyAfterOpening = notifyAfterOpenFinal,
            afterOpeningDays = afterOpeningNum
        )
    }

    fun deleteProduct(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        existingProduct?.let { product ->
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.Main) {
                        isDeleting = true
                    }

                    val deleteJob = launch(Dispatchers.IO) {
                        vm.delete(product)
                    }

                    // Add minimum delay for better UX
                    delay(600)

                    // wait for delete to finish
                    deleteJob.join()

                    withContext(Dispatchers.Main) {
                        showSuccessDelete = true
                    }

                    delay(400)

                    withContext(Dispatchers.Main) {
                        showSuccessDelete = false
                        isDeleting = false
                        onSuccess()
                    }
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        isDeleting = false
                        showSuccessDelete = false
                        onError("✗ Delete failed: ${t.message ?: "error"}")
                    }
                }
            }
        }
    }

    fun consumeProduct(
        consumedQuantity: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        existingProduct?.let { product ->
            coroutineScope.launch {
                try {
                    // Set loading state on Main thread first
                    withContext(Dispatchers.Main) {
                        isSaving = true
                    }

                    // Add minimum delay for better UX
                    delay(600)

                    val remaining = product.quantity - consumedQuantity

                    // Do IO work
                    withContext(Dispatchers.IO) {
                        if (remaining <= 0.0) {
                            vm.delete(product)
                        } else {
                            val updated = product.copy(quantity = remaining)
                            vm.upsert(updated)
                        }
                    }

                    // Show success state for a moment while still loading
                    withContext(Dispatchers.Main) {
                        // Change loading message to success but keep loading active
                        showSuccessConsume = true
                        consumeSuccessMessage =
                            if (remaining <= 0.0) "✓ Product consumed completely" else "✓ Consumed ${consumedQuantity.toInt()} items"
                    }

                    // Show success for a moment
                    delay(1200)

                    // Update UI on Main thread
                    withContext(Dispatchers.Main) {
                        isSaving = false
                        showConsumeDialog = false
                        showSuccessConsume = false
                        onSuccess(consumeSuccessMessage)
                    }
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        isSaving = false
                        onError("✗ Error: ${t.message ?: "unknown"}")
                    }
                }
            }
        }
    }
}

@Composable
fun rememberEditProductState(
    productId: Long?,
    vm: ProductViewModel,
    context: Context
): EditProductState {
    val coroutineScope = rememberCoroutineScope()
    val state = remember(productId) {
        EditProductState(
            isEditMode = productId != null,
            context = context,
            vm = vm,
            productId = productId,
            coroutineScope = coroutineScope
        )
    }

    // Setup launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val saved = copyUriToInternalFile(context, uri)
                withContext(Dispatchers.Main) {
                    state.imageUri = saved?.toUri() ?: uri
                }
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch {
                val saved = saveBitmapToInternalFile(context, bitmap)
                withContext(Dispatchers.Main) {
                    state.imageUri = saved?.toUri()
                }
            }
        }
    }

    state.setupLaunchers(imagePickerLauncher, takePictureLauncher)

    LaunchedEffect(productId) {
        if (productId != null) {
            state.isLoading = true

            vm.getProductFlow(productId).collect { product ->
                if (product != null) {
                    state.existingProduct = product
                    state.populateFromProduct(product)
                }
                state.isLoading = false
            }
        } else {
            state.isLoading = false
        }
    }
    val prefill by vm.prefill.collectAsState()

    LaunchedEffect(prefill) {
        if (!state.isEditMode && prefill != null) {
            prefill?.let {
                state.name = it.name
                state.barcode = it.barcode ?: ""
                state.quantity = it.quantity
                state.unit = it.unit
                state.daysUntilExpiryStr = it.daysUntilExpiry.toString()
                state.category = it.category ?: "No category"
                state.location = it.location ?: "Not stored"
                state.imageUrlFromApi = it.imageUrl
            }
        }
    }
    return state
}