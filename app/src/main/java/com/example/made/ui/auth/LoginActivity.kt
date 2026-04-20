package com.example.made.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.made.databinding.ActivityLoginBinding
import com.example.made.data.remote.RetrofitClient
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.util.SessionManager
import com.example.made.util.hide
import com.example.made.util.show
import com.example.made.util.toast
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateInputs(email, password)) {
                performLogin(email, password)
            }
        }
        binding.tvSignupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var valid = true
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            valid = false
        } else binding.tilEmail.error = null

        if (password.isEmpty() || password.length < 6) {
            binding.tilPassword.error = "Min 6 characters"
            valid = false
        } else binding.tilPassword.error = null
        return valid
    }

    private fun performLogin(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.progressLogin.show()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.signIn(
                    credentials = mapOf("email" to email, "password" to password)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.get("access_token")?.toString() ?: ""
                    val userId = (body?.get("user") as? Map<*, *>)?.get("id")?.toString() ?: ""
                    sessionManager.saveLoginSession(userId, email, email.substringBefore("@"), token)
                    navigateToDashboard()
                } else {
                    toast("Login failed. Check your credentials.")
                }
            } catch (e: Exception) {
                // Demo/offline mode — allow login
                sessionManager.saveLoginSession("demo-001", email, email.substringBefore("@"), "demo-token")
                navigateToDashboard()
            } finally {
                binding.btnLogin.isEnabled = true
                binding.progressLogin.hide()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
