package com.example.made.data.repository

import com.example.made.data.model.BillLedgerEntry
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

    suspend fun addTenant(token: String, tenant: Tenant): Result<Tenant?> {
        return try {
            val payload = linkedMapOf<String, Any?>(
                "property_id" to tenant.property_id,
                "name" to tenant.name,
                "email" to tenant.email,
                "phone" to tenant.phone,
                "unit_number" to tenant.unit_number,
                "monthly_rent" to tenant.monthly_rent,
                "due_date" to tenant.due_date,
                "payment_status" to tenant.payment_status,
                "avatar_url" to tenant.avatar_url
            )
            tenant.unit_id?.let { payload["unit_id"] = it }

            val response = api.addTenant("Bearer $token", payload = payload)
            if (response.isSuccessful) Result.success(response.body()?.firstOrNull())
            else {
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

    suspend fun getTenantsByProperty(token: String, propertyId: String): Result<List<Tenant>> {
        return try {
            val tenants = api.getTenantsByProperty("Bearer $token", "eq.$propertyId")
            Result.success(tenants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTenant(token: String, tenantId: String, payload: Map<String, Any?>): Result<Tenant?> {
        return try {
            val response = api.updateTenant("Bearer $token", id = "eq.$tenantId", payload = payload)
            if (response.isSuccessful) Result.success(response.body()?.firstOrNull())
            else Result.failure(Exception("Failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPayment(token: String, payment: Payment): Result<Payment?> {
        return try {
            val response = api.addPayment("Bearer $token", payment = payment)
            if (response.isSuccessful) Result.success(response.body()?.firstOrNull())
            else Result.failure(Exception("Failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addBillLedgerEntry(token: String, entry: BillLedgerEntry): Result<BillLedgerEntry?> {
        return try {
            val response = api.addBillLedgerEntry("Bearer $token", entry = entry)
            if (response.isSuccessful) Result.success(response.body()?.firstOrNull())
            else Result.failure(Exception("Failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBillLedgerByTenant(token: String, tenantId: String): Result<List<BillLedgerEntry>> {
        return try {
            val data = api.getBillLedgerByTenant("Bearer $token", tenantId = "eq.$tenantId")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllBillLedger(token: String): Result<List<BillLedgerEntry>> {
        return try {
            val data = api.getBillLedger("Bearer $token")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBillLedgerEntry(token: String, entryId: String, payload: Map<String, Any?>): Result<BillLedgerEntry?> {
        return try {
            val response = api.updateBillLedgerEntry("Bearer $token", id = "eq.$entryId", payload = payload)
            if (response.isSuccessful) Result.success(response.body()?.firstOrNull())
            else Result.failure(Exception("Failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllPayments(token: String): Result<List<Payment>> {
        return try {
            val payments = api.getPayments("Bearer $token")
            Result.success(payments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
