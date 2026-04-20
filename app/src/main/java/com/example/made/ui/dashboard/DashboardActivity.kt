package com.example.made.ui.dashboard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.made.R
import com.example.made.databinding.ActivityDashboardBinding
import com.example.made.ui.calendar.DayCellBinder
import com.example.made.ui.calendar.RentDueBottomSheetFragment
import com.example.made.ui.property.AddPropertyActivity
import com.example.made.ui.property.PropertyPortfolioActivity
import com.example.made.ui.tenant.TenantAdapter
import com.example.made.ui.tenant.TenantDetailsActivity
import com.example.made.ui.tenant.TenantStatusActivity
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import com.example.made.util.toCurrency
import com.example.made.worker.RentReminderWorker
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.YearMonth
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var tenantAdapter: TenantAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        setupRecyclerView()
        setupLineChart()
        setupCalendar()
        setupBottomNav()
        observeViewModel()
        scheduleRentReminder()
        viewModel.loadDashboardData(sessionManager.authToken ?: "")
    }

    private fun setupRecyclerView() {
        tenantAdapter = TenantAdapter(
            onTenantClick = { tenant ->
                val intent = Intent(this, TenantDetailsActivity::class.java)
                intent.putExtra(Constants.EXTRA_TENANT_ID, tenant.id)
                intent.putExtra(Constants.EXTRA_TENANT_NAME, tenant.name)
                intent.putExtra(Constants.EXTRA_TENANT_PHONE, tenant.phone)
                startActivity(intent)
            },
            onMarkPaid = { },
            onRemind = { }
        )
        binding.rvTenantPreview.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = tenantAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupLineChart() {
        val entries = listOf(
            Entry(0f,28000f), Entry(1f,32000f), Entry(2f,30000f),
            Entry(3f,35000f), Entry(4f,33000f), Entry(5f,38000f), Entry(6f,42000f)
        )
        val dataSet = LineDataSet(entries, "Revenue").apply {
            color = Color.parseColor("#7C5CFC")
            lineWidth = 2.5f
            setDrawCircles(true)
            setCircleColor(Color.parseColor("#7C5CFC"))
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleColor = Color.parseColor("#1C1C22")
            circleHoleRadius = 2f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = Color.parseColor("#7C5CFC")
            fillAlpha = 25
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        binding.lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = false
            setScaleEnabled(false)
            setBackgroundColor(Color.TRANSPARENT)
            setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#8E8E9A")
                axisLineColor = Color.parseColor("#2E2E38")
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(arrayOf("Apr","May","Jun","Jul","Aug","Sep","Oct"))
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1E1E26")
                textColor = Color.parseColor("#8E8E9A")
                axisLineColor = Color.TRANSPARENT
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) = "$${(value/1000).toInt()}K"
                }
            }
            axisRight.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        binding.calendarView.dayBinder = DayCellBinder(this) { day ->
            if (day.position == DayPosition.MonthDate) {
                RentDueBottomSheetFragment.newInstance(day.date.toString())
                    .show(supportFragmentManager, "RentDueBottomSheet")
            }
        }
        binding.calendarView.setup(
            currentMonth.minusMonths(6), currentMonth.plusMonths(6), firstDayOfWeekFromLocale()
        )
        binding.calendarView.scrollToMonth(currentMonth)
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_dashboard
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_properties -> { startActivity(Intent(this, PropertyPortfolioActivity::class.java)); true }
                R.id.nav_tenants -> { startActivity(Intent(this, TenantStatusActivity::class.java)); true }
                R.id.nav_setup -> { startActivity(Intent(this, AddPropertyActivity::class.java)); true }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        viewModel.totalExpected.observe(this) { binding.tvTotalExpected.text = it.toCurrency() }
        viewModel.totalCollected.observe(this) { collected ->
            binding.tvCollected.text = collected.toCurrency()
            val total = viewModel.totalExpected.value ?: 1.0
            val pct = ((collected / total) * 100).toInt()
            binding.progressCollected.progress = pct
            binding.tvCollectedPercent.text = "$pct%"
        }
        viewModel.totalOutstanding.observe(this) { binding.tvOutstanding.text = it.toCurrency() }
        viewModel.tenants.observe(this) { tenants ->
            tenantAdapter.submitList(tenants)
            val pending = tenants.count { it.payment_status != "paid" }
            binding.tvPendingCount.text = "$pending TENANTS PENDING"
            binding.tvActiveLeases.text = "Across ${tenants.size} Active Leases"
        }
    }

    private fun scheduleRentReminder() {
        val workRequest = PeriodicWorkRequestBuilder<RentReminderWorker>(1, TimeUnit.DAYS)
            .addTag(Constants.WORK_TAG_RENT_REMINDER).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.WORK_TAG_RENT_REMINDER, ExistingPeriodicWorkPolicy.KEEP, workRequest
        )
    }
}
