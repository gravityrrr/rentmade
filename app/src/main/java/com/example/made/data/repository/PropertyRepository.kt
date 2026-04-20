package com.example.made.data.repository

import com.example.made.data.model.Property
import com.example.made.data.remote.RetrofitClient

class PropertyRepository {
    private val api = RetrofitClient.api

    suspend fun getProperties(token: String): Result<List<Property>> {
        return try {
            val properties = api.getProperties("Bearer $token")
            Result.success(properties)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addProperty(token: String, property: Property): Result<Property?> {
        return try {
            val response = api.addProperty("Bearer $token", property = property)
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
