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
            val payload: Map<String, Any?> = mapOf(
                "landlord_id" to property.landlord_id,
                "name" to property.name,
                "address" to property.address,
                "total_units" to property.total_units,
                "monthly_target_revenue" to property.monthly_target_revenue,
                "image_url" to property.image_url
            )
            val response = api.addProperty("Bearer $token", payload = payload)
            if (response.isSuccessful) {
                Result.success(response.body()?.firstOrNull())
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                val message = if (errorBody.isNotBlank()) {
                    "Failed: ${response.code()} - $errorBody"
                } else {
                    "Failed: ${response.code()}"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProperty(token: String, propertyId: String, property: Property): Result<Property?> {
        return try {
            val payload: Map<String, Any?> = mapOf(
                "name" to property.name,
                "address" to property.address,
                "total_units" to property.total_units,
                "monthly_target_revenue" to property.monthly_target_revenue,
                "image_url" to property.image_url
            )
            val response = api.updateProperty("Bearer $token", id = "eq.$propertyId", payload = payload)
            if (response.isSuccessful) {
                Result.success(response.body()?.firstOrNull())
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                val message = if (errorBody.isNotBlank()) {
                    "Failed: ${response.code()} - $errorBody"
                } else {
                    "Failed: ${response.code()}"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
