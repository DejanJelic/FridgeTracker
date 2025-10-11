package com.example.fridgetracker.data

import androidx.room.*
import com.example.fridgetracker.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY bestBeforeEpochDay ASC")
    fun observeAll(): Flow<List<Product>>
    @Query("SELECT * FROM products")
    suspend fun getAllSync():  List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product): Long

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products WHERE bestBeforeEpochDay <= :threshold")
    suspend fun getExpiringBefore(threshold: Long): List<Product>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): Product?
}