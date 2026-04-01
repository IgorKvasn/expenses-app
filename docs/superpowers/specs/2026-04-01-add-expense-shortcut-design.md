# Add Expense Homescreen Shortcut

## Overview

Add a static Android app shortcut that lets users quickly add a new expense from the homescreen. The shortcut appears when long-pressing the app icon and can be dragged to the homescreen as a standalone icon.

## Approach

Static shortcut defined in XML. No runtime registration needed.

## Changes

### 1. New file: `res/xml/shortcuts.xml`

Defines one shortcut:
- **ID:** `add_expense`
- **Short label:** "Add Expense"
- **Long label:** "Add New Expense"
- **Icon:** Custom vector drawable (`ic_shortcut_add_expense`)
- **Intent:** Targets `MainActivity` with extra `shortcut_action=add_expense`

### 2. New file: `res/drawable/ic_shortcut_add_expense.xml`

Vector drawable icon for the shortcut (add/plus symbol with receipt/wallet theme).

### 3. `AndroidManifest.xml`

Add inside the `<activity>` tag for `MainActivity`:
```xml
<meta-data
    android:name="android.app.shortcuts"
    android:resource="@xml/shortcuts" />
```

### 4. `MainActivity.kt`

In `onCreate()`, check `intent.getStringExtra("shortcut_action")`. If value is `"add_expense"`, pass a flag to `NavGraph`.

### 5. `NavGraph.kt`

Accept `navigateToAddExpense: Boolean` parameter. When true, use `LaunchedEffect` to navigate to `Screen.AddEditExpense.createRoute()` after initial composition.

## Back Stack Behavior

The shortcut starts the app normally with `ExpenseList` as start destination, then immediately navigates to `AddEditExpense`. Pressing back from the form returns to the expense list.

## Icon

A dedicated vector drawable. Android shortcut icons should be adaptive (foreground layer on transparent background) for best appearance across launchers.

## Files Modified

- `src/main/res/xml/shortcuts.xml` (new)
- `src/main/res/drawable/ic_shortcut_add_expense.xml` (new)
- `src/main/AndroidManifest.xml`
- `src/main/java/com/example/expensetracker/MainActivity.kt`
- `src/main/java/com/example/expensetracker/ui/navigation/NavGraph.kt`
