package com.example.made.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.made.data.remote.SupabaseConfig
import com.example.made.data.repository.UserRepository
import com.example.made.databinding.ActivityAdminDashboardBinding
import com.example.made.ui.auth.LoginActivity
import com.example.made.util.SessionManager
import com.example.made.util.toast
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var sessionManager: SessionManager
    private val repository = UserRepository()
    private lateinit var adapter: AdminUsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        if (!sessionManager.userEmail.equals(SupabaseConfig.ADMIN_EMAIL, ignoreCase = true)) {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        adapter = AdminUsersAdapter(
            onToggleRole = { profile -> toggleRole(profile.id, profile.role) },
            onToggleActive = { profile -> toggleActive(profile.id, profile.is_active) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        val token = sessionManager.authToken.orEmpty()
        if (token.isBlank()) {
            toast("Session expired")
            return
        }

        lifecycleScope.launch {
            repository.getProfiles(token).onSuccess { users ->
                adapter.submit(users)
                binding.rvUsers.scheduleLayoutAnimation()
                binding.tvUserCount.text = "${users.size} users"
            }.onFailure {
                toast("Unable to load users. Apply SQL migration in Supabase.")
            }
        }
    }

    private fun toggleRole(userId: String, currentRole: String) {
        val token = sessionManager.authToken.orEmpty()
        val nextRole = if (currentRole == "admin") "landlord" else "admin"
        lifecycleScope.launch {
            repository.updateProfile(token, userId, mapOf("role" to nextRole)).onSuccess {
                loadUsers()
            }.onFailure {
                toast("Role update failed")
            }
        }
    }

    private fun toggleActive(userId: String, isActive: Boolean) {
        val token = sessionManager.authToken.orEmpty()
        lifecycleScope.launch {
            repository.updateProfile(token, userId, mapOf("is_active" to !isActive)).onSuccess {
                loadUsers()
            }.onFailure {
                toast("Status update failed")
            }
        }
    }
}
