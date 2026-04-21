package com.example.made.ui.dashboard

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
import com.example.made.util.attachTabSwipeNavigation
import com.example.made.util.Constants
import com.example.made.util.navigateTabInstant
import com.example.made.util.parseFlexibleDate
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var tenantAdapter: TenantAdapter
    private lateinit var dayBinder: DayCellBinder
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
        setupSwipeNavigation()
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
                overridePendingTransition(0, 0)
            },
            onMarkPaid = { },
            onRemind = { },
            showActions = false
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
                    override fun getFormattedValue(value: Float) = "₹${(value/1000).toInt()}K"
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
        dayBinder = DayCellBinder(this) { day ->
            if (day.position == DayPosition.MonthDate) {
                RentDueBottomSheetFragment.newInstance(day.date.toString())
                    .show(supportFragmentManager, "RentDueBottomSheet")
            }
        }
        binding.calendarView.dayBinder = dayBinder
        binding.calendarView.setup(
            currentMonth.minusMonths(6), currentMonth.plusMonths(6), firstDayOfWeekFromLocale()
        )
        binding.calendarView.monthScrollListener = { month ->
            val monthLabel = month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            binding.tvCalendarMonthYear.text = getString(
                R.string.calendar_month_year,
                monthLabel,
                month.yearMonth.year
            )
        }
        binding.calendarView.scrollToMonth(currentMonth)
        val currentLabel = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        binding.tvCalendarMonthYear.text = getString(R.string.calendar_month_year, currentLabel, currentMonth.year)
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_dashboard
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_properties -> { navigateTabInstant(PropertyPortfolioActivity::class.java); true }
                R.id.nav_tenants -> { navigateTabInstant(TenantStatusActivity::class.java); true }
                R.id.nav_setup -> { navigateTabInstant(SettingsActivity::class.java); true }
                else -> false
            }
        }
    }

    private fun setupSwipeNavigation() {
        attachTabSwipeNavigation(
            activity = this,
            touchSurface = binding.root,
            onSwipeLeft = { navigateTabInstant(PropertyPortfolioActivity::class.java) },
            onSwipeRight = null
        )
    }

    private fun observeViewModel() {
        viewModel.totalExpected.observe(this) { binding.tvTotalExpected.text = it.toCurrency() }
        viewModel.totalCollected.observe(this) { collected ->
            binding.tvCollected.text = collected.toCurrency()
            val total = viewModel.totalExpected.value ?: 0.0
            val pct = if (total > 0.0) ((collected / total) * 100).toInt().coerceIn(0, 100) else 0
            binding.progressCollected.progress = pct
            binding.tvCollectedPercent.text = "$pct%"
        }
        viewModel.totalOutstanding.observe(this) { binding.tvOutstanding.text = it.toCurrency() }
        viewModel.tenants.observe(this) { tenants ->
            tenantAdapter.submitList(tenants)
            val pending = tenants.count { isDueSoonPending(it) }
            binding.tvPendingCount.text = getString(
                R.string.pending_for_month,
                pending,
                resolvePendingMonthLabel()
            )
            val activeLeaseCount = tenants.count { hasAssignedUnit(it) }
            binding.tvActiveLeases.text = "Across $activeLeaseCount Active Leases"
            val dueDates = tenants.mapNotNull { parseDueDate(it.due_date) }.toSet()
            dayBinder.setDueDates(dueDates)
            binding.calendarView.notifyCalendarChanged()
        }
        viewModel.revenueLabels.observe(this) { labels ->
            updateLineChart(labels, viewModel.revenueValues.value ?: emptyList())
        }
        viewModel.revenueValues.observe(this) { values ->
            updateLineChart(viewModel.revenueLabels.value ?: emptyList(), values)
        }
    }

    private fun parseDueDate(raw: String): LocalDate? {
        return parseFlexibleDate(raw)
    }

    private fun scheduleRentReminder() {
        val workRequest = PeriodicWorkRequestBuilder<RentReminderWorker>(1, TimeUnit.DAYS)
            .addTag(Constants.WORK_TAG_RENT_REMINDER).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.WORK_TAG_RENT_REMINDER, ExistingPeriodicWorkPolicy.KEEP, workRequest
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboardData(sessionManager.authToken ?: "")
    }

    private fun hasAssignedUnit(tenant: com.example.made.data.model.Tenant): Boolean {
        return !tenant.unit_id.isNullOrBlank()
    }

    private fun isDueSoonPending(tenant: com.example.made.data.model.Tenant): Boolean {
        if (tenant.payment_status.equals(Constants.STATUS_PAID, ignoreCase = true)) return false
        val dueDate = parseDueDate(tenant.due_date) ?: return false
        val daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
        return daysUntilDue in 0..3
    }

    private fun resolvePendingMonthLabel(): String {
        val isAdvance = sessionManager.collectionCycle == SessionManager.COLLECTION_CYCLE_ADVANCE
        val period = if (isAdvance) YearMonth.now().plusMonths(1) else YearMonth.now()
        val monthName = period.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$monthName ${period.year}"
    }
}
