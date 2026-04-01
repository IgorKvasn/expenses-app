# Save Success Animation — Coin Spin

## Overview

Add a full-screen overlay animation that plays when an expense or income is successfully saved. A coin emoji spins, transforms into a green checkmark, then the app navigates back to the list screen.

## Animation Sequence (~1.6s total)

1. **Overlay fade-in** (300ms) — semi-transparent dark scrim covers the screen
2. **Coin spin** (800ms) — 🪙 emoji renders centered, rotates 720° on the Y-axis with a slight scale-up (1.0→1.3→1.0) using ease-in-out timing
3. **Coin shrink** (300ms) — coin scales to 0 and fades out
4. **Checkmark pop** (400ms) — green circle with white ✓ pops in with overshoot easing (`cubic-bezier(0.34, 1.56, 0.64, 1)`)
5. **Label fade-up** (300ms, starts with step 4) — "Expense Added!" or "Income Added!" text fades up below the checkmark
6. **Hold + navigate** (~500ms pause) — brief hold, then `popBackStack()` to return to the list

## Behavior

- Same animation for both expense and income; only the label text differs
- Animation is non-interruptible (no back-press or tap-to-dismiss during playback)
- If save fails (validation error), no animation plays — existing inline error behavior unchanged

## Components

### `SuccessAnimationOverlay` composable

- Shared composable in `ui/components/`
- Parameters:
  - `visible: Boolean` — controls whether the overlay is shown
  - `label: String` — text to show below checkmark (e.g., "Expense Added!")
  - `onAnimationEnd: () -> Unit` — called after the full sequence, triggers navigation
- Uses `Animatable` for coin rotation/scale, `AnimatedVisibility` for overlay and checkmark
- Coin rendered as Text composable with `graphicsLayer { rotationY = ... }`
- Checkmark rendered as a `Box` with circular green background + white ✓ `Text`

### Screen integration

- `AddEditExpenseScreen` and `AddEditIncomeScreen` both get a `showSuccessAnimation` state (`mutableStateOf(false)`)
- On save: instead of calling `onNavigateBack` directly, set `showSuccessAnimation = true`
- `SuccessAnimationOverlay` composable is placed in the screen's `Box` layout, on top of the form
- `onAnimationEnd` callback calls `onNavigateBack`
- The ViewModel `save()` function signature stays the same — the screen-level code manages the animation state

### State management

- `showSuccessAnimation` lives in the Screen composable (not ViewModel) since it's purely UI state
- ViewModel's `save(onComplete)` callback sets the flag: `onComplete = { showSuccessAnimation = true }`
- No new ViewModel state needed

## Dependencies

None — Compose animation APIs (`Animatable`, `AnimatedVisibility`, `graphicsLayer`) are already available via the Compose BOM.

## Files to create/modify

- **Create:** `app/src/main/java/com/example/expensetracker/ui/components/SuccessAnimationOverlay.kt`
- **Modify:** `app/src/main/java/com/example/expensetracker/ui/expenses/AddEditExpenseScreen.kt` — add overlay + state
- **Modify:** `app/src/main/java/com/example/expensetracker/ui/income/AddEditIncomeScreen.kt` — add overlay + state
