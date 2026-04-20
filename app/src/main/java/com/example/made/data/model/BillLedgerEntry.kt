package com.example.made.data.model

data class BillLedgerEntry(
    val id: String = "",
    val tenant_id: String = "",
    val property_id: String = "",
    val unit_id: String? = null,
    val period_month: String = "",
    val due_date: String = "",
    val rent_amount: Double = 0.0,
    val water_amount: Double = 0.0,
    val electricity_amount: Double = 0.0,
    val trash_amount: Double = 0.0,
    val total_amount: Double = 0.0,
    val status: String = "pending",
    val paid_on: String? = null,
    val created_at: String? = null
)
