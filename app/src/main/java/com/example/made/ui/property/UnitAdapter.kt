package com.example.made.ui.property

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.made.data.model.Tenant
import com.example.made.data.model.Unit as RentalUnit
import com.example.made.databinding.ItemUnitCardBinding

class UnitAdapter(
    private val onEdit: (RentalUnit) -> kotlin.Unit,
    private val onAddTenant: (RentalUnit) -> kotlin.Unit
) : ListAdapter<RentalUnit, UnitAdapter.UnitVH>(object : DiffUtil.ItemCallback<RentalUnit>() {
    override fun areItemsTheSame(oldItem: RentalUnit, newItem: RentalUnit): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: RentalUnit, newItem: RentalUnit): Boolean = oldItem == newItem
}) {

    private var tenantMap: Map<String, Tenant> = emptyMap()

    fun submitTenants(tenants: List<Tenant>) {
        tenantMap = tenants.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitVH {
        return UnitVH(ItemUnitCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: UnitVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UnitVH(private val b: ItemUnitCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(unit: RentalUnit) {
            b.tvDoorNumber.text = "Door ${unit.door_number}"
            b.tvAppliances.text = "Fans ${unit.fan_count} • Geysers ${unit.geyser_count}"
            val tenantName = unit.tenant_id?.let { tenantMap[it]?.name }
            b.tvTenant.text = if (tenantName.isNullOrBlank()) "Tenant: Unassigned" else "Tenant: $tenantName"
            b.btnEditUnit.setOnClickListener { onEdit(unit) }
            b.btnAddTenant.setOnClickListener { onAddTenant(unit) }
        }
    }
}
