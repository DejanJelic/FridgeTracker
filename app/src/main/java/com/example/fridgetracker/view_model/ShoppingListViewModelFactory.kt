package com.example.fridgetracker.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fridgetracker.repository.ShoppingListRepository

class ShoppingListViewModelFactory(private val repo: ShoppingListRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            return ShoppingListViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown VM")
    }
}
