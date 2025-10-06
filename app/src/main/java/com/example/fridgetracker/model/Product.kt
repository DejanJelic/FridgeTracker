package com.example.fridgetracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val addedAtEpochDay: Long = java.time.LocalDate.now().toEpochDay(),
    val bestBeforeEpochDay: Long,
    val openedAtEpochDay: Long? = null,
    val location: String? = "Fridge",
    val photoUri: String? = null,
    val barcode: String? = null,
    val category: String? = null
)