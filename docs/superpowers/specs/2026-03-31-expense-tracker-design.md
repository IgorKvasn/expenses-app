# Expense Tracker Android App — Design Spec

## Overview

A single-purpose Android app for tracking personal expenses, income, and recurring financial items. Provides visual reports and Excel export capability.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Min SDK:** 35 (Android 15)
- **Database:** Room (SQLite)
- **Charts:** Vico (Compose-native)
- **Export:** Apache POI (.xlsx)
- **Architecture:** MVVM (ViewModel per screen, Repository over Room DAOs)
- **DI:** Hilt
- **Navigation:** Compose Navigation with bottom navigation bar
- **Date handling:** java.time (LocalDate, YearMonth)
- **Currency:** Euro (€), amounts stored as Long in cents

## Project Structure

Single-module app with package separation:

```
com.example.expensetracker/
├── data/
│   ├── db/            # Room database, entities, DAOs, type converters
│   ├── repository/    # Repository implementations
│   └── seed/          # Default category seeding
├── domain/
│   ├── model/         # Domain models, enums
│   └── usecase/       # Report generation, recurring expense logic
├── ui/
│   ├── expenses/      # Expense list, add/edit screens
│   ├── income/        # Income list, add/edit screens
│   ├── recurring/     # Recurring expense/income management
│   ├── reports/       # Reports screen, charts, export
│   ├── categories/    # Category management screen
│   ├── navigation/    # Nav graph, bottom nav setup
│   └── components/    # Shared UI components (filters, pickers)
└── di/                # Hilt modules
```

## Data Model

### Entities

**Expense**
| Field | Type | Notes |
|---|---|---|
| id | Long | PK, auto-generated |
| amountCents | Long | Amount in euro cents |
| categoryId | Long | FK → Category.id |
| date | LocalDate | |
| note | String? | Optional |
| recurringExpenseId | Long? | FK → RecurringExpense.id, null if manual |

**Income**
| Field | Type | Notes |
|---|---|---|
| id | Long | PK, auto-generated |
| amountCents | Long | Amount in euro cents |
| source | String | e.g., "Salary", "Freelance" |
| date | LocalDate | |
| note | String? | Optional |
| isRecurring | Boolean | |
| recurrenceInterval | Interval? | MONTHLY, QUARTERLY, or HALF_YEARLY, null if not recurring |
| startDate | String? | ISO LocalDate (e.g., "2026-01-15"), null if not recurring |

**RecurringExpense**
| Field | Type | Notes |
|---|---|---|
| id | Long | PK, auto-generated |
| amountCents | Long | Amount in euro cents |
| categoryId | Long | FK → Category.id |
| interval | Interval | MONTHLY, QUARTERLY, or HALF_YEARLY |
| note | String? | Optional |
| startDate | String | ISO LocalDate (e.g., "2026-01-15") — first occurrence date, day is used for recurring day-of-month |

**Category**
| Field | Type | Notes |
|---|---|---|
| id | Long | PK, auto-generated |
| name | String | Unique |
| icon | String? | Material icon name |
| isDefault | Boolean | True for pre-seeded, prevents deletion |

**RecurringExpenseGeneration**
| Field | Type | Notes |
|---|---|---|
| id | Long | PK, auto-generated |
| recurringExpenseId | Long | FK → RecurringExpense.id |
| generatedForMonth | YearMonth | The month this was generated for |
| expenseId | Long | FK → Expense.id (the generated expense) |

### Enum

**Interval**
- `MONTHLY`
- `QUARTERLY`

### Relationships

- Expense.categoryId → Category.id
- Expense.recurringExpenseId → RecurringExpense.id (nullable)
- RecurringExpense.categoryId → Category.id
- RecurringExpenseGeneration.recurringExpenseId → RecurringExpense.id
- RecurringExpenseGeneration.expenseId → Expense.id

## Screens & Navigation

### Bottom Navigation (4 tabs)

#### 1. Expenses (default tab)
- List of expenses for current month (default view)
- FAB to add new expense
- Swipe to delete
- Tap to edit
- **Filters:** category, date range, amount range (min/max), note full-text search (case-insensitive)
- **Sort:** by date (default, newest first) or by amount (ascending/descending)
- Filters and sort are combinable

#### 2. Income
- List of income entries for current month
- Add one-time or recurring income
- **Filters:** source (text search, case-insensitive), date range, amount range (min/max), note full-text search (case-insensitive)
- Sort by date or amount

#### 3. Recurring
- List of all recurring expense and income templates
- Toggle active/inactive
- Shows next occurrence date
- Add/edit/delete recurring items

#### 4. Reports
- Period selector: Month / Quarter / Year / Custom date range
- Two report views (tabs or toggle):

