package com.example.made.ui.property

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.made.R
import com.example.made.data.model.Property
import com.example.made.databinding.ItemPropertyCardBinding

class PropertyAdapter(
    private val onPropertyClick: (Property) -> Unit
) : ListAdapter<Property, PropertyAdapter.PropertyViewHolder>(object : DiffUtil.ItemCallback<Property>() {
    override fun areItemsTheSame(a: Property, b: Property) = a.id == b.id
    override fun areContentsTheSame(a: Property, b: Property) = a == b
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PropertyViewHolder(
        ItemPropertyCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) = holder.bind(getItem(position))

    inner class PropertyViewHolder(private val b: ItemPropertyCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Property) {
            b.tvPropertyName.text = p.name
            b.tvPropertyAddress.text = p.address
            b.tvUnitsCount.text = p.total_units.toString()
            b.tvOccupancyValue.text = "${(p.occupancy_rate * 100).toInt()}%"
            b.tvPropertyStatus.text = p.status.uppercase()

            when (p.status.lowercase()) {
                "active" -> {
                    b.tvPropertyStatus.setBackgroundResource(R.drawable.bg_status_pill_paid)
                    b.tvPropertyStatus.setTextColor(b.root.context.getColor(R.color.colorSuccess))
                }
                "maintenance" -> {
                    b.tvPropertyStatus.setBackgroundResource(R.drawable.bg_status_pill_pending)
                    b.tvPropertyStatus.setTextColor(b.root.context.getColor(R.color.colorWarning))
                }
                else -> {
                    b.tvPropertyStatus.setBackgroundResource(R.drawable.bg_status_pill_verified)
                    b.tvPropertyStatus.setTextColor(b.root.context.getColor(R.color.colorPrimary))
                }
            }
            if (!p.image_url.isNullOrEmpty()) {
                Glide.with(b.root.context).load(p.image_url).centerCrop().into(b.ivPropertyImage)
            }
            b.root.setOnClickListener { onPropertyClick(p) }
        }
    }
}
