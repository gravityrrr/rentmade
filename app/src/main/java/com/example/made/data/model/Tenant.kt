package com.example.made.data.model

data class Tenant(
    val id: String = "",
    val property_id: String = "",
    val unit_id: String? = null,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val unit_number: String = "",
    val lease_start: String = "",
    val lease_end: String = "",
    val monthly_rent: Double = 0.0,
    val payment_status: String = "pending",
    val last_payment_date: String? = null,
    val avatar_url: String? = null,
    val due_date: String = "",
    val property_name: String = "",
    val water_bill: Double = 0.0,
    val electricity_bill: Double = 0.0,
    val trash_bill: Double = 0.0,
    val aadhar_number: String? = null,
    val aadhar_url: String? = null,
    val aadhar_path: String? = null,
    val lease_agreement_path: String? = null,
    val lease_agreement_url: String? = null
)
