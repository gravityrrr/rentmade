package com.example.made.ui.tenant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.made.R
import com.example.made.data.model.BillLedgerEntry
import com.example.made.databinding.ItemBillLedgerBinding
import com.example.made.util.toDisplayDateOrSelf
import com.example.made.util.toCurrency

class BillLedgerAdapter(
    private val onEdit: (BillLedgerEntry) -> Unit,
    private val onReverse: (BillLedgerEntry) -> Unit
) : ListAdapter<BillLedgerEntry, BillLedgerAdapter.LedgerVH>(object : DiffUtil.ItemCallback<BillLedgerEntry>() {
    override fun areItemsTheSame(oldItem: BillLedgerEntry, newItem: BillLedgerEntry): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: BillLedgerEntry, newItem: BillLedgerEntry): Boolean = oldItem == newItem
}) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerVH {
        return LedgerVH(ItemBillLedgerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: LedgerVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LedgerVH(private val b: ItemBillLedgerBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: BillLedgerEntry) {
            b.tvPeriod.text = item.period_month.toDisplayDateOrSelf()
            b.tvDueDate.text = "Due: ${item.due_date.toDisplayDateOrSelf()}"
            b.tvSplit.text = "Rent ${item.rent_amount.toCurrency()} • Water ${item.water_amount.toCurrency()} • " +
                "Electricity ${item.electricity_amount.toCurrency()} • Trash ${item.trash_amount.toCurrency()}"
            b.tvTotal.text = "Total ${item.total_amount.toCurrency()}"

            when (item.status.lowercase()) {
                "paid" -> {
                    b.tvLedgerStatus.text = "PAID"
                    b.tvLedgerStatus.setBackgroundResource(R.drawable.bg_status_pill_paid)
                    b.tvLedgerStatus.setTextColor(b.root.context.getColor(R.color.colorSuccess))
                    b.btnReverseLedger.visibility = View.VISIBLE
                }
                "overdue" -> {
                    b.tvLedgerStatus.text = "OVERDUE"
                    b.tvLedgerStatus.setBackgroundResource(R.drawable.bg_status_pill_overdue)
                    b.tvLedgerStatus.setTextColor(b.root.context.getColor(R.color.colorError))
                    b.btnReverseLedger.visibility = View.GONE
                }
                else -> {
                    b.tvLedgerStatus.text = "PENDING"
                    b.tvLedgerStatus.setBackgroundResource(R.drawable.bg_status_pill_pending)
                    b.tvLedgerStatus.setTextColor(b.root.context.getColor(R.color.colorWarning))
                    b.btnReverseLedger.visibility = View.GONE
                }
            }

            b.btnEditLedger.setOnClickListener { onEdit(item) }
            b.btnReverseLedger.setOnClickListener { onReverse(item) }
        }
    }
}
