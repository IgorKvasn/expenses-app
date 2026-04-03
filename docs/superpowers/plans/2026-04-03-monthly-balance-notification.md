# Monthly Balance Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show an Android system notification on the 1st of each month with previous month's balance, navigating to Reports on tap, with an opt-in toggle in Settings.

**Architecture:** Daily WorkManager job checks date + DataStore preference, queries Room DB for previous month totals, posts notification via NotificationManager. Settings screen gets a new switch backed by Preferences DataStore. Notification tap deep-links to Reports via intent extra on MainActivity.

**Tech Stack:** Kotlin, Jetpack Compose, WorkManager, Preferences DataStore, Room, Hilt, NotificationManager

---

## File Structure

### New Files
- `app/src/main/java/com/example/expensetracker/data/repository/NotificationPreferenceRepository.kt` — DataStore wrapper for notification preference
- `app/src/main/java/com/example/expensetracker/domain/worker/MonthlyBalanceNotificationWorker.kt` — Daily worker that conditionally posts notification
- `app/src/main/java/com/example/expensetracker/ui/notifications/NotificationHelper.kt` — Channel creation + notification posting

### Modified Files
- `gradle/libs.versions.toml` — Add DataStore dependency
- `app/build.gradle.kts` — Add DataStore dependency
- `app/src/main/AndroidManifest.xml` — Add POST_NOTIFICATIONS permission
- `app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt` — Create notification channel, schedule worker
- `app/src/main/java/com/example/expensetracker/MainActivity.kt` — Handle `navigate_to` intent extra
- `app/src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt` — Accept and handle `navigateToReports` parameter
- `app/src/main/java/com/example/expensetracker/ui/settings/SettingsViewModel.kt` — Add notification preference state + toggle
- `app/src/main/java/com/example/expensetracker/ui/settings/SettingsScreen.kt` — Add notification switch UI
- `app/src/main/java/com/example/expensetracker/di/DatabaseModule.kt` — Provide NotificationPreferenceRepository

---

### Task 1: Add DataStore dependency

**Files:**
- Modify: `gradle/libs.versions.toml:16` (add version + library)
- Modify: `app/build.gradle.kts:91` (add implementation)

- [ ] **Step 1: Add DataStore to version catalog**

In `gradle/libs.versions.toml`, add the datastore version and library entries:

```toml
# In [versions] section, after "glance = ..." (line 15):
datastore = "1.1.4"

# In [libraries] section, after "glance-material3 = ..." (line 48):
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```

- [ ] **Step 2: Add DataStore to app dependencies**

In `app/build.gradle.kts`, add after the `work-runtime-ktx` line (line 91):

```kotlin
    implementation(libs.datastore.preferences)
```

- [ ] **Step 3: Verify build compiles**

Run: `cd /data/projects/expenses-app/app && ./gradlew app:assembleDebug --dry-run 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL (dry run lists tasks without errors)

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat: add DataStore preferences dependency"
```

---

### Task 2: Create NotificationPreferenceRepository

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/data/repository/NotificationPreferenceRepository.kt`
- Modify: `app/src/main/java/com/example/expensetracker/di/DatabaseModule.kt`

- [ ] **Step 1: Create NotificationPreferenceRepository**

Create `app/src/main/java/com/example/expensetracker/data/repository/NotificationPreferenceRepository.kt`:

```kotlin
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
```

- [ ] **Step 2: Provide via Hilt in DatabaseModule**

In `app/src/main/java/com/example/expensetracker/di/DatabaseModule.kt`, add the import and provider:

Add import at top:
```kotlin
import com.example.expensetracker.data.repository.NotificationPreferenceRepository
```

Add provider function inside the `DatabaseModule` object, after the last DAO provider (after line 72):

```kotlin
    @Provides
    @Singleton
    fun provideNotificationPreferenceRepository(
        @ApplicationContext context: Context,
    ): NotificationPreferenceRepository = NotificationPreferenceRepository(context)
```

Also ensure these imports are present at the top of DatabaseModule.kt (add any missing):
```kotlin
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
```

- [ ] **Step 3: Verify build compiles**

Run: `cd /data/projects/expenses-app/app && ./gradlew app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/data/repository/NotificationPreferenceRepository.kt app/src/main/java/com/example/expensetracker/di/DatabaseModule.kt
git commit -m "feat: add NotificationPreferenceRepository with DataStore"
```

---

### Task 3: Create NotificationHelper

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/notifications/NotificationHelper.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt`

