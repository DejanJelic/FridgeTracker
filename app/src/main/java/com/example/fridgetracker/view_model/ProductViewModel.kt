package com.example.fridgetracker.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fridgetracker.repository.ProductRepository
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.model.ProductDraft
import com.example.fridgetracker.model.ProductInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(private val repo: ProductRepository) : ViewModel() {
    val products = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _prefill = MutableStateFlow<ProductDraft?>(null)
    val prefill: StateFlow<ProductDraft?> = _prefill.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    fun setPrefill(draft: ProductDraft?) {
        _prefill.value = draft
    }
    fun findByBarcode(barcode: String, onResult: (Product?) -> Unit) {
        viewModelScope.launch {
            val result = repo.findByBarcode(barcode)
            onResult(result)
        }
    }
    fun upsert(product: Product) = viewModelScope.launch { repo.upsert(product) }
    fun delete(product: Product) = viewModelScope.launch { repo.delete(product) }
    fun getProductFlow(id: Long): Flow<Product?> = repo.observeById(id)
    fun lookupBarcodeOnline(barcode: String, onResult: (ProductInfo?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repo.lookupBarcode(barcode)
                onResult(result)
            } catch (e: Exception) {
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }
}