package com.example.made.data.repository

import com.example.made.data.model.UserProfile
import com.example.made.data.remote.RetrofitClient

class UserRepository {
    private val api = RetrofitClient.api

    suspend fun getProfile(token: String, userId: String): Result<UserProfile?> {
        return try {
            val profile = api.getProfileById("Bearer $token", "eq.$userId").firstOrNull()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfiles(token: String): Result<List<UserProfile>> {
        return try {
            Result.success(api.getProfiles("Bearer $token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(token: String, userId: String, payload: Map<String, Any>): Result<UserProfile?> {
        return try {
            val response = api.updateProfile("Bearer $token", id = "eq.$userId", payload = payload)
            if (response.isSuccessful) {
                Result.success(response.body()?.firstOrNull())
            } else {
                Result.failure(Exception("Failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
