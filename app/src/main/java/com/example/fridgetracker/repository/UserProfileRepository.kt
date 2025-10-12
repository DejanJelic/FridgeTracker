package com.example.fridgetracker.repository

import com.example.fridgetracker.data.UserProfileDao
import com.example.fridgetracker.model.UserProfile
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val dao: UserProfileDao) {

    fun observeProfile(): Flow<UserProfile?> = dao.observeProfile()

    suspend fun getProfile(): UserProfile? = dao.getProfile()

    suspend fun saveProfile(profile: UserProfile) {
        dao.upsertProfile(profile)
    }

    suspend fun deleteProfile() {
        dao.deleteProfile()
    }
}