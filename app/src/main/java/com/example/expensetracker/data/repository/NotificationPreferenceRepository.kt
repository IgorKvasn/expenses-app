package com.example.expensetracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "notification_preferences")

class NotificationPreferenceRepository(private val context: Context) {

    private val monthlyBalanceEnabledKey = booleanPreferencesKey("monthly_balance_notification_enabled")

    val isMonthlyBalanceNotificationEnabled: Flow<Boolean> =
        context.notificationDataStore.data.map { preferences ->
            preferences[monthlyBalanceEnabledKey] ?: false
        }

    suspend fun setMonthlyBalanceNotificationEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[monthlyBalanceEnabledKey] = enabled
        }
    }
}
