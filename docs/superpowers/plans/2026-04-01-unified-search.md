# Unified Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace note-only search with a unified search bar that matches note, amount (and source for income) on both expense and income list screens.

**Architecture:** Rename `noteSearch` to `search` in filter models, DAOs, repositories, and ViewModels. Update SQL queries to OR-match against additional columns. Merge income's two separate search fields into one. Update UI labels.

**Tech Stack:** Room (SQL), Kotlin Coroutines Flow, Jetpack Compose

---

### File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `domain/model/ExpenseFilter.kt` | Modify | Rename `noteSearch` -> `search` |
| `domain/model/IncomeFilter.kt` | Modify | Replace `sourceSearch` + `noteSearch` with `search` |
| `data/db/dao/ExpenseDao.kt` | Modify | Update SQL: match `note` OR `amountCents` on `:search` |
| `data/db/dao/IncomeDao.kt` | Modify | Update SQL: single `:search` matches `source`, `note`, `amountCents` |
| `data/repository/ExpenseRepository.kt` | Modify | Pass renamed `search` param |
| `data/repository/IncomeRepository.kt` | Modify | Pass single `search` param |
| `ui/expenses/ExpenseListViewModel.kt` | Modify | Rename `noteSearch` -> `search` |
| `ui/income/IncomeListViewModel.kt` | Modify | Merge `sourceSearch` + `noteSearch` -> `search` |
| `ui/expenses/ExpenseListScreen.kt` | Modify | Pass renamed field |
| `ui/income/IncomeListScreen.kt` | Modify | Remove separate source search, pass unified `search` |
| `ui/components/FilterBar.kt` | Modify | Rename param + update label/placeholder |

All paths relative to `src/main/java/com/example/expensetracker/`.

---

### Task 1: Update domain filter models

**Files:**
- Modify: `domain/model/ExpenseFilter.kt:11`
- Modify: `domain/model/IncomeFilter.kt:7-8`

- [ ] **Step 1: Update ExpenseFilter**

In `ExpenseFilter.kt`, rename `noteSearch` to `search`:

```kotlin
data class ExpenseFilter(
    val categoryId: Long? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val amountMinCents: Long? = null,
    val amountMaxCents: Long? = null,
    val search: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
)
```

- [ ] **Step 2: Update IncomeFilter**

In `IncomeFilter.kt`, replace `sourceSearch` and `noteSearch` with single `search`:

```kotlin
data class IncomeFilter(
    val isRecurring: Boolean? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val amountMinCents: Long? = null,
    val amountMaxCents: Long? = null,
    val search: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
)
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/expensetracker/domain/model/ExpenseFilter.kt \
       src/main/java/com/example/expensetracker/domain/model/IncomeFilter.kt
git commit -m "refactor: rename noteSearch/sourceSearch to search in filter models"
```

---

### Task 2: Update ExpenseDao SQL query

**Files:**
- Modify: `data/db/dao/ExpenseDao.kt:13-35`

- [ ] **Step 1: Update getFiltered query and parameter**

Replace the `getFiltered` method. The SQL adds an OR clause matching `CAST(amountCents AS TEXT)` against `:search`. Rename parameter from `noteSearch` to `search`:

```kotlin
@Query("""
    SELECT * FROM expenses
    WHERE (:categoryId IS NULL OR categoryId = :categoryId)
      AND (:dateFrom IS NULL OR date >= :dateFrom)
      AND (:dateTo IS NULL OR date <= :dateTo)
      AND (:amountMin IS NULL OR amountCents >= :amountMin)
      AND (:amountMax IS NULL OR amountCents <= :amountMax)
      AND (:search IS NULL OR note LIKE '%' || :search || '%' COLLATE NOCASE
           OR CAST(amountCents AS TEXT) LIKE '%' || :search || '%')
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
    search: String?,
    sortOrder: String,
): Flow<List<ExpenseEntity>>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/expensetracker/data/db/dao/ExpenseDao.kt
git commit -m "feat: expense search matches note and amountCents"
```

