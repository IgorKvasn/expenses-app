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
                value = search,
                onValueChange = onSearchChange,
                label = { Text("Search") },
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
                        value = dateFrom?.let { DateFormatter.format(it) } ?: "",
                        onValueChange = {},
                        label = { Text("From date") },
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.weight(1f).clickable { showDateFromPicker = true },
                    )
                    OutlinedTextField(
                        value = dateTo?.let { DateFormatter.format(it) } ?: "",
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
