package com.example.expensetracker.ui.widget

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.color.DayNightColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.ui.components.CurrencyFormatter
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.ExpenseRedDark
import com.example.expensetracker.ui.theme.IncomeGreen
import com.example.expensetracker.ui.theme.IncomeGreenDark
import com.example.expensetracker.ui.theme.OnSurfaceDark
import com.example.expensetracker.ui.theme.OnSurfaceLight
import com.example.expensetracker.ui.theme.OnSurfaceVariantDark
import com.example.expensetracker.ui.theme.OnSurfaceVariantLight
import com.example.expensetracker.ui.theme.PrimaryContainerDark
import com.example.expensetracker.ui.theme.PrimaryContainerLight
import com.example.expensetracker.ui.theme.PrimaryDark
import com.example.expensetracker.ui.theme.PrimaryLight
import com.example.expensetracker.ui.theme.SurfaceContainerDark
import com.example.expensetracker.ui.theme.SurfaceContainerLight
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

class MonthlySnapshotWidget : GlanceAppWidget() {

    companion object {
        val KEY_EXPENSE_TOTAL = longPreferencesKey("expense_total")
        val KEY_INCOME_TOTAL = longPreferencesKey("income_total")
        val KEY_BALANCE = longPreferencesKey("balance")
        val KEY_MONTH_LABEL = stringPreferencesKey("month_label")

        private val colors = ColorProviders(
            light = lightColorScheme(
                surface = SurfaceContainerLight,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceVariantLight,
                primary = PrimaryLight,
                primaryContainer = PrimaryContainerLight,
            ),
            dark = darkColorScheme(
                surface = SurfaceContainerDark,
                onSurface = OnSurfaceDark,
                onSurfaceVariant = OnSurfaceVariantDark,
                primary = PrimaryDark,
                primaryContainer = PrimaryContainerDark,
            ),
        )
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        updateWidgetState(context, id)

        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val expenseTotal = prefs[KEY_EXPENSE_TOTAL] ?: 0L
            val incomeTotal = prefs[KEY_INCOME_TOTAL] ?: 0L
            val balance = prefs[KEY_BALANCE] ?: 0L
            val monthLabel = prefs[KEY_MONTH_LABEL] ?: ""

            GlanceTheme(colors = colors) {
                WidgetContent(
                    monthLabel = monthLabel,
                    expenseTotal = expenseTotal,
                    incomeTotal = incomeTotal,
                    balance = balance,
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    monthLabel: String,
    expenseTotal: Long,
    incomeTotal: Long,
    balance: Long,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = monthLabel,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
            ),
        )
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_arrow_down),
                    contentDescription = "Expenses",
                    modifier = GlanceModifier.size(14.dp),
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                Text(
                    text = CurrencyFormatter.format(expenseTotal),
                    style = TextStyle(
                        color = DayNightColorProvider(ExpenseRed, ExpenseRedDark),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_arrow_up),
                    contentDescription = "Income",
                    modifier = GlanceModifier.size(14.dp),
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                Text(
                    text = CurrencyFormatter.format(incomeTotal),
                    style = TextStyle(
                        color = DayNightColorProvider(IncomeGreen, IncomeGreenDark),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_arrow_up_down),
                    contentDescription = "Balance",
                    modifier = GlanceModifier.size(14.dp),
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                Text(
                    text = formatBalance(balance),
                    style = TextStyle(
                        color = if (balance >= 0) {
                            DayNightColorProvider(IncomeGreen, IncomeGreenDark)
                        } else {
                            DayNightColorProvider(ExpenseRed, ExpenseRedDark)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

private fun formatBalance(balanceCents: Long): String {
    val prefix = if (balanceCents >= 0) "+" else ""
    return prefix + CurrencyFormatter.format(balanceCents)
}

suspend fun updateWidgetState(context: Context, glanceId: GlanceId) {
    val yearMonth = YearMonth.from(LocalDate.now())
    val firstDay = yearMonth.atDay(1).toString()
    val lastDay = yearMonth.atEndOfMonth().toString()

    val database = AppDatabase.getInstance(context)
    val expenseTotal = database.expenseDao().getTotalInRange(firstDay, lastDay) ?: 0L
    val incomeTotal = database.incomeDao().getTotalInRange(firstDay, lastDay) ?: 0L
    val balance = incomeTotal - expenseTotal
    val monthLabel = yearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()) +
        " " + yearMonth.year

    updateAppWidgetState(context, glanceId) { prefs ->
        prefs[MonthlySnapshotWidget.KEY_EXPENSE_TOTAL] = expenseTotal
        prefs[MonthlySnapshotWidget.KEY_INCOME_TOTAL] = incomeTotal
        prefs[MonthlySnapshotWidget.KEY_BALANCE] = balance
        prefs[MonthlySnapshotWidget.KEY_MONTH_LABEL] = monthLabel
    }
}

class MonthlySnapshotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthlySnapshotWidget()
}
