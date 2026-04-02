package com.example.expensetracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.model.SortOrder
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FilterBar(
    search: String,
    onSearchChange: (String) -> Unit,
    amountMin: String,
    onAmountMinChange: (String) -> Unit,
    amountMax: String,
    onAmountMaxChange: (String) -> Unit,
    selectedMonth: YearMonth?,
    onSelectedMonthChange: (YearMonth?) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    onClearFilters: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    extraFilters: @Composable () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val hasActiveFilters = search.isNotEmpty() || amountMin.isNotEmpty() || amountMax.isNotEmpty() ||
        sortOrder != SortOrder.DATE_DESC

    Column(modifier = modifier.fillMaxWidth()) {
        MonthPicker(
            selectedMonth = selectedMonth,
            onSelectedMonthChange = onSelectedMonthChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            if (hasActiveFilters && onClearFilters != null) {
                IconButton(onClick = onClearFilters) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear filters")
                }
            }
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
}

private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

@Composable
private fun MonthPicker(
    selectedMonth: YearMonth?,
    onSelectedMonthChange: (YearMonth?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectedMonth != null) {
            IconButton(onClick = { onSelectedMonthChange(selectedMonth.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
        }

        Text(
            text = selectedMonth?.format(monthYearFormatter) ?: "All months",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )

        if (selectedMonth != null) {
            IconButton(onClick = { onSelectedMonthChange(selectedMonth.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        TextButton(
            onClick = {
                if (selectedMonth != null) {
                    onSelectedMonthChange(null)
                } else {
                    onSelectedMonthChange(YearMonth.now())
                }
            },
        ) {
            Text(if (selectedMonth != null) "All" else "Current")
        }
    }
}

private fun SortOrder.displayName(): String = when (this) {
    SortOrder.DATE_DESC -> "Date (newest)"
    SortOrder.DATE_ASC -> "Date (oldest)"
    SortOrder.AMOUNT_DESC -> "Amount (highest)"
    SortOrder.AMOUNT_ASC -> "Amount (lowest)"
}
