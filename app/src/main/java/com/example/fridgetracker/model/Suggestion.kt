package com.example.fridgetracker.model

data class Suggestion(
    val name: String,
    val unit: String = "No unit",
    val category: String = "Fruits",
    val location: String = "Not stored",
    val abbreviation: String = name.take(3).uppercase(),
    val color: androidx.compose.ui.graphics.Color
)