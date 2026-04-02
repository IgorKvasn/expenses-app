package com.example.expensetracker.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MonthRolloverWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        MonthlySnapshotWidget().updateAll(applicationContext)
        return Result.success()
    }
}
