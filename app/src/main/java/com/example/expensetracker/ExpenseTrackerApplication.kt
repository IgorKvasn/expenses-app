package com.example.expensetracker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.ui.widget.MonthRolloverWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class ExpenseTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        enqueueMonthRolloverWorker()
    }

    private fun enqueueMonthRolloverWorker() {
        val request = PeriodicWorkRequestBuilder<MonthRolloverWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "month_rollover_widget_update",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
