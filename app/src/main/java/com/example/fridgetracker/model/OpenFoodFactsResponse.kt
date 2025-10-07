package com.example.fridgetracker.model

import com.google.gson.annotations.SerializedName

data class OpenFoodFactsResponse(
    val status: Int,
    val product: ProductInfo?
)

data class ProductInfo(
    @SerializedName("product_name")
    val productName: String?,

    @SerializedName("brands")
    val brands: String?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("categories")
    val categories: String?,

    @SerializedName("quantity")
    val quantity: String?,

    @SerializedName("expiration_date")
    val expirationDate: String?,

    @SerializedName("nutriscore_grade")
    val nutriscoreGrade: String?
)