---

### Task 3: Update IncomeDao SQL query

**Files:**
- Modify: `data/db/dao/IncomeDao.kt:13-37`

- [ ] **Step 1: Update getFiltered query and parameters**

Replace the `getFiltered` method. Merge `sourceSearch` and `noteSearch` into single `search` param. The SQL matches `source`, `note`, and `CAST(amountCents AS TEXT)` with OR logic:

```kotlin
@Query("""
    SELECT * FROM income
    WHERE (:isRecurring IS NULL OR isRecurring = :isRecurring)
      AND (:dateFrom IS NULL OR date >= :dateFrom)
      AND (:dateTo IS NULL OR date <= :dateTo)
      AND (:amountMin IS NULL OR amountCents >= :amountMin)
      AND (:amountMax IS NULL OR amountCents <= :amountMax)
      AND (:search IS NULL OR source LIKE '%' || :search || '%' COLLATE NOCASE
           OR note LIKE '%' || :search || '%' COLLATE NOCASE
           OR CAST(amountCents AS TEXT) LIKE '%' || :search || '%')
    ORDER BY
        CASE WHEN :sortOrder = 'DATE_DESC' THEN date END DESC,
        CASE WHEN :sortOrder = 'DATE_ASC' THEN date END ASC,
        CASE WHEN :sortOrder = 'AMOUNT_DESC' THEN amountCents END DESC,
        CASE WHEN :sortOrder = 'AMOUNT_ASC' THEN amountCents END ASC
""")
fun getFiltered(
    isRecurring: Boolean?,
    dateFrom: String?,
    dateTo: String?,
    amountMin: Long?,
    amountMax: Long?,
    search: String?,
    sortOrder: String,
): Flow<List<IncomeEntity>>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/expensetracker/data/db/dao/IncomeDao.kt
git commit -m "feat: income search matches source, note, and amountCents"
```

---

### Task 4: Update repositories

**Files:**
- Modify: `data/repository/ExpenseRepository.kt:16-25`
- Modify: `data/repository/IncomeRepository.kt:22-32`

- [ ] **Step 1: Update ExpenseRepository**

Change `noteSearch` to `search` in the `getFiltered` call:

```kotlin
fun getFiltered(filter: ExpenseFilter): Flow<List<ExpenseEntity>> =
    expenseDao.getFiltered(
        categoryId = filter.categoryId,
        dateFrom = filter.dateFrom?.toString(),
        dateTo = filter.dateTo?.toString(),
        amountMin = filter.amountMinCents,
        amountMax = filter.amountMaxCents,
        search = filter.search,
        sortOrder = filter.sortOrder.name,
    )
```

- [ ] **Step 2: Update IncomeRepository**

Replace `sourceSearch` and `noteSearch` with single `search` in the `getFiltered` call:

```kotlin
fun getFiltered(filter: IncomeFilter): Flow<List<IncomeEntity>> =
    incomeDao.getFiltered(
        isRecurring = filter.isRecurring,
        dateFrom = filter.dateFrom?.toString(),
        dateTo = filter.dateTo?.toString(),
        amountMin = filter.amountMinCents,
        amountMax = filter.amountMaxCents,
        search = filter.search,
        sortOrder = filter.sortOrder.name,
    )
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/expensetracker/data/repository/ExpenseRepository.kt \
       src/main/java/com/example/expensetracker/data/repository/IncomeRepository.kt
git commit -m "refactor: pass unified search param through repositories"
```

---

### Task 5: Update FilterBar component

**Files:**
- Modify: `ui/components/FilterBar.kt:41-74`

- [ ] **Step 1: Rename parameter and update label**

