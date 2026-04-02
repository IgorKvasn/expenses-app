# Monthly Snapshot Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a 4×1 home screen widget showing current month's expenses, income, and balance.

**Architecture:** Jetpack Glance widget uses `GlanceStateDefinition` (Preferences DataStore) to store snapshot data. `WidgetUpdater` reads from Room DAOs, writes to widget state via `updateAppWidgetState`, then calls `widget.update()` to trigger recomposition. This ensures data is always fresh — even when a Glance session is already running (~45s window), the `UpdateGlanceState` event recomposes with the latest preferences. Repositories trigger widget updates after data mutations. A daily WorkManager job handles month rollover.

**Tech Stack:** Jetpack Glance 1.1.0, Glance Material 3 1.1.0, WorkManager 2.10.0, Room (existing)

---

### Task 1: Add Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add version entries to libs.versions.toml**

Add these entries to the `[versions]` section:

```toml
glance = "1.1.0"
work = "2.10.0"
```

Add these entries to the `[libraries]` section:

```toml
glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
```

- [ ] **Step 2: Add dependencies to app/build.gradle.kts**

Add to the `dependencies` block after the existing `kotlinx-serialization-json` line:

```kotlin
implementation(libs.glance.appwidget)
implementation(libs.glance.material3)
implementation(libs.work.runtime.ktx)
```

- [ ] **Step 3: Sync and verify build**

Run: `./gradlew app:dependencies --configuration releaseRuntimeClasspath | grep -E "glance|work"`
Expected: Lines showing glance-appwidget:1.1.0, glance-material3:1.1.0, work-runtime-ktx:2.10.0

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add Glance and WorkManager dependencies for widget"
```

---

### Task 2: Extract AppDatabase Singleton Factory

The widget cannot use Hilt, so it needs a way to get the database instance. Extract the Room builder into `AppDatabase.Companion` so both Hilt and the widget share a single instance.

**Files:**
- Modify: `app/src/main/java/com/example/expensetracker/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/example/expensetracker/di/DatabaseModule.kt`

- [ ] **Step 1: Add companion object factory to AppDatabase**

Add a companion object to `AppDatabase` that provides a thread-safe singleton. The seed callback for default categories is kept in `DatabaseModule` since it depends on `CategoryDao` via Hilt — the widget doesn't need seeding (it only reads).

Add these imports to `AppDatabase.kt`:

```kotlin
import android.content.Context
import androidx.room.Room
```

Add inside the `AppDatabase` class body, after the abstract DAO declarations:

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

- [ ] **Step 2: Update DatabaseModule to use the singleton factory**

Replace the `provideDatabase` function body in `DatabaseModule.kt`. The seed callback needs to be added to the instance, but since `Room.databaseBuilder` is only called once (the companion caches it), we need the seed callback in the companion. However, the seed callback depends on `CategoryDao` via Hilt's `Provider`, which isn't available in the companion.

The cleanest approach: keep `DatabaseModule` building the database with the seed callback as before, but store the result in the companion's `instance` field so the widget reuses it. Update the companion to also accept an externally-built instance:

Replace the companion object in `AppDatabase.kt` with:

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

    fun setInstance(database: AppDatabase) {
        instance = database
    }
}
```

Then in `DatabaseModule.provideDatabase`, after the `.build()` call, add `.also { AppDatabase.setInstance(it) }`:

Replace the return statement in `provideDatabase`:

```kotlin
return Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    "expense_tracker.db",
)
    .fallbackToDestructiveMigration(dropAllTables = true)
    .addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                categoryDaoProvider.get().insertAll(defaultCategories)
            }
        }
    })
    .build()
    .also { AppDatabase.setInstance(it) }
```

This way: if the app is running, Hilt builds the DB with the seed callback and stores it in the companion. If the widget runs without the app process, the companion builds a plain instance (no seed needed — categories were already seeded on first app launch).

- [ ] **Step 3: Verify build**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/data/db/AppDatabase.kt app/src/main/java/com/example/expensetracker/di/DatabaseModule.kt
git commit -m "refactor: extract AppDatabase singleton for widget access"
```

---

### Task 3: Create Widget XML Metadata

**Files:**
- Create: `app/src/main/res/xml/monthly_snapshot_widget_info.xml`

- [ ] **Step 1: Create the widget info XML**

Create `app/src/main/res/xml/monthly_snapshot_widget_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:minWidth="250dp"
    android:minHeight="40dp"
    android:resizeMode="horizontal"
    android:targetCellWidth="4"
    android:targetCellHeight="1"
    android:widgetCategory="home_screen"
    android:description="@string/widget_description" />
