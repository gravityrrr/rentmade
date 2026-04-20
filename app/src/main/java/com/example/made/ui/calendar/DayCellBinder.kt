package com.example.made.ui.calendar

import android.content.Context
import android.view.View
import android.widget.TextView
import com.example.made.R
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.LocalDate

class DayCellBinder(
    private val context: Context,
    private val onDayClick: (CalendarDay) -> Unit
) : MonthDayBinder<DayCellBinder.DayViewContainer> {

    // Demo: days with rent due (unpaid) and paid
    private val unpaidDays = setOf(1, 5, 12, 18, 25)
    private val paidDays = setOf(2, 3, 8, 15, 20, 22, 28)

    class DayViewContainer(view: View) : ViewContainer(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val vIndicator: View = view.findViewById(R.id.vIndicator)
    }

    override fun create(view: View) = DayViewContainer(view)

    override fun bind(container: DayViewContainer, data: CalendarDay) {
        container.tvDay.text = data.date.dayOfMonth.toString()

        if (data.position == DayPosition.MonthDate) {
            container.tvDay.setTextColor(context.getColor(R.color.colorTextPrimary))

            val dayOfMonth = data.date.dayOfMonth
            when {
                unpaidDays.contains(dayOfMonth) -> {
                    container.vIndicator.visibility = View.VISIBLE
                    container.vIndicator.setBackgroundResource(R.drawable.indicator_dot_red)
                }
                paidDays.contains(dayOfMonth) -> {
                    container.vIndicator.visibility = View.VISIBLE
                    container.vIndicator.setBackgroundResource(R.drawable.indicator_dot_green)
                }
                else -> {
                    container.vIndicator.visibility = View.GONE
                }
            }

            // Highlight today
            if (data.date == LocalDate.now()) {
                container.tvDay.setBackgroundResource(R.drawable.bg_status_pill_verified)
            } else {
                container.tvDay.background = null
            }

            container.view.setOnClickListener { onDayClick(data) }
        } else {
            container.tvDay.setTextColor(context.getColor(R.color.colorTextDisabled))
            container.vIndicator.visibility = View.GONE
            container.view.setOnClickListener(null)
        }
    }
}
