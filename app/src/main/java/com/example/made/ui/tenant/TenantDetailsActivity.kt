package com.example.made.ui.tenant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.example.made.data.repository.TenantRepository
import com.example.made.databinding.ActivityTenantDetailsBinding
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import com.example.made.util.toast
import kotlinx.coroutines.launch

class TenantDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTenantDetailsBinding
    private val paymentAdapter = PaymentHistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowTransitions()
        binding = ActivityTenantDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.alpha = 0f
        binding.root.translationY = 18f
        binding.root.animate().alpha(1f).translationY(0f).setDuration(300L).start()

        val tenantId = intent.getStringExtra(Constants.EXTRA_TENANT_ID) ?: ""
        val tenantName = intent.getStringExtra(Constants.EXTRA_TENANT_NAME) ?: ""
        val tenantPhone = intent.getStringExtra(Constants.EXTRA_TENANT_PHONE) ?: ""

        binding.tvTenantName.text = tenantName
        binding.tvPhone.text = tenantPhone
        binding.rvPayments.apply {
            layoutManager = LinearLayoutManager(this@TenantDetailsActivity)
            adapter = paymentAdapter; isNestedScrollingEnabled = false
        }

        loadTenantDetails(tenantId)
    }

    private fun loadTenantDetails(tenantId: String) {
        val sm = SessionManager(this)
        lifecycleScope.launch {
            val repo = TenantRepository()
            repo.getTenantById(sm.authToken ?: "", tenantId).onSuccess { tenant ->
                tenant?.let {
                    binding.tvTenantName.text = it.name
                    binding.tvEmail.text = it.email
                    binding.tvPhone.text = it.phone
                    binding.tvUnitProperty.text = "${it.unit_number} · ${it.property_name}".uppercase()
                }
            }.onFailure {
                toast("Unable to load tenant details")
            }

            repo.getPaymentsByTenant(sm.authToken ?: "", tenantId).onSuccess {
                paymentAdapter.submitList(it)
                binding.rvPayments.scheduleLayoutAnimation()
            }.onFailure {
                paymentAdapter.submitList(emptyList())
                toast("Unable to load payments")
            }
        }
    }

    private fun setupWindowTransitions() {
        window.enterTransition = MaterialFadeThrough().apply { duration = 260L }
        window.returnTransition = MaterialFadeThrough().apply { duration = 220L }
    }
}
