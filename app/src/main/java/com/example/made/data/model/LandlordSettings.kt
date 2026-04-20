package com.example.made.data.model

data class LandlordSettings(
    val landlord_id: String = "",
    val grace_days: Int = 3,
    val auto_overdue_enabled: Boolean = true,
    val reminder_window_days: Int = 3,
    val created_at: String? = null,
    val updated_at: String? = null
)
