package com.example.made.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.made.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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

        // Demo data for the bottom sheet
        val rv = view.findViewById<RecyclerView>(R.id.rvRentDue)
        rv.layoutManager = LinearLayoutManager(requireContext())

        val demoItems = listOf(
            Triple("James Smith", "Apt 4B · The Grand", "$2,500"),
            Triple("Sarah Connor", "Unit 12 · Riverside", "$3,200"),
            Triple("Julian Davis", "Apt 402 · The Aria", "$2,450")
        )

        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val item = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_tenant_card, parent, false)
                return object : RecyclerView.ViewHolder(item) {}
            }
            override fun getItemCount() = demoItems.size
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val (name, unit, amount) = demoItems[position]
                holder.itemView.findViewById<TextView>(R.id.tvTenantName).text = name
                holder.itemView.findViewById<TextView>(R.id.tvUnitInfo).text = unit
                holder.itemView.findViewById<TextView>(R.id.tvRentAmount).text = amount
                val initials = name.split(" ").take(2).joinToString("") { it.first().uppercase() }
                holder.itemView.findViewById<TextView>(R.id.tvAvatar).text = initials
                holder.itemView.findViewById<TextView>(R.id.tvStatus).apply {
                    text = "PENDING"
                    setBackgroundResource(R.drawable.bg_status_pill_pending)
                    setTextColor(context.getColor(R.color.colorWarning))
                }
                holder.itemView.findViewById<View>(R.id.actionButtons).visibility = View.GONE
            }
        }
    }

    override fun getTheme(): Int = R.style.Theme_Made
}
