package com.example.fridgetracker.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fridgetracker.repository.UserProfileRepository

class UserProfileViewModelFactory(private val repo: UserProfileRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserProfileViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}