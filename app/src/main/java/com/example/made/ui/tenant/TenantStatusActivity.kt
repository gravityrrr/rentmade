package com.example.made.ui.tenant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.made.data.model.BillLedgerEntry
import com.example.made.data.model.Payment
import com.example.made.data.model.Tenant
import com.example.made.R
import com.example.made.data.repository.TenantRepository
import com.example.made.databinding.ActivityTenantStatusBinding
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.ui.property.PropertyPortfolioActivity
import com.example.made.ui.settings.SettingsActivity
import com.example.made.util.attachTabSwipeNavigation
import com.example.made.util.Constants
import com.example.made.util.navigateTabInstant
import com.example.made.util.SessionManager
import com.example.made.util.handleAuthExpired
import com.example.made.util.parseFlexibleDate
import com.example.made.util.toast
import com.example.made.util.toCurrency
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID

class TenantStatusActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTenantStatusBinding
    private val viewModel: TenantStatusViewModel by viewModels()
    private lateinit var tenantAdapter: TenantAdapter
    private var allTenants: List<Tenant> = emptyList()

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
                overridePendingTransition(0, 0)
            },
            onMarkPaid = null,
            onEdit = { tenant ->
                startActivity(Intent(this, TenantDetailsActivity::class.java).apply {
                    putExtra(Constants.EXTRA_TENANT_ID, tenant.id)
                    putExtra(Constants.EXTRA_TENANT_NAME, tenant.name)
                    putExtra(Constants.EXTRA_TENANT_PHONE, tenant.phone)
                })
                overridePendingTransition(0, 0)
            },
            onRemind = { tenant -> handleRemindOrCall(tenant) },
            showMarkPaid = false
        )
        binding.rvTenants.apply {
            layoutManager = LinearLayoutManager(this@TenantStatusActivity)
            adapter = tenantAdapter; isNestedScrollingEnabled = false
        }
        binding.fabAddTenant.setOnClickListener {
            startActivity(Intent(this, AddTenantActivity::class.java))
        }
        binding.etSearchTenant.doAfterTextChanged { text ->
            val q = text?.toString()?.trim().orEmpty()
            val filtered = if (q.isBlank()) allTenants else {
                allTenants.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.email.contains(q, ignoreCase = true) ||
                        it.unit_number.contains(q, ignoreCase = true)
                }
            }
            bindTenantStats(filtered)
        }
        binding.bottomNav.selectedItemId = R.id.nav_tenants
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { navigateTabInstant(DashboardActivity::class.java); true }
                R.id.nav_properties -> { navigateTabInstant(PropertyPortfolioActivity::class.java); true }
                R.id.nav_tenants -> true
                R.id.nav_setup -> { navigateTabInstant(SettingsActivity::class.java); true }
                else -> false
            }
        }
        attachTabSwipeNavigation(
            activity = this,
            touchSurface = binding.root,
            onSwipeLeft = { navigateTabInstant(SettingsActivity::class.java) },
            onSwipeRight = { navigateTabInstant(PropertyPortfolioActivity::class.java) }
        )
        viewModel.tenants.observe(this) { tenants ->
            allTenants = tenants
            bindTenantStats(tenants)
        }
        viewModel.loadTenants(SessionManager(this).authToken ?: "")
    }

    private fun bindTenantStats(tenants: List<Tenant>) {
        tenantAdapter.submitList(tenants)
        binding.rvTenants.scheduleLayoutAnimation()
        val dueSoonPending = tenants.count { isDueSoonPending(it) }
        binding.tvPendingCount.text = getString(
            R.string.pending_for_month,
            dueSoonPending,
            resolvePendingMonthLabel()
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTenants(SessionManager(this).authToken ?: "")
    }

    private fun handleRemindOrCall(tenant: Tenant) {
        if (tenant.payment_status.equals(Constants.STATUS_OVERDUE, ignoreCase = true) || isPastDue(tenant)) {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${tenant.phone}")))
            return
        }

        val total = tenant.monthly_rent + tenant.water_bill + tenant.electricity_bill + tenant.trash_bill
        val message = "Hi ${tenant.name}, gentle reminder for upcoming dues. " +
            "Rent: ${tenant.monthly_rent.toCurrency()}, Water: ${tenant.water_bill.toCurrency()}, " +
            "Electricity: ${tenant.electricity_bill.toCurrency()}, Trash: ${tenant.trash_bill.toCurrency()}. " +
            "Total: ${total.toCurrency()}. Please pay by ${tenant.due_date}."
        val url = "https://wa.me/${tenant.phone.replace("+", "")}?text=${Uri.encode(message)}"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            toast("WhatsApp not found")
        }
    }

    private fun markAsPaid(tenant: Tenant) {
        val token = SessionManager(this).authToken.orEmpty()
        if (token.isBlank()) return
        lifecycleScope.launch {
            val repo = TenantRepository()
            val now = LocalDate.now().toString()
            val update = repo.updateTenant(
                token,
                tenant.id,
                mapOf("payment_status" to Constants.STATUS_PAID, "last_payment_date" to now)
            )
            if (update.isSuccess) {
                repo.addPayment(
                    token,
                    Payment(
                        id = UUID.randomUUID().toString(),
                        tenant_id = tenant.id,
                        property_id = tenant.property_id,
                        unit_id = tenant.unit_id,
                        amount = tenant.monthly_rent + tenant.water_bill + tenant.electricity_bill + tenant.trash_bill,
                        rent_amount = tenant.monthly_rent,
                        water_amount = tenant.water_bill,
                        electricity_amount = tenant.electricity_bill,
                        trash_amount = tenant.trash_bill,
                        payment_date = now,
                        month_label = LocalDate.now().month.name.take(3),
                        status = Constants.STATUS_PAID
                    )
                )
                repo.addBillLedgerEntry(
                    token,
                    BillLedgerEntry(
                        id = UUID.randomUUID().toString(),
                        tenant_id = tenant.id,
                        property_id = tenant.property_id,
                        unit_id = tenant.unit_id,
                        period_month = LocalDate.now().withDayOfMonth(1).toString(),
                        due_date = tenant.due_date,
                        rent_amount = tenant.monthly_rent,
                        water_amount = tenant.water_bill,
                        electricity_amount = tenant.electricity_bill,
                        trash_amount = tenant.trash_bill,
                        total_amount = tenant.monthly_rent + tenant.water_bill + tenant.electricity_bill + tenant.trash_bill,
                        status = Constants.STATUS_PAID,
                        paid_on = now
                    )
                )
                toast("Marked paid")
                viewModel.loadTenants(token)
            } else {
                val message = update.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast("Unable to mark paid")
                }
            }
        }
    }

    private fun isPastDue(tenant: Tenant): Boolean {
        if (tenant.payment_status.equals(Constants.STATUS_PAID, ignoreCase = true)) return false
        val dueDate = runCatching { LocalDate.parse(tenant.due_date) }.getOrNull()
        if (dueDate != null) return dueDate.isBefore(LocalDate.now())
        val day = tenant.due_date.toIntOrNull() ?: return false
        val thisMonth = LocalDate.now().withDayOfMonth(day.coerceIn(1, LocalDate.now().lengthOfMonth()))
        return thisMonth.isBefore(LocalDate.now())
    }

    private fun isDueSoonPending(tenant: Tenant): Boolean {
        if (tenant.payment_status.equals(Constants.STATUS_PAID, ignoreCase = true)) return false
        val due = parseDueDate(tenant.due_date) ?: return false
        val daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), due)
        return daysUntilDue in 0..3
    }

    private fun parseDueDate(raw: String): LocalDate? {
        return parseFlexibleDate(raw)
    }

    private fun resolvePendingMonthLabel(): String {
        val session = SessionManager(this)
        val isAdvance = session.collectionCycle == SessionManager.COLLECTION_CYCLE_ADVANCE
        val period = if (isAdvance) YearMonth.now().plusMonths(1) else YearMonth.now()
        val monthName = period.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$monthName ${period.year}"
    }
}
