package com.example.made.ui.auth

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.made.databinding.ActivityLoginBinding
import com.example.made.data.remote.RetrofitClient
import com.example.made.data.remote.SupabaseConfig
import com.example.made.ui.admin.AdminDashboardActivity
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.util.SessionManager
import com.example.made.util.hide
import com.example.made.util.show
import com.example.made.util.toast
import com.google.android.material.transition.platform.MaterialSharedAxis
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowTransitions()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)
        setupClickListeners()
    }

    private fun setupWindowTransitions() {
        window.enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 280L }
        window.returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220L }
        window.exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 220L }
        window.reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220L }
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
            val intent = Intent(this, SignupActivity::class.java)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                binding.ivLoginLogo,
                "app_logo_transition"
            )
            startActivity(intent, options.toBundle())
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
                    if (token.isBlank() || userId.isBlank()) {
                        toast("Login failed. Please verify your email first.")
                        return@launch
                    }
                    sessionManager.saveLoginSession(userId, email, email.substringBefore("@"), token)
                    if (email.equals(SupabaseConfig.ADMIN_EMAIL, ignoreCase = true)) {
                        navigateToAdminDashboard()
                    } else {
                        navigateToDashboard()
                    }
                } else {
                    toast("Login failed. Check your credentials.")
                }
            } catch (e: Exception) {
                toast("Unable to login. Check connection and email verification.")
            } finally {
                binding.btnLogin.isEnabled = true
                binding.progressLogin.hide()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val options = ActivityOptions
            .makeSceneTransitionAnimation(this, binding.ivLoginLogo, "app_logo_transition")
            .toBundle()
        startActivity(intent, options)
        finish()
    }

    private fun navigateToAdminDashboard() {
        val intent = Intent(this, AdminDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val options = ActivityOptions
            .makeSceneTransitionAnimation(this, binding.ivLoginLogo, "app_logo_transition")
            .toBundle()
        startActivity(intent, options)
        finish()
    }
}
