# Monthly Balance Notification

## Overview

On the 1st of every month, show an Android system notification with the previous month's balance summary (income, expenses, net balance). Tapping the notification navigates to the Reports screen. Users opt in via a checkbox on the Settings screen.

## Components

### 1. MonthlyBalanceNotificationWorker

A `CoroutineWorker` scheduled as a daily `PeriodicWorkRequest` (24h interval) via WorkManager.

**On each execution:**
1. Check if today is the 1st of the month. If not, return `Result.success()`.
2. Read the `monthly_balance_notification_enabled` preference from DataStore. If false, return `Result.success()`.
3. Query `ExpenseDao` and `IncomeDao` for the previous month's date range to get total expenses and total income (in cents).
4. Build and post a system notification via `NotificationHelper`.

**Scheduling:**
- Enqueued in `MainActivity.onCreate()` using `ExistingPeriodicWorkPolicy.KEEP` (won't replace if already scheduled).
- Always scheduled regardless of the preference — the worker itself checks the preference before posting.

### 2. NotificationPreferenceRepository

Wraps `androidx.datastore.preferences.core` DataStore.

**Key:** `booleanPreferencesKey("monthly_balance_notification_enabled")`, default `false` (opt-in).

**Interface:**
- `val isEnabled: Flow<Boolean>`
- `suspend fun setEnabled(enabled: Boolean)`

Provided via Hilt `@Singleton`.

### 3. NotificationHelper

Handles notification channel creation and posting.

**Channel:**
- ID: `"monthly_balance"`
- Name: "Monthly Balance Summary"
- Importance: `NotificationManager.IMPORTANCE_DEFAULT`

**Notification:**
- Title: "Balance for {Month} {Year}" (e.g., "Balance for March 2026")
- Body: "Income: {income} | Expenses: {expenses} | Balance: {net}" (e.g., "Income: $3,000 | Expenses: $2,550 | Balance: +$450")
- Small icon: app icon
- Tap action: `PendingIntent` launching `MainActivity` with intent extra `"navigate_to" = "reports"`
- Auto-cancel: true

**Channel creation** called in `ExpenseTrackerApplication.onCreate()`.

### 4. Settings UI Changes

Add a "Notifications" section to `SettingsScreen` below the existing Export/Import section.

**Row contents:**
- Switch labeled "Monthly balance summary"
- Subtitle: "Get notified on the 1st of each month with last month's balance"

**Behavior:**
- Toggling ON requests `POST_NOTIFICATIONS` runtime permission (Android 13+ / API 33+). If denied, toggle stays off.
- Toggling OFF writes `false` to DataStore. No permission revocation needed.

**SettingsViewModel changes:**
- Inject `NotificationPreferenceRepository`
- Expose `isNotificationEnabled: StateFlow<Boolean>`
- Add `setNotificationEnabled(enabled: Boolean)` method

### 5. Deep Link to Reports

Extends the existing intent-extra pattern in `MainActivity`.

- Notification `PendingIntent` sets extra: `"navigate_to" = "reports"`
- `MainActivity.onCreate()` checks for `"navigate_to"` extra (in addition to existing `"shortcut_action"` check)
- If value is `"reports"`, navigates to `Screen.Reports`

## Data Flow

```
App startup → enqueue daily PeriodicWorkRequest (KEEP policy)
    ↓
WorkManager fires daily → MonthlyBalanceNotificationWorker.doWork()
    ↓
Is today the 1st? → No → Result.success() (no-op)
    ↓ Yes
Is preference enabled? → No → Result.success() (no-op)
    ↓ Yes
Query ExpenseDao.getTotalInRange(monthStart, monthEnd) → totalExpenses
Query IncomeDao.getTotalInRange(monthStart, monthEnd) → totalIncome
    ↓
NotificationHelper.postMonthlyBalance(month, year, income, expenses, net)
    ↓
User taps notification → PendingIntent → MainActivity (navigate_to=reports)
    ↓
MainActivity.onCreate() → navController.navigate(Screen.Reports.route)
```

## Permission

- `POST_NOTIFICATIONS` — runtime permission, requested when user enables the setting
- No new manifest permissions needed for WorkManager (already in use)

## Files to Create

- `data/repository/NotificationPreferenceRepository.kt`
- `domain/worker/MonthlyBalanceNotificationWorker.kt`
- `ui/notifications/NotificationHelper.kt`

## Files to Modify

- `ui/settings/SettingsScreen.kt` — add notifications section
- `ui/settings/SettingsViewModel.kt` — add preference state and toggle
- `MainActivity.kt` — schedule worker, handle navigate_to intent extra
- `ExpenseTrackerApplication.kt` — create notification channel
- `di/DatabaseModule.kt` or new `di/NotificationModule.kt` — provide DataStore and NotificationHelper via Hilt
- `AndroidManifest.xml` — add `POST_NOTIFICATIONS` permission declaration
- `data/db/dao/IncomeDao.kt` — add `getTotalInRange` query if not present

## Amount Formatting

Reuse the existing cents-to-display formatting pattern used throughout the app (dividing by 100, formatting with currency symbol).

## Testing Considerations

- Worker logic is testable by mocking DataStore and DAOs
- Preference toggle testable via ViewModel unit tests
- Deep link navigation testable via instrumentation tests
