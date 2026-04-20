package com.example.made.ui.property

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.made.R
import com.example.made.databinding.ActivityPropertyPortfolioBinding
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.ui.settings.SettingsActivity
import com.example.made.ui.tenant.TenantStatusActivity
import com.example.made.util.Constants
import com.example.made.util.NavMotion
import com.example.made.util.SessionManager

class PropertyPortfolioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPropertyPortfolioBinding
    private val viewModel: PropertyPortfolioViewModel by viewModels()
    private lateinit var propertyAdapter: PropertyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyPortfolioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        propertyAdapter = PropertyAdapter { property ->
            startActivity(Intent(this, PropertyUnitsActivity::class.java).apply {
                putExtra(Constants.EXTRA_PROPERTY_ID, property.id)
                putExtra(Constants.EXTRA_PROPERTY_NAME, property.name)
            })
        }
        binding.rvProperties.apply {
            layoutManager = LinearLayoutManager(this@PropertyPortfolioActivity)
            adapter = propertyAdapter
            isNestedScrollingEnabled = false
        }
        binding.fabAddProperty.setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }
        binding.bottomNav.selectedItemId = R.id.nav_properties
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { startSmooth(DashboardActivity::class.java); true }
                R.id.nav_properties -> true
                R.id.nav_tenants -> { startSmooth(TenantStatusActivity::class.java); true }
                R.id.nav_setup -> { startSmooth(SettingsActivity::class.java); true }
                else -> false
            }
        }
        viewModel.properties.observe(this) { props ->
            propertyAdapter.submitList(props)
            binding.rvProperties.scheduleLayoutAnimation()
            binding.tvTotalAssets.text = props.size.toString()
            val avg = if (props.isNotEmpty()) (props.sumOf { it.occupancy_rate } / props.size * 100).toInt() else 0
            binding.tvAvgOccupancy.text = "$avg%"
        }
        viewModel.loadProperties(SessionManager(this).authToken ?: "")
    }

    private fun startSmooth(target: Class<*>) {
        NavMotion.startWithDirection(this, PropertyPortfolioActivity::class.java, target)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProperties(SessionManager(this).authToken ?: "")
    }
}
