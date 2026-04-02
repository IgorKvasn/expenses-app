package com.example.expensetracker.ui.widget

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {
    suspend fun update(context: Context) {
        withContext(Dispatchers.Main) {
            MonthlySnapshotWidget().updateAll(context)
        }
    }
}
