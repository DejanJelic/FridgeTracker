package com.example.fridgetracker.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fridgetracker.repository.ProductRepository
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.model.ProductDraft
import com.example.fridgetracker.model.ProductInfo
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

    fun setPrefill(draft: ProductDraft?) {
        _prefill.value = draft
    }
    suspend fun findByBarcode(barcode: String): Product? = repo.findByBarcode(barcode)
    fun upsert(product: Product) = viewModelScope.launch { repo.upsert(product) }
    fun delete(product: Product) = viewModelScope.launch { repo.delete(product) }
    fun getProductFlow(id: Long) = repo.observeById(id)
    suspend fun lookupBarcodeOnline(barcode: String): ProductInfo? {
        return repo.lookupBarcode(barcode)
    }
}