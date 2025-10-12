package com.example.fridgetracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Long = 1L, // Single user profile
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "", // Format: yyyy-MM-dd
    val weight: Double? = null, // in kg
    val gender: String = "", // Male, Female, Other, Prefer not to say

    // Preferences (dietary choices)
    val isVegan: Boolean = false,
    val isVegetarian: Boolean = false,
    val isPorkFree: Boolean = false,
    val isMeatFree: Boolean = false,
    val isNoBeef: Boolean = false,

    // Restrictions (allergies/intolerances)
    val isGlutenFree: Boolean = false,
    val isNoLactose: Boolean = false,
    val isNoAlcohol: Boolean = false,
    val isNoShellfish: Boolean = false,
    val isNoNuts: Boolean = false
)