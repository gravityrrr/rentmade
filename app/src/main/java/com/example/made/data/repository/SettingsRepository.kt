package com.example.made.data.repository

import com.example.made.data.model.LandlordSettings
import com.example.made.data.remote.RetrofitClient

class SettingsRepository {
    private val api = RetrofitClient.api

    suspend fun getSettings(token: String, landlordId: String): Result<LandlordSettings?> {
        return try {
            val data = api.getLandlordSettings("Bearer $token", landlordId = "eq.$landlordId")
            Result.success(data.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertSettings(token: String, settings: LandlordSettings): Result<LandlordSettings?> {
        return try {
            val response = api.upsertLandlordSettings("Bearer $token", payload = settings)
            if (response.isSuccessful) Result.success(response.body()?.firstOrNull())
            else Result.failure(Exception("Failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
