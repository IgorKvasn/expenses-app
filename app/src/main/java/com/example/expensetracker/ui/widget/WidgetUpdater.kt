package com.example.expensetracker.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdater {
    suspend fun update(context: Context) {
        try {
            MonthlySnapshotWidget().updateAll(context)
        } catch (_: Exception) {
            // Best-effort — widget update failure must not break data operations
        }
    }
}
