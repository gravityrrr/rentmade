package com.example.made.ui.tenant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.made.R
import com.example.made.data.model.Tenant
import com.example.made.databinding.ItemTenantCardBinding
import com.example.made.util.Constants
import com.example.made.util.toDisplayDateOrSelf
import com.example.made.util.toCurrency

class TenantAdapter(
    private val onTenantClick: (Tenant) -> Unit,
    private val onMarkPaid: ((Tenant) -> Unit)? = null,
    private val onEdit: ((Tenant) -> Unit)? = null,
    private val onRemind: ((Tenant) -> Unit)? = null,
    private val showActions: Boolean = true,
    private val showMarkPaid: Boolean = true
) : ListAdapter<Tenant, TenantAdapter.TenantViewHolder>(object : DiffUtil.ItemCallback<Tenant>() {
    override fun areItemsTheSame(a: Tenant, b: Tenant) = a.id == b.id
    override fun areContentsTheSame(a: Tenant, b: Tenant) = a == b
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TenantViewHolder(
        ItemTenantCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: TenantViewHolder, position: Int) = holder.bind(getItem(position))

    inner class TenantViewHolder(private val b: ItemTenantCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(t: Tenant) {
            val initials = t.name.split(" ").take(2).joinToString("") { it.first().uppercase() }
            b.tvAvatar.text = initials
            b.tvTenantName.text = t.name
            b.tvUnitInfo.text = "${t.unit_number} · ${t.property_name}"
            b.tvMetaInfo.text = "Due ${t.due_date.toDisplayDateOrSelf()} · ${t.phone}"
            b.tvRentAmount.text = t.monthly_rent.toCurrency()
            b.btnRemind.setTextColor(b.root.context.getColor(R.color.colorPrimary))
            b.btnRemind.text = b.root.context.getString(R.string.edit)

            if (!showActions) {
                b.actionButtons.visibility = View.GONE
            }

            b.btnMarkPaid.visibility = if (showMarkPaid) View.VISIBLE else View.GONE

            when (t.payment_status.lowercase()) {
                Constants.STATUS_PAID -> {
                    b.tvStatus.text = b.root.context.getString(R.string.status_paid)
                    b.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_paid)
                    b.tvStatus.setTextColor(b.root.context.getColor(R.color.colorSuccess))
                    b.actionButtons.visibility = if (showActions) View.VISIBLE else View.GONE
                }
                Constants.STATUS_PENDING -> {
                    b.tvStatus.text = b.root.context.getString(R.string.status_pending)
                    b.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_pending)
                    b.tvStatus.setTextColor(b.root.context.getColor(R.color.colorWarning))
                    b.actionButtons.visibility = if (showActions) View.VISIBLE else View.GONE
                }
                else -> {
                    b.tvStatus.text = b.root.context.getString(R.string.status_overdue)
                    b.tvStatus.setBackgroundResource(R.drawable.bg_status_pill_overdue)
                    b.tvStatus.setTextColor(b.root.context.getColor(R.color.colorError))
                    b.actionButtons.visibility = if (showActions) View.VISIBLE else View.GONE
                }
            }
            b.root.setOnClickListener { onTenantClick(t) }
            b.btnMarkPaid.setOnClickListener { onMarkPaid?.invoke(t) }
            b.btnRemind.setOnClickListener {
                if (onEdit != null) onEdit.invoke(t) else onRemind?.invoke(t)
            }
        }
    }
}