- [ ] **Step 1: Add POST_NOTIFICATIONS permission to manifest**

In `app/src/main/AndroidManifest.xml`, add after line 5 (`<uses-permission android:name="android.permission.CAMERA" />`):

```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

- [ ] **Step 2: Create NotificationHelper**

Create `app/src/main/java/com/example/expensetracker/ui/notifications/NotificationHelper.kt`:

```kotlin
package com.example.expensetracker.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.ui.components.CurrencyFormatter
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "monthly_balance"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monthly Balance Summary",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Monthly notification with previous month's balance summary"
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun postMonthlyBalance(
        context: Context,
        yearMonth: YearMonth,
        incomeCents: Long,
        expenseCents: Long,
    ) {
        val balance = incomeCents - expenseCents
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val year = yearMonth.year

        val title = "Balance for $monthName $year"
        val balancePrefix = if (balance >= 0) "+" else ""
        val body = "Income: ${CurrencyFormatter.format(incomeCents)} | " +
            "Expenses: ${CurrencyFormatter.format(expenseCents)} | " +
            "Balance: $balancePrefix${CurrencyFormatter.format(balance)}"

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "reports")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
```

- [ ] **Step 3: Create notification channel in ExpenseTrackerApplication**

In `app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt`, add import:

```kotlin
import com.example.expensetracker.ui.notifications.NotificationHelper
```

Add call in `onCreate()`, after `enqueueMonthRolloverWorker()` (line 15):

```kotlin
        NotificationHelper.createChannel(this)
```

- [ ] **Step 4: Verify build compiles**

Run: `cd /data/projects/expenses-app/app && ./gradlew app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/example/expensetracker/ui/notifications/NotificationHelper.kt app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt
git commit -m "feat: add NotificationHelper with channel and posting logic"
```

---

### Task 4: Create MonthlyBalanceNotificationWorker

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/domain/worker/MonthlyBalanceNotificationWorker.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt`

- [ ] **Step 1: Create the worker**

Create `app/src/main/java/com/example/expensetracker/domain/worker/MonthlyBalanceNotificationWorker.kt`:

```kotlin
package com.example.expensetracker.domain.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.repository.NotificationPreferenceRepository
import com.example.expensetracker.ui.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

class MonthlyBalanceNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        if (today.dayOfMonth != 1) {
            return Result.success()
        }

        val preferenceRepository = NotificationPreferenceRepository(applicationContext)
        val isEnabled = preferenceRepository.isMonthlyBalanceNotificationEnabled.first()
        if (!isEnabled) {
            return Result.success()
        }

        val previousMonth = YearMonth.from(today).minusMonths(1)
        val firstDay = previousMonth.atDay(1).toString()
        val lastDay = previousMonth.atEndOfMonth().toString()

        val database = AppDatabase.getInstance(applicationContext)
        val expenseTotal = database.expenseDao().getTotalInRange(firstDay, lastDay) ?: 0L
        val incomeTotal = database.incomeDao().getTotalInRange(firstDay, lastDay) ?: 0L

        NotificationHelper.postMonthlyBalance(
            context = applicationContext,
            yearMonth = previousMonth,
            incomeCents = incomeTotal,
            expenseCents = expenseTotal,
        )

        return Result.success()
    }
}
```

- [ ] **Step 2: Schedule the worker in ExpenseTrackerApplication**

In `app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt`, add import:

```kotlin
import com.example.expensetracker.domain.worker.MonthlyBalanceNotificationWorker
```

Add a new scheduling method after `enqueueMonthRolloverWorker()`:

```kotlin
    private fun enqueueMonthlyBalanceNotificationWorker() {
        val request = PeriodicWorkRequestBuilder<MonthlyBalanceNotificationWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "monthly_balance_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
```

Call it in `onCreate()`, after `NotificationHelper.createChannel(this)`:

```kotlin
        enqueueMonthlyBalanceNotificationWorker()
```

- [ ] **Step 3: Verify build compiles**

Run: `cd /data/projects/expenses-app/app && ./gradlew app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/domain/worker/MonthlyBalanceNotificationWorker.kt app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt
git commit -m "feat: add MonthlyBalanceNotificationWorker with daily schedule"
```

---

### Task 5: Handle deep link in MainActivity and NavGraph

**Files:**
- Modify: `app/src/main/java/com/example/expensetracker/MainActivity.kt:35-39`
- Modify: `app/src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt:46,55-59`

- [ ] **Step 1: Add navigate_to handling in MainActivity**

