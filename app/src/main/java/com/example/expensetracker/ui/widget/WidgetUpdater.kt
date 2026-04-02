package com.example.expensetracker.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager

object WidgetUpdater {
    private const val TAG = "WidgetUpdater"

    suspend fun update(context: Context) {
        try {
            val widget = MonthlySnapshotWidget()
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(widget.javaClass)
            for (glanceId in glanceIds) {
                updateWidgetState(context, glanceId)
                widget.update(context, glanceId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Widget update failed", e)
        }
    }
}
