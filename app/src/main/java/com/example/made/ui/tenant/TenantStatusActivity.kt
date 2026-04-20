package com.example.made.ui.tenant

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.made.R
import com.example.made.databinding.ActivityTenantStatusBinding
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.ui.property.PropertyPortfolioActivity
import com.example.made.ui.settings.SettingsActivity
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import com.example.made.util.toCurrency

class TenantStatusActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTenantStatusBinding
    private val viewModel: TenantStatusViewModel by viewModels()
    private lateinit var tenantAdapter: TenantAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTenantStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tenantAdapter = TenantAdapter(
            onTenantClick = { t ->
                startActivity(Intent(this, TenantDetailsActivity::class.java).apply {
                    putExtra(Constants.EXTRA_TENANT_ID, t.id)
                    putExtra(Constants.EXTRA_TENANT_NAME, t.name)
                    putExtra(Constants.EXTRA_TENANT_PHONE, t.phone)
                })
            }, onMarkPaid = { }, onRemind = { }
        )
        binding.rvTenants.apply {
            layoutManager = LinearLayoutManager(this@TenantStatusActivity)
            adapter = tenantAdapter; isNestedScrollingEnabled = false
        }
        binding.bottomNav.selectedItemId = R.id.nav_tenants
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { startInstant(DashboardActivity::class.java); true }
                R.id.nav_properties -> { startInstant(PropertyPortfolioActivity::class.java); true }
                R.id.nav_tenants -> true
                R.id.nav_setup -> { startInstant(SettingsActivity::class.java); true }
                else -> false
            }
        }
        viewModel.tenants.observe(this) { tenants ->
            tenantAdapter.submitList(tenants)
            binding.rvTenants.scheduleLayoutAnimation()
            val expected = tenants.sumOf { it.monthly_rent }
            val collected = tenants.filter { it.payment_status == "paid" }.sumOf { it.monthly_rent }
            val pct = if (expected > 0) ((collected / expected) * 100).toInt() else 0
            binding.tvTotalExpected.text = expected.toCurrency()
            binding.tvCollected.text = collected.toCurrency()
            binding.tvOutstanding.text = (expected - collected).toCurrency()
            binding.tvPendingCount.text = "${tenants.count { it.payment_status != "paid" }} TENANTS PENDING"
            binding.tvLeaseCount.text = "Across ${tenants.size} Active Leases"
            binding.progressCollected.progress = pct
            binding.tvPercent.text = "$pct%"
        }
        viewModel.loadTenants(SessionManager(this).authToken ?: "")
    }

    private fun startInstant(target: Class<*>) {
        val intent = Intent(this, target)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val options = ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
        startActivity(intent, options)
        finish()
    }
}
