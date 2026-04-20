package com.example.made.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.made.R
import com.example.made.data.model.UserProfile
import com.google.android.material.button.MaterialButton

class AdminUsersAdapter(
    private val onToggleRole: (UserProfile) -> Unit,
    private val onToggleActive: (UserProfile) -> Unit
) : RecyclerView.Adapter<AdminUsersAdapter.Holder>() {

    private val items = mutableListOf<UserProfile>()

    fun submit(list: List<UserProfile>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position], onToggleRole, onToggleActive)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val email = itemView.findViewById<TextView>(R.id.tvEmail)
        private val meta = itemView.findViewById<TextView>(R.id.tvMeta)
        private val toggleRole = itemView.findViewById<MaterialButton>(R.id.btnToggleRole)
        private val toggleActive = itemView.findViewById<MaterialButton>(R.id.btnToggleActive)

        fun bind(
            profile: UserProfile,
            onToggleRole: (UserProfile) -> Unit,
            onToggleActive: (UserProfile) -> Unit
        ) {
            email.text = profile.email
            meta.text = "role=${profile.role} · active=${profile.is_active}"
            toggleRole.text = if (profile.role == "admin") "Set Landlord" else "Set Admin"
            toggleActive.text = if (profile.is_active) "Disable" else "Enable"
            toggleRole.setOnClickListener { onToggleRole(profile) }
            toggleActive.setOnClickListener { onToggleActive(profile) }
        }
    }
}
