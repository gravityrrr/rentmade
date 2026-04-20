package com.example.made.ui.auth

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.example.made.databinding.ActivitySignupBinding
import com.example.made.data.remote.RetrofitClient
import com.example.made.data.remote.SupabaseConfig
import com.example.made.util.SessionManager
import com.example.made.util.hide
import com.example.made.util.show
import com.example.made.util.toast
import com.google.android.material.transition.platform.MaterialSharedAxis
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowTransitions()
        binding = ActivitySignupBinding.inflate(layoutInflater)
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
        binding.btnSignup.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etSignupEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etSignupPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()
            if (validateInputs(name, email, phone, password, confirm)) {
                performSignup(name, email, password)
            }
        }
        binding.tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                binding.ivSignupLogo,
                "app_logo_transition"
            )
            startActivity(intent, options.toBundle())
            finish()
        }
    }

    private fun validateInputs(name: String, email: String, phone: String, pw: String, confirm: String): Boolean {
        var valid = true
        if (name.isEmpty()) { binding.tilFullName.error = "Required"; valid = false } else binding.tilFullName.error = null
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilSignupEmail.error = "Valid email required"; valid = false
        } else binding.tilSignupEmail.error = null
        if (phone.isEmpty()) { binding.tilPhone.error = "Required"; valid = false } else binding.tilPhone.error = null
        if (pw.length < 6) { binding.tilSignupPassword.error = "Min 6 chars"; valid = false } else binding.tilSignupPassword.error = null
        if (confirm != pw) { binding.tilConfirmPassword.error = "Passwords don't match"; valid = false } else binding.tilConfirmPassword.error = null
        return valid
    }

    private fun performSignup(name: String, email: String, password: String) {
        binding.btnSignup.isEnabled = false
        binding.progressSignup.show()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.signUp(
                    credentials = mapOf(
                        "email" to email,
                        "password" to password,
                        "data" to mapOf("full_name" to name),
                        "options" to mapOf(
                            "emailRedirectTo" to SupabaseConfig.EMAIL_CONFIRM_REDIRECT,
                            "email_redirect_to" to SupabaseConfig.EMAIL_CONFIRM_REDIRECT
                        )
                    )
                )
                if (response.isSuccessful) {
                    toast("Check your email for confirmation link before logging in.")
                    navigateToLoginWithTransition()
                } else {
                    toast("Signup failed. Please try again.")
                }
            } catch (e: Exception) {
                toast("Signup failed. Please try again.")
            } finally {
                binding.btnSignup.isEnabled = true
                binding.progressSignup.hide()
            }
        }
    }

    private fun navigateToLoginWithTransition() {
        val intent = Intent(this, LoginActivity::class.java)
        val options = ActivityOptions
            .makeSceneTransitionAnimation(this, binding.ivSignupLogo, "app_logo_transition")
            .toBundle()
        startActivity(intent, options)
        finish()
    }
}
