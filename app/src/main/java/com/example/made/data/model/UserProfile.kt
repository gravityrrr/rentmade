package com.example.made.data.model

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val full_name: String? = null,
    val role: String = "landlord",
    val is_active: Boolean = true,
    val last_sign_in_at: String? = null,
    val created_at: String? = null
)
