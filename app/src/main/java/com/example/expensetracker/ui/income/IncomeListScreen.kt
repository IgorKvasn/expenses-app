package com.example.expensetracker.ui.income

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.ui.components.CurrencyFormatter
import com.example.expensetracker.ui.components.FilterBar
import com.example.expensetracker.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeListScreen(
    onAddIncome: () -> Unit,
    onEditIncome: (Long) -> Unit,
    viewModel: IncomeListViewModel = hiltViewModel(),
) {
    val incomeItems by viewModel.incomeItems.collectAsStateWithLifecycle()
    val noteSearch by viewModel.noteSearch.collectAsStateWithLifecycle()
    val amountMin by viewModel.amountMin.collectAsStateWithLifecycle()
    val amountMax by viewModel.amountMax.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val sourceSearch by viewModel.sourceSearch.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Income") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddIncome) {
                Icon(Icons.Filled.Add, contentDescription = "Add income")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val dateFrom by viewModel.dateFrom.collectAsStateWithLifecycle()
            val dateTo by viewModel.dateTo.collectAsStateWithLifecycle()

            FilterBar(
                noteSearch = noteSearch,
                onNoteSearchChange = { viewModel.noteSearch.value = it },
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
                extraFilters = {
                    OutlinedTextField(
                        value = sourceSearch,
                        onValueChange = { viewModel.sourceSearch.value = it },
                        label = { Text("Search source") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )

            if (incomeItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No income entries yet", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(incomeItems, key = { it.id }) { income ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteIncome(income)
                                    true
                                } else false
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { onEditIncome(income.id) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(income.source, style = MaterialTheme.typography.titleMedium)
                                        Text(income.date.toString(), style = MaterialTheme.typography.bodySmall)
                                        if (!income.note.isNullOrBlank()) {
                                            Text(income.note, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Text(
                                        CurrencyFormatter.format(income.amountCents),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IncomeGreen,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
