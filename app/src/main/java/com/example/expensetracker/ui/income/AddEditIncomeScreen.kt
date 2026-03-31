package com.example.expensetracker.ui.income

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.ui.components.AmountInput
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIncomeScreen(
    incomeId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditIncomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(incomeId) {
        if (incomeId != null) viewModel.loadIncome(incomeId)
    }

    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val source by viewModel.source.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val isRecurring by viewModel.isRecurring.collectAsStateWithLifecycle()
    val recurrenceInterval by viewModel.recurrenceInterval.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (incomeId != null) "Edit Income" else "Add Income") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            AmountInput(
                value = amount,
                onValueChange = { viewModel.amount.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = source,
                onValueChange = { viewModel.source.value = it },
                label = { Text("Source") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = date.toString(),
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    showDatePicker = true
                                }
                            }
                        }
                    },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recurring", modifier = Modifier.weight(1f))
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { viewModel.isRecurring.value = it },
                )
            }
            if (isRecurring) {
                Spacer(modifier = Modifier.height(8.dp))
                var intervalExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = intervalExpanded,
                    onExpandedChange = { intervalExpanded = it },
                ) {
                    OutlinedTextField(
                        value = recurrenceInterval.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Interval") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = intervalExpanded,
                        onDismissRequest = { intervalExpanded = false },
                    ) {
                        Interval.entries.forEach { interval ->
                            DropdownMenuItem(
                                text = { Text(interval.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.recurrenceInterval.value = interval
                                    intervalExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { viewModel.note.value = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save(onComplete = onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (incomeId != null) "Update" else "Add")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.date.value = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
