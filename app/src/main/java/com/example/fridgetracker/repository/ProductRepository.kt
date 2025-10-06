package com.example.fridgetracker.repository

import com.example.fridgetracker.data.ProductDao
import com.example.fridgetracker.model.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val dao: ProductDao) {
    fun observeAll(): Flow<List<Product>> = dao.observeAll()
    fun observeById(id:Long): Flow<Product?> = dao.observeById(id)
    suspend fun upsert(product: Product) = dao.upsert(product)
    suspend fun delete(product: Product) = dao.delete(product)
    suspend fun findByBarcode(barcode: String): Product? = dao.getByBarcode(barcode)
    suspend fun getExpiringBefore(threshold: Long) = dao.getExpiringBefore(threshold)
}