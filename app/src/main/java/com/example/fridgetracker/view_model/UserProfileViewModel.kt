package com.example.fridgetracker.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fridgetracker.model.UserProfile
import com.example.fridgetracker.repository.UserProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserProfileViewModel(private val repo: UserProfileRepository) : ViewModel() {

    val profileState: StateFlow<UserProfile?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repo.saveProfile(profile)
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            repo.deleteProfile()
        }
    }
}