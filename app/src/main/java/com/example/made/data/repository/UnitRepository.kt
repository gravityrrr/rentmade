package com.example.made.data.repository

import com.example.made.data.model.Unit
import com.example.made.data.remote.RetrofitClient

class UnitRepository {
    private val api = RetrofitClient.api

    suspend fun getUnitsByProperty(token: String, propertyId: String): Result<List<Unit>> {
        return try {
            val units = api.getUnitsByProperty("Bearer $token", "eq.$propertyId")
            Result.success(units)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addUnit(token: String, unit: Unit): Result<Unit?> {
        return try {
            val response = api.addUnit("Bearer $token", unit = unit)
            if (response.isSuccessful) Result.success(response.body()?.firstOrNull())
            else Result.failure(Exception("Failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUnit(token: String, unitId: String, payload: Map<String, Any?>): Result<Unit?> {
        return try {
            val response = api.updateUnit("Bearer $token", id = "eq.$unitId", payload = payload)
            if (response.isSuccessful) Result.success(response.body()?.firstOrNull())
            else Result.failure(Exception("Failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
