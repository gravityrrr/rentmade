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
import com.example.made.R
import com.example.made.data.model.BillLedgerEntry
import com.example.made.databinding.DialogEditBillLedgerBinding
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.example.made.data.model.Payment
import com.example.made.data.model.Tenant
import com.example.made.data.repository.DocumentVaultRepository
import com.example.made.data.repository.TenantRepository
import com.example.made.databinding.ActivityTenantDetailsBinding
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import com.example.made.util.toast
import com.example.made.util.toCurrency
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
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
        binding.btnMarkPaidTenant.setOnClickListener { currentTenant?.let { markPaid(it) } }
        binding.btnUploadAadhar.setOnClickListener { pickAadhar.launch("application/pdf") }
        binding.btnUploadLease.setOnClickListener { pickLease.launch("application/pdf") }
        binding.btnOpenAadhar.setOnClickListener { openSignedUrl(signedAadharUrl) }
        binding.btnOpenLease.setOnClickListener { openSignedUrl(signedLeaseUrl) }

        loadTenantDetails(tenantId)
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
                    binding.etAadhar.setText(it.aadhar_number.orEmpty())
                    binding.etWaterBill.setText(it.water_bill.toString())
                    binding.etElectricityBill.setText(it.electricity_bill.toString())
                    binding.etTrashBill.setText(it.trash_bill.toString())
                    binding.etDueDateDetails.setText(it.due_date)
                    refreshSignedUrls(it)
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

            repo.getBillLedgerByTenant(sm.authToken ?: "", tenantId).onSuccess {
                billLedgerAdapter.submitList(it)
                binding.rvBillLedger.scheduleLayoutAnimation()
            }.onFailure {
                billLedgerAdapter.submitList(emptyList())
            }
        }
    }

    private fun setupWindowTransitions() {
        window.enterTransition = MaterialFadeThrough().apply { duration = 260L }
        window.returnTransition = MaterialFadeThrough().apply { duration = 220L }
    }

    private fun saveTenantMeta() {
        val tenant = currentTenant ?: return
        val token = SessionManager(this).authToken.orEmpty()
        lifecycleScope.launch {
            val payload = mapOf(
                "aadhar_number" to binding.etAadhar.text?.toString()?.trim().orEmpty(),
                "water_bill" to (binding.etWaterBill.text?.toString()?.toDoubleOrNull() ?: 0.0),
                "electricity_bill" to (binding.etElectricityBill.text?.toString()?.toDoubleOrNull() ?: 0.0),
                "trash_bill" to (binding.etTrashBill.text?.toString()?.toDoubleOrNull() ?: 0.0),
                "due_date" to binding.etDueDateDetails.text?.toString()?.trim().orEmpty()
            )
            val result = TenantRepository().updateTenant(token, tenant.id, payload)
            if (result.isSuccess) {
                toast("Tenant details saved")
                loadTenantDetails(tenant.id)
            } else {
                toast("Unable to save details")
            }
        }
    }

    private fun sendWhatsappReminder(tenant: Tenant) {
        val water = binding.etWaterBill.text?.toString()?.toDoubleOrNull() ?: tenant.water_bill
        val electricity = binding.etElectricityBill.text?.toString()?.toDoubleOrNull() ?: tenant.electricity_bill
        val trash = binding.etTrashBill.text?.toString()?.toDoubleOrNull() ?: tenant.trash_bill
        val rent = tenant.monthly_rent
        val total = rent + water + electricity + trash
        val msg = "Hi ${tenant.name}, this is a payment reminder. " +
            "Rent: ${rent.toCurrency()}, Water: ${water.toCurrency()}, Electricity: ${electricity.toCurrency()}, " +
            "Trash: ${trash.toCurrency()}. Total due: ${total.toCurrency()} by ${binding.etDueDateDetails.text}."
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

    private fun markPaid(tenant: Tenant) {
        val token = SessionManager(this).authToken.orEmpty()
        val now = LocalDate.now().toString()
        val water = binding.etWaterBill.text?.toString()?.toDoubleOrNull() ?: tenant.water_bill
        val electricity = binding.etElectricityBill.text?.toString()?.toDoubleOrNull() ?: tenant.electricity_bill
        val trash = binding.etTrashBill.text?.toString()?.toDoubleOrNull() ?: tenant.trash_bill
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
            if (update.isSuccess) {
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
                repo.addBillLedgerEntry(
                    token,
                    BillLedgerEntry(
                        id = UUID.randomUUID().toString(),
                        tenant_id = tenant.id,
                        property_id = tenant.property_id,
                        unit_id = tenant.unit_id,
                        period_month = LocalDate.now().withDayOfMonth(1).toString(),
                        due_date = binding.etDueDateDetails.text?.toString().orEmpty(),
                        rent_amount = tenant.monthly_rent,
                        water_amount = water,
                        electricity_amount = electricity,
                        trash_amount = trash,
                        total_amount = total,
                        status = Constants.STATUS_PAID,
                        paid_on = now
                    )
                )
                toast("Payment updated")
                loadTenantDetails(tenant.id)
            } else {
                toast("Unable to update payment")
            }
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
                toast("Upload failed")
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
                toast("Could not save document link")
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
        d.etRent.setText(entry.rent_amount.toString())
        d.etWater.setText(entry.water_amount.toString())
        d.etElectricity.setText(entry.electricity_amount.toString())
        d.etTrash.setText(entry.trash_amount.toString())

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_ledger_entry))
            .setView(d.root)
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .setPositiveButton(getString(R.string.btn_save_property)) { _, _ ->
                val rent = d.etRent.text?.toString()?.toDoubleOrNull() ?: 0.0
                val water = d.etWater.text?.toString()?.toDoubleOrNull() ?: 0.0
                val electricity = d.etElectricity.text?.toString()?.toDoubleOrNull() ?: 0.0
                val trash = d.etTrash.text?.toString()?.toDoubleOrNull() ?: 0.0
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
                        toast("Unable to update ledger")
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
                        toast("Entry reversed")
                        currentTenant?.let { loadTenantDetails(it.id) }
                    } else {
                        toast("Unable to reverse entry")
                    }
                }
            }
            .show()
    }
}
