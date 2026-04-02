# Monthly Snapshot Widget (4x1) — Design Spec

## Overview

A small Android home screen widget (4x1) that shows the current month's expenses, income, and balance at a glance. Tapping it opens the app's default screen.

## Visual Layout

```
┌──────────────────────────────────────────────┐
│  April 2026                                  │
│  Expenses: €1,234.00  Income: €3,200.00  +€1,966.00 │
└──────────────────────────────────────────────┘
```

- **Top row:** Month and year label, small secondary text style
- **Bottom row:** Three values — Expenses total, Income total, Balance (income - expenses)
- **Balance coloring:** Green when positive or zero, red when negative
- **Theming:** Uses Glance Material 3 `GlanceTheme` to match app colors and support dark mode
- **Tap action:** Tapping anywhere on the widget opens `MainActivity`

## Technology

- **Jetpack Glance** for Compose-based widget UI
- **WorkManager** for daily month-rollover check
- No Hilt in the widget — access `AppDatabase` directly via `AppDatabase.getInstance(context)`

## Data Flow

The widget reads current-month totals directly from Room DAOs:

1. `provideGlance()` is called by the system or by an explicit update trigger.
2. Inside `provideGlance()`, a coroutine computes the first and last day of the current month.
3. Calls `ExpenseDao.getTotalInRange(firstDay, lastDay)` and `IncomeDao.getTotalInRange(firstDay, lastDay)`.
4. Renders the three values using the existing `CurrencyFormatter.format()`.

No ViewModel is used. The widget accesses the database directly through the application context.

## Update Triggers

### On data change

After any expense or income insert, update, or delete, the respective repository (`ExpenseRepository`, `IncomeRepository`) calls `WidgetUpdater.update(context)`, which invokes `MonthlySnapshotWidget().updateAll(context)`.

### On month rollover

A daily `WorkManager` periodic job (`MonthRolloverWorker`) runs once per day. It unconditionally triggers a widget update. This ensures the month label stays current without needing frequent polling. The worker is enqueued at app startup in `ExpenseTrackerApplication` with `ExistingPeriodicWorkPolicy.KEEP` to avoid duplicates.

## New Files

| File | Purpose |
|------|---------|
| `ui/widget/MonthlySnapshotWidget.kt` | `GlanceAppWidget` subclass with Compose-style UI and `GlanceAppWidgetReceiver` |
| `ui/widget/WidgetUpdater.kt` | Static helper to trigger widget updates from repositories |
| `ui/widget/MonthRolloverWorker.kt` | Daily `PeriodicWorkRequest` worker that triggers widget update |
| `res/xml/monthly_snapshot_widget_info.xml` | Widget metadata: min size (4x1), preview, description, resize mode |

## Modified Files

| File | Change |
|------|--------|
| `AndroidManifest.xml` | Register `MonthlySnapshotWidgetReceiver` and `MonthRolloverWorker` |
| `ExpenseTrackerApplication.kt` | Enqueue the daily `MonthRolloverWorker` |
| `data/repository/ExpenseRepository.kt` | Call `WidgetUpdater.update()` after insert/update/delete |
| `data/repository/IncomeRepository.kt` | Call `WidgetUpdater.update()` after insert/update/delete |
| `build.gradle.kts` | Add Glance and WorkManager dependencies |
| `libs.versions.toml` | Add version entries for Glance and WorkManager |

## Dependencies to Add

- `androidx.glance:glance-appwidget` (latest stable)
- `androidx.glance:glance-material3` (latest stable)
- `androidx.work:work-runtime-ktx` (latest stable)

## Database Access in Widget

Since Hilt dependency injection is not available in `GlanceAppWidget`, the widget needs a way to obtain the database instance directly.

Currently, `DatabaseModule` builds the database with `Room.databaseBuilder()` using the name `"expense_tracker.db"` and a seed callback. Rather than duplicating this setup, we extract a thread-safe singleton factory into `AppDatabase` itself:

```kotlin
companion object {
    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "expense_tracker.db",
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
```

`DatabaseModule` is updated to delegate to `AppDatabase.getInstance()` (adding the seed callback there). This ensures both Hilt and the widget use the same singleton instance and database configuration.

The widget then accesses data as:

```kotlin
val database = AppDatabase.getInstance(context)
val expenseTotal = database.expenseDao().getTotalInRange(firstDay, lastDay) ?: 0L
val incomeTotal = database.incomeDao().getTotalInRange(firstDay, lastDay) ?: 0L
```
