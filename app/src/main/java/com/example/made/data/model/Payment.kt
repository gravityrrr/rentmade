package com.example.made.data.model

data class Payment(
    val id: String = "",
    val tenant_id: String = "",
    val amount: Double = 0.0,
    val payment_date: String = "",
    val month_label: String = "",
    val status: String = "paid"
)