```

- [ ] **Step 2: Create the Glance default loading layout**

Glance needs an `initialLayout` — a simple placeholder shown while the widget loads. Create `app/src/main/res/layout/glance_default_loading_layout.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:text="@string/widget_loading"
    android:textSize="12sp" />
```

- [ ] **Step 3: Add string resources**

Add to `app/src/main/res/values/strings.xml` (create if it doesn't exist):

```xml
<resources>
    <string name="app_name">Expense Tracker</string>
    <string name="widget_description">Monthly expenses, income, and balance</string>
    <string name="widget_loading">Loading…</string>
</resources>
```

If `strings.xml` already exists, add just the two new `widget_description` and `widget_loading` entries.

- [ ] **Step 4: Verify build**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/xml/monthly_snapshot_widget_info.xml app/src/main/res/layout/glance_default_loading_layout.xml app/src/main/res/values/strings.xml
git commit -m "feat: add widget XML metadata and string resources"
```

---

### Task 4: Create the Glance Widget

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/widget/MonthlySnapshotWidget.kt`

**Important:** The widget uses Glance's `GlanceStateDefinition` (Preferences DataStore) to store and read data. Data is read from `currentState<Preferences>()` inside `provideContent {}`, NOT from the database directly in `provideGlance()`. This is critical because Glance sessions stay alive for ~45 seconds — when `update()` is called on a running session, it sends an `UpdateGlanceState` event that triggers recomposition but does NOT re-run `provideGlance()`. Only data read from `currentState()` inside `provideContent {}` is refreshed on recomposition.

A top-level `updateWidgetState()` function reads current month totals from Room and writes them to the widget's Preferences DataStore via `updateAppWidgetState()`. This is called by both `provideGlance()` (initial render) and `WidgetUpdater` (data change updates).

- [ ] **Step 1: Create the MonthlySnapshotWidget class**

Create `app/src/main/java/com/example/expensetracker/ui/widget/MonthlySnapshotWidget.kt`:

```kotlin
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
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.expensetracker.MainActivity
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
            modifier = GlanceModifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = CurrencyFormatter.format(expenseTotal),
                style = TextStyle(
                    color = DayNightColorProvider(ExpenseRed, ExpenseRedDark),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
            Text(
                text = CurrencyFormatter.format(incomeTotal),
                style = TextStyle(
                    color = DayNightColorProvider(IncomeGreen, IncomeGreenDark),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
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
```

- [ ] **Step 2: Verify build**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/widget/MonthlySnapshotWidget.kt
git commit -m "feat: create MonthlySnapshotWidget with Glance state-based updates"
```

---

### Task 5: Create WidgetUpdater Helper

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/widget/WidgetUpdater.kt`

The `WidgetUpdater` iterates over all placed widget instances via `GlanceAppWidgetManager`, writes fresh data to each widget's Preferences DataStore via `updateWidgetState()`, then calls `widget.update()` to trigger recomposition. This two-step approach (write state, then update) ensures the widget always reads current data — even when a Glance session is already running and `provideGlance()` is not re-invoked.

- [ ] **Step 1: Create WidgetUpdater**

Create `app/src/main/java/com/example/expensetracker/ui/widget/WidgetUpdater.kt`:

```kotlin
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
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/widget/WidgetUpdater.kt
git commit -m "feat: add WidgetUpdater with state-based updates"
```

---

### Task 6: Hook Widget Updates into Repositories

**Files:**
- Modify: `app/src/main/java/com/example/expensetracker/data/repository/ExpenseRepository.kt`
- Modify: `app/src/main/java/com/example/expensetracker/data/repository/IncomeRepository.kt`

- [ ] **Step 1: Add context injection and widget updates to ExpenseRepository**

The repository needs an application `Context` to call `WidgetUpdater`. Add it via Hilt's `@ApplicationContext`.

Update `ExpenseRepository.kt`:

Add imports:
```kotlin
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.expensetracker.ui.widget.WidgetUpdater
```

Update the constructor to inject context:
```kotlin
@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    @ApplicationContext private val context: Context,
)
```

Update the `insert`, `update`, and `delete` methods to trigger widget updates:

```kotlin
suspend fun insert(expense: ExpenseEntity): Long {
    return expenseDao.insert(expense).also { WidgetUpdater.update(context) }
}

suspend fun update(expense: ExpenseEntity) {
    expenseDao.update(expense)
    WidgetUpdater.update(context)
}

suspend fun delete(expense: ExpenseEntity) {
    expenseDao.delete(expense)
    WidgetUpdater.update(context)
}
```

- [ ] **Step 2: Add context injection and widget updates to IncomeRepository**

Update `IncomeRepository.kt`:

Add imports:
```kotlin
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.expensetracker.ui.widget.WidgetUpdater
```

Update the constructor to inject context:
```kotlin
@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao,
    private val generationDao: RecurringIncomeGenerationDao,
    @ApplicationContext private val context: Context,
)
```

Update the `insert`, `update`, and `delete` methods:

```kotlin
suspend fun insert(income: IncomeEntity): Long {
    return incomeDao.insert(income).also { WidgetUpdater.update(context) }
}

suspend fun update(income: IncomeEntity) {
    incomeDao.update(income)
    WidgetUpdater.update(context)
}

suspend fun delete(income: IncomeEntity) {
    incomeDao.delete(income)
    WidgetUpdater.update(context)
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/data/repository/ExpenseRepository.kt app/src/main/java/com/example/expensetracker/data/repository/IncomeRepository.kt
git commit -m "feat: trigger widget update on expense/income changes"
```

---

### Task 7: Create MonthRolloverWorker

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/widget/MonthRolloverWorker.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt`

- [ ] **Step 1: Create MonthRolloverWorker**

Create `app/src/main/java/com/example/expensetracker/ui/widget/MonthRolloverWorker.kt`:

```kotlin
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
```

- [ ] **Step 2: Enqueue the worker in ExpenseTrackerApplication**

Update `ExpenseTrackerApplication.kt`:

```kotlin
package com.example.expensetracker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.ui.widget.MonthRolloverWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class ExpenseTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        enqueueMonthRolloverWorker()
    }

    private fun enqueueMonthRolloverWorker() {
        val request = PeriodicWorkRequestBuilder<MonthRolloverWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "month_rollover_widget_update",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/widget/MonthRolloverWorker.kt app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt
git commit -m "feat: add daily MonthRolloverWorker for widget month updates"
```

---

### Task 8: Register Widget in AndroidManifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add the widget receiver to the manifest**

Add inside the `<application>` tag, after the existing `<provider>` element:

```xml
<receiver
    android:name=".ui.widget.MonthlySnapshotWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/monthly_snapshot_widget_info" />
</receiver>
```

- [ ] **Step 2: Verify build**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: register MonthlySnapshotWidgetReceiver in manifest"
```

---

### Task 9: Fix Tests (if needed)

The repository constructor changes (added `Context` parameter) may break existing unit tests that construct repositories directly.

**Files:**
- Modify: Any test files under `app/src/test/` that instantiate `ExpenseRepository` or `IncomeRepository`

- [ ] **Step 1: Find affected tests**

Run: `grep -rn "ExpenseRepository\|IncomeRepository" app/src/test/ --include="*.kt"`

Check if any tests instantiate these repositories directly. If so, they need a mock `Context` parameter added.

- [ ] **Step 2: Fix failing tests**

For each test that constructs a repository, add a mock context:

```kotlin
private val mockContext = mockk<Context>(relaxed = true)
```

And pass it to the constructor:

```kotlin
// ExpenseRepository
ExpenseRepository(expenseDao, mockContext)

// IncomeRepository
IncomeRepository(incomeDao, generationDao, mockContext)
```

- [ ] **Step 3: Run all tests**

Run: `./gradlew app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 4: Commit (if changes were needed)**

```bash
git add app/src/test/
git commit -m "test: fix repository tests after Context injection"
```

---

### Task 10: Final Integration Verification

- [ ] **Step 1: Full build**

Run: `./gradlew app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all tests**

Run: `./gradlew app:testDebugUnitTest`
Expected: All tests pass

- [ ] **Step 3: Commit any remaining changes**

If there are any remaining uncommitted files, commit them with an appropriate message.
