package com.example.expensetracker.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.ui.components.CurrencyFormatter
import com.example.expensetracker.ui.components.DateFormatter
import androidx.compose.material3.HorizontalDivider
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.ExpenseRedDark
import com.example.expensetracker.ui.theme.ExpenseRedDarkContainer
import com.example.expensetracker.ui.theme.ExpenseRedLight
import com.example.expensetracker.ui.theme.IncomeGreen
import com.example.expensetracker.ui.theme.IncomeGreenDark
import com.example.expensetracker.ui.theme.IncomeGreenDarkContainer
import com.example.expensetracker.ui.theme.IncomeGreenLight

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
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    var showAddMenu by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Scaffold(
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
                            Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = if (isDark) ExpenseRedDark else ExpenseRed)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Recurring Income") },
                        onClick = {
                            showAddMenu = false
                            onAddRecurringIncome()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = if (isDark) IncomeGreenDark else IncomeGreen)
                        },
                    )
                }
                FloatingActionButton(
                    onClick = { showAddMenu = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
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
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    "No recurring items",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "Tap + to set up recurring expenses or income",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                item {
                    RecurringSummaryCard(summary = summary, isDark = isDark)
                }
                if (recurringExpenses.isNotEmpty()) {
                    item {
                        Text(
                            "Expenses",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isDark) ExpenseRedDark else ExpenseRed,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(recurringExpenses, key = { "expense-${it.id}" }) { item ->
                        val categoryName = categories.find { it.id == item.categoryId }?.name ?: "Unknown"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onEditRecurring(item.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) ExpenseRedDarkContainer else ExpenseRedLight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.ArrowDownward,
                                        contentDescription = "Expense",
                                        tint = if (isDark) ExpenseRedDark else ExpenseRed,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(categoryName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${item.interval.displayName}, from ${DateFormatter.format(item.startDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (!item.note.isNullOrBlank()) {
                                        Text(
                                            item.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Text(
                                    CurrencyFormatter.format(item.amountCents),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) ExpenseRedDark else ExpenseRed,
                                )
                            }
                        }
                    }
                }
                if (recurringExpenses.isNotEmpty() && recurringIncome.isNotEmpty()) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
                if (recurringIncome.isNotEmpty()) {
                    item {
                        Text(
                            "Income",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isDark) IncomeGreenDark else IncomeGreen,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(recurringIncome, key = { "income-${it.id}" }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onEditRecurringIncome(item.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) IncomeGreenDarkContainer else IncomeGreenLight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.ArrowUpward,
                                        contentDescription = "Income",
                                        tint = if (isDark) IncomeGreenDark else IncomeGreen,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.source, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        buildString {
                                            append(item.recurrenceInterval?.displayName ?: "Recurring")
                                            if (item.startDate != null) {
                                                append(", from ${DateFormatter.format(item.startDate!!)}")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (!item.note.isNullOrBlank()) {
                                        Text(
                                            item.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Text(
                                    CurrencyFormatter.format(item.amountCents),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) IncomeGreenDark else IncomeGreen,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringSummaryCard(summary: RecurringSummary, isDark: Boolean) {
    val netColor = when {
        summary.monthlyNetCents > 0 -> if (isDark) IncomeGreenDark else IncomeGreen
        summary.monthlyNetCents < 0 -> if (isDark) ExpenseRedDark else ExpenseRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Monthly Summary",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Income", style = MaterialTheme.typography.bodyMedium)
                Text(
                    CurrencyFormatter.format(summary.monthlyIncomeCents),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) IncomeGreenDark else IncomeGreen,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Expenses", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "-${CurrencyFormatter.format(summary.monthlyExpenseCents)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) ExpenseRedDark else ExpenseRed,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Net", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    CurrencyFormatter.format(summary.monthlyNetCents),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = netColor,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
