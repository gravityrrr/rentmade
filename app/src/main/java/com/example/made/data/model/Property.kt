package com.example.made.data.model

data class Property(
    val id: String = "",
    val landlord_id: String = "",
    val name: String = "",
    val address: String = "",
    val total_units: Int = 0,
    val monthly_target_revenue: Double = 0.0,
    val image_url: String? = null,
    val occupancy_rate: Double = 0.0,
    val estimated_value: Double = 0.0,
    val created_at: String = "",
    val property_type: String = "residential",
    val status: String = "active"
)
