package com.example.made.ui.calendar

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.made.R
import com.example.made.data.model.BillLedgerEntry
import com.example.made.data.model.Payment
import com.example.made.data.model.Tenant
import com.example.made.data.repository.TenantRepository
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import com.example.made.util.parseFlexibleDate
import com.example.made.util.toDisplayDateOrSelf
import com.example.made.util.toCurrency
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.UUID

class RentDueBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_DATE = "arg_date"
        fun newInstance(date: String): RentDueBottomSheetFragment {
            return RentDueBottomSheetFragment().apply {
                arguments = Bundle().apply { putString(ARG_DATE, date) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rent_due_bottom_sheet, c, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val date = arguments?.getString(ARG_DATE) ?: ""
        view.findViewById<TextView>(R.id.tvBottomSheetTitle).text = "Rent Due on ${date.toDisplayDateOrSelf()}"

        val rv = view.findViewById<RecyclerView>(R.id.rvRentDue)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = RentDueAdapter(
            onMarkPaid = { tenant -> markAsPaid(tenant) },
            onRemind = { tenant -> remindOrCall(tenant) }
        )
        rv.adapter = adapter

        val token = SessionManager(requireContext()).authToken.orEmpty()
        if (token.isBlank()) {
            adapter.submit(emptyList())
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            TenantRepository().getTenants(token).onSuccess { tenants ->
                val selectedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                val due = tenants.filter { tenant ->
                    if (tenant.payment_status == Constants.STATUS_PAID) {
                        false
                    } else {
                        tenant.due_date == date ||
                            (selectedDate != null && tenant.due_date.toIntOrNull() == selectedDate.dayOfMonth)
                    }
                }
                adapter.submit(due)
            }.onFailure {
                adapter.submit(emptyList())
            }
        }
    }

    private fun remindOrCall(tenant: Tenant) {
        if (tenant.payment_status.equals(Constants.STATUS_OVERDUE, ignoreCase = true) || isPastDue(tenant)) {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${tenant.phone}")))
            return
        }
        val total = tenant.monthly_rent + tenant.water_bill + tenant.electricity_bill + tenant.trash_bill
        val message = "Hi ${tenant.name}, reminder for upcoming dues. " +
            "Rent: ${tenant.monthly_rent.toCurrency()}, Water: ${tenant.water_bill.toCurrency()}, " +
            "Electricity: ${tenant.electricity_bill.toCurrency()}, Trash: ${tenant.trash_bill.toCurrency()}. " +
            "Total: ${total.toCurrency()}."
        val url = "https://wa.me/${tenant.phone.replace("+", "")}?text=${Uri.encode(message)}"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun markAsPaid(tenant: Tenant) {
        val token = SessionManager(requireContext()).authToken.orEmpty()
        if (token.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val repo = TenantRepository()
            val now = LocalDate.now().toString()
            val currentMonthKey = LocalDate.now().withDayOfMonth(1).toString()
            val updated = repo.updateTenant(token, tenant.id, mapOf(
                "payment_status" to Constants.STATUS_PAID,
                "last_payment_date" to now
            ))
            if (updated.isSuccess) {
                val total = tenant.monthly_rent + tenant.water_bill + tenant.electricity_bill + tenant.trash_bill

                val monthlyLedger = repo.getBillLedgerByTenant(token, tenant.id).getOrNull().orEmpty()
                    .firstOrNull { normalizeMonthKey(it.period_month) == currentMonthKey }
                if (monthlyLedger == null) {
                    repo.addBillLedgerEntry(
                        token,
                        BillLedgerEntry(
                            id = UUID.randomUUID().toString(),
                            tenant_id = tenant.id,
                            property_id = tenant.property_id,
                            unit_id = tenant.unit_id,
                            period_month = currentMonthKey,
                            due_date = tenant.due_date,
                            rent_amount = tenant.monthly_rent,
                            water_amount = tenant.water_bill,
                            electricity_amount = tenant.electricity_bill,
                            trash_amount = tenant.trash_bill,
                            total_amount = total,
                            status = Constants.STATUS_PAID,
                            paid_on = now
                        )
                    )
                } else {
                    repo.updateBillLedgerEntry(
                        token,
                        monthlyLedger.id,
                        mapOf("status" to Constants.STATUS_PAID, "paid_on" to now)
                    )
                }

                val hasPaymentForMonth = repo.getPaymentsByTenant(token, tenant.id).getOrNull().orEmpty().any {
                    parseYearMonth(it.payment_date) == YearMonth.now()
                }
                if (!hasPaymentForMonth) {
                    repo.addPayment(
                        token,
                        Payment(
                            id = UUID.randomUUID().toString(),
                            tenant_id = tenant.id,
                            property_id = tenant.property_id,
                            unit_id = tenant.unit_id,
                            amount = total,
                            rent_amount = tenant.monthly_rent,
                            water_amount = tenant.water_bill,
                            electricity_amount = tenant.electricity_bill,
                            trash_amount = tenant.trash_bill,
                            payment_date = now,
                            month_label = LocalDate.now().month.name.take(3),
                            status = Constants.STATUS_PAID
                        )
                    )
                }
            }
        }
    }

    private fun isPastDue(tenant: Tenant): Boolean {
        if (tenant.payment_status.equals(Constants.STATUS_PAID, ignoreCase = true)) return false
        val dueDate = parseFlexibleDate(tenant.due_date)
        if (dueDate != null) return dueDate.isBefore(LocalDate.now())
        val day = tenant.due_date.toIntOrNull() ?: return false
        val thisMonth = LocalDate.now().withDayOfMonth(day.coerceIn(1, LocalDate.now().lengthOfMonth()))
        return thisMonth.isBefore(LocalDate.now())
    }

    private class RentDueAdapter(
        private val onMarkPaid: (Tenant) -> Unit,
        private val onRemind: (Tenant) -> Unit
    ) : RecyclerView.Adapter<RentDueAdapter.Holder>() {
        private val items = mutableListOf<Tenant>()

        fun submit(data: List<Tenant>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val item = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tenant_card, parent, false)
            return Holder(item)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(items[position])
        }

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(tenant: Tenant) {
                val initials = tenant.name.split(" ").take(2).joinToString("") { it.take(1).uppercase() }
                itemView.findViewById<TextView>(R.id.tvAvatar).text = initials
                itemView.findViewById<TextView>(R.id.tvTenantName).text = tenant.name
                itemView.findViewById<TextView>(R.id.tvUnitInfo).text = "${tenant.unit_number} · ${tenant.property_name}"
                itemView.findViewById<TextView>(R.id.tvRentAmount).text = tenant.monthly_rent.toCurrency()
                itemView.findViewById<TextView>(R.id.tvStatus).apply {
                    text = tenant.payment_status.uppercase()
                    setBackgroundResource(R.drawable.bg_status_pill_pending)
                    setTextColor(context.getColor(R.color.colorWarning))
                }
                itemView.findViewById<View>(R.id.actionButtons).visibility = View.VISIBLE
                itemView.findViewById<View>(R.id.btnMarkPaid).setOnClickListener { onMarkPaid(tenant) }
                itemView.findViewById<View>(R.id.btnRemind).setOnClickListener { onRemind(tenant) }
            }
        }
    }

    override fun getTheme(): Int = R.style.Theme_Made

    private fun parseYearMonth(value: String): YearMonth? {
        val parsed = parseFlexibleDate(value)
        if (parsed != null) return YearMonth.from(parsed)
        return try {
            val trimmed = value.trim()
            if (trimmed.length >= 7 && trimmed[4] == '-') YearMonth.parse(trimmed.take(7)) else null
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun normalizeMonthKey(value: String): String? = parseYearMonth(value)?.atDay(1)?.toString()
}
