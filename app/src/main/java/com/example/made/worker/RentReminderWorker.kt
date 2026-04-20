package com.example.made.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.made.data.remote.RetrofitClient
import com.example.made.data.repository.SettingsRepository
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
        val userId = sessionManager.userId.orEmpty()

        return try {
            val settings = SettingsRepository().getSettings(token, userId).getOrNull()
            val graceDays = settings?.grace_days ?: sessionManager.graceDays
            val autoOverdueEnabled = settings?.auto_overdue_enabled ?: sessionManager.autoOverdueEnabled
            sessionManager.graceDays = graceDays
            sessionManager.autoOverdueEnabled = autoOverdueEnabled

            val tenants = RetrofitClient.api.getTenants("Bearer $token")
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE

            tenants.forEachIndexed { index, tenant ->
                if (tenant.payment_status != Constants.STATUS_PAID && tenant.due_date.isNotEmpty()) {
                    try {
                        val dueDate = tenant.due_date.toIntOrNull()?.let {
                            today.withDayOfMonth(it.coerceIn(1, today.lengthOfMonth()))
                        } ?: LocalDate.parse(tenant.due_date, formatter)

                        if (autoOverdueEnabled && dueDate.plusDays(graceDays.toLong()).isBefore(today)) {
                            RetrofitClient.api.updateTenant(
                                token = "Bearer $token",
                                id = "eq.${tenant.id}",
                                payload = mapOf("payment_status" to Constants.STATUS_OVERDUE)
                            )
                        }

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
