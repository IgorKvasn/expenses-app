# Add Expense Homescreen Shortcut Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a static Android app shortcut so users can add a new expense directly from the homescreen.

**Architecture:** Static shortcut defined in XML, targeting MainActivity with an intent extra. MainActivity reads the extra and passes a flag to NavGraph, which uses LaunchedEffect to navigate to the AddEditExpense screen.

**Tech Stack:** Android static shortcuts XML, Jetpack Compose Navigation, vector drawables.

---

### File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `src/main/res/drawable/ic_shortcut_add_expense.xml` | Create | Shortcut icon (adaptive foreground) |
| `src/main/res/xml/shortcuts.xml` | Create | Static shortcut definition |
| `src/main/AndroidManifest.xml` | Modify | Reference shortcuts.xml in activity |
| `src/main/java/com/example/expensetracker/MainActivity.kt` | Modify | Read shortcut intent extra, pass flag to NavGraph |
| `src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt` | Modify | Accept flag, navigate to AddEditExpense on launch |

---

### Task 1: Create shortcut icon drawable

**Files:**
- Create: `src/main/res/drawable/ic_shortcut_add_expense.xml`

- [ ] **Step 1: Create the drawable directory and icon**

Create `src/main/res/drawable/ic_shortcut_add_expense.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Circle background -->
    <path
        android:fillColor="#4CAF50"
        android:pathData="M54,54m-40,0a40,40 0,1 1,80 0a40,40 0,1 1,-80 0" />
    <!-- Plus sign -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M50,34L58,34L58,50L74,50L74,58L58,58L58,74L50,74L50,58L34,58L34,50L50,50Z" />
</vector>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/res/drawable/ic_shortcut_add_expense.xml
git commit -m "feat: add shortcut icon for add-expense"
```

---

### Task 2: Create shortcuts.xml

**Files:**
- Create: `src/main/res/xml/shortcuts.xml`

- [ ] **Step 1: Create the shortcuts definition**

Create `src/main/res/xml/shortcuts.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="add_expense"
        android:enabled="true"
        android:icon="@drawable/ic_shortcut_add_expense"
        android:shortcutShortLabel="@string/shortcut_add_expense_short"
        android:shortcutLongLabel="@string/shortcut_add_expense_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.example.expensetracker"
            android:targetClass="com.example.expensetracker.MainActivity">
            <extra
                android:name="shortcut_action"
                android:value="add_expense" />
        </intent>
    </shortcut>
</shortcuts>
```

- [ ] **Step 2: Create string resources**

Create `src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="shortcut_add_expense_short">Add Expense</string>
    <string name="shortcut_add_expense_long">Add New Expense</string>
</resources>
```

Note: If `strings.xml` already exists, add the two `<string>` entries to it instead of creating a new file.

- [ ] **Step 3: Commit**

```bash
git add src/main/res/xml/shortcuts.xml src/main/res/values/strings.xml
git commit -m "feat: add shortcuts.xml with add-expense shortcut"
```

---

### Task 3: Wire up AndroidManifest.xml

**Files:**
- Modify: `src/main/AndroidManifest.xml:10-18`

- [ ] **Step 1: Add shortcuts meta-data to the activity**

In `src/main/AndroidManifest.xml`, inside the `<activity>` tag (after the `</intent-filter>` closing tag on line 17), add:

```xml
            <meta-data
                android:name="android.app.shortcuts"
                android:resource="@xml/shortcuts" />
```

The activity block should look like:

```xml
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Material.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <meta-data
                android:name="android.app.shortcuts"
                android:resource="@xml/shortcuts" />
        </activity>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/AndroidManifest.xml
git commit -m "feat: register shortcuts.xml in AndroidManifest"
```

---

### Task 4: Handle shortcut intent in MainActivity

**Files:**
- Modify: `src/main/java/com/example/expensetracker/MainActivity.kt:35-38`

- [ ] **Step 1: Read intent extra and pass flag to NavGraph**

In `MainActivity.kt`, change the `setContent` block (lines 35-39) to:

```kotlin
        val navigateToAddExpense = intent.getStringExtra("shortcut_action") == "add_expense"

        setContent {
            ExpenseTrackerTheme {
                NavGraph(navigateToAddExpense = navigateToAddExpense)
            }
        }
```

The full `onCreate` method should be:

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            generateRecurringExpenses(YearMonth.now())
            generateRecurringIncome(YearMonth.now())
        }

        val navigateToAddExpense = intent.getStringExtra("shortcut_action") == "add_expense"

        setContent {
            ExpenseTrackerTheme {
                NavGraph(navigateToAddExpense = navigateToAddExpense)
            }
        }
    }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/expensetracker/MainActivity.kt
git commit -m "feat: read shortcut_action intent extra in MainActivity"
```

---

### Task 5: Navigate to AddEditExpense in NavGraph

**Files:**
- Modify: `src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt:34,58-62`

- [ ] **Step 1: Add navigateToAddExpense parameter and LaunchedEffect**

In `NavGraph.kt`:

1. Change the function signature (line 34) to accept the flag:

```kotlin
fun NavGraph(navigateToAddExpense: Boolean = false) {
```

2. Add the `LaunchedEffect` import at the top of the file:

```kotlin
import androidx.compose.runtime.LaunchedEffect
```

3. Inside the `NavHost` block, right after the opening brace (after line 62), add:

```kotlin
            if (navigateToAddExpense) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.AddEditExpense.createRoute())
                }
            }
```

The NavHost block should start like:

```kotlin
        NavHost(
            navController = navController,
            startDestination = Screen.ExpenseList.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            if (navigateToAddExpense) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.AddEditExpense.createRoute())
                }
            }
            composable(Screen.ExpenseList.route) {
```

Wait — `LaunchedEffect` cannot be called inside `NavGraphBuilder` (it's not a `@Composable` scope). Move the `LaunchedEffect` outside the `NavHost`, after the `navController` is created but before the `Scaffold`. The correct placement:

```kotlin
fun NavGraph(navigateToAddExpense: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showTopBar = currentRoute in bottomNavItems.map { it.screen.route }

    if (navigateToAddExpense) {
        LaunchedEffect(Unit) {
            navController.navigate(Screen.AddEditExpense.createRoute())
        }
    }

    Scaffold(
```

This navigates to AddEditExpense after the NavHost is composed with ExpenseList as start destination, so the back stack is: ExpenseList → AddEditExpense. Pressing back returns to the expense list.

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt
git commit -m "feat: navigate to AddEditExpense on shortcut launch"
```

---

### Task 6: Manual testing

- [ ] **Step 1: Build and install the app**

```bash
./gradlew installDebug
```

- [ ] **Step 2: Test shortcut via long-press**

On the device/emulator:
1. Long-press the "Expense Tracker" app icon
2. Verify "Add Expense" shortcut appears with the green plus icon
3. Tap the shortcut → should open the Add Expense form
4. Press back → should return to the Expense List
5. Drag the shortcut to the homescreen → tap it → should open the Add Expense form directly

- [ ] **Step 3: Test shortcut via adb**

```bash
adb shell am start -n com.example.expensetracker/.MainActivity --es shortcut_action add_expense
```

Expected: App opens directly to the Add Expense form.

- [ ] **Step 4: Test normal app launch still works**

Open the app normally (tap main icon). Verify it opens to the Expense List as usual, not the Add Expense form.
