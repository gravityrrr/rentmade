package com.example.made.data.model

data class Tenant(
    val id: String = "",
    val property_id: String = "",
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
    val property_name: String = ""
)
