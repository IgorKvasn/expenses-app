# Expense Tracker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android expense tracker app with expense/income management, recurring items, visual reports, and Excel export.

**Architecture:** Single-module MVVM with Kotlin + Jetpack Compose + Material 3. Room for persistence, Hilt for DI, Vico for charts, Apache POI for .xlsx export. Bottom navigation with 4 tabs: Expenses (default), Income, Recurring, Reports.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room (SQLite), Hilt, Vico, Apache POI, Compose Navigation, java.time, min SDK 35

---

## File Structure

```
app/
├── build.gradle.kts                          # App-level build config, dependencies
├── schemas/
│   └── export-schema.json                    # JSON Schema (draft 2020-12) for export format
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/com/example/expensetracker/
│       ├── ExpenseTrackerApplication.kt      # Hilt application class
│       ├── MainActivity.kt                   # Single activity, Compose entry point
│       ├── data/
│       │   ├── db/
│       │   │   ├── AppDatabase.kt            # Room database definition
│       │   │   ├── Converters.kt             # TypeConverters for LocalDate, YearMonth, Interval
│       │   │   ├── entity/
│       │   │   │   ├── CategoryEntity.kt
│       │   │   │   ├── ExpenseEntity.kt
│       │   │   │   ├── IncomeEntity.kt
│       │   │   │   ├── RecurringExpenseEntity.kt
│       │   │   │   └── RecurringExpenseGenerationEntity.kt
│       │   │   └── dao/
│       │   │       ├── CategoryDao.kt
│       │   │       ├── ExpenseDao.kt
│       │   │       ├── IncomeDao.kt
│       │   │       ├── RecurringExpenseDao.kt
│       │   │       └── RecurringExpenseGenerationDao.kt
│       │   ├── repository/
│       │   │   ├── CategoryRepository.kt
│       │   │   ├── ExpenseRepository.kt
│       │   │   ├── IncomeRepository.kt
│       │   │   └── RecurringExpenseRepository.kt
│       │   └── seed/
│       │       └── DefaultCategories.kt      # Pre-seeded category data
│       ├── domain/
│       │   ├── model/
│       │   │   ├── Interval.kt               # MONTHLY, QUARTERLY, HALF_YEARLY enum
│       │   │   ├── SortOrder.kt              # DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC
│       │   │   ├── ExpenseFilter.kt          # Filter data class
│       │   │   ├── IncomeFilter.kt           # Filter data class
│       │   │   └── ReportData.kt             # Report result models
│       │   └── usecase/
│       │       ├── GenerateRecurringExpensesUseCase.kt
│       │       ├── GenerateRecurringIncomeUseCase.kt
│       │       ├── CategoryReportUseCase.kt
│       │       ├── IncomeVsExpensesReportUseCase.kt
│       │       ├── ExportToExcelUseCase.kt
│       │       └── ExportImportJsonUseCase.kt  # JSON backup export/import
│       ├── ui/
│       │   ├── navigation/
│       │   │   ├── Screen.kt                 # Sealed class for nav routes
│       │   │   ├── BottomNavBar.kt           # Bottom navigation composable
│       │   │   └── NavGraph.kt               # Navigation graph setup
│       │   ├── theme/
│       │   │   ├── Theme.kt
│       │   │   ├── Color.kt
│       │   │   └── Type.kt
│       │   ├── components/
│       │   │   ├── AmountInput.kt            # Euro amount input field
│       │   │   ├── CategoryIcon.kt           # Maps icon name strings to Material Icons
│       │   │   ├── CategoryPicker.kt         # Category selection dialog with icons
│       │   │   ├── FilterBar.kt              # Expandable filter/sort bar with date range pickers
│       │   │   └── CurrencyFormatter.kt      # Cents to "€12.50" formatting
│       │   ├── expenses/
│       │   │   ├── ExpenseListScreen.kt      # Main expense list with filters
│       │   │   ├── ExpenseListViewModel.kt
│       │   │   ├── AddEditExpenseScreen.kt   # Add/edit expense form
│       │   │   └── AddEditExpenseViewModel.kt
│       │   ├── income/
│       │   │   ├── IncomeListScreen.kt       # One-time income list only
│       │   │   ├── IncomeListViewModel.kt
│       │   │   ├── AddEditIncomeScreen.kt    # One-time income form (no recurring toggle)
│       │   │   └── AddEditIncomeViewModel.kt
│       │   ├── recurring/
│       │   │   ├── RecurringListScreen.kt    # Both recurring expenses and income, differentiated by icon/color
│       │   │   ├── RecurringListViewModel.kt
│       │   │   ├── AddEditRecurringExpenseScreen.kt
│       │   │   ├── AddEditRecurringExpenseViewModel.kt
│       │   │   ├── AddEditRecurringIncomeScreen.kt
│       │   │   └── AddEditRecurringIncomeViewModel.kt
│       │   ├── reports/
│       │   │   ├── ReportsScreen.kt
│       │   │   ├── ReportsViewModel.kt
│       │   │   ├── CategoryBarChart.kt       # Vico horizontal bar chart
│       │   │   └── IncomeVsExpensesChart.kt  # Vico grouped column chart
│       │   ├── categories/
│       │   │   ├── CategoryManagementScreen.kt
│       │   │   └── CategoryManagementViewModel.kt
│       │   └── settings/
│       │       ├── SettingsScreen.kt             # Settings with export/import options
│       │       └── SettingsViewModel.kt
│       └── di/
│           ├── DatabaseModule.kt             # Room database provider
│           └── RepositoryModule.kt           # Repository bindings
├── src/test/java/com/example/expensetracker/
│   ├── domain/usecase/
│   │   ├── GenerateRecurringExpensesUseCaseTest.kt
│   │   ├── CategoryReportUseCaseTest.kt
│   │   ├── IncomeVsExpensesReportUseCaseTest.kt
│   │   ├── ExportToExcelUseCaseTest.kt
│   │   └── ExportImportJsonUseCaseTest.kt
│   └── data/repository/
│       ├── ExpenseRepositoryTest.kt
│       └── IncomeRepositoryTest.kt
└── src/androidTest/java/com/example/expensetracker/
    └── data/db/
        ├── ExpenseDaoTest.kt
        ├── IncomeDaoTest.kt
        └── CategoryDaoTest.kt
```

---

### Task 1: Project Scaffolding and Gradle Setup

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (project-level)
- Create: `app/build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Initialize the Android project**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ExpenseTracker"
include(":app")
```

- [ ] **Step 2: Create version catalog**

Create `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
compose-bom = "2025.01.01"
room = "2.7.1"
hilt = "2.54"
hilt-navigation-compose = "1.2.0"
vico = "2.1.2"
poi = "5.3.0"
navigation-compose = "2.8.6"
lifecycle = "2.8.7"
coroutines = "1.10.1"
junit = "4.13.2"
mockk = "1.13.14"
turbine = "1.2.0"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.3" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }
poi = { group = "org.apache.poi", name = "poi", version.ref = "poi" }
poi-ooxml = { group = "org.apache.poi", name = "poi-ooxml", version.ref = "poi" }
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 3: Create project-level build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 4: Create app/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.expensetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.expensetracker"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.vico.compose.m3)

    implementation(libs.poi)
    implementation(libs.poi.ooxml)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.room.testing)
}
```

- [ ] **Step 5: Create gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 6: Create AndroidManifest.xml**

Create `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".ExpenseTrackerApplication"
        android:allowBackup="true"
        android:label="Expense Tracker"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Material3.DayNight.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 7: Create Hilt application class**

Create `app/src/main/java/com/example/expensetracker/ExpenseTrackerApplication.kt`:

```kotlin
package com.example.expensetracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ExpenseTrackerApplication : Application()
```

- [ ] **Step 8: Commit**

```bash
git init
git add -A
git commit -m "chore: scaffold Android project with Gradle, Hilt, Compose, Room, Vico, POI dependencies"
```

---

### Task 2: Domain Models and Enums

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/domain/model/Interval.kt`
- Create: `app/src/main/java/com/example/expensetracker/domain/model/SortOrder.kt`
- Create: `app/src/main/java/com/example/expensetracker/domain/model/ExpenseFilter.kt`
- Create: `app/src/main/java/com/example/expensetracker/domain/model/IncomeFilter.kt`
- Create: `app/src/main/java/com/example/expensetracker/domain/model/ReportData.kt`

- [ ] **Step 1: Create Interval enum**

Create `app/src/main/java/com/example/expensetracker/domain/model/Interval.kt`:

```kotlin
package com.example.expensetracker.domain.model

enum class Interval(val months: Int, val displayName: String) {
    MONTHLY(1, "Monthly"),
    QUARTERLY(3, "Quarterly"),
    HALF_YEARLY(6, "Half-yearly"),
}
```

- [ ] **Step 2: Create SortOrder enum**

Create `app/src/main/java/com/example/expensetracker/domain/model/SortOrder.kt`:

```kotlin
package com.example.expensetracker.domain.model

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC,
}
```

- [ ] **Step 3: Create ExpenseFilter**

Create `app/src/main/java/com/example/expensetracker/domain/model/ExpenseFilter.kt`:

```kotlin
package com.example.expensetracker.domain.model

import java.time.LocalDate

data class ExpenseFilter(
    val categoryId: Long? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val amountMinCents: Long? = null,
    val amountMaxCents: Long? = null,
    val noteSearch: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
)
```

- [ ] **Step 4: Create IncomeFilter**

Create `app/src/main/java/com/example/expensetracker/domain/model/IncomeFilter.kt`:

```kotlin
package com.example.expensetracker.domain.model

import java.time.LocalDate

data class IncomeFilter(
    val isRecurring: Boolean? = null,
    val sourceSearch: String? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val amountMinCents: Long? = null,
    val amountMaxCents: Long? = null,
    val noteSearch: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
)
```

- [ ] **Step 5: Create ReportData models**

Create `app/src/main/java/com/example/expensetracker/domain/model/ReportData.kt`:

```kotlin
package com.example.expensetracker.domain.model

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String?,
    val totalCents: Long,
    val percentage: Double,
)

data class CategoryReport(
    val items: List<CategorySpending>,
    val totalCents: Long,
)

data class MonthlyBalance(
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long,
)

data class IncomeVsExpensesReport(
    val items: List<MonthlyBalance>,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
)
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/domain/
git commit -m "feat: add domain models — Interval, SortOrder, filters, report data classes"
```

---

### Task 3: Room Entities

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/data/db/entity/CategoryEntity.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/db/entity/ExpenseEntity.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/db/entity/IncomeEntity.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/db/entity/RecurringExpenseEntity.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/db/entity/RecurringExpenseGenerationEntity.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/db/Converters.kt`

- [ ] **Step 1: Create CategoryEntity**

```kotlin
package com.example.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String? = null,
    val isDefault: Boolean = false,
)
```

- [ ] **Step 2: Create ExpenseEntity**

```kotlin
package com.example.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = RecurringExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringExpenseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["recurringExpenseId"]),
        Index(value = ["date"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val categoryId: Long,
    val date: LocalDate,
    val note: String? = null,
    val recurringExpenseId: Long? = null,
)
```

- [ ] **Step 3: Create IncomeEntity**

```kotlin
package com.example.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.expensetracker.domain.model.Interval
import java.time.LocalDate

@Entity(
    tableName = "income",
    indices = [Index(value = ["date"])],
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val source: String,
    val date: LocalDate,
    val note: String? = null,
    val isRecurring: Boolean = false,
    val recurrenceInterval: Interval? = null,
    val startDate: String? = null,
)
```

- [ ] **Step 4: Create RecurringExpenseEntity**

```kotlin
package com.example.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.expensetracker.domain.model.Interval

@Entity(
    tableName = "recurring_expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["categoryId"])],
)
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val categoryId: Long,
    val interval: Interval,
    val note: String? = null,
    val startDate: String, // LocalDate stored as "2026-01-15" string
)
```

- [ ] **Step 5: Create RecurringExpenseGenerationEntity**

```kotlin
package com.example.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_expense_generations",
    foreignKeys = [
        ForeignKey(
            entity = RecurringExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringExpenseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["recurringExpenseId", "generatedForMonth"], unique = true),
        Index(value = ["expenseId"]),
    ],
)
data class RecurringExpenseGenerationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recurringExpenseId: Long,
    val generatedForMonth: String, // YearMonth as "2026-01"
    val expenseId: Long,
)
```

- [ ] **Step 6: Create TypeConverters**

```kotlin
package com.example.expensetracker.data.db

import androidx.room.TypeConverter
import com.example.expensetracker.domain.model.Interval
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromInterval(interval: Interval): String = interval.name

    @TypeConverter
    fun toInterval(value: String): Interval = Interval.valueOf(value)
}
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/data/db/
git commit -m "feat: add Room entities and type converters for expenses, income, categories, recurring"
```

---

### Task 4: Room DAOs

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/data/db/dao/CategoryDao.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/db/dao/ExpenseDao.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/db/dao/IncomeDao.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/db/dao/RecurringExpenseDao.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/db/dao/RecurringExpenseGenerationDao.kt`

