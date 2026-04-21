package com.example.made.util

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.made.ui.auth.LoginActivity

fun isAuthExpiredError(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    return message.contains("JWT expired", ignoreCase = true) ||
        Regex("\\b401\\b").containsMatchIn(message)
}

fun AppCompatActivity.handleAuthExpired(message: String?): Boolean {
    if (!isAuthExpiredError(message)) return false
    toast("Session expired. Please sign in again")
    SessionManager(this).clearSession()
    startActivity(Intent(this, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    })
    finish()
    return true
}
