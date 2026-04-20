package com.example.made.data.remote

import com.example.made.data.model.Payment
import com.example.made.data.model.Property
import com.example.made.data.model.Tenant
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {

    // ── Properties ──
    @GET("rest/v1/properties")
    suspend fun getProperties(
        @Header("Authorization") token: String,
        @Query("select") select: String = "*"
    ): List<Property>

    @POST("rest/v1/properties")
    suspend fun addProperty(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Body property: Property
    ): Response<List<Property>>

    // ── Tenants ──
    @GET("rest/v1/tenants")
    suspend fun getTenants(
        @Header("Authorization") token: String,
        @Query("select") select: String = "*"
    ): List<Tenant>

    @GET("rest/v1/tenants")
    suspend fun getTenantById(
        @Header("Authorization") token: String,
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): List<Tenant>

    @PATCH("rest/v1/tenants")
    suspend fun updateTenantStatus(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Query("id") id: String,
        @Body status: Map<String, String>
    ): Response<Unit>

    // ── Payments ──
    @GET("rest/v1/payments")
    suspend fun getPaymentsByTenant(
        @Header("Authorization") token: String,
        @Query("tenant_id") tenantId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "payment_date.desc"
    ): List<Payment>

    // ── Auth ──
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String = SupabaseConfig.ANON_KEY,
        @Header("Content-Type") contentType: String = "application/json",
        @Body credentials: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String = SupabaseConfig.ANON_KEY,
        @Header("Content-Type") contentType: String = "application/json",
        @Body credentials: Map<String, String>
    ): Response<Map<String, Any>>
}