In `app/src/main/java/com/example/expensetracker/MainActivity.kt`, after line 35 (`val navigateToAddExpense = ...`), add:

```kotlin
        val navigateToReports = intent.getStringExtra("navigate_to") == "reports"
```

Update the `setContent` block (lines 37-41) to pass the new parameter:

```kotlin
        setContent {
            ExpenseTrackerTheme {
                NavGraph(
                    navigateToAddExpense = navigateToAddExpense,
                    navigateToReports = navigateToReports,
                )
            }
        }
```

- [ ] **Step 2: Handle navigateToReports in NavGraph**

In `app/src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt`, update the function signature (line 46):

```kotlin
fun NavGraph(navigateToAddExpense: Boolean = false, navigateToReports: Boolean = false) {
```

Add a LaunchedEffect after the existing `navigateToAddExpense` one (after line 59):

```kotlin
    if (navigateToReports) {
        LaunchedEffect(Unit) {
            navController.navigate(Screen.Reports.route) {
                popUpTo(Screen.ExpenseList.route) { saveState = true }
                launchSingleTop = true
            }
        }
    }
```

- [ ] **Step 3: Verify build compiles**

Run: `cd /data/projects/expenses-app/app && ./gradlew app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/MainActivity.kt app/src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt
git commit -m "feat: handle notification deep link to Reports screen"
```

---

### Task 6: Add notification toggle to Settings

**Files:**
- Modify: `app/src/main/java/com/example/expensetracker/ui/settings/SettingsViewModel.kt:24-26`
- Modify: `app/src/main/java/com/example/expensetracker/ui/settings/SettingsScreen.kt:107-151`

- [ ] **Step 1: Add notification preference to SettingsViewModel**

In `app/src/main/java/com/example/expensetracker/ui/settings/SettingsViewModel.kt`:

Add imports at top:
```kotlin
import com.example.expensetracker.data.repository.NotificationPreferenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
```

Update the constructor (line 24) to inject the repository:

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportImportJsonUseCase: ExportImportJsonUseCase,
    private val notificationPreferenceRepository: NotificationPreferenceRepository,
) : ViewModel() {
```

Add the notification state and toggle method after the `pendingImportJson` field (after line 37):

```kotlin
    val isMonthlyBalanceNotificationEnabled: StateFlow<Boolean> =
        notificationPreferenceRepository.isMonthlyBalanceNotificationEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setMonthlyBalanceNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferenceRepository.setMonthlyBalanceNotificationEnabled(enabled)
        }
    }
```

- [ ] **Step 2: Add notification switch UI to SettingsScreen**

In `app/src/main/java/com/example/expensetracker/ui/settings/SettingsScreen.kt`:

Add imports at top:
```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.core.content.ContextCompat
```

Add state collection after the existing `showImportConfirmation` collection (after line 45):
```kotlin
    val isNotificationEnabled by viewModel.isMonthlyBalanceNotificationEnabled.collectAsStateWithLifecycle()
```

Add permission launcher after the `importFilePicker` launcher (after line 52):
```kotlin
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.setMonthlyBalanceNotificationEnabled(true)
        }
    }
```

Add the notification section in the `Column`, after the Import Data `ListItem` (after line 151, before the closing `}` of Column):

```kotlin
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Monthly balance summary") },
                supportingContent = { Text("Get notified on the 1st of each month with last month's balance") },
                leadingContent = {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                    != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setMonthlyBalanceNotificationEnabled(true)
                                }
                            } else {
                                viewModel.setMonthlyBalanceNotificationEnabled(false)
                            }
                        },
                    )
                },
            )
```

- [ ] **Step 3: Verify build compiles**

Run: `cd /data/projects/expenses-app/app && ./gradlew app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/settings/SettingsViewModel.kt app/src/main/java/com/example/expensetracker/ui/settings/SettingsScreen.kt
git commit -m "feat: add monthly balance notification toggle to Settings"
```

---

### Task 7: Final verification

- [ ] **Step 1: Full build**

Run: `cd /data/projects/expenses-app/app && ./gradlew app:assembleDebug 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all new files exist**

```bash
ls -la app/src/main/java/com/example/expensetracker/data/repository/NotificationPreferenceRepository.kt app/src/main/java/com/example/expensetracker/domain/worker/MonthlyBalanceNotificationWorker.kt app/src/main/java/com/example/expensetracker/ui/notifications/NotificationHelper.kt
```

- [ ] **Step 3: Commit if any remaining changes**

```bash
git status
# If anything unstaged, add and commit
```