- [ ] **Step 1: Create CategoryDao**

```kotlin
package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
```

- [ ] **Step 2: Create ExpenseDao**

```kotlin
package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("""
        SELECT * FROM expenses
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:dateFrom IS NULL OR date >= :dateFrom)
          AND (:dateTo IS NULL OR date <= :dateTo)
          AND (:amountMin IS NULL OR amountCents >= :amountMin)
          AND (:amountMax IS NULL OR amountCents <= :amountMax)
          AND (:noteSearch IS NULL OR note LIKE '%' || :noteSearch || '%' COLLATE NOCASE)
        ORDER BY
            CASE WHEN :sortOrder = 'DATE_DESC' THEN date END DESC,
            CASE WHEN :sortOrder = 'DATE_ASC' THEN date END ASC,
            CASE WHEN :sortOrder = 'AMOUNT_DESC' THEN amountCents END DESC,
            CASE WHEN :sortOrder = 'AMOUNT_ASC' THEN amountCents END ASC
    """)
    fun getFiltered(
        categoryId: Long?,
        dateFrom: String?,
        dateTo: String?,
        amountMin: Long?,
        amountMax: Long?,
        noteSearch: String?,
        sortOrder: String,
    ): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("""
        SELECT e.categoryId, c.name AS categoryName, c.icon AS categoryIcon, SUM(e.amountCents) AS totalCents
        FROM expenses e
        JOIN categories c ON e.categoryId = c.id
        WHERE e.date >= :dateFrom AND e.date <= :dateTo
        GROUP BY e.categoryId
        ORDER BY totalCents DESC
    """)
    suspend fun getCategoryTotals(dateFrom: String, dateTo: String): List<CategoryTotal>

    @Query("""
        SELECT SUM(amountCents) FROM expenses
        WHERE date >= :dateFrom AND date <= :dateTo
    """)
    suspend fun getTotalInRange(dateFrom: String, dateTo: String): Long?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}

data class CategoryTotal(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String?,
    val totalCents: Long,
)
```

- [ ] **Step 3: Create IncomeDao**

```kotlin
package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.db.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("""
        SELECT * FROM income
        WHERE (:isRecurring IS NULL OR isRecurring = :isRecurring)
          AND (:sourceSearch IS NULL OR source LIKE '%' || :sourceSearch || '%' COLLATE NOCASE)
          AND (:dateFrom IS NULL OR date >= :dateFrom)
          AND (:dateTo IS NULL OR date <= :dateTo)
          AND (:amountMin IS NULL OR amountCents >= :amountMin)
          AND (:amountMax IS NULL OR amountCents <= :amountMax)
          AND (:noteSearch IS NULL OR note LIKE '%' || :noteSearch || '%' COLLATE NOCASE)
        ORDER BY
            CASE WHEN :sortOrder = 'DATE_DESC' THEN date END DESC,
            CASE WHEN :sortOrder = 'DATE_ASC' THEN date END ASC,
            CASE WHEN :sortOrder = 'AMOUNT_DESC' THEN amountCents END DESC,
            CASE WHEN :sortOrder = 'AMOUNT_ASC' THEN amountCents END ASC
    """)
    fun getFiltered(
        isRecurring: Boolean?,
        sourceSearch: String?,
        dateFrom: String?,
        dateTo: String?,
        amountMin: Long?,
        amountMax: Long?,
        noteSearch: String?,
        sortOrder: String,
    ): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income WHERE isRecurring = 1 ORDER BY source ASC")
    fun getRecurring(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income WHERE id = :id")
    suspend fun getById(id: Long): IncomeEntity?

    @Query("""
        SELECT SUM(amountCents) FROM income
        WHERE date >= :dateFrom AND date <= :dateTo
    """)
    suspend fun getTotalInRange(dateFrom: String, dateTo: String): Long?

    @Insert
    suspend fun insert(income: IncomeEntity): Long

    @Update
    suspend fun update(income: IncomeEntity)

    @Delete
    suspend fun delete(income: IncomeEntity)
}
```

- [ ] **Step 4: Create RecurringExpenseDao**

```kotlin
package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY startDate ASC")
    fun getAll(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getAllSuspend(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getById(id: Long): RecurringExpenseEntity?

    @Insert
    suspend fun insert(recurringExpense: RecurringExpenseEntity): Long

    @Update
    suspend fun update(recurringExpense: RecurringExpenseEntity)

    @Delete
    suspend fun delete(recurringExpense: RecurringExpenseEntity)
}
```

- [ ] **Step 5: Create RecurringExpenseGenerationDao**

```kotlin
package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity

@Dao
interface RecurringExpenseGenerationDao {
    @Query("""
        SELECT * FROM recurring_expense_generations
        WHERE recurringExpenseId = :recurringExpenseId
    """)
    suspend fun getByRecurringExpenseId(recurringExpenseId: Long): List<RecurringExpenseGenerationEntity>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM recurring_expense_generations
            WHERE recurringExpenseId = :recurringExpenseId AND generatedForMonth = :month
        )
    """)
    suspend fun existsForMonth(recurringExpenseId: Long, month: String): Boolean

    @Insert
    suspend fun insert(generation: RecurringExpenseGenerationEntity): Long
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/data/db/dao/
git commit -m "feat: add Room DAOs with filtering, sorting, and report queries"
```

---

### Task 5: Room Database, Default Categories, and Hilt Modules

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/data/db/AppDatabase.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/seed/DefaultCategories.kt`
- Create: `app/src/main/java/com/example/expensetracker/di/DatabaseModule.kt`

- [ ] **Step 1: Create DefaultCategories**

```kotlin
package com.example.expensetracker.data.seed

import com.example.expensetracker.data.db.entity.CategoryEntity

val defaultCategories = listOf(
    CategoryEntity(name = "Housing", icon = "home", isDefault = true),
    CategoryEntity(name = "Food & Groceries", icon = "restaurant", isDefault = true),
    CategoryEntity(name = "Transport", icon = "directions_car", isDefault = true),
    CategoryEntity(name = "Utilities", icon = "bolt", isDefault = true),
    CategoryEntity(name = "Healthcare", icon = "local_hospital", isDefault = true),
    CategoryEntity(name = "Entertainment", icon = "movie", isDefault = true),
    CategoryEntity(name = "Clothing", icon = "checkroom", isDefault = true),
    CategoryEntity(name = "Education", icon = "school", isDefault = true),
    CategoryEntity(name = "Savings & Investments", icon = "savings", isDefault = true),
    CategoryEntity(name = "Other", icon = "more_horiz", isDefault = true),
)
```

- [ ] **Step 2: Create AppDatabase**

```kotlin
package com.example.expensetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expensetracker.data.db.dao.CategoryDao
import com.example.expensetracker.data.db.dao.ExpenseDao
import com.example.expensetracker.data.db.dao.IncomeDao
import com.example.expensetracker.data.db.dao.RecurringExpenseDao
import com.example.expensetracker.data.db.dao.RecurringExpenseGenerationDao
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        IncomeEntity::class,
        RecurringExpenseEntity::class,
        RecurringExpenseGenerationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun recurringExpenseGenerationDao(): RecurringExpenseGenerationDao
}
```

- [ ] **Step 3: Create DatabaseModule**

```kotlin
package com.example.expensetracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.db.dao.CategoryDao
import com.example.expensetracker.data.db.dao.ExpenseDao
import com.example.expensetracker.data.db.dao.IncomeDao
import com.example.expensetracker.data.db.dao.RecurringExpenseDao
import com.example.expensetracker.data.db.dao.RecurringExpenseGenerationDao
import com.example.expensetracker.data.seed.defaultCategories
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expense_tracker.db",
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        categoryDaoProvider.get().insertAll(defaultCategories)
                    }
                }
            })
            .build()
    }

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideIncomeDao(db: AppDatabase): IncomeDao = db.incomeDao()

    @Provides
    fun provideRecurringExpenseDao(db: AppDatabase): RecurringExpenseDao = db.recurringExpenseDao()

    @Provides
    fun provideRecurringExpenseGenerationDao(db: AppDatabase): RecurringExpenseGenerationDao =
        db.recurringExpenseGenerationDao()
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/data/db/AppDatabase.kt \
       app/src/main/java/com/example/expensetracker/data/seed/ \
       app/src/main/java/com/example/expensetracker/di/
git commit -m "feat: add Room database, default category seeding, and Hilt DI modules"
```

---

### Task 6: Repositories

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/data/repository/CategoryRepository.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/repository/ExpenseRepository.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/repository/IncomeRepository.kt`
- Create: `app/src/main/java/com/example/expensetracker/data/repository/RecurringExpenseRepository.kt`
- Create: `app/src/main/java/com/example/expensetracker/di/RepositoryModule.kt`

- [ ] **Step 1: Create CategoryRepository**

```kotlin
package com.example.expensetracker.data.repository

import com.example.expensetracker.data.db.dao.CategoryDao
import com.example.expensetracker.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun getAll(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun insert(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun update(category: CategoryEntity) = categoryDao.update(category)

    suspend fun delete(category: CategoryEntity) = categoryDao.delete(category)
}
```

- [ ] **Step 2: Create ExpenseRepository**

```kotlin
package com.example.expensetracker.data.repository

import com.example.expensetracker.data.db.dao.CategoryTotal
import com.example.expensetracker.data.db.dao.ExpenseDao
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.domain.model.ExpenseFilter
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
) {
    fun getFiltered(filter: ExpenseFilter): Flow<List<ExpenseEntity>> =
        expenseDao.getFiltered(
            categoryId = filter.categoryId,
            dateFrom = filter.dateFrom?.toString(),
            dateTo = filter.dateTo?.toString(),
            amountMin = filter.amountMinCents,
            amountMax = filter.amountMaxCents,
            noteSearch = filter.noteSearch,
            sortOrder = filter.sortOrder.name,
        )

    suspend fun getById(id: Long): ExpenseEntity? = expenseDao.getById(id)

    suspend fun getCategoryTotals(from: LocalDate, to: LocalDate): List<CategoryTotal> =
        expenseDao.getCategoryTotals(from.toString(), to.toString())

    suspend fun getTotalInRange(from: LocalDate, to: LocalDate): Long =
        expenseDao.getTotalInRange(from.toString(), to.toString()) ?: 0L

    suspend fun insert(expense: ExpenseEntity): Long = expenseDao.insert(expense)

    suspend fun update(expense: ExpenseEntity) = expenseDao.update(expense)

    suspend fun delete(expense: ExpenseEntity) = expenseDao.delete(expense)
}
```

- [ ] **Step 3: Create IncomeRepository**

```kotlin
package com.example.expensetracker.data.repository

import com.example.expensetracker.data.db.dao.IncomeDao
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.domain.model.IncomeFilter
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao,
) {
    fun getRecurring(): Flow<List<IncomeEntity>> = incomeDao.getRecurring()

    fun getFiltered(filter: IncomeFilter): Flow<List<IncomeEntity>> =
        incomeDao.getFiltered(
            isRecurring = filter.isRecurring,
            sourceSearch = filter.sourceSearch,
            dateFrom = filter.dateFrom?.toString(),
            dateTo = filter.dateTo?.toString(),
            amountMin = filter.amountMinCents,
            amountMax = filter.amountMaxCents,
            noteSearch = filter.noteSearch,
            sortOrder = filter.sortOrder.name,
        )

    suspend fun getById(id: Long): IncomeEntity? = incomeDao.getById(id)

    suspend fun getTotalInRange(from: LocalDate, to: LocalDate): Long =
        incomeDao.getTotalInRange(from.toString(), to.toString()) ?: 0L

    suspend fun insert(income: IncomeEntity): Long = incomeDao.insert(income)

    suspend fun update(income: IncomeEntity) = incomeDao.update(income)

    suspend fun delete(income: IncomeEntity) = incomeDao.delete(income)
}
```

- [ ] **Step 4: Create RecurringExpenseRepository**

