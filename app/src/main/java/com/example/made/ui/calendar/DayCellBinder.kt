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

    private val dueDates = mutableSetOf<LocalDate>()

    fun setDueDates(dates: Set<LocalDate>) {
        dueDates.clear()
        dueDates.addAll(dates)
    }

    class DayViewContainer(view: View) : ViewContainer(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val vIndicator: View = view.findViewById(R.id.vIndicator)
    }

    override fun create(view: View) = DayViewContainer(view)

    override fun bind(container: DayViewContainer, data: CalendarDay) {
        container.tvDay.text = data.date.dayOfMonth.toString()

        if (data.position == DayPosition.MonthDate) {
            container.tvDay.setTextColor(context.getColor(R.color.colorTextPrimary))

            if (dueDates.contains(data.date)) {
                container.vIndicator.visibility = View.VISIBLE
                container.vIndicator.setBackgroundResource(R.drawable.indicator_dot_due)
            } else {
                container.vIndicator.visibility = View.GONE
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
