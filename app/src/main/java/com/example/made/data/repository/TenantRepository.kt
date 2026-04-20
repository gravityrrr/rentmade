package com.example.made.data.repository

import com.example.made.data.model.Payment
import com.example.made.data.model.Tenant
import com.example.made.data.remote.RetrofitClient

class TenantRepository {
    private val api = RetrofitClient.api

    suspend fun getTenants(token: String): Result<List<Tenant>> {
        return try {
            val tenants = api.getTenants("Bearer $token")
            Result.success(tenants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTenantById(token: String, id: String): Result<Tenant?> {
        return try {
            val tenants = api.getTenantById("Bearer $token", "eq.$id")
            Result.success(tenants.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTenantStatus(token: String, tenantId: String, status: String): Result<Unit> {
        return try {
            val response = api.updateTenantStatus(
                "Bearer $token", id = "eq.$tenantId",
                status = mapOf("payment_status" to status)
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPaymentsByTenant(token: String, tenantId: String): Result<List<Payment>> {
        return try {
            val payments = api.getPaymentsByTenant("Bearer $token", "eq.$tenantId")
            Result.success(payments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
