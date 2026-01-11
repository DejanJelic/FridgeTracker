package com.example.fridgetracker.repository

import com.example.fridgetracker.data.UserProfileDao
import com.example.fridgetracker.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(private val dao: UserProfileDao) {

    // In-memory cache to reduce DB calls
    private val _cachedProfile = MutableStateFlow<UserProfile?>(null)
    private var isCacheInitialized = false

    /**
     * Observes the profile from DB and keeps cache in sync.
     * This ensures cache is always up-to-date when profile changes elsewhere.
     */
    fun observeProfile(): Flow<UserProfile?> = dao.observeProfile()
        .onEach { profile ->
            // Keep cache in sync with DB changes
            _cachedProfile.value = profile
            isCacheInitialized = true
        }

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
        invalidateCache()
    }

    /**
     * Invalidates the cache, forcing next getProfile() to fetch from DB.
     * Call this if you suspect the cache might be stale.
     */
    fun invalidateCache() {
        _cachedProfile.value = null
        isCacheInitialized = false
    }

    // Expose cached profile as StateFlow (optional)
    fun getCachedProfileFlow(): Flow<UserProfile?> = _cachedProfile.asStateFlow()
}