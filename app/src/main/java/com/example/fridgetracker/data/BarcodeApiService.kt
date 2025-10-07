package com.example.fridgetracker.data

import com.example.fridgetracker.model.OpenFoodFactsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface BarcodeApiService {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsResponse>
}