```kotlin
package com.example.expensetracker.data.repository

import com.example.expensetracker.data.db.dao.RecurringExpenseDao
import com.example.expensetracker.data.db.dao.RecurringExpenseGenerationDao
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseRepository @Inject constructor(
    private val recurringExpenseDao: RecurringExpenseDao,
    private val generationDao: RecurringExpenseGenerationDao,
) {
    fun getAll(): Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.getAll()

    suspend fun getAllSuspend(): List<RecurringExpenseEntity> = recurringExpenseDao.getAllSuspend()

    suspend fun getById(id: Long): RecurringExpenseEntity? = recurringExpenseDao.getById(id)

    suspend fun insert(recurringExpense: RecurringExpenseEntity): Long =
        recurringExpenseDao.insert(recurringExpense)

    suspend fun update(recurringExpense: RecurringExpenseEntity) =
        recurringExpenseDao.update(recurringExpense)

    suspend fun delete(recurringExpense: RecurringExpenseEntity) =
        recurringExpenseDao.delete(recurringExpense)

    suspend fun isGeneratedForMonth(recurringExpenseId: Long, month: String): Boolean =
        generationDao.existsForMonth(recurringExpenseId, month)

    suspend fun recordGeneration(generation: RecurringExpenseGenerationEntity): Long =
        generationDao.insert(generation)
}
```

- [ ] **Step 5: Create RepositoryModule**

```kotlin
package com.example.expensetracker.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
// Repositories use @Inject constructor and @Singleton, so no explicit bindings needed.
// This module exists as a placeholder if future manual bindings are required.
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/data/repository/ \
       app/src/main/java/com/example/expensetracker/di/RepositoryModule.kt
git commit -m "feat: add repository layer for expenses, income, categories, recurring expenses"
```

---

### Task 7: Recurring Expense Generation Use Cases

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/domain/usecase/GenerateRecurringExpensesUseCase.kt`
- Create: `app/src/test/java/com/example/expensetracker/domain/usecase/GenerateRecurringExpensesUseCaseTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/expensetracker/domain/usecase/GenerateRecurringExpensesUseCaseTest.kt`:

```kotlin
package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import com.example.expensetracker.domain.model.Interval
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GenerateRecurringExpensesUseCaseTest {

    private val expenseRepository = mockk<ExpenseRepository>(relaxed = true)
    private val recurringExpenseRepository = mockk<RecurringExpenseRepository>(relaxed = true)
    private val useCase = GenerateRecurringExpensesUseCase(expenseRepository, recurringExpenseRepository)

    @Test
    fun `generates monthly expense for current month`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1,
            amountCents = 50000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-01-15",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        val currentMonth = YearMonth.of(2026, 3)
        useCase(currentMonth)

        val expenseSlot = slot<ExpenseEntity>()
        coVerify(atLeast = 1) { expenseRepository.insert(capture(expenseSlot)) }
        assertEquals(50000L, expenseSlot.captured.amountCents)
        assertEquals(1L, expenseSlot.captured.recurringExpenseId)
    }

    @Test
    fun `skips already generated months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1,
            amountCents = 50000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-01-15",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-03") } returns true
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-01") } returns true
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-02") } returns true

        useCase(YearMonth.of(2026, 3))

        coVerify(exactly = 0) { expenseRepository.insert(any()) }
    }

    @Test
    fun `quarterly generates only on correct months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 2,
            amountCents = 100000,
            categoryId = 2,
            interval = Interval.QUARTERLY,
            startDate = "2026-01-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 6))

        // Should generate for 2026-01 and 2026-04 (quarterly from Jan)
        coVerify(exactly = 2) { expenseRepository.insert(any()) }
    }

    @Test
    fun `clamps day of month for short months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 3,
            amountCents = 30000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-02-28",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 2))

        val expenseSlot = slot<ExpenseEntity>()
        coVerify { expenseRepository.insert(capture(expenseSlot)) }
        assertEquals(LocalDate.of(2026, 2, 28), expenseSlot.captured.date)
    }
}
```

- [ ] **Step 2: Implement GenerateRecurringExpensesUseCase**

Create `app/src/main/java/com/example/expensetracker/domain/usecase/GenerateRecurringExpensesUseCase.kt`:

```kotlin
package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class GenerateRecurringExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
) {
    suspend operator fun invoke(currentMonth: YearMonth) {
        val activeRecurring = recurringExpenseRepository.getAllSuspend()
        for (recurring in activeRecurring) {
            val startDate = LocalDate.parse(recurring.startDate)
            val startMonth = YearMonth.from(startDate)
            val dayOfMonth = startDate.dayOfMonth
            val monthsToGenerate = generateDueMonths(recurring, startMonth, currentMonth)
            for (month in monthsToGenerate) {
                val monthString = month.toString()
                if (recurringExpenseRepository.isGeneratedForMonth(recurring.id, monthString)) {
                    continue
                }
                val day = minOf(dayOfMonth, month.lengthOfMonth())
                val date = month.atDay(day)
                val expenseId = expenseRepository.insert(
                    ExpenseEntity(
                        amountCents = recurring.amountCents,
                        categoryId = recurring.categoryId,
                        date = date,
                        note = recurring.note,
                        recurringExpenseId = recurring.id,
                    )
                )
                recurringExpenseRepository.recordGeneration(
                    RecurringExpenseGenerationEntity(
                        recurringExpenseId = recurring.id,
                        generatedForMonth = monthString,
                        expenseId = expenseId,
                    )
                )
            }
        }
    }

    private fun generateDueMonths(
        recurring: RecurringExpenseEntity,
        startMonth: YearMonth,
        currentMonth: YearMonth,
    ): List<YearMonth> {
        val months = mutableListOf<YearMonth>()
        var month = startMonth
        while (!month.isAfter(currentMonth)) {
            months.add(month)
            month = month.plusMonths(recurring.interval.months.toLong())
        }
        return months
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/domain/usecase/GenerateRecurringExpensesUseCase.kt \
       app/src/test/java/com/example/expensetracker/domain/usecase/GenerateRecurringExpensesUseCaseTest.kt
git commit -m "feat: add recurring expense generation use case with tests"
```

---

### Task 8: Report Use Cases

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/domain/usecase/CategoryReportUseCase.kt`
- Create: `app/src/main/java/com/example/expensetracker/domain/usecase/IncomeVsExpensesReportUseCase.kt`
- Create: `app/src/test/java/com/example/expensetracker/domain/usecase/CategoryReportUseCaseTest.kt`
- Create: `app/src/test/java/com/example/expensetracker/domain/usecase/IncomeVsExpensesReportUseCaseTest.kt`

- [ ] **Step 1: Write CategoryReportUseCase test**

Create `app/src/test/java/com/example/expensetracker/domain/usecase/CategoryReportUseCaseTest.kt`:

```kotlin
package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.dao.CategoryTotal
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.domain.model.CategorySpending
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CategoryReportUseCaseTest {

    private val expenseRepository = mockk<ExpenseRepository>()
    private val useCase = CategoryReportUseCase(expenseRepository)

    @Test
    fun `calculates percentages correctly`() = runTest {
        coEvery {
            expenseRepository.getCategoryTotals(any(), any())
        } returns listOf(
            CategoryTotal(1, "Food", "restaurant", 60000),
            CategoryTotal(2, "Transport", "directions_car", 40000),
        )

        val report = useCase(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
        )

        assertEquals(100000L, report.totalCents)
        assertEquals(2, report.items.size)
        assertEquals(60.0, report.items[0].percentage, 0.01)
        assertEquals(40.0, report.items[1].percentage, 0.01)
    }

    @Test
    fun `returns empty report when no expenses`() = runTest {
        coEvery { expenseRepository.getCategoryTotals(any(), any()) } returns emptyList()

        val report = useCase(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
        )

        assertEquals(0L, report.totalCents)
        assertEquals(0, report.items.size)
    }
}
```

- [ ] **Step 2: Implement CategoryReportUseCase**

Create `app/src/main/java/com/example/expensetracker/domain/usecase/CategoryReportUseCase.kt`:

```kotlin
package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.domain.model.CategoryReport
import com.example.expensetracker.domain.model.CategorySpending
import java.time.LocalDate
import javax.inject.Inject

class CategoryReportUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(from: LocalDate, to: LocalDate): CategoryReport {
        val totals = expenseRepository.getCategoryTotals(from, to)
        val grandTotal = totals.sumOf { it.totalCents }
        val items = totals.map { ct ->
            CategorySpending(
                categoryId = ct.categoryId,
                categoryName = ct.categoryName,
                categoryIcon = ct.categoryIcon,
                totalCents = ct.totalCents,
                percentage = if (grandTotal > 0) ct.totalCents.toDouble() / grandTotal * 100.0 else 0.0,
            )
        }
        return CategoryReport(items = items, totalCents = grandTotal)
    }
}
```

- [ ] **Step 3: Write IncomeVsExpensesReportUseCase test**

Create `app/src/test/java/com/example/expensetracker/domain/usecase/IncomeVsExpensesReportUseCaseTest.kt`:

```kotlin
package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.IncomeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class IncomeVsExpensesReportUseCaseTest {

    private val expenseRepository = mockk<ExpenseRepository>()
    private val incomeRepository = mockk<IncomeRepository>()
    private val useCase = IncomeVsExpensesReportUseCase(expenseRepository, incomeRepository)

    @Test
    fun `generates monthly buckets for a quarter`() = runTest {
        coEvery { incomeRepository.getTotalInRange(any(), any()) } returns 200000L
        coEvery { expenseRepository.getTotalInRange(any(), any()) } returns 150000L

        val report = useCase(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
        )

        assertEquals(3, report.items.size)
        assertEquals("Jan", report.items[0].label)
        assertEquals("Feb", report.items[1].label)
        assertEquals("Mar", report.items[2].label)
        assertEquals(600000L, report.totalIncomeCents)
        assertEquals(450000L, report.totalExpenseCents)
    }
}
```

- [ ] **Step 4: Implement IncomeVsExpensesReportUseCase**

Create `app/src/main/java/com/example/expensetracker/domain/usecase/IncomeVsExpensesReportUseCase.kt`:

```kotlin
package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.IncomeVsExpensesReport
import com.example.expensetracker.domain.model.MonthlyBalance
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class IncomeVsExpensesReportUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
) {
    suspend operator fun invoke(from: LocalDate, to: LocalDate): IncomeVsExpensesReport {
        val items = mutableListOf<MonthlyBalance>()
        var month = YearMonth.from(from)
        val endMonth = YearMonth.from(to)

        while (!month.isAfter(endMonth)) {
            val monthStart = maxOf(month.atDay(1), from)
            val monthEnd = minOf(month.atEndOfMonth(), to)
            val income = incomeRepository.getTotalInRange(monthStart, monthEnd)
            val expenses = expenseRepository.getTotalInRange(monthStart, monthEnd)
            val label = month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            items.add(MonthlyBalance(label = label, incomeCents = income, expenseCents = expenses))
            month = month.plusMonths(1)
        }

        return IncomeVsExpensesReport(
            items = items,
            totalIncomeCents = items.sumOf { it.incomeCents },
            totalExpenseCents = items.sumOf { it.expenseCents },
        )
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/domain/usecase/CategoryReportUseCase.kt \
       app/src/main/java/com/example/expensetracker/domain/usecase/IncomeVsExpensesReportUseCase.kt \
       app/src/test/java/com/example/expensetracker/domain/usecase/
git commit -m "feat: add report use cases for category spending and income vs expenses"
```

---

### Task 9: Excel Export Use Case

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/domain/usecase/ExportToExcelUseCase.kt`
- Create: `app/src/test/java/com/example/expensetracker/domain/usecase/ExportToExcelUseCaseTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/expensetracker/domain/usecase/ExportToExcelUseCaseTest.kt`:

```kotlin
package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate

class ExportToExcelUseCaseTest {

    private val useCase = ExportToExcelUseCase()

    @Test
    fun `generates xlsx with expense and income sheets`() = runTest {
        val expenses = listOf(
            ExpenseEntity(id = 1, amountCents = 1250, categoryId = 1, date = LocalDate.of(2026, 3, 15), note = "Lunch"),
        )
        val income = listOf(
            IncomeEntity(id = 1, amountCents = 300000, source = "Salary", date = LocalDate.of(2026, 3, 1)),
        )
        val categories = mapOf(1L to CategoryEntity(id = 1, name = "Food"))

        val file = File.createTempFile("test_export", ".xlsx")
        try {
            useCase(expenses, income, categories, file)

            val workbook = XSSFWorkbook(FileInputStream(file))
            assertEquals(2, workbook.numberOfSheets)
            assertEquals("Expenses", workbook.getSheetAt(0).sheetName)
            assertEquals("Income", workbook.getSheetAt(1).sheetName)

            val expenseSheet = workbook.getSheetAt(0)
            assertEquals("Date", expenseSheet.getRow(0).getCell(0).stringCellValue)
            assertEquals("2026-03-15", expenseSheet.getRow(1).getCell(0).stringCellValue)
            assertEquals(12.50, expenseSheet.getRow(1).getCell(1).numericCellValue, 0.01)
            assertEquals("Food", expenseSheet.getRow(1).getCell(2).stringCellValue)

            val incomeSheet = workbook.getSheetAt(1)
            assertEquals("Salary", incomeSheet.getRow(1).getCell(2).stringCellValue)

            workbook.close()
        } finally {
            file.delete()
        }
    }
}
```

- [ ] **Step 2: Implement ExportToExcelUseCase**

Create `app/src/main/java/com/example/expensetracker/domain/usecase/ExportToExcelUseCase.kt`:

```kotlin
package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ExportToExcelUseCase @Inject constructor() {

    operator fun invoke(
        expenses: List<ExpenseEntity>,
        income: List<IncomeEntity>,
        categories: Map<Long, CategoryEntity>,
        outputFile: File,
    ) {
        val workbook = XSSFWorkbook()
        val headerStyle = createHeaderStyle(workbook)

        writeExpenseSheet(workbook, headerStyle, expenses, categories)
        writeIncomeSheet(workbook, headerStyle, income)

        FileOutputStream(outputFile).use { workbook.write(it) }
        workbook.close()
    }

    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        style.setFont(font)
        return style
    }

    private fun writeExpenseSheet(
        workbook: XSSFWorkbook,
        headerStyle: CellStyle,
        expenses: List<ExpenseEntity>,
        categories: Map<Long, CategoryEntity>,
    ) {
        val sheet = workbook.createSheet("Expenses")
        val headers = listOf("Date", "Amount (€)", "Category", "Note")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, header ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        expenses.forEachIndexed { index, expense ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(expense.date.toString())
            row.createCell(1).setCellValue(expense.amountCents / 100.0)
            row.createCell(2).setCellValue(categories[expense.categoryId]?.name ?: "Unknown")
            row.createCell(3).setCellValue(expense.note ?: "")
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }

    private fun writeIncomeSheet(
        workbook: XSSFWorkbook,
        headerStyle: CellStyle,
        income: List<IncomeEntity>,
    ) {
        val sheet = workbook.createSheet("Income")
        val headers = listOf("Date", "Amount (€)", "Source", "Note")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, header ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        income.forEachIndexed { index, item ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(item.date.toString())
            row.createCell(1).setCellValue(item.amountCents / 100.0)
            row.createCell(2).setCellValue(item.source)
            row.createCell(3).setCellValue(item.note ?: "")
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/domain/usecase/ExportToExcelUseCase.kt \
       app/src/test/java/com/example/expensetracker/domain/usecase/ExportToExcelUseCaseTest.kt
git commit -m "feat: add Excel export use case with Apache POI"
```

---

### Task 10: Theme and Shared UI Components

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/theme/Type.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/components/CurrencyFormatter.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/components/AmountInput.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/components/CategoryIcon.kt`

- [ ] **Step 1: Create Color.kt**

```kotlin
package com.example.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val IncomeGreen = Color(0xFF4CAF50)
val ExpenseRed = Color(0xFFF44336)
```

- [ ] **Step 2: Create Type.kt**

```kotlin
package com.example.expensetracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)
```

- [ ] **Step 3: Create Theme.kt**

```kotlin
package com.example.expensetracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
```

- [ ] **Step 4: Create CurrencyFormatter.kt**

```kotlin
package com.example.expensetracker.ui.components

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val euroFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY).apply {
        currency = java.util.Currency.getInstance("EUR")
    }

    fun format(cents: Long): String = euroFormat.format(cents / 100.0)
}
```

- [ ] **Step 5: Create AmountInput.kt**

```kotlin
package com.example.expensetracker.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount (€)",
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Allow only valid decimal input: digits and at most one decimal point with max 2 decimal places
            val filtered = newValue.filter { it.isDigit() || it == '.' }
            val parts = filtered.split(".")
            val isValid = parts.size <= 2 && (parts.size < 2 || parts[1].length <= 2)
            if (isValid) {
                onValueChange(filtered)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier,
    )
}

