package com.example.made.ui.tenant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.made.R
import com.example.made.data.model.BillLedgerEntry
import com.example.made.databinding.DialogEditBillLedgerBinding
import com.example.made.data.model.Payment
import com.example.made.data.model.Tenant
import com.example.made.data.repository.DocumentVaultRepository
import com.example.made.data.repository.TenantRepository
import com.example.made.databinding.ActivityTenantDetailsBinding
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.ui.property.PropertyPortfolioActivity
import com.example.made.ui.settings.SettingsActivity
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import com.example.made.util.attachTabSwipeNavigation
import com.example.made.util.handleAuthExpired
import com.example.made.util.navigateTabInstant
import com.example.made.util.toDisplayDateOrSelf
import com.example.made.util.toAmountOrNull
import com.example.made.util.toAmountOrZero
import com.example.made.util.toStorageIsoDateOrSelf
import com.example.made.util.toGroupedNumber
import com.example.made.util.toast
import com.example.made.util.toCurrency
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.UUID

class TenantDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTenantDetailsBinding
    private val paymentAdapter = PaymentHistoryAdapter()
    private lateinit var billLedgerAdapter: BillLedgerAdapter
    private var currentTenant: Tenant? = null
    private val docRepo = DocumentVaultRepository()
    private var signedAadharUrl: String? = null
    private var signedLeaseUrl: String? = null

    private val pickAadhar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadDocument("aadhar", uri)
    }
    private val pickLease = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadDocument("lease", uri)
    }

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
        billLedgerAdapter = BillLedgerAdapter(
            onEdit = { entry -> showEditLedgerDialog(entry) },
            onReverse = { entry -> reverseLedgerEntry(entry) }
        )
        binding.rvBillLedger.apply {
            layoutManager = LinearLayoutManager(this@TenantDetailsActivity)
            adapter = billLedgerAdapter
            isNestedScrollingEnabled = false
        }

        binding.tabLedger.addTab(binding.tabLedger.newTab().setText(getString(R.string.payment_history)))
        binding.tabLedger.addTab(binding.tabLedger.newTab().setText(getString(R.string.bill_ledger)))
        binding.tabLedger.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val showLedger = tab.position == 1
                binding.rvPayments.visibility = if (showLedger) android.view.View.GONE else android.view.View.VISIBLE
                binding.rvBillLedger.visibility = if (showLedger) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        binding.btnSaveTenantMeta.setOnClickListener { saveTenantMeta() }
        binding.btnWhatsappReminder.setOnClickListener { currentTenant?.let { sendWhatsappReminder(it) } }
        binding.btnCallTenant.setOnClickListener { currentTenant?.let { callTenant(it) } }
        binding.btnMarkPaidTenant.setOnClickListener { currentTenant?.let { togglePaymentStatus(it) } }
        binding.btnUploadAadhar.setOnClickListener { pickAadhar.launch("*/*") }
        binding.btnUploadLease.setOnClickListener { pickLease.launch("*/*") }
        binding.btnOpenAadhar.setOnClickListener { openSignedUrl(signedAadharUrl) }
        binding.btnOpenLease.setOnClickListener { openSignedUrl(signedLeaseUrl) }

        setupBottomNav()
        setupSwipeNavigation()

        loadTenantDetails(tenantId)
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_tenants
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
                R.id.nav_setup -> {
                    navigateTabInstant(SettingsActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSwipeNavigation() {
        attachTabSwipeNavigation(
            activity = this,
            touchSurface = binding.root,
            onSwipeLeft = { navigateTabInstant(SettingsActivity::class.java) },
            onSwipeRight = { navigateTabInstant(PropertyPortfolioActivity::class.java) }
        )
    }

    private fun loadTenantDetails(tenantId: String) {
        val sm = SessionManager(this)
        lifecycleScope.launch {
            val repo = TenantRepository()
            repo.getTenantById(sm.authToken ?: "", tenantId).onSuccess { tenant ->
                tenant?.let {
                    currentTenant = it
                    binding.tvTenantName.text = it.name
                    binding.tvEmail.text = it.email
                    binding.tvPhone.text = it.phone
                    binding.tvUnitProperty.text = "${it.unit_number} · ${it.property_name}".uppercase()
                    if (!it.avatar_url.isNullOrBlank()) {
                        Glide.with(this@TenantDetailsActivity)
                            .load(it.avatar_url)
                            .centerCrop()
                            .into(binding.ivTenantAvatar)
                    } else {
                        binding.ivTenantAvatar.setImageResource(R.drawable.ic_tenants)
                    }
                    binding.etAadhar.setText(it.aadhar_number.orEmpty())
                    binding.etWaterBill.setText(it.water_bill.toGroupedNumber())
                    binding.etElectricityBill.setText(it.electricity_bill.toGroupedNumber())
                    binding.etTrashBill.setText(it.trash_bill.toGroupedNumber())
                    binding.etDueDateDetails.setText(it.due_date.toDisplayDateOrSelf())
                    updatePaymentActionButton(it)
                    refreshSignedUrls(it)
                }
            }.onFailure { err ->
                if (!handleAuthExpired(err.message)) {
                    toast("Unable to load tenant details")
                }
            }

            repo.getPaymentsByTenant(sm.authToken ?: "", tenantId).onSuccess {
                paymentAdapter.submitList(it)
                binding.rvPayments.scheduleLayoutAnimation()
            }.onFailure { err ->
                paymentAdapter.submitList(emptyList())
                if (!handleAuthExpired(err.message)) {
                    toast("Unable to load payments")
                }
            }

            repo.getBillLedgerByTenant(sm.authToken ?: "", tenantId).onSuccess {
                billLedgerAdapter.submitList(it)
                binding.rvBillLedger.scheduleLayoutAnimation()
            }.onFailure { err ->
                billLedgerAdapter.submitList(emptyList())
                handleAuthExpired(err.message)
            }
        }
    }

    private fun saveTenantMeta() {
        val tenant = currentTenant ?: return
        val token = SessionManager(this).authToken.orEmpty()
        lifecycleScope.launch {
            val payload = mapOf(
                "aadhar_number" to binding.etAadhar.text?.toString()?.trim().orEmpty(),
                "water_bill" to binding.etWaterBill.text?.toString().toAmountOrZero(),
                "electricity_bill" to binding.etElectricityBill.text?.toString().toAmountOrZero(),
                "trash_bill" to binding.etTrashBill.text?.toString().toAmountOrZero(),
                "due_date" to binding.etDueDateDetails.text?.toString()?.trim().orEmpty().toStorageIsoDateOrSelf()
            )
            val result = TenantRepository().updateTenant(token, tenant.id, payload)
            if (result.isSuccess) {
                toast("Tenant details saved")
                loadTenantDetails(tenant.id)
            } else {
                val message = result.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast("Unable to save details")
                }
            }
        }
    }

    private fun sendWhatsappReminder(tenant: Tenant) {
        val water = binding.etWaterBill.text?.toString().toAmountOrNull() ?: tenant.water_bill
        val electricity = binding.etElectricityBill.text?.toString().toAmountOrNull() ?: tenant.electricity_bill
        val trash = binding.etTrashBill.text?.toString().toAmountOrNull() ?: tenant.trash_bill
        val rent = tenant.monthly_rent
        val total = rent + water + electricity + trash
        val msg = "Hi ${tenant.name}, this is a payment reminder. " +
            "Rent: ${rent.toCurrency()}, Water: ${water.toCurrency()}, Electricity: ${electricity.toCurrency()}, " +
            "Trash: ${trash.toCurrency()}. Total due: ${total.toCurrency()} by ${binding.etDueDateDetails.text?.toString().orEmpty().toDisplayDateOrSelf()}."
        val url = "https://wa.me/${tenant.phone.replace("+", "")}?text=${Uri.encode(msg)}"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            toast("WhatsApp not found")
        }
    }

    private fun callTenant(tenant: Tenant) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${tenant.phone}")))
    }

    private fun togglePaymentStatus(tenant: Tenant) {
        if (tenant.payment_status.equals(Constants.STATUS_PAID, ignoreCase = true)) {
            markUnpaid(tenant)
        } else {
            markPaid(tenant)
        }
    }

    private fun markPaid(tenant: Tenant) {
        val token = SessionManager(this).authToken.orEmpty()
        if (token.isBlank()) return
        val now = LocalDate.now().toString()
        val periodMonth = LocalDate.now().withDayOfMonth(1).toString()
        val water = binding.etWaterBill.text?.toString().toAmountOrNull() ?: tenant.water_bill
        val electricity = binding.etElectricityBill.text?.toString().toAmountOrNull() ?: tenant.electricity_bill
        val trash = binding.etTrashBill.text?.toString().toAmountOrNull() ?: tenant.trash_bill
        val dueDate = binding.etDueDateDetails.text?.toString()?.trim().orEmpty().toStorageIsoDateOrSelf()
        val total = tenant.monthly_rent + water + electricity + trash

        lifecycleScope.launch {
            val repo = TenantRepository()
            val update = repo.updateTenant(token, tenant.id, mapOf(
                "payment_status" to Constants.STATUS_PAID,
                "last_payment_date" to now,
                "water_bill" to water,
                "electricity_bill" to electricity,
                "trash_bill" to trash
            ))
            if (update.isFailure) {
                val message = update.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast("Unable to update payment")
                }
                return@launch
            }

            val ledgerResult = repo.getBillLedgerByTenant(token, tenant.id)
            if (ledgerResult.isFailure) {
                val message = ledgerResult.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast("Unable to sync bill ledger")
                }
                return@launch
            }

            val monthlyEntry = ledgerResult.getOrNull()
                .orEmpty()
                .firstOrNull { normalizeMonthKey(it.period_month) == periodMonth }

            val ledgerSync = if (monthlyEntry == null) {
                repo.addBillLedgerEntry(
                    token,
                    BillLedgerEntry(
                        id = UUID.randomUUID().toString(),
                        tenant_id = tenant.id,
                        property_id = tenant.property_id,
                        unit_id = tenant.unit_id,
                        period_month = periodMonth,
                        due_date = dueDate,
                        rent_amount = tenant.monthly_rent,
                        water_amount = water,
                        electricity_amount = electricity,
                        trash_amount = trash,
                        total_amount = total,
                        status = Constants.STATUS_PAID,
                        paid_on = now
                    )
                ).isSuccess
            } else {
                repo.updateBillLedgerEntry(
                    token,
                    monthlyEntry.id,
                    mapOf(
                        "due_date" to dueDate,
                        "rent_amount" to tenant.monthly_rent,
                        "water_amount" to water,
                        "electricity_amount" to electricity,
                        "trash_amount" to trash,
                        "total_amount" to total,
                        "status" to Constants.STATUS_PAID,
                        "paid_on" to now
                    )
                ).isSuccess
            }

            if (!ledgerSync) {
                toast("Payment saved, but ledger sync failed")
                loadTenantDetails(tenant.id)
                return@launch
            }

            val paymentsResult = repo.getPaymentsByTenant(token, tenant.id)
            val hasPaymentForThisMonth = paymentsResult.getOrNull().orEmpty().any {
                parseYearMonth(it.payment_date) == YearMonth.now()
            }
            if (!hasPaymentForThisMonth) {
                repo.addPayment(
                    token,
                    Payment(
                        id = UUID.randomUUID().toString(),
                        tenant_id = tenant.id,
                        property_id = tenant.property_id,
                        unit_id = tenant.unit_id,
                        amount = total,
                        rent_amount = tenant.monthly_rent,
                        water_amount = water,
                        electricity_amount = electricity,
                        trash_amount = trash,
                        payment_date = now,
                        month_label = LocalDate.now().month.name.take(3),
                        status = Constants.STATUS_PAID
                    )
                )
            }

            toast("Marked as paid")
            loadTenantDetails(tenant.id)
        }
    }

    private fun markUnpaid(tenant: Tenant) {
        val token = SessionManager(this).authToken.orEmpty()
        if (token.isBlank()) return
        val periodMonth = LocalDate.now().withDayOfMonth(1).toString()

        lifecycleScope.launch {
            val repo = TenantRepository()
            val update = repo.updateTenant(
                token,
                tenant.id,
                mapOf(
                    "payment_status" to Constants.STATUS_PENDING,
                    "last_payment_date" to null
                )
            )
            if (update.isFailure) {
                val message = update.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast("Unable to mark unpaid")
                }
                return@launch
            }

            repo.getBillLedgerByTenant(token, tenant.id).getOrNull()
                .orEmpty()
                .firstOrNull { normalizeMonthKey(it.period_month) == periodMonth }
                ?.let { entry ->
                    repo.updateBillLedgerEntry(
                        token,
                        entry.id,
                        mapOf(
                            "status" to Constants.STATUS_PENDING,
                            "paid_on" to null
                        )
                    )
                }

            toast("Marked as unpaid")
            loadTenantDetails(tenant.id)
        }
    }

    private fun uploadDocument(type: String, uri: Uri) {
        val tenant = currentTenant ?: return
        val session = SessionManager(this)
        val token = session.authToken.orEmpty()
        val userId = session.userId.orEmpty()
        if (token.isBlank() || userId.isBlank()) return

        lifecycleScope.launch {
            val uploadedPath = docRepo.uploadTenantDocument(
                context = this@TenantDetailsActivity,
                token = token,
                userId = userId,
                tenantId = tenant.id,
                type = type,
                fileUri = uri
            )
            if (uploadedPath.isFailure) {
                val message = uploadedPath.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast(message ?: "Upload failed")
                }
                return@launch
            }
            val path = uploadedPath.getOrNull().orEmpty()
            val payload = if (type == "aadhar") {
                mapOf("aadhar_path" to path)
            } else {
                mapOf("lease_agreement_path" to path)
            }
            val updated = TenantRepository().updateTenant(token, tenant.id, payload)
            if (updated.isSuccess) {
                toast("Document uploaded")
                loadTenantDetails(tenant.id)
            } else {
                val message = updated.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast("Could not save document link")
                }
            }
        }
    }

    private fun refreshSignedUrls(tenant: Tenant) {
        val token = SessionManager(this).authToken.orEmpty()
        lifecycleScope.launch {
            signedAadharUrl = null
            signedLeaseUrl = null
            tenant.aadhar_path?.takeIf { it.isNotBlank() }?.let {
                signedAadharUrl = docRepo.createSignedUrl(token, it).getOrNull()
            }
            tenant.lease_agreement_path?.takeIf { it.isNotBlank() }?.let {
                signedLeaseUrl = docRepo.createSignedUrl(token, it).getOrNull()
            }

            val hasAadhar = !signedAadharUrl.isNullOrBlank()
            val hasLease = !signedLeaseUrl.isNullOrBlank()
            binding.tvAadharDocStatus.text = if (hasAadhar) "Uploaded" else "Not uploaded"
            binding.tvLeaseDocStatus.text = if (hasLease) "Uploaded" else "Not uploaded"
            binding.btnOpenAadhar.visibility = if (hasAadhar) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnOpenLease.visibility = if (hasLease) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun openSignedUrl(url: String?) {
        if (url.isNullOrBlank()) {
            toast("No document available")
            return
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun showEditLedgerDialog(entry: BillLedgerEntry) {
        val d = DialogEditBillLedgerBinding.inflate(layoutInflater)
        d.etDueDate.setText(entry.due_date)
        d.etRent.setText(entry.rent_amount.toGroupedNumber())
        d.etWater.setText(entry.water_amount.toGroupedNumber())
        d.etElectricity.setText(entry.electricity_amount.toGroupedNumber())
        d.etTrash.setText(entry.trash_amount.toGroupedNumber())

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_ledger_entry))
            .setView(d.root)
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .setPositiveButton(getString(R.string.btn_save_property)) { _, _ ->
                val rent = d.etRent.text?.toString().toAmountOrZero()
                val water = d.etWater.text?.toString().toAmountOrZero()
                val electricity = d.etElectricity.text?.toString().toAmountOrZero()
                val trash = d.etTrash.text?.toString().toAmountOrZero()
                val dueDate = d.etDueDate.text?.toString()?.trim().orEmpty()
                val total = rent + water + electricity + trash
                val payload = mapOf(
                    "due_date" to dueDate,
                    "rent_amount" to rent,
                    "water_amount" to water,
                    "electricity_amount" to electricity,
                    "trash_amount" to trash,
                    "total_amount" to total
                )
                lifecycleScope.launch {
                    val token = SessionManager(this@TenantDetailsActivity).authToken.orEmpty()
                    val updated = TenantRepository().updateBillLedgerEntry(token, entry.id, payload)
                    if (updated.isSuccess) {
                        toast("Ledger updated")
                        currentTenant?.let { loadTenantDetails(it.id) }
                    } else {
                        val message = updated.exceptionOrNull()?.message
                        if (!handleAuthExpired(message)) {
                            toast("Unable to update ledger")
                        }
                    }
                }
            }
            .show()
    }

    private fun reverseLedgerEntry(entry: BillLedgerEntry) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.reverse_paid_entry))
            .setMessage("This will move the ledger row back to pending.")
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .setPositiveButton("Reverse") { _, _ ->
                lifecycleScope.launch {
                    val token = SessionManager(this@TenantDetailsActivity).authToken.orEmpty()
                    val updated = TenantRepository().updateBillLedgerEntry(
                        token,
                        entry.id,
                        mapOf("status" to Constants.STATUS_PENDING, "paid_on" to null)
                    )
                    if (updated.isSuccess) {
                        currentTenant?.let { tenant ->
                            if (normalizeMonthKey(entry.period_month) == LocalDate.now().withDayOfMonth(1).toString()) {
                                TenantRepository().updateTenant(
                                    token,
                                    tenant.id,
                                    mapOf(
                                        "payment_status" to Constants.STATUS_PENDING,
                                        "last_payment_date" to null
                                    )
                                )
                            }
                            toast("Entry reversed")
                            loadTenantDetails(tenant.id)
                        }
                    } else {
                        val message = updated.exceptionOrNull()?.message
                        if (!handleAuthExpired(message)) {
                            toast("Unable to reverse entry")
                        }
                    }
                }
            }
            .show()
    }

    private fun updatePaymentActionButton(tenant: Tenant) {
        val isPaid = tenant.payment_status.equals(Constants.STATUS_PAID, ignoreCase = true)
        binding.btnMarkPaidTenant.text = if (isPaid) "Mark Unpaid" else "Mark Paid"
    }

    private fun parseYearMonth(value: String): YearMonth? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        return try {
            when {
                trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-' -> YearMonth.from(LocalDate.parse(trimmed.take(10)))
                trimmed.length >= 7 && trimmed[4] == '-' -> YearMonth.parse(trimmed.take(7))
                else -> null
            }
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun normalizeMonthKey(value: String): String? {
        return parseYearMonth(value)?.atDay(1)?.toString()
    }
}
