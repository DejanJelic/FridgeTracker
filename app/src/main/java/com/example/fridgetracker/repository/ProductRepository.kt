package com.example.fridgetracker.repository

import android.content.Context
import android.util.Log
import com.example.fridgetracker.data.BarcodeApiService
import com.example.fridgetracker.data.ProductDao
import com.example.fridgetracker.model.Product
import com.example.fridgetracker.model.ProductInfo
import com.example.fridgetracker.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val dao: ProductDao,
    private val apiService: BarcodeApiService,
    private val context: Context
) {
    companion object {
        private const val TAG = "ProductRepository"
        private const val MAX_RETRY_COUNT = 3
    }

    fun observeAll(): Flow<List<Product>> = dao.observeAll()
    fun observeById(id: Long): Flow<Product?> = dao.observeById(id)

    suspend fun upsert(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // if update (id != 0), we retrieve the old value to delete the old image later
            val old = if (product.id != 0L) dao.getById(product.id) else null
            val oldUri = old?.photoUri
            val newUri = product.photoUri

            // upsert u DB (insert onConflict=REPLACE)
            dao.upsert(product)

            // Only delete old image AFTER successful DB operation
            if (!oldUri.isNullOrBlank() && oldUri != newUri) {
                try {
                    com.example.fridgetracker.utilities.ImageFileUtils.deleteInternalFileIfExists(oldUri, context)
                } catch (t: Throwable) {
                    // Log but don't fail - the DB operation succeeded
                    Log.w(TAG, "Failed to delete old image", t)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upsert product: ${e.message}", e)
            Result.error("Failed to save product: ${e.message}", e)
        }
    }

    suspend fun delete(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val photoUri = product.photoUri

            // Delete from DB first
            dao.delete(product)

            // Only delete image AFTER successful DB deletion
            if (!photoUri.isNullOrBlank()) {
                try {
                    com.example.fridgetracker.utilities.ImageFileUtils.deleteInternalFileIfExists(photoUri, context)
                } catch (t: Throwable) {
                    // Log but don't fail - the DB operation succeeded
                    Log.w(TAG, "Failed to delete product image", t)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete product: ${e.message}", e)
            Result.error("Failed to delete product: ${e.message}", e)
        }
    }

    suspend fun findByBarcode(barcode: String): Product? = dao.getByBarcode(barcode)

    suspend fun getExpiringBefore(threshold: Long) = dao.getExpiringBefore(threshold)

    /**
     * Looks up a barcode online with retry logic for network errors.
     */
    suspend fun lookupBarcode(barcode: String): Result<ProductInfo?> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                val response = apiService.getProductByBarcode(barcode)
                return@withContext if (response.isSuccessful && response.body()?.status == 1) {
                    Result.success(response.body()?.product)
                } else {
                    Result.success(null) // Product not found, but no error
                }
            } catch (e: IOException) {
                // Network error - retry
                lastException = e
                Log.w(TAG, "Barcode lookup attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRY_COUNT - 1) {
                    kotlinx.coroutines.delay(1000L * (attempt + 1)) // Exponential backoff
                }
            } catch (e: Exception) {
                // Other error - don't retry
                Log.e(TAG, "Error looking up barcode: ${e.message}", e)
                return@withContext Result.error("Failed to lookup barcode: ${e.message}", e)
            }
        }

        Result.error("Network error after $MAX_RETRY_COUNT attempts", lastException)
    }

    /**
     * Legacy method for backward compatibility - returns null instead of Result
     */
    suspend fun lookupBarcodeSimple(barcode: String): ProductInfo? {
        return lookupBarcode(barcode).getOrNull()
    }
}