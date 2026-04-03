package com.example.expensetracker.domain.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.repository.NotificationPreferenceRepository
import com.example.expensetracker.ui.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

class MonthlyBalanceNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        if (today.dayOfMonth != 1) {
            return Result.success()
        }

        val preferenceRepository = NotificationPreferenceRepository(applicationContext)
        val isEnabled = preferenceRepository.isMonthlyBalanceNotificationEnabled.first()
        if (!isEnabled) {
            return Result.success()
        }

        val previousMonth = YearMonth.from(today).minusMonths(1)
        val firstDay = previousMonth.atDay(1).toString()
        val lastDay = previousMonth.atEndOfMonth().toString()

        val database = AppDatabase.getInstance(applicationContext)
        val expenseTotal = database.expenseDao().getTotalInRange(firstDay, lastDay) ?: 0L
        val incomeTotal = database.incomeDao().getTotalInRange(firstDay, lastDay) ?: 0L

        NotificationHelper.postMonthlyBalance(
            context = applicationContext,
            yearMonth = previousMonth,
            incomeCents = incomeTotal,
            expenseCents = expenseTotal,
        )

        return Result.success()
    }
}
