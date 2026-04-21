package com.example.made.ui.settings

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import com.example.made.R
import com.example.made.data.model.LandlordSettings
import com.example.made.data.repository.SettingsRepository
import com.example.made.databinding.ActivitySettingsBinding
import com.example.made.ui.auth.LoginActivity
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.ui.property.PropertyPortfolioActivity
import com.example.made.ui.tenant.TenantStatusActivity
import com.example.made.util.attachTabSwipeNavigation
import com.example.made.util.handleAuthExpired
import com.example.made.util.navigateTabInstant
import com.example.made.util.SessionManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sessionManager: SessionManager
    private val settingsRepository = SettingsRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setupViews()
        setupBottomNav()
        setupSwipeNavigation()
        loadRemoteSettings()
    }

    private fun setupViews() {
        binding.switchCompactCards.isChecked = sessionManager.compactMode
        binding.switchReminders.isChecked = sessionManager.rentRemindersEnabled
        binding.switchAutoOverdue.isChecked = sessionManager.autoOverdueEnabled
        binding.switchCollectionInAdvance.isChecked =
            sessionManager.collectionCycle == SessionManager.COLLECTION_CYCLE_ADVANCE
        binding.etGraceDays.setText(sessionManager.graceDays.toString())

        binding.switchCompactCards.setOnCheckedChangeListener { _, checked ->
            sessionManager.compactMode = checked
        }
        binding.switchReminders.setOnCheckedChangeListener { _, checked ->
            sessionManager.rentRemindersEnabled = checked
            saveRemoteSettings()
        }
        binding.switchAutoOverdue.setOnCheckedChangeListener { _, checked ->
            sessionManager.autoOverdueEnabled = checked
            saveRemoteSettings()
        }
        binding.switchCollectionInAdvance.setOnCheckedChangeListener { _, checked ->
            sessionManager.collectionCycle = if (checked) {
                SessionManager.COLLECTION_CYCLE_ADVANCE
            } else {
                SessionManager.COLLECTION_CYCLE_ARREARS
            }
            saveRemoteSettings()
        }
        binding.etGraceDays.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = binding.etGraceDays.text?.toString()?.toIntOrNull() ?: 3
                sessionManager.graceDays = value
                saveRemoteSettings()
            }
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.nav_enter, R.anim.nav_exit).toBundle()
            startActivity(intent, options)
            finish()
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_setup
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    navigateTabInstant(DashboardActivity::class.java)
                    true
                }
                R.id.nav_properties -> {
                    navigateTabInstant(PropertyPortfolioActivity::class.java)
                    true
                }
                R.id.nav_tenants -> {
                    navigateTabInstant(TenantStatusActivity::class.java)
                    true
                }
                R.id.nav_setup -> true
                else -> false
            }
        }
    }

    private fun setupSwipeNavigation() {
        attachTabSwipeNavigation(
            activity = this,
            touchSurface = binding.root,
            onSwipeLeft = null,
            onSwipeRight = { navigateTabInstant(TenantStatusActivity::class.java) }
        )
    }

    private fun loadRemoteSettings() {
        val token = sessionManager.authToken.orEmpty()
        val userId = sessionManager.userId.orEmpty()
        if (token.isBlank() || userId.isBlank()) return
        lifecycleScope.launch {
            settingsRepository.getSettings(token, userId).onSuccess { remote ->
                if (remote != null) {
                    sessionManager.graceDays = remote.grace_days
                    sessionManager.autoOverdueEnabled = remote.auto_overdue_enabled
                    sessionManager.collectionCycle = remote.collection_cycle
                    binding.etGraceDays.setText(remote.grace_days.toString())
                    binding.switchAutoOverdue.isChecked = remote.auto_overdue_enabled
                    binding.switchCollectionInAdvance.isChecked =
                        remote.collection_cycle != SessionManager.COLLECTION_CYCLE_ARREARS
                } else {
                    saveRemoteSettings()
                }
            }.onFailure { err ->
                handleAuthExpired(err.message)
            }
        }
    }

    private fun saveRemoteSettings() {
        val token = sessionManager.authToken.orEmpty()
        val userId = sessionManager.userId.orEmpty()
        if (token.isBlank() || userId.isBlank()) return
        val payload = LandlordSettings(
            landlord_id = userId,
            grace_days = sessionManager.graceDays,
            auto_overdue_enabled = sessionManager.autoOverdueEnabled,
            reminder_window_days = 3,
            collection_cycle = sessionManager.collectionCycle
        )
        lifecycleScope.launch {
            settingsRepository.upsertSettings(token, payload)
                .onFailure { err ->
                    handleAuthExpired(err.message)
                }
        }
    }
}