Rename `noteSearch`/`onNoteSearchChange` to `search`/`onSearchChange` and update the label text:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    search: String,
    onSearchChange: (String) -> Unit,
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
```

And update the OutlinedTextField inside (line 68-74):

```kotlin
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/example/expensetracker/ui/components/FilterBar.kt
git commit -m "refactor: rename FilterBar noteSearch to search, update label"
```

---

### Task 6: Update ExpenseListViewModel and ExpenseListScreen

**Files:**
- Modify: `ui/expenses/ExpenseListViewModel.kt:35,44,49`
- Modify: `ui/expenses/ExpenseListScreen.kt:75,129-130`

- [ ] **Step 1: Update ExpenseListViewModel**

Rename `noteSearch` to `search` on line 35:

```kotlin
val search = MutableStateFlow("")
```

Update the combine block (line 44 and 49) — replace `noteSearch` with `search`:

```kotlin
    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<ExpenseEntity>> = combine(
        selectedCategoryId, search, amountMin, amountMax, sortOrder, dateFrom, dateTo,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ExpenseFilter(
            categoryId = values[0] as Long?,
            search = (values[1] as String).ifBlank { null },
            amountMinCents = amountStringToCents(values[2] as String),
            amountMaxCents = amountStringToCents(values[3] as String),
            sortOrder = values[4] as SortOrder,
            dateFrom = values[5] as LocalDate?,
            dateTo = values[6] as LocalDate?,
        )
    }.flatMapLatest { filter ->
        expenseRepository.getFiltered(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- [ ] **Step 2: Update ExpenseListScreen**

Change line 75 to collect from `search`:

```kotlin
    val search by viewModel.search.collectAsStateWithLifecycle()
```

Update the FilterBar call (lines 128-130):

```kotlin
            FilterBar(
                search = search,
                onSearchChange = { viewModel.search.value = it },
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/expensetracker/ui/expenses/ExpenseListViewModel.kt \
       src/main/java/com/example/expensetracker/ui/expenses/ExpenseListScreen.kt
git commit -m "refactor: use unified search in expense list"
```

---

### Task 7: Update IncomeListViewModel and IncomeListScreen

**Files:**
- Modify: `ui/income/IncomeListViewModel.kt:28-29,38,41-44`
- Modify: `ui/income/IncomeListScreen.kt:66,70,114-136`

- [ ] **Step 1: Update IncomeListViewModel**

Replace `sourceSearch` and `noteSearch` with single `search` on lines 28-29:

```kotlin
    val search = MutableStateFlow("")
```

Update the combine block — change from 7 flows to 6 (remove `sourceSearch` and `noteSearch`, add `search`):

```kotlin
    @OptIn(ExperimentalCoroutinesApi::class)
    val incomeItems: StateFlow<List<IncomeEntity>> = combine(
        search, amountMin, amountMax, sortOrder, dateFrom, dateTo,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        IncomeFilter(
            isRecurring = null,
            search = (values[0] as String).ifBlank { null },
            amountMinCents = amountStringToCents(values[1] as String),
            amountMaxCents = amountStringToCents(values[2] as String),
            sortOrder = values[3] as SortOrder,
            dateFrom = values[4] as LocalDate?,
            dateTo = values[5] as LocalDate?,
        )
    }.flatMapLatest { filter ->
        incomeRepository.getFiltered(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- [ ] **Step 2: Update IncomeListScreen**

Replace lines 66 and 70 (remove `noteSearch` and `sourceSearch` collections, add `search`):

```kotlin
    val search by viewModel.search.collectAsStateWithLifecycle()
```

Remove the `sourceSearch` collection entirely (line 70).

Update the FilterBar call (lines 114-136) — pass `search`/`onSearchChange` and remove the `extraFilters` block with the source search OutlinedTextField:

```kotlin
            FilterBar(
                search = search,
                onSearchChange = { viewModel.search.value = it },
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
            )
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/expensetracker/ui/income/IncomeListViewModel.kt \
       src/main/java/com/example/expensetracker/ui/income/IncomeListScreen.kt
git commit -m "refactor: use unified search in income list"
```

---

### Task 8: Build verification

- [ ] **Step 1: Build the project**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. If there are compilation errors, fix references to old field names (`noteSearch`, `sourceSearch`) that were missed.

- [ ] **Step 2: Commit any fixes if needed**
