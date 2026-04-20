package com.example.made.ui.tenant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.made.data.model.Payment
import com.example.made.data.repository.TenantRepository
import com.example.made.databinding.ActivityTenantDetailsBinding
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import kotlinx.coroutines.launch

class TenantDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTenantDetailsBinding
    private val paymentAdapter = PaymentHistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTenantDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            }.onFailure { loadDemoDetails() }

            repo.getPaymentsByTenant(sm.authToken ?: "", tenantId).onSuccess {
                paymentAdapter.submitList(it)
            }.onFailure { loadDemoPayments() }
        }
    }

    private fun loadDemoDetails() {
        binding.tvUnitProperty.text = "APT 4B · THE LUMINA"
        binding.tvEmail.text = "elena.r@example.com"
    }

    private fun loadDemoPayments() {
        paymentAdapter.submitList(listOf(
            Payment(id="1",tenant_id="4",amount=3450.0,payment_date="Oct 28, 2023",month_label="November",status="paid"),
            Payment(id="2",tenant_id="4",amount=3450.0,payment_date="Sep 29, 2023",month_label="October",status="paid"),
            Payment(id="3",tenant_id="4",amount=3450.0,payment_date="Aug 30, 2023",month_label="September",status="paid")
        ))
    }
}
