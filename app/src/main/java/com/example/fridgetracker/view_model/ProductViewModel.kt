package com.example.fridgetracker.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fridgetracker.repository.ProductRepository
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.model.ProductDraft
import com.example.fridgetracker.model.ProductInfo
import com.example.fridgetracker.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(private val repo: ProductRepository) : ViewModel() {
    val products = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _prefill = MutableStateFlow<ProductDraft?>(null)
    val prefill: StateFlow<ProductDraft?> = _prefill.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setPrefill(draft: ProductDraft?) {
        _prefill.value = draft
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun findByBarcode(barcode: String, onResult: (Product?) -> Unit) {
        viewModelScope.launch {
            val result = repo.findByBarcode(barcode)
            onResult(result)
        }
    }

    fun upsert(product: Product, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repo.upsert(product)
            result.onError { message, _ ->
                _errorMessage.value = message
            }
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun delete(product: Product, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repo.delete(product)
            result.onError { message, _ ->
                _errorMessage.value = message
            }
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun getProductFlow(id: Long): Flow<Product?> = repo.observeById(id)

    fun lookupBarcodeOnline(barcode: String, onResult: (ProductInfo?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repo.lookupBarcode(barcode)
            result.onSuccess { productInfo ->
                onResult(productInfo)
            }.onError { message, _ ->
                _errorMessage.value = message
                onResult(null)
            }
            _isLoading.value = false
        }
    }
}