fun amountStringToCents(value: String): Long? {
    val amount = value.toDoubleOrNull() ?: return null
    return (amount * 100).toLong()
}

fun centsToAmountString(cents: Long): String {
    val euros = cents / 100
    val remainingCents = cents % 100
    return if (remainingCents == 0L) "$euros" else "$euros.${"%02d".format(remainingCents)}"
}
```

- [ ] **Step 6: Create CategoryIcon**

Create `app/src/main/java/com/example/expensetracker/ui/components/CategoryIcon.kt`:

```kotlin
package com.example.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private val iconMap = mapOf(
    "home" to Icons.Filled.Home,
    "restaurant" to Icons.Filled.Restaurant,
    "directions_car" to Icons.Filled.DirectionsCar,
    "bolt" to Icons.Filled.Bolt,
    "local_hospital" to Icons.Filled.LocalHospital,
    "movie" to Icons.Filled.Movie,
    "checkroom" to Icons.Filled.Checkroom,
    "school" to Icons.Filled.School,
    "savings" to Icons.Filled.Savings,
    "more_horiz" to Icons.Filled.MoreHoriz,
)

fun categoryIconFor(iconName: String?): ImageVector =
    iconName?.let { iconMap[it] } ?: Icons.Filled.MoreHoriz

@Composable
fun CategoryIcon(
    iconName: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = categoryIconFor(iconName),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/theme/ \
       app/src/main/java/com/example/expensetracker/ui/components/
git commit -m "feat: add Material 3 theme, currency formatter, amount input, and category icon component"
```

---

### Task 11: Navigation Setup

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/navigation/BottomNavBar.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt`
- Create: `app/src/main/java/com/example/expensetracker/MainActivity.kt`

- [ ] **Step 1: Create Screen routes**

```kotlin
package com.example.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object ExpenseList : Screen("expenses")
    data object AddEditExpense : Screen("expenses/edit?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "expenses/edit?id=$id" else "expenses/edit"
    }
    data object IncomeList : Screen("income")
    data object AddEditIncome : Screen("income/edit?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "income/edit?id=$id" else "income/edit"
    }
    data object RecurringList : Screen("recurring")
    data object AddEditRecurringExpense : Screen("recurring/edit?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "recurring/edit?id=$id" else "recurring/edit"
    }
    data object AddEditRecurringIncome : Screen("recurring/income/edit?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "recurring/income/edit?id=$id" else "recurring/income/edit"
    }
    data object Reports : Screen("reports")
    data object CategoryManagement : Screen("categories")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.ExpenseList, "Expenses", Icons.Filled.AccountBalanceWallet),
    BottomNavItem(Screen.IncomeList, "Income", Icons.Filled.AttachMoney),
    BottomNavItem(Screen.RecurringList, "Recurring", Icons.Filled.Repeat),
    BottomNavItem(Screen.Reports, "Reports", Icons.Filled.Assessment),
)
```

- [ ] **Step 2: Create BottomNavBar**

```kotlin
package com.example.expensetracker.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.screen.route,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.ExpenseList.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}
```

- [ ] **Step 3: Create NavGraph (placeholder screens for now)**

```kotlin
package com.example.expensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensetracker.ui.expenses.ExpenseListScreen
import com.example.expensetracker.ui.expenses.AddEditExpenseScreen
import com.example.expensetracker.ui.income.IncomeListScreen
import com.example.expensetracker.ui.income.AddEditIncomeScreen
import com.example.expensetracker.ui.recurring.RecurringListScreen
import com.example.expensetracker.ui.recurring.AddEditRecurringExpenseScreen
import com.example.expensetracker.ui.recurring.AddEditRecurringIncomeScreen
import com.example.expensetracker.ui.reports.ReportsScreen
import com.example.expensetracker.ui.categories.CategoryManagementScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ExpenseList.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.ExpenseList.route) {
                ExpenseListScreen(
                    onAddExpense = { navController.navigate(Screen.AddEditExpense.createRoute()) },
                    onEditExpense = { id -> navController.navigate(Screen.AddEditExpense.createRoute(id)) },
                    onManageCategories = { navController.navigate(Screen.CategoryManagement.route) },
                )
            }
            composable(
                route = Screen.AddEditExpense.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id")?.takeIf { it != -1L }
                AddEditExpenseScreen(
                    expenseId = id,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.IncomeList.route) {
                IncomeListScreen(
                    onAddIncome = { navController.navigate(Screen.AddEditIncome.createRoute()) },
                    onEditIncome = { id -> navController.navigate(Screen.AddEditIncome.createRoute(id)) },
                )
            }
            composable(
                route = Screen.AddEditIncome.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id")?.takeIf { it != -1L }
                AddEditIncomeScreen(
                    incomeId = id,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.RecurringList.route) {
                RecurringListScreen(
                    onAddRecurring = { navController.navigate(Screen.AddEditRecurringExpense.createRoute()) },
                    onEditRecurring = { id -> navController.navigate(Screen.AddEditRecurringExpense.createRoute(id)) },
                    onAddRecurringIncome = { navController.navigate(Screen.AddEditRecurringIncome.createRoute()) },
                    onEditRecurringIncome = { id -> navController.navigate(Screen.AddEditRecurringIncome.createRoute(id)) },
                )
            }
            composable(
                route = Screen.AddEditRecurringExpense.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id")?.takeIf { it != -1L }
                AddEditRecurringExpenseScreen(
                    recurringExpenseId = id,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Screen.AddEditRecurringIncome.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id")?.takeIf { it != -1L }
                AddEditRecurringIncomeScreen(
                    recurringIncomeId = id,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen()
            }
            composable(Screen.CategoryManagement.route) {
                CategoryManagementScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
```

- [ ] **Step 4: Create MainActivity**

```kotlin
package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.expensetracker.ui.navigation.NavGraph
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                NavGraph()
            }
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/navigation/ \
       app/src/main/java/com/example/expensetracker/MainActivity.kt
git commit -m "feat: add navigation graph with bottom nav bar and screen routing"
```

---

### Task 12: Expense List Screen and ViewModel

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/expenses/ExpenseListViewModel.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/expenses/ExpenseListScreen.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/components/FilterBar.kt`

- [ ] **Step 1: Create FilterBar component**

```kotlin
package com.example.expensetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.model.SortOrder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    noteSearch: String,
    onNoteSearchChange: (String) -> Unit,
    amountMin: String,
    onAmountMinChange: (String) -> Unit,
    amountMax: String,
    onAmountMaxChange: (String) -> Unit,
    dateFrom: LocalDate?,
    onDateFromChange: (LocalDate?) -> Unit,
    dateTo: LocalDate?,
    onDateToChange: (LocalDate?) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    extraFilters: @Composable () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = noteSearch,
                onValueChange = onNoteSearchChange,
                label = { Text("Search notes") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filters")
            }
            IconButton(onClick = { sortMenuExpanded = true }) {
                Icon(Icons.Filled.Sort, contentDescription = "Sort")
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.displayName()) },
                            onClick = {
                                onSortOrderChange(order)
                                sortMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateFrom?.toString() ?: "",
                        onValueChange = {},
                        label = { Text("From date") },
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.weight(1f).clickable { showDateFromPicker = true },
                    )
                    OutlinedTextField(
                        value = dateTo?.toString() ?: "",
                        onValueChange = {},
                        label = { Text("To date") },
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.weight(1f).clickable { showDateToPicker = true },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmountInput(
                        value = amountMin,
                        onValueChange = onAmountMinChange,
                        label = "Min (€)",
                        modifier = Modifier.weight(1f),
                    )
                    AmountInput(
                        value = amountMax,
                        onValueChange = onAmountMaxChange,
                        label = "Max (€)",
                        modifier = Modifier.weight(1f),
                    )
                }
                extraFilters()
            }
        }
    }

    if (showDateFromPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateFrom?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDateFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateFromChange(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDateFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateFromPicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }

    if (showDateToPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateTo?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDateToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateToChange(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDateToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateToPicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }
}

private fun SortOrder.displayName(): String = when (this) {
    SortOrder.DATE_DESC -> "Date (newest)"
    SortOrder.DATE_ASC -> "Date (oldest)"
    SortOrder.AMOUNT_DESC -> "Amount (highest)"
    SortOrder.AMOUNT_ASC -> "Amount (lowest)"
}
```

- [ ] **Step 2: Create ExpenseListViewModel**

```kotlin
package com.example.expensetracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.domain.model.ExpenseFilter
import com.example.expensetracker.domain.model.SortOrder
import com.example.expensetracker.ui.components.amountStringToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCategoryId = MutableStateFlow<Long?>(null)
    val noteSearch = MutableStateFlow("")
    val amountMin = MutableStateFlow("")
    val amountMax = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val dateFrom = MutableStateFlow<LocalDate?>(YearMonth.now().atDay(1))
    val dateTo = MutableStateFlow<LocalDate?>(YearMonth.now().atEndOfMonth())

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<ExpenseEntity>> = combine(
        selectedCategoryId, noteSearch, amountMin, amountMax, sortOrder, dateFrom, dateTo,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ExpenseFilter(
            categoryId = values[0] as Long?,
            noteSearch = (values[1] as String).ifBlank { null },
            amountMinCents = amountStringToCents(values[2] as String),
            amountMaxCents = amountStringToCents(values[3] as String),
            sortOrder = values[4] as SortOrder,
            dateFrom = values[5] as LocalDate?,
            dateTo = values[6] as LocalDate?,
        )
    }.flatMapLatest { filter ->
        expenseRepository.getFiltered(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseRepository.delete(expense)
        }
    }
}
```

- [ ] **Step 3: Create ExpenseListScreen**

```kotlin
package com.example.expensetracker.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.ui.components.CategoryIcon
import com.example.expensetracker.ui.components.CurrencyFormatter
import com.example.expensetracker.ui.components.FilterBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onManageCategories: () -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val noteSearch by viewModel.noteSearch.collectAsStateWithLifecycle()
    val amountMin by viewModel.amountMin.collectAsStateWithLifecycle()
    val amountMax by viewModel.amountMax.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                actions = {
                    IconButton(onClick = onManageCategories) {
                        Icon(Icons.Filled.Settings, contentDescription = "Manage categories")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val dateFrom by viewModel.dateFrom.collectAsStateWithLifecycle()
            val dateTo by viewModel.dateTo.collectAsStateWithLifecycle()

            FilterBar(
                noteSearch = noteSearch,
                onNoteSearchChange = { viewModel.noteSearch.value = it },
                amountMin = amountMin,
                onAmountMinChange = { viewModel.amountMin.value = it },
                amountMax = amountMax,
                onAmountMaxChange = { viewModel.amountMax.value = it },
                dateFrom = dateFrom,
                onDateFromChange = { viewModel.dateFrom.value = it },
                dateTo = dateTo,
                onDateToChange = { viewModel.dateTo.value = it },
                sortOrder = sortOrder,
                onSortOrderChange = { viewModel.sortOrder.value = it },
                extraFilters = {
                    var categoryMenuExpanded by remember { mutableStateOf(false) }
                    val selectedCategory = categories.find { it.id == selectedCategoryId }
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "All categories",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("All categories") },
                                onClick = {
                                    viewModel.selectedCategoryId.value = null
                                    categoryMenuExpanded = false
                                },
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CategoryIcon(category.icon, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(category.name)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectedCategoryId.value = category.id
                                        categoryMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
            )

            if (expenses.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No expenses yet", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(expenses, key = { it.id }) { expense ->
                        val category = categories.find { it.id == expense.categoryId }
                        val categoryName = category?.name ?: "Unknown"
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteExpense(expense)
                                    true
                                } else false
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                        ) {
                            ExpenseItem(
                                expense = expense,
                                categoryName = categoryName,
                                categoryIcon = category?.icon,
                                onClick = { onEditExpense(expense.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseItem(
    expense: ExpenseEntity,
    categoryName: String,
    categoryIcon: String?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                CategoryIcon(categoryIcon, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(categoryName, style = MaterialTheme.typography.titleMedium)
                    Text(expense.date.toString(), style = MaterialTheme.typography.bodySmall)
                    if (!expense.note.isNullOrBlank()) {
                        Text(expense.note, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Text(
                CurrencyFormatter.format(expense.amountCents),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/expenses/ExpenseListScreen.kt \
       app/src/main/java/com/example/expensetracker/ui/expenses/ExpenseListViewModel.kt \
       app/src/main/java/com/example/expensetracker/ui/components/FilterBar.kt
git commit -m "feat: add expense list screen with filtering, sorting, and swipe-to-delete"
```

---

### Task 13: Add/Edit Expense Screen

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/expenses/AddEditExpenseViewModel.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/expenses/AddEditExpenseScreen.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/components/CategoryPicker.kt`

- [ ] **Step 1: Create CategoryPicker**

```kotlin
package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.db.entity.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "Select category",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            leadingIcon = selectedCategory?.let {
                { CategoryIcon(it.icon, modifier = Modifier.size(20.dp)) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryIcon(category.icon, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category.name)
                        }
                    },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create AddEditExpenseViewModel**

```kotlin
package com.example.expensetracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val amount = MutableStateFlow("")
    val categoryId = MutableStateFlow<Long?>(null)
    val date = MutableStateFlow(LocalDate.now())
    val note = MutableStateFlow("")

    private var editingExpenseId: Long? = null

    fun loadExpense(id: Long) {
        viewModelScope.launch {
            val expense = expenseRepository.getById(id) ?: return@launch
            editingExpenseId = expense.id
            amount.value = centsToAmountString(expense.amountCents)
            categoryId.value = expense.categoryId
            date.value = expense.date
            note.value = expense.note ?: ""
        }
    }

    fun delete(onComplete: () -> Unit) {
        val id = editingExpenseId ?: return
        viewModelScope.launch {
            val entity = expenseRepository.getById(id) ?: return@launch
            expenseRepository.delete(entity)
            onComplete()
        }
    }

    fun save(onComplete: () -> Unit) {
        val cents = amountStringToCents(amount.value) ?: return
        val catId = categoryId.value ?: return

        viewModelScope.launch {
            val entity = ExpenseEntity(
                id = editingExpenseId ?: 0,
                amountCents = cents,
                categoryId = catId,
                date = date.value,
                note = note.value.ifBlank { null },
            )
            if (editingExpenseId != null) {
                expenseRepository.update(entity)
            } else {
                expenseRepository.insert(entity)
            }
            onComplete()
        }
    }
}
```

- [ ] **Step 3: Create AddEditExpenseScreen**

```kotlin
package com.example.expensetracker.ui.expenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.ui.components.AmountInput
import com.example.expensetracker.ui.components.CategoryPicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    expenseId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditExpenseViewModel = hiltViewModel(),
) {
    LaunchedEffect(expenseId) {
        if (expenseId != null) viewModel.loadExpense(expenseId)
    }

    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val categoryId by viewModel.categoryId.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId != null) "Edit Expense" else "Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (expenseId != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete expense",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
        ) {
            AmountInput(
                value = amount,
                onValueChange = { viewModel.amount.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            CategoryPicker(
                categories = categories,
                selectedCategoryId = categoryId,
                onCategorySelected = { viewModel.categoryId.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = date.toString(),
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    showDatePicker = true
                                }
                            }
                        }
                    },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { viewModel.note.value = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save(onComplete = onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (expenseId != null) "Update" else "Add")
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete expense?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.delete(onComplete = onNavigateBack)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.date.value = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/expenses/AddEditExpenseScreen.kt \
       app/src/main/java/com/example/expensetracker/ui/expenses/AddEditExpenseViewModel.kt \
       app/src/main/java/com/example/expensetracker/ui/components/CategoryPicker.kt
git commit -m "feat: add add/edit expense screen with category picker and date picker"
```

---

### Task 14: Income List and Add/Edit Screens (One-Time Income Only)

**Note:** The Income page handles one-time income only. Recurring income is managed on the Recurring page (Task 15).

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/income/IncomeListViewModel.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/income/IncomeListScreen.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/income/AddEditIncomeViewModel.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/income/AddEditIncomeScreen.kt`

- [ ] **Step 1: Create IncomeListViewModel**

The IncomeListViewModel filters for `isRecurring = false` to show only one-time income entries.

```kotlin
package com.example.expensetracker.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.IncomeFilter
import com.example.expensetracker.domain.model.SortOrder
import com.example.expensetracker.ui.components.amountStringToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class IncomeListViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    val sourceSearch = MutableStateFlow("")
    val noteSearch = MutableStateFlow("")
    val amountMin = MutableStateFlow("")
    val amountMax = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val dateFrom = MutableStateFlow<LocalDate?>(YearMonth.now().atDay(1))
    val dateTo = MutableStateFlow<LocalDate?>(YearMonth.now().atEndOfMonth())

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomeItems: StateFlow<List<IncomeEntity>> = combine(
        sourceSearch, noteSearch, amountMin, amountMax, sortOrder, dateFrom, dateTo,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        IncomeFilter(
            isRecurring = false,
            sourceSearch = (values[0] as String).ifBlank { null },
            noteSearch = (values[1] as String).ifBlank { null },
            amountMinCents = amountStringToCents(values[2] as String),
            amountMaxCents = amountStringToCents(values[3] as String),
            sortOrder = values[4] as SortOrder,
            dateFrom = values[5] as LocalDate?,
            dateTo = values[6] as LocalDate?,
        )
    }.flatMapLatest { filter ->
        incomeRepository.getFiltered(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch { incomeRepository.delete(income) }
    }
}
```

- [ ] **Step 2: Create IncomeListScreen**

```kotlin
package com.example.expensetracker.ui.income

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.ui.components.CurrencyFormatter
import com.example.expensetracker.ui.components.FilterBar
import com.example.expensetracker.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeListScreen(
    onAddIncome: () -> Unit,
    onEditIncome: (Long) -> Unit,
    viewModel: IncomeListViewModel = hiltViewModel(),
) {
    val incomeItems by viewModel.incomeItems.collectAsStateWithLifecycle()
    val noteSearch by viewModel.noteSearch.collectAsStateWithLifecycle()
    val amountMin by viewModel.amountMin.collectAsStateWithLifecycle()
    val amountMax by viewModel.amountMax.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val sourceSearch by viewModel.sourceSearch.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Income") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddIncome) {
                Icon(Icons.Filled.Add, contentDescription = "Add income")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val dateFrom by viewModel.dateFrom.collectAsStateWithLifecycle()
            val dateTo by viewModel.dateTo.collectAsStateWithLifecycle()

            FilterBar(
                noteSearch = noteSearch,
                onNoteSearchChange = { viewModel.noteSearch.value = it },
                amountMin = amountMin,
                onAmountMinChange = { viewModel.amountMin.value = it },
                amountMax = amountMax,
                onAmountMaxChange = { viewModel.amountMax.value = it },
                dateFrom = dateFrom,
                onDateFromChange = { viewModel.dateFrom.value = it },
                dateTo = dateTo,
                onDateToChange = { viewModel.dateTo.value = it },
                sortOrder = sortOrder,
                onSortOrderChange = { viewModel.sortOrder.value = it },
                extraFilters = {
                    OutlinedTextField(
                        value = sourceSearch,
                        onValueChange = { viewModel.sourceSearch.value = it },
                        label = { Text("Search source") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )

            if (incomeItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No income entries yet", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(incomeItems, key = { it.id }) { income ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteIncome(income)
                                    true
                                } else false
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { onEditIncome(income.id) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(income.source, style = MaterialTheme.typography.titleMedium)
                                        Text(income.date.toString(), style = MaterialTheme.typography.bodySmall)
                                        if (!income.note.isNullOrBlank()) {
                                            Text(income.note, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Text(
                                        CurrencyFormatter.format(income.amountCents),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IncomeGreen,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create AddEditIncomeViewModel**

This ViewModel handles one-time income only. No recurring toggle — recurring income is managed on the Recurring page.

```kotlin
package com.example.expensetracker.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddEditIncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    val amount = MutableStateFlow("")
    val source = MutableStateFlow("")
    val date = MutableStateFlow(LocalDate.now())
    val note = MutableStateFlow("")

    private var editingIncomeId: Long? = null

    fun loadIncome(id: Long) {
        viewModelScope.launch {
            val income = incomeRepository.getById(id) ?: return@launch
            editingIncomeId = income.id
            amount.value = centsToAmountString(income.amountCents)
            source.value = income.source
            date.value = income.date
            note.value = income.note ?: ""
        }
    }

    fun delete(onComplete: () -> Unit) {
        val id = editingIncomeId ?: return
        viewModelScope.launch {
            val entity = incomeRepository.getById(id) ?: return@launch
            incomeRepository.delete(entity)
            onComplete()
        }
    }

    fun save(onComplete: () -> Unit) {
        val cents = amountStringToCents(amount.value) ?: return
        if (source.value.isBlank()) return

        viewModelScope.launch {
            val entity = IncomeEntity(
                id = editingIncomeId ?: 0,
                amountCents = cents,
                source = source.value,
                date = date.value,
                note = note.value.ifBlank { null },
                isRecurring = false,
                recurrenceInterval = null,
            )
            if (editingIncomeId != null) {
                incomeRepository.update(entity)
            } else {
                incomeRepository.insert(entity)
            }
            onComplete()
        }
    }
}
```

- [ ] **Step 4: Create AddEditIncomeScreen**

This screen handles one-time income only. No recurring toggle — recurring income is managed on the Recurring page via AddEditRecurringIncomeScreen.

```kotlin
package com.example.expensetracker.ui.income

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.ui.components.AmountInput
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIncomeScreen(
    incomeId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditIncomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(incomeId) {
        if (incomeId != null) viewModel.loadIncome(incomeId)
    }

    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val source by viewModel.source.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (incomeId != null) "Edit Income" else "Add Income") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (incomeId != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete income",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            AmountInput(
                value = amount,
                onValueChange = { viewModel.amount.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = source,
                onValueChange = { viewModel.source.value = it },
                label = { Text("Source") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = date.toString(),
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    showDatePicker = true
                                }
                            }
                        }
                    },
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { viewModel.note.value = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save(onComplete = onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (incomeId != null) "Update" else "Add")
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete income?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.delete(onComplete = onNavigateBack)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.date.value = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/income/
git commit -m "feat: add income list and add/edit screens for one-time income"
```

---

### Task 15: Recurring List and Add/Edit Screens (Expenses + Income)

**Note:** The Recurring page shows both recurring expenses and recurring income. They are differentiated by icon (ArrowDownward/ArrowUpward) and color (ExpenseRed/IncomeGreen). The FAB opens a dropdown menu to choose between adding a recurring expense or recurring income.

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/recurring/RecurringListViewModel.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/recurring/RecurringListScreen.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/recurring/AddEditRecurringExpenseViewModel.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/recurring/AddEditRecurringExpenseScreen.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/recurring/AddEditRecurringIncomeViewModel.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/recurring/AddEditRecurringIncomeScreen.kt`

- [ ] **Step 1: Create RecurringListViewModel**

The ViewModel loads both recurring expenses and recurring income. Delete is handled in the AddEdit screens.

```kotlin
package com.example.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val incomeRepository: IncomeRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val recurringExpenses: StateFlow<List<RecurringExpenseEntity>> =
        recurringExpenseRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringIncome: StateFlow<List<IncomeEntity>> =
        incomeRepository.getRecurring()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

- [ ] **Step 2: Create RecurringListScreen**

The screen shows both recurring expenses and recurring income in sections. Expenses are shown with a red ArrowDownward icon, income with a green ArrowUpward icon. The FAB opens a dropdown to choose what to add. Delete is handled in the AddEdit detail screens (trash icon in TopAppBar).

```kotlin
package com.example.expensetracker.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.ui.components.CurrencyFormatter
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    onAddRecurring: () -> Unit,
    onEditRecurring: (Long) -> Unit,
    onAddRecurringIncome: () -> Unit,
    onEditRecurringIncome: (Long) -> Unit,
    viewModel: RecurringListViewModel = hiltViewModel(),
) {
    val recurringExpenses by viewModel.recurringExpenses.collectAsStateWithLifecycle()
    val recurringIncome by viewModel.recurringIncome.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recurring") }) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Recurring Expense") },
                        onClick = {
                            showAddMenu = false
                            onAddRecurring()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = ExpenseRed)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Recurring Income") },
                        onClick = {
                            showAddMenu = false
                            onAddRecurringIncome()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = IncomeGreen)
                        },
                    )
                }
                FloatingActionButton(onClick = { showAddMenu = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add recurring")
                }
            }
        },
    ) { padding ->
        if (recurringExpenses.isEmpty() && recurringIncome.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No recurring items", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (recurringExpenses.isNotEmpty()) {
                    item {
                        Text(
                            "Expenses",
                            style = MaterialTheme.typography.titleSmall,
                            color = ExpenseRed,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(recurringExpenses, key = { "expense-${it.id}" }) { item ->
                        val categoryName = categories.find { it.id == item.categoryId }?.name ?: "Unknown"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onEditRecurring(item.id) },
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.ArrowDownward,
                                    contentDescription = "Expense",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(categoryName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${item.interval.displayName}, from ${item.startDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (!item.note.isNullOrBlank()) {
                                        Text(item.note, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Text(
                                    CurrencyFormatter.format(item.amountCents),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ExpenseRed,
                                )
                            }
                        }
                    }
                }
                if (recurringIncome.isNotEmpty()) {
                    item {
                        Text(
                            "Income",
                            style = MaterialTheme.typography.titleSmall,
                            color = IncomeGreen,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(recurringIncome, key = { "income-${it.id}" }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onEditRecurringIncome(item.id) },
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.ArrowUpward,
                                    contentDescription = "Income",
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.source, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        buildString {
                                            append(item.recurrenceInterval?.displayName ?: "Recurring")
                                            if (item.startDate != null) {
                                                append(", from ${item.startDate}")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (!item.note.isNullOrBlank()) {
                                        Text(item.note, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Text(
                                    CurrencyFormatter.format(item.amountCents),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IncomeGreen,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create AddEditRecurringExpenseViewModel**

```kotlin
package com.example.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddEditRecurringExpenseViewModel @Inject constructor(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val amount = MutableStateFlow("")
    val categoryId = MutableStateFlow<Long?>(null)
    val startDate = MutableStateFlow(LocalDate.now())
    val interval = MutableStateFlow(Interval.MONTHLY)
    val note = MutableStateFlow("")

    private var editingId: Long? = null

    fun loadRecurringExpense(id: Long) {
        viewModelScope.launch {
            val item = recurringExpenseRepository.getById(id) ?: return@launch
            editingId = item.id
            amount.value = centsToAmountString(item.amountCents)
            categoryId.value = item.categoryId
            startDate.value = LocalDate.parse(item.startDate)
            interval.value = item.interval
            note.value = item.note ?: ""
        }
    }

    fun delete(onComplete: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch {
            val entity = recurringExpenseRepository.getById(id) ?: return@launch
            recurringExpenseRepository.delete(entity)
            onComplete()
        }
    }

    fun save(onComplete: () -> Unit) {
        val cents = amountStringToCents(amount.value) ?: return
        val catId = categoryId.value ?: return

        viewModelScope.launch {
            val entity = RecurringExpenseEntity(
                id = editingId ?: 0,
                amountCents = cents,
                categoryId = catId,
                interval = interval.value,
                note = note.value.ifBlank { null },
                startDate = startDate.value.toString(),
            )
            if (editingId != null) {
                recurringExpenseRepository.update(entity)
            } else {
                recurringExpenseRepository.insert(entity)
            }
            onComplete()
        }
    }
}
```

- [ ] **Step 4: Create AddEditRecurringExpenseScreen**

Uses a Material3 DatePickerDialog for the "First occurrence date" field and a frequency dropdown (Monthly, Quarterly, Half-yearly).

```kotlin
package com.example.expensetracker.ui.recurring

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.ui.components.AmountInput
import com.example.expensetracker.ui.components.CategoryPicker
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringExpenseScreen(
    recurringExpenseId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditRecurringExpenseViewModel = hiltViewModel(),
) {
    LaunchedEffect(recurringExpenseId) {
        if (recurringExpenseId != null) viewModel.loadRecurringExpense(recurringExpenseId)
    }

    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val categoryId by viewModel.categoryId.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val interval by viewModel.interval.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete recurring expense?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.delete(onComplete = onNavigateBack)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.startDate.value = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recurringExpenseId != null) "Edit Recurring" else "Add Recurring") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (recurringExpenseId != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete recurring expense",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            AmountInput(
                value = amount,
                onValueChange = { viewModel.amount.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            CategoryPicker(
                categories = categories,
                selectedCategoryId = categoryId,
                onCategorySelected = { viewModel.categoryId.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = startDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("First occurrence date") },
                modifier = Modifier.fillMaxWidth(),
                interactionSource = remember { MutableInteractionSource() }.also { source ->
                    LaunchedEffect(source) {
                        source.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) {
                                showDatePicker = true
                            }
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            var intervalExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = intervalExpanded,
                onExpandedChange = { intervalExpanded = it },
            ) {
                OutlinedTextField(
                    value = interval.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = intervalExpanded,
                    onDismissRequest = { intervalExpanded = false },
                ) {
                    Interval.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.displayName) },
                            onClick = {
                                viewModel.interval.value = item
                                intervalExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { viewModel.note.value = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save(onComplete = onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (recurringExpenseId != null) "Update" else "Add")
            }
        }
    }
}
```

- [ ] **Step 5: Create AddEditRecurringIncomeViewModel**

```kotlin
package com.example.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddEditRecurringIncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    val amount = MutableStateFlow("")
    val source = MutableStateFlow("")
    val recurrenceInterval = MutableStateFlow(Interval.MONTHLY)
    val startDate = MutableStateFlow(LocalDate.now())
    val note = MutableStateFlow("")

    private var editingIncomeId: Long? = null

    fun loadIncome(id: Long) {
        viewModelScope.launch {
            val income = incomeRepository.getById(id) ?: return@launch
            editingIncomeId = income.id
            amount.value = centsToAmountString(income.amountCents)
            source.value = income.source
            recurrenceInterval.value = income.recurrenceInterval ?: Interval.MONTHLY
            if (income.startDate != null) {
                startDate.value = LocalDate.parse(income.startDate)
            }
            note.value = income.note ?: ""
        }
    }

    fun delete(onComplete: () -> Unit) {
        val id = editingIncomeId ?: return
        viewModelScope.launch {
            val entity = incomeRepository.getById(id) ?: return@launch
            incomeRepository.delete(entity)
            onComplete()
        }
    }

    fun save(onComplete: () -> Unit) {
        val cents = amountStringToCents(amount.value) ?: return
        if (source.value.isBlank()) return

        viewModelScope.launch {
            val entity = IncomeEntity(
                id = editingIncomeId ?: 0,
                amountCents = cents,
                source = source.value,
                date = startDate.value,
                note = note.value.ifBlank { null },
                isRecurring = true,
                recurrenceInterval = recurrenceInterval.value,
                startDate = startDate.value.toString(),
            )
            if (editingIncomeId != null) {
                incomeRepository.update(entity)
            } else {
                incomeRepository.insert(entity)
            }
            onComplete()
        }
    }
}
```

- [ ] **Step 6: Create AddEditRecurringIncomeScreen**

Uses a Material3 DatePickerDialog for the "First occurrence date" field and a frequency dropdown (Monthly, Quarterly, Half-yearly).

```kotlin
package com.example.expensetracker.ui.recurring

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.ui.components.AmountInput
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringIncomeScreen(
    recurringIncomeId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditRecurringIncomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(recurringIncomeId) {
        if (recurringIncomeId != null) viewModel.loadIncome(recurringIncomeId)
    }

    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val source by viewModel.source.collectAsStateWithLifecycle()
    val recurrenceInterval by viewModel.recurrenceInterval.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete recurring income?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.delete(onComplete = onNavigateBack)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.startDate.value = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recurringIncomeId != null) "Edit Recurring Income" else "Add Recurring Income") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (recurringIncomeId != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete recurring income",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            AmountInput(
                value = amount,
                onValueChange = { viewModel.amount.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = source,
                onValueChange = { viewModel.source.value = it },
                label = { Text("Source") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = startDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("First occurrence date") },
                modifier = Modifier.fillMaxWidth(),
                interactionSource = remember { MutableInteractionSource() }.also { source ->
                    LaunchedEffect(source) {
                        source.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) {
                                showDatePicker = true
                            }
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            var intervalExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = intervalExpanded,
                onExpandedChange = { intervalExpanded = it },
            ) {
                OutlinedTextField(
                    value = recurrenceInterval.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = intervalExpanded,
                    onDismissRequest = { intervalExpanded = false },
                ) {
                    Interval.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.displayName) },
                            onClick = {
                                viewModel.recurrenceInterval.value = item
                                intervalExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { viewModel.note.value = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save(onComplete = onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (recurringIncomeId != null) "Update" else "Add")
            }
        }
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/recurring/
git commit -m "feat: add recurring list with expenses and income, add/edit screens for both"
```

---

### Task 16: Reports Screen with Charts

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/reports/ReportsViewModel.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/reports/CategoryBarChart.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/reports/IncomeVsExpensesChart.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/reports/ReportsScreen.kt`

- [ ] **Step 1: Create ReportsViewModel**

```kotlin
package com.example.expensetracker.ui.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.CategoryReport
import com.example.expensetracker.domain.model.ExpenseFilter
import com.example.expensetracker.domain.model.IncomeFilter
import com.example.expensetracker.domain.model.IncomeVsExpensesReport
import com.example.expensetracker.domain.usecase.CategoryReportUseCase
import com.example.expensetracker.domain.usecase.ExportToExcelUseCase
import com.example.expensetracker.domain.usecase.IncomeVsExpensesReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class ReportPeriod { MONTH, QUARTER, YEAR, CUSTOM }
enum class ReportTab { CATEGORY, INCOME_VS_EXPENSES }

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val categoryReportUseCase: CategoryReportUseCase,
    private val incomeVsExpensesReportUseCase: IncomeVsExpensesReportUseCase,
    private val exportToExcelUseCase: ExportToExcelUseCase,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)
    val selectedTab = MutableStateFlow(ReportTab.CATEGORY)
    val customDateFrom = MutableStateFlow(YearMonth.now().atDay(1))
    val customDateTo = MutableStateFlow(YearMonth.now().atEndOfMonth())

    private val _categoryReport = MutableStateFlow(CategoryReport(emptyList(), 0L))
    val categoryReport: StateFlow<CategoryReport> = _categoryReport

    private val _incomeVsExpensesReport = MutableStateFlow(IncomeVsExpensesReport(emptyList(), 0L, 0L))
    val incomeVsExpensesReport: StateFlow<IncomeVsExpensesReport> = _incomeVsExpensesReport

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            val (from, to) = getDateRange()
            _categoryReport.value = categoryReportUseCase(from, to)
            _incomeVsExpensesReport.value = incomeVsExpensesReportUseCase(from, to)
        }
    }

    fun exportToExcel(context: Context) {
        viewModelScope.launch {
            val (from, to) = getDateRange()
            val expenses = expenseRepository.getFiltered(
                ExpenseFilter(dateFrom = from, dateTo = to)
            ).first()
            val income = incomeRepository.getFiltered(
                IncomeFilter(dateFrom = from, dateTo = to)
            ).first()
            val categories = categoryRepository.getAll().first().associateBy { it.id }

            val file = File(context.cacheDir, "expense_report.xlsx")
            exportToExcelUseCase(expenses, income, categories, file)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Report"))
        }
    }

    private fun getDateRange(): Pair<LocalDate, LocalDate> {
        val now = YearMonth.now()
        return when (selectedPeriod.value) {
            ReportPeriod.MONTH -> now.atDay(1) to now.atEndOfMonth()
            ReportPeriod.QUARTER -> {
                val quarterStart = now.withMonth(((now.monthValue - 1) / 3) * 3 + 1)
                quarterStart.atDay(1) to quarterStart.plusMonths(2).atEndOfMonth()
            }
            ReportPeriod.YEAR -> LocalDate.of(now.year, 1, 1) to LocalDate.of(now.year, 12, 31)
            ReportPeriod.CUSTOM -> customDateFrom.value to customDateTo.value
        }
    }
}
```

- [ ] **Step 2: Create CategoryBarChart**

Uses `ExtraStore` to pass category names alongside chart data, and a `CartesianValueFormatter` to display them on the x-axis instead of numeric indices.

```kotlin
package com.example.expensetracker.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.example.expensetracker.domain.model.CategoryReport

@Composable
fun CategoryBarChart(
    report: CategoryReport,
    modifier: Modifier = Modifier,
) {
    if (report.items.isEmpty()) return

    val labelListKey = remember { ExtraStore.Key<List<String>>() }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(report) {
        modelProducer.runTransaction {
            columnSeries {
                series(report.items.map { it.totalCents / 100.0 })
            }
            extras { it[labelListKey] = report.items.map { item -> item.categoryName } }
        }
    }

    val labelFormatter = remember(labelListKey) {
        CartesianValueFormatter { context, x, _ ->
            context.model.extraStore[labelListKey].getOrElse(x.toInt()) { "" }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(fill = Fill(Color(0xFF6650a4).toArgb()), thickness = 24.dp),
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = labelFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}
```

- [ ] **Step 3: Create IncomeVsExpensesChart**

Uses `ExtraStore` to pass month labels (from `MonthlyBalance.label`, e.g. "Jan", "Feb") alongside chart data, and a `CartesianValueFormatter` to display them on the x-axis instead of numeric indices.

```kotlin
package com.example.expensetracker.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.example.expensetracker.domain.model.IncomeVsExpensesReport
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen

@Composable
fun IncomeVsExpensesChart(
    report: IncomeVsExpensesReport,
    modifier: Modifier = Modifier,
) {
    if (report.items.isEmpty()) return

    val monthLabelKey = remember { ExtraStore.Key<List<String>>() }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(report) {
        modelProducer.runTransaction {
            columnSeries {
                series(report.items.map { it.incomeCents / 100.0 })
                series(report.items.map { it.expenseCents / 100.0 })
            }
            extras { it[monthLabelKey] = report.items.map { item -> item.label } }
        }
    }

    val monthFormatter = remember(monthLabelKey) {
        CartesianValueFormatter { context, x, _ ->
            context.model.extraStore[monthLabelKey].getOrElse(x.toInt()) { "" }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(fill = Fill(IncomeGreen.toArgb()), thickness = 16.dp),
                    rememberLineComponent(fill = Fill(ExpenseRed.toArgb()), thickness = 16.dp),
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = monthFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}
```

- [ ] **Step 4: Create ReportsScreen**

```kotlin
package com.example.expensetracker.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.ui.components.CategoryIcon
import com.example.expensetracker.ui.components.CurrencyFormatter
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val categoryReport by viewModel.categoryReport.collectAsStateWithLifecycle()
    val incomeVsExpensesReport by viewModel.incomeVsExpensesReport.collectAsStateWithLifecycle()
    val customDateFrom by viewModel.customDateFrom.collectAsStateWithLifecycle()
    val customDateTo by viewModel.customDateTo.collectAsStateWithLifecycle()
    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                actions = {
                    IconButton(onClick = { viewModel.exportToExcel(context) }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Export")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = {
                            viewModel.selectedPeriod.value = period
                            viewModel.loadReports()
                        },
                        label = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            if (selectedPeriod == ReportPeriod.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { showDateFromPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(customDateFrom.toString())
                    }
                    Text("to")
                    OutlinedButton(
                        onClick = { showDateToPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(customDateTo.toString())
                    }
                }
            }

            TabRow(selectedTabIndex = ReportTab.entries.indexOf(selectedTab)) {
                Tab(
                    selected = selectedTab == ReportTab.CATEGORY,
                    onClick = { viewModel.selectedTab.value = ReportTab.CATEGORY },
                    text = { Text("By Category") },
                )
                Tab(
                    selected = selectedTab == ReportTab.INCOME_VS_EXPENSES,
                    onClick = { viewModel.selectedTab.value = ReportTab.INCOME_VS_EXPENSES },
                    text = { Text("Income vs Expenses") },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                ReportTab.CATEGORY -> {
                    CategoryBarChart(
                        report = categoryReport,
                        modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Total: ${CurrencyFormatter.format(categoryReport.totalCents)}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    categoryReport.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(item.categoryIcon, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.categoryName)
                            }
                            Text("${CurrencyFormatter.format(item.totalCents)} (${"%.1f".format(item.percentage)}%)")
                        }
                    }
                }
                ReportTab.INCOME_VS_EXPENSES -> {
                    IncomeVsExpensesChart(
                        report = incomeVsExpensesReport,
                        modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Income:", color = IncomeGreen)
                        Text(CurrencyFormatter.format(incomeVsExpensesReport.totalIncomeCents), color = IncomeGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Expenses:", color = ExpenseRed)
                        Text(CurrencyFormatter.format(incomeVsExpensesReport.totalExpenseCents), color = ExpenseRed)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val net = incomeVsExpensesReport.totalIncomeCents - incomeVsExpensesReport.totalExpenseCents
                        Text("Net:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            CurrencyFormatter.format(kotlin.math.abs(net)),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (net >= 0) IncomeGreen else ExpenseRed,
                        )
                    }
                }
            }
        }
    }

    if (showDateFromPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = customDateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDateFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.customDateFrom.value = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.loadReports()
                    }
                    showDateFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateFromPicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }

    if (showDateToPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = customDateTo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDateToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.customDateTo.value = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.loadReports()
                    }
                    showDateToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateToPicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/reports/
git commit -m "feat: add reports screen with Vico bar charts, custom date range, and Excel export"
```

---

### Task 17: Category Management Screen

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/categories/CategoryManagementViewModel.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/categories/CategoryManagementScreen.kt`

- [ ] **Step 1: Create CategoryManagementViewModel**

```kotlin
package com.example.expensetracker.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newCategoryName = MutableStateFlow("")

    fun addCategory() {
        val name = newCategoryName.value.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.insert(CategoryEntity(name = name))
            newCategoryName.value = ""
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (category.isDefault) return
        viewModelScope.launch {
            categoryRepository.delete(category)
        }
    }

    fun renameCategory(category: CategoryEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            categoryRepository.update(category.copy(name = newName.trim()))
        }
    }
}
```

- [ ] **Step 2: Create CategoryManagementScreen**

```kotlin
package com.example.expensetracker.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.components.CategoryIcon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val newCategoryName by viewModel.newCategoryName.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { viewModel.newCategoryName.value = it },
                    label = { Text("New category") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.addCategory() }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(categories, key = { it.id }) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                CategoryIcon(category.icon, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(category.name, style = MaterialTheme.typography.titleMedium)
                                    if (category.isDefault) {
                                        Text("Default", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            if (!category.isDefault) {
                                IconButton(onClick = { viewModel.deleteCategory(category) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/categories/
git commit -m "feat: add category management screen with add/delete"
```

---

### Task 18: FileProvider and Recurring Generation on Startup

**Files:**
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/expensetracker/MainActivity.kt`

- [ ] **Step 1: Create file_paths.xml**

Create `app/src/main/res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="reports" path="/" />
</paths>
```

- [ ] **Step 2: Update AndroidManifest.xml to add FileProvider**

Replace `app/src/main/AndroidManifest.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".ExpenseTrackerApplication"
        android:allowBackup="true"
        android:label="Expense Tracker"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Material3.DayNight.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>

</manifest>
```

- [ ] **Step 3: Update MainActivity to trigger recurring generation on startup**

Replace `app/src/main/java/com/example/expensetracker/MainActivity.kt` with:

```kotlin
package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.domain.usecase.GenerateRecurringExpensesUseCase
import com.example.expensetracker.ui.navigation.NavGraph
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var generateRecurringExpenses: GenerateRecurringExpensesUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            generateRecurringExpenses(YearMonth.now())
        }

        setContent {
            ExpenseTrackerTheme {
                NavGraph()
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/xml/file_paths.xml \
       app/src/main/AndroidManifest.xml \
       app/src/main/java/com/example/expensetracker/MainActivity.kt
git commit -m "feat: add FileProvider for Excel sharing and recurring expense generation on startup"
```

---

### Task 19: Gradle Wrapper and Build Verification

**Files:**
- Verify build compiles

- [ ] **Step 1: Add Gradle wrapper**

```bash
cd /data/projects/expenses-app/app
gradle wrapper --gradle-version 8.11.1
```

- [ ] **Step 2: Verify the project compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. If there are compilation errors, fix them.

- [ ] **Step 3: Run unit tests**

```bash
./gradlew testDebugUnitTest
```

Expected: Tests pass (GenerateRecurringExpensesUseCaseTest, CategoryReportUseCaseTest, IncomeVsExpensesReportUseCaseTest, ExportToExcelUseCaseTest).

- [ ] **Step 4: Commit any fixes**

```bash
git add -A
git commit -m "fix: resolve compilation and test issues from build verification"
```

---

### Task 20: Form Validation Errors

**Files:**
- Modify: `app/src/main/java/com/example/expensetracker/ui/components/AmountInput.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/components/CategoryPicker.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/expenses/AddEditExpenseViewModel.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/expenses/AddEditExpenseScreen.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/income/AddEditIncomeViewModel.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/income/AddEditIncomeScreen.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/recurring/AddEditRecurringExpenseViewModel.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/recurring/AddEditRecurringExpenseScreen.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/recurring/AddEditRecurringIncomeViewModel.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/recurring/AddEditRecurringIncomeScreen.kt`
- Create: `app/src/test/java/com/example/expensetracker/ui/expenses/AddEditExpenseViewModelTest.kt`
- Create: `app/src/test/java/com/example/expensetracker/ui/income/AddEditIncomeViewModelTest.kt`
- Modify: `app/src/test/java/com/example/expensetracker/ui/recurring/AddEditRecurringExpenseViewModelTest.kt`
- Modify: `app/src/test/java/com/example/expensetracker/ui/recurring/AddEditRecurringIncomeViewModelTest.kt`

Previously, all form validation was silent — `save()` returned early without feedback. This task adds visible validation errors using Material 3's `isError` and `supportingText` on `OutlinedTextField`.

**Approach:**
- Each ViewModel exposes `MutableStateFlow<String?>` error fields (null = no error) for each validated input.
- `save()` sets error messages instead of silently returning. All fields are validated before returning, so multiple errors can be shown at once.
- Errors are cleared when the user modifies the corresponding input field.
- `AmountInput` and `CategoryPicker` components accept an optional `errorMessage: String?` parameter that drives `isError` and `supportingText`.
- Source fields (income forms) use `isError`/`supportingText` directly on the `OutlinedTextField`.

**Validation rules:**
| Form | Field | Error condition | Message |
|------|-------|----------------|---------|
| Expense | Amount | Empty or non-numeric | "Enter a valid amount" |
| Expense | Category | Not selected | "Select a category" |
| Income | Amount | Empty or non-numeric | "Enter a valid amount" |
| Income | Source | Blank | "Enter a source" |
| Recurring Expense | Amount | Empty or non-numeric | "Enter a valid amount" |
| Recurring Expense | Category | Not selected | "Select a category" |
| Recurring Income | Amount | Empty or non-numeric | "Enter a valid amount" |
| Recurring Income | Source | Blank | "Enter a source" |

- [ ] **Step 1: Add errorMessage parameter to AmountInput and CategoryPicker**

Add `errorMessage: String? = null` parameter. Wire to `isError` and `supportingText` on the internal `OutlinedTextField`.

- [ ] **Step 2: Add error StateFlows and validation logic to all 4 ViewModels**

Each ViewModel gets `amountError` plus either `categoryError` or `sourceError` as `MutableStateFlow<String?>`. The `save()` method sets all error values before the early return, so multiple errors show simultaneously.

- [ ] **Step 3: Wire error states in all 4 Screens**

Collect error states with `collectAsStateWithLifecycle()`. Pass to `AmountInput`/`CategoryPicker`/`OutlinedTextField` via `errorMessage`/`isError`/`supportingText`. Clear errors in `onValueChange`/`onCategorySelected` callbacks.

- [ ] **Step 4: Add and update ViewModel tests**

Create new test files for `AddEditExpenseViewModel` and `AddEditIncomeViewModel`. Update existing `AddEditRecurringExpenseViewModelTest` and `AddEditRecurringIncomeViewModelTest`. Each test class covers: error set on invalid input, correct error per field, both errors set when both invalid, errors cleared on successful save.

---

## Task 21: Consistent Date Formatting

**Status:** Complete

Change all date display throughout the app from ISO format (`2026-04-01`) to human-readable format (`1 Apr 2026`).

**Approach:**
- Create a shared `DateFormatter` object in `ui/components/DateFormatter.kt` with a `DateTimeFormatter.ofPattern("d MMM yyyy")` formatter.
- Two overloads: `format(date: LocalDate)` for typed dates, `format(isoDate: String)` for raw ISO date strings (parses then formats).
- Replace all `date.toString()`, `date.format(localFormatter)`, and raw `item.startDate` interpolation across the app with `DateFormatter.format(...)`.

**Files changed:**
| File | What changed |
|------|-------------|
| `ui/components/DateFormatter.kt` | New shared formatter object |
| `ui/components/FilterBar.kt` | Filter date fields use `DateFormatter` |
| `ui/expenses/ExpenseListScreen.kt` | Replaced private `dateFormatter` with shared `DateFormatter` |
| `ui/expenses/AddEditExpenseScreen.kt` | Date field uses `DateFormatter.format(date)` |
| `ui/income/IncomeListScreen.kt` | Replaced private `dateFormatter` with shared `DateFormatter` |
| `ui/income/AddEditIncomeScreen.kt` | Date field uses `DateFormatter.format(date)` |
| `ui/recurring/RecurringListScreen.kt` | `item.startDate` formatted via `DateFormatter.format(isoDate)` |
| `ui/recurring/AddEditRecurringExpenseScreen.kt` | Replaced locale-dependent formatter with `DateFormatter` |
| `ui/recurring/AddEditRecurringIncomeScreen.kt` | Replaced locale-dependent formatter with `DateFormatter` |
| `ui/reports/ReportsScreen.kt` | Custom date range buttons use `DateFormatter` |
| `domain/usecase/ExportToExcelUseCase.kt` | Excel export date cells use `DateFormatter` |
| `ExportToExcelUseCaseTest.kt` | Updated expected date string to `"15 Mar 2026"` |

---

### Task 22: JSON Export/Import Data Classes and Schema

**Files:**
- Create: `app/schemas/export-schema.json`
- Create: `app/src/main/java/com/example/expensetracker/domain/model/ExportData.kt`
- Modify: `app/build.gradle.kts`

This task defines the JSON export format as both a JSON Schema file (documentation) and Kotlin data classes (runtime serialization).

**Approach:**
- Add `kotlinx.serialization` dependency to `build.gradle.kts` (plugin + library)
- Define `@Serializable` data classes in `ExportData.kt` mirroring each entity but decoupled from Room annotations. One wrapper class `AppExportData` holds all collections plus `schemaVersion: Int` and `exportedAt: String`
- Create `export-schema.json` as a JSON Schema (draft 2020-12) documenting the format

- [ ] **Step 1: Add kotlinx.serialization dependency**

Add the Kotlin serialization Gradle plugin and `kotlinx-serialization-json` library dependency to `build.gradle.kts`.

- [ ] **Step 2: Create export data classes**

Create `ExportData.kt` with `@Serializable` data classes:

```kotlin
@Serializable
data class AppExportData(
    val schemaVersion: Int,
    val exportedAt: String,
    val categories: List<ExportCategory>,
    val expenses: List<ExportExpense>,
    val income: List<ExportIncome>,
    val recurringExpenses: List<ExportRecurringExpense>,
    val recurringExpenseGenerations: List<ExportRecurringExpenseGeneration>,
    val recurringIncomeGenerations: List<ExportRecurringIncomeGeneration>,
)
```

Each `Export*` class has the same fields as the corresponding entity, using primitive types (`Long`, `String`, `Boolean`, `String?`). No Room annotations, no type converters needed.

- [ ] **Step 3: Create JSON Schema file**

Create `app/schemas/export-schema.json` with JSON Schema (draft 2020-12) defining the export format. All fields, types, required properties, and descriptions matching the data classes.

---

### Task 23: ExportImportJsonUseCase

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/domain/usecase/ExportImportJsonUseCase.kt`
- Modify: `app/src/main/java/com/example/expensetracker/data/db/dao/CategoryDao.kt`
- Modify: `app/src/main/java/com/example/expensetracker/data/db/dao/ExpenseDao.kt`
- Modify: `app/src/main/java/com/example/expensetracker/data/db/dao/IncomeDao.kt`
- Modify: `app/src/main/java/com/example/expensetracker/data/db/dao/RecurringExpenseDao.kt`
- Modify: `app/src/main/java/com/example/expensetracker/data/db/dao/RecurringExpenseGenerationDao.kt`
- Modify: `app/src/main/java/com/example/expensetracker/data/db/dao/RecurringIncomeGenerationDao.kt`
- Modify: `app/src/main/java/com/example/expensetracker/data/db/AppDatabase.kt`
- Create: `app/src/test/java/com/example/expensetracker/domain/usecase/ExportImportJsonUseCaseTest.kt`

**Approach:**
- The use case depends on `AppDatabase` directly (not individual repositories) because import needs a single `@Transaction` across all tables
- Export: queries all DAOs, maps entities to `Export*` data classes, serializes to JSON string via `kotlinx.serialization`
- Import: deserializes JSON string, validates `schemaVersion == 1`, validates FK consistency, then in a single `@Transaction`: deletes all rows from all tables, inserts all imported data
- DAOs need new methods: `getAllSuspend()` (where missing), `deleteAll()`, and `insertAll(list)` for bulk operations

- [ ] **Step 1: Add bulk DAO methods**

Add to each DAO:
- `deleteAll()` — `@Query("DELETE FROM <table>")`
- `insertAll(items: List<Entity>)` — `@Insert`
- `getAllSuspend()` — suspend version returning `List<Entity>` (where only Flow version exists)

Add a `runInTransaction` helper or use `AppDatabase.withTransaction` for the import operation.

- [ ] **Step 2: Create ExportImportJsonUseCase**

```kotlin
@Singleton
class ExportImportJsonUseCase @Inject constructor(
    private val database: AppDatabase,
) {
    suspend fun exportToJson(): String { ... }
    suspend fun importFromJson(jsonString: String) { ... }
}
```

**Export logic:**
1. Query all entities from all 6 DAOs
2. Map each entity to its `Export*` counterpart
3. Wrap in `AppExportData(schemaVersion = 1, exportedAt = Instant.now().toString(), ...)`
4. Serialize with `Json.encodeToString()`

**Import logic:**
1. Deserialize with `Json.decodeFromString<AppExportData>()`
2. Validate `schemaVersion == 1` (throw `IllegalArgumentException` otherwise)
3. Validate FK consistency:
   - All `expense.categoryId` values exist in imported `categories`
   - All `expense.recurringExpenseId` (non-null) exist in imported `recurringExpenses`
   - All `recurringExpense.categoryId` exist in imported `categories`
   - All `recurringExpenseGeneration.recurringExpenseId` exist in imported `recurringExpenses`
   - All `recurringExpenseGeneration.expenseId` exist in imported `expenses`
   - All `recurringIncomeGeneration.recurringIncomeId` and `.incomeId` exist in imported `income`
4. On validation failure: throw descriptive exception, no data modified
5. In a single `withTransaction`:
   - Delete all from all 6 tables (generations first, then main tables, then categories — respecting FK order)
   - Insert all from imported data (categories first, then main tables, then generations)

- [ ] **Step 3: Write unit tests**

Test cases:
- Export produces valid JSON with all fields
- Round-trip: export → import → export produces identical JSON
- Import rejects unknown `schemaVersion`
- Import rejects broken FK references (e.g., expense referencing non-existent category)
- Import replaces all existing data (verify old data gone, new data present)

---

### Task 24: Settings Screen and Navigation

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/example/expensetracker/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt`

**Approach:**
- Add `Screen.Settings` route to `Screen.kt`
- Add gear icon (`Icons.Filled.Settings`) to the top app bar in `NavGraph.kt` scaffold, navigating to Settings screen
- Settings screen is a simple list with two items: "Export Data" and "Import Data"
- SettingsViewModel handles export/import orchestration, delegates to `ExportImportJsonUseCase`

- [ ] **Step 1: Add Settings route and top bar navigation**

Add `data object Settings : Screen("settings")` to `Screen.kt`.

In `NavGraph.kt`, add a `Settings` icon button to the `TopAppBar` actions. Add the `composable("settings")` route to the `NavHost`.

- [ ] **Step 2: Create SettingsViewModel**

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportImportJsonUseCase: ExportImportJsonUseCase,
) : ViewModel() {
    // UI state
    val exportState: StateFlow<ExportImportState>  // Idle, Loading, Success(uri), Error(message)
    val importState: StateFlow<ExportImportState>   // Idle, Loading, Success, Error(message)
    val showImportConfirmation: StateFlow<Boolean>

    fun exportData(context: Context) { ... }
    fun onImportFileSelected(uri: Uri, contentResolver: ContentResolver) { ... }
    fun confirmImport() { ... }
    fun dismissImportConfirmation() { ... }
    fun clearExportState() { ... }
    fun clearImportState() { ... }
}
```

**Export flow:**
1. Call `exportImportJsonUseCase.exportToJson()`
2. Write JSON string to cache file: `{cacheDir}/expense_tracker_backup_YYYY-MM-DD.json`
3. Get URI via FileProvider
4. Set `exportState` to `Success(uri)` — screen launches share intent

**Import flow:**
1. `onImportFileSelected()` reads file content, stores it, sets `showImportConfirmation = true`
2. User confirms → `confirmImport()` calls `exportImportJsonUseCase.importFromJson(jsonString)`
3. On success: `importState = Success`
4. On failure: `importState = Error(exception.message)`

- [ ] **Step 3: Create SettingsScreen**

Material 3 screen with:
- Top app bar with "Settings" title and back navigation
- Two list items with icons:
  - **Export Data** (`Icons.Filled.Upload`): triggers export, launches share sheet on success
  - **Import Data** (`Icons.Filled.Download`): launches file picker (`ActivityResultContracts.OpenDocument`)
- Confirmation dialog for import: "This will replace all existing data. Are you sure?"
- Snackbar for success/error feedback
- Loading indicator during export/import operations

---

### Task 25: Recurring Item Indicator in List Screens

**Goal:** Add a repeat icon next to the date on expense and income list items that are recurring, so users can visually distinguish them from one-time entries.

**Files modified:**
- `app/src/main/java/com/example/expensetracker/ui/expenses/ExpenseListScreen.kt`
- `app/src/main/java/com/example/expensetracker/ui/income/IncomeListScreen.kt`
- `app/src/main/java/com/example/expensetracker/ui/income/IncomeListViewModel.kt`

- [x] **Step 1: Add repeat icon to ExpenseListScreen**

In the `ExpenseItem` composable, wrap the date `Text` in a `Row` and conditionally show `Icons.Filled.Repeat` (16dp, `primary` tint) when `expense.recurringExpenseId != null`.

- [x] **Step 2: Include recurring income in IncomeListViewModel**

Change `isRecurring = false` to `isRecurring = null` in the `IncomeFilter` so that both one-time and recurring income items appear in the income list.

- [x] **Step 3: Add repeat icon to IncomeListScreen**

In the income card composable, wrap the date `Text` in a `Row` and conditionally show `Icons.Filled.Repeat` (16dp, `primary` tint) when `income.isRecurring`.

---
