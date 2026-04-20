package com.example.made.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.made.R
import com.example.made.data.model.Tenant
import com.example.made.data.repository.TenantRepository
import com.example.made.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

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
        view.findViewById<TextView>(R.id.tvBottomSheetTitle).text = "Rent Due on $date"

        val rv = view.findViewById<RecyclerView>(R.id.rvRentDue)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = RentDueAdapter()
        rv.adapter = adapter

        val token = SessionManager(requireContext()).authToken.orEmpty()
        if (token.isBlank()) {
            adapter.submit(emptyList())
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            TenantRepository().getTenants(token).onSuccess { tenants ->
                val due = tenants.filter { tenant ->
                    tenant.due_date == date && tenant.payment_status != "paid"
                }
                adapter.submit(due)
            }.onFailure {
                adapter.submit(emptyList())
            }
        }
    }

    private class RentDueAdapter : RecyclerView.Adapter<RentDueAdapter.Holder>() {
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

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(tenant: Tenant) {
                val initials = tenant.name.split(" ").take(2).joinToString("") { it.take(1).uppercase() }
                itemView.findViewById<TextView>(R.id.tvAvatar).text = initials
                itemView.findViewById<TextView>(R.id.tvTenantName).text = tenant.name
                itemView.findViewById<TextView>(R.id.tvUnitInfo).text = "${tenant.unit_number} · ${tenant.property_name}"
                itemView.findViewById<TextView>(R.id.tvRentAmount).text = "$${tenant.monthly_rent.toInt()}"
                itemView.findViewById<TextView>(R.id.tvStatus).apply {
                    text = tenant.payment_status.uppercase()
                    setBackgroundResource(R.drawable.bg_status_pill_pending)
                    setTextColor(context.getColor(R.color.colorWarning))
                }
                itemView.findViewById<View>(R.id.actionButtons).visibility = View.GONE
            }
        }
    }

    override fun getTheme(): Int = R.style.Theme_Made
}
