package com.example.made.ui.dashboard

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.made.R
import com.example.made.databinding.ActivityDashboardBinding
import com.example.made.ui.calendar.DayCellBinder
import com.example.made.ui.calendar.RentDueBottomSheetFragment
import com.example.made.ui.property.PropertyPortfolioActivity
import com.example.made.ui.settings.SettingsActivity
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
    private var chartLabels: List<String> = emptyList()

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
        val lineColor = ContextCompat.getColor(this, R.color.colorChartLine)
        val chartFillColor = ContextCompat.getColor(this, R.color.colorChartFill)
        val accentColor = ContextCompat.getColor(this, R.color.colorChartAccent)
        val textMuted = ContextCompat.getColor(this, R.color.colorTextMuted)
        val chartGridColor = ContextCompat.getColor(this, R.color.colorDivider)
        val dataSet = LineDataSet(emptyList(), "Revenue").apply {
            color = lineColor
            lineWidth = 2.5f
            setDrawCircles(true)
            setCircleColor(accentColor)
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleColor = ContextCompat.getColor(this@DashboardActivity, R.color.colorSurface)
            circleHoleRadius = 2f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = chartFillColor
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
                textColor = textMuted
                axisLineColor = chartGridColor
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(emptyList())
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = chartGridColor
                textColor = textMuted
                axisLineColor = Color.TRANSPARENT
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) = "$${(value/1000).toInt()}K"
                }
            }
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun updateLineChart(labels: List<String>, values: List<Float>) {
        chartLabels = labels
        val entries = values.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val dataSet = LineDataSet(entries, "Revenue")
        binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.lineChart.data = LineData(dataSet.apply {
            color = ContextCompat.getColor(this@DashboardActivity, R.color.colorChartLine)
            lineWidth = 2.5f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(this@DashboardActivity, R.color.colorChartAccent))
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleColor = ContextCompat.getColor(this@DashboardActivity, R.color.colorSurface)
            circleHoleRadius = 2f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@DashboardActivity, R.color.colorChartFill)
            fillAlpha = 35
            mode = LineDataSet.Mode.CUBIC_BEZIER
        })
        binding.lineChart.animateX(500)
        binding.lineChart.invalidate()
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
                R.id.nav_properties -> { startInstant(PropertyPortfolioActivity::class.java); true }
                R.id.nav_tenants -> { startInstant(TenantStatusActivity::class.java); true }
                R.id.nav_setup -> { startInstant(SettingsActivity::class.java); true }
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
        viewModel.revenueLabels.observe(this) { labels ->
            updateLineChart(labels, viewModel.revenueValues.value ?: emptyList())
        }
        viewModel.revenueValues.observe(this) { values ->
            updateLineChart(viewModel.revenueLabels.value ?: emptyList(), values)
        }
    }

    private fun startInstant(target: Class<*>) {
        val intent = Intent(this, target)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val options = ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
        startActivity(intent, options)
        finish()
    }

    private fun scheduleRentReminder() {
        val workRequest = PeriodicWorkRequestBuilder<RentReminderWorker>(1, TimeUnit.DAYS)
            .addTag(Constants.WORK_TAG_RENT_REMINDER).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.WORK_TAG_RENT_REMINDER, ExistingPeriodicWorkPolicy.KEEP, workRequest
        )
    }
}
