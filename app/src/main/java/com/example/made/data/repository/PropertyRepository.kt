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
            val payload = mapOf(
                "id" to property.id,
                "landlord_id" to property.landlord_id,
                "name" to property.name,
                "address" to property.address,
                "total_units" to property.total_units,
                "monthly_target_revenue" to property.monthly_target_revenue,
                "property_type" to property.property_type,
                "status" to property.status
            )
            val response = api.addProperty("Bearer $token", payload = payload)
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
