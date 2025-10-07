package com.example.fridgetracker.repository

import android.util.Log
import com.example.fridgetracker.data.ProductDao
import com.example.fridgetracker.data.RetrofitInstance
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.model.ProductInfo
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val dao: ProductDao) {
    fun observeAll(): Flow<List<Product>> = dao.observeAll()
    fun observeById(id:Long): Flow<Product?> = dao.observeById(id)
    suspend fun upsert(product: Product) = dao.upsert(product)
    suspend fun delete(product: Product) = dao.delete(product)
    suspend fun findByBarcode(barcode: String): Product? = dao.getByBarcode(barcode)
    suspend fun getExpiringBefore(threshold: Long) = dao.getExpiringBefore(threshold)
    suspend fun lookupBarcode(barcode: String): ProductInfo? {
        return try {
            val response = RetrofitInstance.api.getProductByBarcode(barcode)
            if (response.isSuccessful && response.body()?.status == 1) {
                response.body()?.product
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error looking up barcode: ${e.message}")
            null
        }
    }
}