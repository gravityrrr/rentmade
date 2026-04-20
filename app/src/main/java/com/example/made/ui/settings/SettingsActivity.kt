package com.example.made.ui.settings

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.made.R
import com.example.made.databinding.ActivitySettingsBinding
import com.example.made.ui.auth.LoginActivity
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.ui.property.PropertyPortfolioActivity
import com.example.made.ui.tenant.TenantStatusActivity
import com.example.made.util.SessionManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupViews()
        setupBottomNav()
    }

    private fun setupViews() {
        binding.switchCompactCards.isChecked = sessionManager.compactMode
        binding.switchReminders.isChecked = sessionManager.rentRemindersEnabled

        binding.switchCompactCards.setOnCheckedChangeListener { _, checked ->
            sessionManager.compactMode = checked
        }
        binding.switchReminders.setOnCheckedChangeListener { _, checked ->
            sessionManager.rentRemindersEnabled = checked
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_setup
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startInstant(DashboardActivity::class.java)
                    true
                }
                R.id.nav_properties -> {
                    startInstant(PropertyPortfolioActivity::class.java)
                    true
                }
                R.id.nav_tenants -> {
                    startInstant(TenantStatusActivity::class.java)
                    true
                }
                R.id.nav_setup -> true
                else -> false
            }
        }
    }

    private fun startInstant(target: Class<*>) {
        val intent = Intent(this, target)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val options = ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
        startActivity(intent, options)
        finish()
    }
}
