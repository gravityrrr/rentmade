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
import com.example.made.util.attachTabSwipeNavigation
import com.example.made.util.Constants
import com.example.made.util.navigateTabInstant
import com.example.made.util.SessionManager
import com.example.made.util.toast

class PropertyPortfolioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPropertyPortfolioBinding
    private val viewModel: PropertyPortfolioViewModel by viewModels()
    private lateinit var propertyAdapter: PropertyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyPortfolioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        propertyAdapter = PropertyAdapter(
            onPropertyClick = { property ->
                startActivity(Intent(this, PropertyUnitsActivity::class.java).apply {
                    putExtra(Constants.EXTRA_PROPERTY_ID, property.id)
                    putExtra(Constants.EXTRA_PROPERTY_NAME, property.name)
                })
            },
            onPropertyLongClick = { property ->
                toast("Editing ${property.name}")
                startActivity(Intent(this, AddPropertyActivity::class.java).apply {
                    putExtra(Constants.EXTRA_EDIT_MODE, true)
                    putExtra(Constants.EXTRA_PROPERTY_ID, property.id)
                    putExtra(Constants.EXTRA_PROPERTY_NAME, property.name)
                    putExtra(Constants.EXTRA_PROPERTY_ADDRESS, property.address)
                    putExtra(Constants.EXTRA_PROPERTY_TOTAL_UNITS, property.total_units)
                    putExtra(Constants.EXTRA_PROPERTY_MONTHLY_TARGET, property.monthly_target_revenue)
                    putExtra(Constants.EXTRA_PROPERTY_IMAGE_URL, property.image_url)
                })
            }
        )
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
                R.id.nav_dashboard -> { navigateTabInstant(DashboardActivity::class.java); true }
                R.id.nav_properties -> true
                R.id.nav_tenants -> { navigateTabInstant(TenantStatusActivity::class.java); true }
                R.id.nav_setup -> { navigateTabInstant(SettingsActivity::class.java); true }
                else -> false
            }
        }
        attachTabSwipeNavigation(
            activity = this,
            touchSurface = binding.root,
            onSwipeLeft = { navigateTabInstant(TenantStatusActivity::class.java) },
            onSwipeRight = { navigateTabInstant(DashboardActivity::class.java) }
        )
        viewModel.properties.observe(this) { props ->
            propertyAdapter.submitList(props)
            binding.rvProperties.scheduleLayoutAnimation()
            binding.tvTotalAssets.text = props.size.toString()
            val avg = if (props.isNotEmpty()) (props.sumOf { it.occupancy_rate } / props.size * 100).toInt() else 0
            binding.tvAvgOccupancy.text = "$avg%"
            binding.tvEmptyProperties.visibility = if (props.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
        viewModel.loadProperties(SessionManager(this).authToken ?: "")
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProperties(SessionManager(this).authToken ?: "")
    }
}
