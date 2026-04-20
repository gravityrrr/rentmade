package com.example.made.data.model

data class Unit(
    val id: String = "",
    val property_id: String = "",
    val door_number: String = "",
    val floor_label: String? = null,
    val bedroom_count: Int = 1,
    val fan_count: Int = 0,
    val geyser_count: Int = 0,
    val notes: String? = null,
    val tenant_id: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)
