package com.example.fridgetracker.model

data class ProductDraft(
    val name: String = "",
    val barcode: String? = null,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val daysUntilExpiry: Int = 7,
    val location: String? = "Fridge",
    val imageUrl: String? = null,
    val category: String? = null
)