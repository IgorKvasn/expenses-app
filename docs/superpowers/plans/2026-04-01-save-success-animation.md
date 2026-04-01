# Save Success Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a coin-spin → checkmark full-screen overlay animation when an expense or income is saved.

**Architecture:** A single shared `SuccessAnimationOverlay` composable orchestrates a sequenced animation using `Animatable`. Each add/edit screen holds a `showSuccessAnimation` boolean state and renders the overlay on top of the `Scaffold`. On save success, the state flips to `true`, the animation plays, and `onAnimationEnd` triggers `popBackStack()`.

**Tech Stack:** Jetpack Compose animation APIs (`Animatable`, `AnimatedVisibility`, `graphicsLayer`) — already available via Compose BOM.

---

### Task 1: Create `SuccessAnimationOverlay` composable

**Files:**
- Create: `app/src/main/java/com/example/expensetracker/ui/components/SuccessAnimationOverlay.kt`

- [ ] **Step 1: Create the composable file**

```kotlin
package com.example.expensetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SuccessAnimationOverlay(
    visible: Boolean,
    label: String,
    onAnimationEnd: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200)),
    ) {
        val coinRotation = remember { Animatable(0f) }
        val coinScale = remember { Animatable(1f) }
        val coinAlpha = remember { Animatable(1f) }
        var showCheckmark by remember { mutableStateOf(false) }
        val checkmarkScale = remember { Animatable(0f) }
        var showLabel by remember { mutableStateOf(false) }
        val labelAlpha = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            // Phase 1: Coin spin (800ms)
            launch {
                coinRotation.animateTo(
                    targetValue = 720f,
                    animationSpec = tween(durationMillis = 800, easing = EaseInOut),
                )
            }
            launch {
                coinScale.animateTo(1.3f, animationSpec = tween(400, easing = EaseInOut))
                coinScale.animateTo(1f, animationSpec = tween(400, easing = EaseInOut))
            }
            delay(800)

            // Phase 2: Coin shrink (300ms)
            launch { coinScale.animateTo(0f, animationSpec = tween(300)) }
            coinAlpha.animateTo(0f, animationSpec = tween(300))

            // Phase 3: Checkmark pop (400ms) + label fade (300ms)
            showCheckmark = true
            showLabel = true
            launch {
                checkmarkScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f),
                    ),
                )
            }
            launch {
                labelAlpha.animateTo(1f, animationSpec = tween(300))
            }

            // Phase 4: Hold then navigate
            delay(500)
            onAnimationEnd()
        }

        // Block back-press during animation
        BackHandler {}

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Coin
                    if (!showCheckmark) {
                        Text(
                            text = "🪙",
                            fontSize = 64.sp,
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationY = coinRotation.value
                                    scaleX = coinScale.value
                                    scaleY = coinScale.value
                                    alpha = coinAlpha.value
                                },
                        )
                    }

                    // Checkmark
                    if (showCheckmark) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .scale(checkmarkScale.value)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "✓",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showLabel) {
                    Text(
                        text = label,
                        color = Color(0xFF4CAF50),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.graphicsLayer { alpha = labelAlpha.value },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd /data/projects/expenses-app/app && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/components/SuccessAnimationOverlay.kt
git commit -m "feat: add SuccessAnimationOverlay composable"
```

---

### Task 2: Integrate animation into `AddEditExpenseScreen`

**Files:**
- Modify: `app/src/main/java/com/example/expensetracker/ui/expenses/AddEditExpenseScreen.kt`

- [ ] **Step 1: Add animation state and change save callback**

At `AddEditExpenseScreen.kt:64` (after `var showDeleteConfirmation`), add:

```kotlin
var showSuccessAnimation by remember { mutableStateOf(false) }
```

At `AddEditExpenseScreen.kt:143`, change the button's `onClick` from:

```kotlin
onClick = { viewModel.save(onComplete = onNavigateBack) },
```

to:

```kotlin
onClick = { viewModel.save(onComplete = { showSuccessAnimation = true }) },
```

- [ ] **Step 2: Wrap Scaffold in Box and add overlay**

Change the top-level structure to wrap the existing `Scaffold` in a `Box` and place the overlay after it. Before the `Scaffold(` call (line 67), add:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
```

After the closing `}` of the `if (showDatePicker) { ... }` block at the end of the function (line 201), add:

```kotlin
    SuccessAnimationOverlay(
        visible = showSuccessAnimation,
        label = if (expenseId != null) "Expense Updated!" else "Expense Added!",
        onAnimationEnd = onNavigateBack,
    )
}
```

- [ ] **Step 3: Add required imports**

Add to the imports section:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.example.expensetracker.ui.components.SuccessAnimationOverlay
```

Note: `Box` and `fillMaxSize` may already be imported — check before adding duplicates.

- [ ] **Step 4: Verify it compiles**

Run: `cd /data/projects/expenses-app/app && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/expenses/AddEditExpenseScreen.kt
git commit -m "feat: add save success animation to AddEditExpenseScreen"
```

---

### Task 3: Integrate animation into `AddEditIncomeScreen`

**Files:**
- Modify: `app/src/main/java/com/example/expensetracker/ui/income/AddEditIncomeScreen.kt`

- [ ] **Step 1: Add animation state and change save callback**

At `AddEditIncomeScreen.kt:62` (after `var showDeleteConfirmation`), add:

```kotlin
var showSuccessAnimation by remember { mutableStateOf(false) }
```

At `AddEditIncomeScreen.kt:140`, change the button's `onClick` from:

```kotlin
onClick = { viewModel.save(onComplete = onNavigateBack) },
```

to:

```kotlin
onClick = { viewModel.save(onComplete = { showSuccessAnimation = true }) },
```

- [ ] **Step 2: Wrap Scaffold in Box and add overlay**

Same pattern as Task 2. Before `Scaffold(` (line 64), add:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
```

After the closing `}` of the `if (showDatePicker) { ... }` block at the end of the function (line 199), add:

```kotlin
    SuccessAnimationOverlay(
        visible = showSuccessAnimation,
        label = if (incomeId != null) "Income Updated!" else "Income Added!",
        onAnimationEnd = onNavigateBack,
    )
}
```

- [ ] **Step 3: Add required imports**

Add to the imports section:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.example.expensetracker.ui.components.SuccessAnimationOverlay
```

- [ ] **Step 4: Verify it compiles**

Run: `cd /data/projects/expenses-app/app && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/expensetracker/ui/income/AddEditIncomeScreen.kt
git commit -m "feat: add save success animation to AddEditIncomeScreen"
```
