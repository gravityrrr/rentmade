package com.example.made.util

import android.content.Context
import android.content.SharedPreferences
import com.example.made.data.remote.SupabaseConfig

class SessionManager(context: Context) {

    companion object {
        private const val PREF_NAME = "vantage_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_COMPACT_MODE = "compact_mode"
        private const val KEY_RENT_REMINDERS = "rent_reminders"
        private const val KEY_GRACE_DAYS = "grace_days"
        private const val KEY_AUTO_OVERDUE = "auto_overdue"
        private const val KEY_COLLECTION_CYCLE = "collection_cycle"

        const val COLLECTION_CYCLE_ADVANCE = "advance"
        const val COLLECTION_CYCLE_ARREARS = "arrears"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var compactMode: Boolean
        get() = prefs.getBoolean(KEY_COMPACT_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPACT_MODE, value).apply()

    var rentRemindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_RENT_REMINDERS, true)
        set(value) = prefs.edit().putBoolean(KEY_RENT_REMINDERS, value).apply()

    var graceDays: Int
        get() = prefs.getInt(KEY_GRACE_DAYS, 3)
        set(value) = prefs.edit().putInt(KEY_GRACE_DAYS, value.coerceAtLeast(0)).apply()

    var autoOverdueEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_OVERDUE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_OVERDUE, value).apply()

    var collectionCycle: String
        get() = prefs.getString(KEY_COLLECTION_CYCLE, COLLECTION_CYCLE_ADVANCE) ?: COLLECTION_CYCLE_ADVANCE
        set(value) {
            val normalized = if (value == COLLECTION_CYCLE_ARREARS) COLLECTION_CYCLE_ARREARS else COLLECTION_CYCLE_ADVANCE
            prefs.edit().putString(KEY_COLLECTION_CYCLE, normalized).apply()
        }

    val isAdminUser: Boolean
        get() = userEmail.equals(SupabaseConfig.ADMIN_EMAIL, ignoreCase = true)

    fun saveLoginSession(userId: String, email: String, name: String, token: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_AUTH_TOKEN, token)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
