package com.example.made.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.made.R
import com.example.made.util.Constants

object NotificationHelper {

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            enableLights(true)
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showRentReminder(
        context: Context,
        notificationId: Int,
        tenantName: String,
        amount: String,
        daysUntilDue: Int,
        unitNumber: String,
        phone: String
    ) {
        createChannel(context)

        // Call action
        val callIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        val callPending = PendingIntent.getActivity(
            context, notificationId * 10, callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // WhatsApp action
        val whatsappMsg = "Hi $tenantName, this is a friendly reminder that your rent of $amount is due in $daysUntilDue days. Please make the payment at your earliest convenience."
        val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/${phone.replace("+","")}?text=${Uri.encode(whatsappMsg)}")
        }
        val whatsappPending = PendingIntent.getActivity(
            context, notificationId * 10 + 1, whatsappIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle("Rent Due: $tenantName")
            .setContentText("$amount due in $daysUntilDue days for Unit $unitNumber")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$amount due in $daysUntilDue days for Unit $unitNumber. Contact $tenantName to send a reminder."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_phone, "📞 Call", callPending)
            .addAction(R.drawable.ic_email, "💬 WhatsApp", whatsappPending)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }
}
