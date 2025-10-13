package com.example.fridgetracker.repository

import com.example.fridgetracker.data.UserProfileDao
import com.example.fridgetracker.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserProfileRepository(private val dao: UserProfileDao) {

    // In-memory cache to reduce DB calls
    private val _cachedProfile = MutableStateFlow<UserProfile?>(null)
    private var isCacheInitialized = false

    fun observeProfile(): Flow<UserProfile?> = dao.observeProfile()

    suspend fun getProfile(): UserProfile? {
        // Return from cache if available
        if (isCacheInitialized) {
            return _cachedProfile.value
        }

        // Fetch from DB and cache
        val profile = dao.getProfile()
        _cachedProfile.value = profile
        isCacheInitialized = true
        return profile
    }

    suspend fun saveProfile(profile: UserProfile) {
        dao.upsertProfile(profile)
        // Update cache
        _cachedProfile.value = profile
        isCacheInitialized = true
    }

    suspend fun deleteProfile() {
        dao.deleteProfile()
        // Clear cache
        _cachedProfile.value = null
        isCacheInitialized = false
    }

    // Expose cached profile as StateFlow (optional)
    fun getCachedProfileFlow(): Flow<UserProfile?> = _cachedProfile.asStateFlow()
}