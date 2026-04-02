package com.example.expensetracker.ui.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MonthRolloverWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        WidgetUpdater.update(applicationContext)
        return Result.success()
    }
}
