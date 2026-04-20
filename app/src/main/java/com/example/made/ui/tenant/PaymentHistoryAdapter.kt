package com.example.made.ui.tenant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.made.R
import com.example.made.data.model.Payment
import com.example.made.databinding.ItemPaymentHistoryBinding
import com.example.made.util.toCurrency

class PaymentHistoryAdapter : ListAdapter<Payment, PaymentHistoryAdapter.PaymentViewHolder>(
    object : DiffUtil.ItemCallback<Payment>() {
        override fun areItemsTheSame(a: Payment, b: Payment) = a.id == b.id
        override fun areContentsTheSame(a: Payment, b: Payment) = a == b
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PaymentViewHolder(
        ItemPaymentHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) = holder.bind(getItem(position))

    inner class PaymentViewHolder(private val b: ItemPaymentHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Payment) {
            b.tvPaymentMonth.text = "${p.month_label} Rent"
            b.tvPaymentDate.text = p.payment_date
            b.tvPaymentAmount.text = p.amount.toCurrency()
            b.tvPaymentStatus.text = p.status.uppercase()
            b.tvPaymentStatus.setBackgroundResource(R.drawable.bg_status_pill_paid)
            b.tvPaymentStatus.setTextColor(b.root.context.getColor(R.color.colorSuccess))
        }
    }
}
