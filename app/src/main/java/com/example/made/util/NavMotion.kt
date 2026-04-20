package com.example.made.util

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import com.example.made.R
import com.example.made.ui.dashboard.DashboardActivity
import com.example.made.ui.property.PropertyPortfolioActivity
import com.example.made.ui.settings.SettingsActivity
import com.example.made.ui.tenant.TenantStatusActivity

object NavMotion {

    private fun indexFor(clazz: Class<*>): Int {
        return when (clazz) {
            DashboardActivity::class.java -> 0
            PropertyPortfolioActivity::class.java -> 1
            TenantStatusActivity::class.java -> 2
            SettingsActivity::class.java -> 3
            else -> 4
        }
    }

    fun startWithDirection(activity: Activity, current: Class<*>, target: Class<*>, finishCurrent: Boolean = true) {
        val currentIndex = indexFor(current)
        val targetIndex = indexFor(target)
        val isForward = targetIndex >= currentIndex
        val enter = if (isForward) R.anim.nav_forward_enter else R.anim.nav_back_enter
        val exit = if (isForward) R.anim.nav_forward_exit else R.anim.nav_back_exit

        val intent = Intent(activity, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val options = ActivityOptions.makeCustomAnimation(activity, enter, exit).toBundle()
        activity.startActivity(intent, options)
        if (finishCurrent) activity.finish()
    }
}
