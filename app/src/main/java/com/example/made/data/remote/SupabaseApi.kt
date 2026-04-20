package com.example.made.data.remote

import com.example.made.data.model.BillLedgerEntry
import com.example.made.data.model.LandlordSettings
import com.example.made.data.model.Payment
import com.example.made.data.model.Property
import com.example.made.data.model.Tenant
import com.example.made.data.model.Unit as RentalUnit
import com.example.made.data.model.UserProfile
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
        @Body payload: Map<String, Any?>
    ): Response<List<Property>>

    // ── Tenants ──
    @GET("rest/v1/tenants")
    suspend fun getTenants(
        @Header("Authorization") token: String,
        @Query("select") select: String = "*"
    ): List<Tenant>

    @GET("rest/v1/tenants")
    suspend fun getTenantsByProperty(
        @Header("Authorization") token: String,
        @Query("property_id") propertyId: String,
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
    ): Response<kotlin.Unit>

    @POST("rest/v1/tenants")
    suspend fun addTenant(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Body tenant: Tenant
    ): Response<List<Tenant>>

    @PATCH("rest/v1/tenants")
    suspend fun updateTenant(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") id: String,
        @Body payload: Map<String, Any?>
    ): Response<List<Tenant>>

    // ── Payments ──
    @GET("rest/v1/payments")
    suspend fun getPaymentsByTenant(
        @Header("Authorization") token: String,
        @Query("tenant_id") tenantId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "payment_date.desc"
    ): List<Payment>

    @GET("rest/v1/payments")
    suspend fun getPayments(
        @Header("Authorization") token: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "payment_date.asc"
    ): List<Payment>

    @POST("rest/v1/payments")
    suspend fun addPayment(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Body payment: Payment
    ): Response<List<Payment>>

    // ── Bill Ledger ──
    @GET("rest/v1/tenant_bill_ledger")
    suspend fun getBillLedgerByTenant(
        @Header("Authorization") token: String,
        @Query("tenant_id") tenantId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "period_month.desc"
    ): List<BillLedgerEntry>

    @POST("rest/v1/tenant_bill_ledger")
    suspend fun addBillLedgerEntry(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Body entry: BillLedgerEntry
    ): Response<List<BillLedgerEntry>>

    @PATCH("rest/v1/tenant_bill_ledger")
    suspend fun updateBillLedgerEntry(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") id: String,
        @Body payload: Map<String, Any?>
    ): Response<List<BillLedgerEntry>>

    // ── Units ──
    @GET("rest/v1/units")
    suspend fun getUnitsByProperty(
        @Header("Authorization") token: String,
        @Query("property_id") propertyId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "door_number.asc"
    ): List<RentalUnit>

    @POST("rest/v1/units")
    suspend fun addUnit(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Body unit: RentalUnit
    ): Response<List<RentalUnit>>

    @PATCH("rest/v1/units")
    suspend fun updateUnit(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") id: String,
        @Body payload: Map<String, Any?>
    ): Response<List<RentalUnit>>

    // ── Landlord Settings ──
    @GET("rest/v1/landlord_settings")
    suspend fun getLandlordSettings(
        @Header("Authorization") token: String,
        @Query("landlord_id") landlordId: String,
        @Query("select") select: String = "*"
    ): List<LandlordSettings>

    @POST("rest/v1/landlord_settings")
    suspend fun upsertLandlordSettings(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Query("on_conflict") onConflict: String = "landlord_id",
        @Body payload: LandlordSettings
    ): Response<List<LandlordSettings>>

    // ── Profiles/Admin ──
    @GET("rest/v1/profiles")
    suspend fun getProfileById(
        @Header("Authorization") token: String,
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): List<UserProfile>

    @GET("rest/v1/profiles")
    suspend fun getProfiles(
        @Header("Authorization") token: String,
        @Query("select") select: String = "id,email,full_name,role,is_active,last_sign_in_at,created_at",
        @Query("order") order: String = "created_at.desc"
    ): List<UserProfile>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") id: String,
        @Body payload: Map<String, Any>
    ): Response<List<UserProfile>>

    // ── Auth ──
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String = SupabaseConfig.ANON_KEY,
        @Header("Content-Type") contentType: String = "application/json",
        @Body credentials: Map<String, Any>
    ): Response<Map<String, Any>>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String = SupabaseConfig.ANON_KEY,
        @Header("Content-Type") contentType: String = "application/json",
        @Body credentials: Map<String, String>
    ): Response<Map<String, Any>>
}
