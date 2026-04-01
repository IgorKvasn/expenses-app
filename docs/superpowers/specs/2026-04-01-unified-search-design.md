# Unified Search Enhancement

## Summary

Replace the current note-only search (expenses) and separate source/note search fields (income) with a single unified search bar on both screens. The search matches against all text-relevant fields plus amount (as raw cents substring).

## Current Behavior

- **Expenses:** `noteSearch` field matches only `note` column (case-insensitive LIKE)
- **Income:** Two separate fields — `sourceSearch` matches `source`, `noteSearch` matches `note`
- Neither screen searches by amount

## New Behavior

- **Expenses:** Single `search` field matches `note` OR `amountCents` (cast to text, substring match)
- **Income:** Single `search` field matches `source` OR `note` OR `amountCents` (cast to text, substring match)
- UI label/placeholder updated to "Search" on both screens

## Amount Matching

Amount is stored as `amountCents: Long`. Searching "150" matches any record where the raw cents value contains "150" as a substring (e.g., 150 cents, 1500 cents, 2150 cents).

SQL approach: `CAST(amountCents AS TEXT) LIKE '%' || :search || '%'`

## Changes by Layer

### Domain Models
- `ExpenseFilter`: rename `noteSearch` -> `search`
- `IncomeFilter`: replace `sourceSearch` + `noteSearch` with single `search`

### Data Layer
- `ExpenseDao`: rename param `noteSearch` -> `search`, add `OR CAST(amountCents AS TEXT) LIKE '%' || :search || '%'`
- `IncomeDao`: replace `sourceSearch`/`noteSearch` params with single `search`, match against `source`, `note`, and `amountCents`
- `ExpenseRepository`: pass renamed param
- `IncomeRepository`: pass single search param

### ViewModel Layer
- `ExpenseListViewModel`: rename `noteSearch` StateFlow -> `search`
- `IncomeListViewModel`: merge `sourceSearch` + `noteSearch` StateFlows into single `search`

### UI Layer
- `ExpenseListScreen`: pass renamed field to FilterBar
- `IncomeListScreen`: remove separate source search OutlinedTextField, pass unified `search`
- `FilterBar`: update label/placeholder from "Search notes" to "Search"

## Out of Scope

- Full-text search indexing
- Searching by category name (would require JOIN)
- Searching by formatted dollar amount