**Spending by category:**
- Vico horizontal bar chart showing category distribution (Vico has no pie chart support)
- List breakdown below: category name, total amount, percentage
- Sorted by amount descending

**Income vs expenses over time:**
- Vico grouped bar chart (green = income, red = expenses)
- X-axis: time buckets (weeks for monthly view, months for yearly)
- Summary row: totals and net balance

**Export:**
- Toolbar button
- Generates .xlsx with two sheets:
  - "Expenses" — date, amount, category, note
  - "Income" — date, amount, source, note
- Bold headers, auto-sized columns, euro formatting (€)
- Opens Android share sheet

### Non-tab screens

**Add/Edit Expense:** amount input (numeric keyboard), category picker, date picker, optional note field.

**Add/Edit Income:** amount input, source text field, date picker, recurring toggle (shows interval picker when on), optional note.

**Add/Edit Recurring Expense:** amount, category picker, day of month picker, interval selector (monthly/quarterly), optional note.

**Category Management:** accessible from Settings icon or from category picker. List of categories, add new, edit name/icon, delete custom categories. Default categories can be hidden but not deleted.

## Recurring Expense Generation Logic

On app launch:

1. Query all active RecurringExpense entries.
2. For each, determine which months need generation:
   - Monthly: every month from creation (or last generation) to current month.
   - Quarterly: every 3 months from creation month offset.
3. For each due month, check RecurringExpenseGeneration to see if already generated.
4. If not generated: create an Expense with the template's amount, category, and the appropriate date (day from startDate clamped to last day of month if needed). Record in RecurringExpenseGeneration.
5. Same logic applies to recurring income entries.
6. User can edit or delete individual generated instances without affecting the template.

## Default Categories

Pre-seeded on first launch (isDefault = true):

1. Housing
2. Food & Groceries
3. Transport
4. Utilities
5. Healthcare
6. Entertainment
7. Clothing
8. Education
9. Savings & Investments
10. Other

Users can add custom categories, rename or delete custom ones. Default categories can be hidden but not deleted to prevent orphaned expenses.

## Currency

- Single currency: Euro (€)
- Stored as Long in cents (e.g., €12.50 → 1250)
- Displayed with euro formatting: "€12.50"
- No currency conversion needed

## Settings Screen

Accessible via gear icon in the top app bar (visible on all screens). Not a bottom nav tab.

### Features

**Export Data (JSON)**
- Serializes all 6 tables to a single JSON file
- Saves to app cache directory, opens Android share sheet (same pattern as Excel export)
- Filename: `expense_tracker_backup_YYYY-MM-DD.json`

**Import Data (JSON)**
- Opens Android file picker (`ACTION_OPEN_DOCUMENT`, MIME type `application/json`)
- Validates structure and foreign key consistency before modifying data
- Confirmation dialog: "This will replace all existing data. Continue?"
- Replace-all strategy: deletes all existing data and inserts imported data in a single Room transaction
- On validation failure: shows error message, no data is modified

## JSON Export Format

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-04-01T12:00:00Z",
  "categories": [
    { "id": 1, "name": "Housing", "icon": "home", "isDefault": true }
  ],
  "expenses": [
    { "id": 1, "amountCents": 1250, "categoryId": 1, "date": "2026-03-15", "note": null, "recurringExpenseId": null }
  ],
  "income": [
    { "id": 1, "amountCents": 300000, "source": "Salary", "date": "2026-03-01", "note": null, "isRecurring": false, "recurrenceInterval": null, "startDate": null, "recurringIncomeId": null }
  ],
  "recurringExpenses": [
    { "id": 1, "amountCents": 50000, "categoryId": 1, "interval": "MONTHLY", "note": "Rent", "startDate": "2026-01-01" }
  ],
  "recurringExpenseGenerations": [
    { "id": 1, "recurringExpenseId": 1, "generatedForMonth": "2026-01", "expenseId": 1 }
  ],
  "recurringIncomeGenerations": [
    { "id": 1, "recurringIncomeId": 1, "generatedForMonth": "2026-01", "incomeId": 1 }
  ]
}
```

All IDs are preserved so foreign key relationships remain valid. `schemaVersion` field supports future migration.

### Import Validation

1. `schemaVersion` is present and equals `1`
2. All required fields present with correct types (handled by deserialization)
3. Foreign key consistency: every `expense.categoryId` exists in imported `categories`, every `recurringExpenseGeneration.recurringExpenseId` exists in imported `recurringExpenses`, etc.
4. On failure: descriptive error message, no data modified

### JSON Schema

A standalone JSON Schema (draft 2020-12) file is committed at `app/schemas/export-schema.json` for documentation. The app validates imports via Kotlin code, not the schema file.

### Serialization

Uses `kotlinx.serialization` with dedicated data classes (not Room entities directly) for the JSON structure
