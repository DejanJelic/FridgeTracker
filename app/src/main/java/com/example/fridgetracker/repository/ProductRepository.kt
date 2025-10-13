package com.example.fridgetracker.repository

import android.content.Context
import android.util.Log
import com.example.fridgetracker.data.ProductDao
import com.example.fridgetracker.data.RetrofitInstance
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.model.ProductInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProductRepository(private val dao: ProductDao, private val context: Context) {
    fun observeAll(): Flow<List<Product>> = dao.observeAll()
    fun observeById(id:Long): Flow<Product?> = dao.observeById(id)
    suspend fun upsert(product: Product) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            // if update (id != 0), we retrieve the old value to delete the old image later
            val old = if (product.id != 0L) dao.getById(product.id) else null

            // upsert u DB (insert onConflict=REPLACE)
            dao.upsert(product)

            // If the old image was internal and different from the new one, delete it
            try {
                val oldUri = old?.photoUri
                val newUri = product.photoUri
                if (!oldUri.isNullOrBlank() && oldUri != newUri) {
                    com.example.fridgetracker.utilities.ImageFileUtils.deleteInternalFileIfExists(oldUri, context)
                }
            } catch (t: Throwable) {
                // log in, but don't interrupt the flow
                Log.w("ProductRepository", "Failed to delete old image", t)
            }
        }
    }
    suspend fun delete(product: Product) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.example.fridgetracker.utilities.ImageFileUtils.deleteInternalFileIfExists(product.photoUri, context)
            } catch (t: Throwable) {
                Log.w("ProductRepository", "Failed to delete product image on delete", t)
            }
            dao.delete(product)
        }
    }

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