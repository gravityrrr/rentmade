package com.example.made.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.made.data.remote.RetrofitClient
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(applicationContext)
        val token = sessionManager.authToken ?: return Result.failure()

        return try {
            val tenants = RetrofitClient.api.getTenants("Bearer $token")
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE

            tenants.forEachIndexed { index, tenant ->
                if (tenant.payment_status != Constants.STATUS_PAID && tenant.due_date.isNotEmpty()) {
                    try {
                        val dueDate = LocalDate.parse(tenant.due_date, formatter)
                        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toInt()

                        if (daysUntilDue in -7..3) {
                            NotificationHelper.showRentReminder(
                                context = applicationContext,
                                notificationId = Constants.NOTIFICATION_ID_BASE + index,
                                tenantName = tenant.name,
                                amount = "$${tenant.monthly_rent}",
                                daysUntilDue = daysUntilDue,
                                unitNumber = tenant.unit_number,
                                phone = tenant.phone
                            )
                        }
                    } catch (e: Exception) {
                        // Skip tenants with invalid dates
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
