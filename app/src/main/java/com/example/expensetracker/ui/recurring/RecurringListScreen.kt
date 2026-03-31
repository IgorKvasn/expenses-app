package com.example.expensetracker.ui.recurring

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.ui.components.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    onAddRecurring: () -> Unit,
    onEditRecurring: (Long) -> Unit,
    viewModel: RecurringListViewModel = hiltViewModel(),
) {
    val recurringExpenses by viewModel.recurringExpenses.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recurring") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecurring) {
                Icon(Icons.Filled.Add, contentDescription = "Add recurring")
            }
        },
    ) { padding ->
        if (recurringExpenses.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No recurring expenses", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(recurringExpenses, key = { it.id }) { item ->
                    val categoryName = categories.find { it.id == item.categoryId }?.name ?: "Unknown"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onEditRecurring(item.id) },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(categoryName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${item.interval.name.lowercase().replaceFirstChar { it.uppercase() }} on day ${item.dayOfMonth}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (!item.note.isNullOrBlank()) {
                                    Text(item.note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(
                                CurrencyFormatter.format(item.amountCents),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Switch(
                                checked = item.isActive,
                                onCheckedChange = { viewModel.toggleActive(item) },
                            )
                            IconButton(onClick = { viewModel.delete(item) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
