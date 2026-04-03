package com.example.expensetracker.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.ui.components.CurrencyFormatter
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "monthly_balance"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monthly Balance Summary",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Monthly notification with previous month's balance summary"
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun postMonthlyBalance(
        context: Context,
        yearMonth: YearMonth,
        incomeCents: Long,
        expenseCents: Long,
    ) {
        val balance = incomeCents - expenseCents
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val year = yearMonth.year

        val title = "Balance for $monthName $year"
        val balancePrefix = if (balance >= 0) "+" else ""
        val body = "Income: ${CurrencyFormatter.format(incomeCents)} | " +
            "Expenses: ${CurrencyFormatter.format(expenseCents)} | " +
            "Balance: $balancePrefix${CurrencyFormatter.format(balance)}"

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "reports")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
