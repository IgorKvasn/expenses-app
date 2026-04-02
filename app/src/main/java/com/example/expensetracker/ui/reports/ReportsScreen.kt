package com.example.expensetracker.ui.reports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.ui.components.CategoryIcon
import com.example.expensetracker.ui.components.CurrencyFormatter
import com.example.expensetracker.ui.components.DateFormatter
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.ExpenseRedDark
import com.example.expensetracker.ui.theme.ExpenseRedDarkContainer
import com.example.expensetracker.ui.theme.ExpenseRedLight
import com.example.expensetracker.ui.theme.IncomeGreen
import com.example.expensetracker.ui.theme.IncomeGreenDark
import com.example.expensetracker.ui.theme.IncomeGreenDarkContainer
import com.example.expensetracker.ui.theme.IncomeGreenLight
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val categoryReport by viewModel.categoryReport.collectAsStateWithLifecycle()
    val incomeVsExpensesReport by viewModel.incomeVsExpensesReport.collectAsStateWithLifecycle()
    val customDateFrom by viewModel.customDateFrom.collectAsStateWithLifecycle()
    val customDateTo by viewModel.customDateTo.collectAsStateWithLifecycle()
    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = { viewModel.exportToExcel(context) }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "Export")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = {
                            viewModel.selectedPeriod.value = period
                            viewModel.loadReports()
                        },
                        label = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            if (selectedPeriod == ReportPeriod.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { showDateFromPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(DateFormatter.format(customDateFrom))
                    }
                    Text("to", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = { showDateToPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(DateFormatter.format(customDateTo))
                    }
                }
            }

            TabRow(
                selectedTabIndex = ReportTab.entries.indexOf(selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Tab(
                    selected = selectedTab == ReportTab.CATEGORY,
                    onClick = { viewModel.selectedTab.value = ReportTab.CATEGORY },
                    text = { Text("By Category") },
                )
                Tab(
                    selected = selectedTab == ReportTab.INCOME_VS_EXPENSES,
                    onClick = { viewModel.selectedTab.value = ReportTab.INCOME_VS_EXPENSES },
                    text = { Text("Income vs Expenses") },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                ReportTab.CATEGORY -> {
                    CategoryBarChart(
                        report = categoryReport,
                        modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleMedium)
                            Text(
                                CurrencyFormatter.format(categoryReport.totalCents),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    categoryReport.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CategoryIcon(item.categoryIcon, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(item.categoryName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${"%.1f".format(item.percentage)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                CurrencyFormatter.format(item.totalCents),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
                ReportTab.INCOME_VS_EXPENSES -> {
                    IncomeVsExpensesChart(
                        report = incomeVsExpensesReport,
                        modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SummaryCard(
                            label = "Income",
                            amount = CurrencyFormatter.format(incomeVsExpensesReport.totalIncomeCents),
                            icon = Icons.Filled.ArrowUpward,
                            backgroundColor = if (isDark) IncomeGreenDarkContainer else IncomeGreenLight,
                            contentColor = if (isDark) IncomeGreenDark else IncomeGreen,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryCard(
                            label = "Expenses",
                            amount = CurrencyFormatter.format(incomeVsExpensesReport.totalExpenseCents),
                            icon = Icons.Filled.ArrowDownward,
                            backgroundColor = if (isDark) ExpenseRedDarkContainer else ExpenseRedLight,
                            contentColor = if (isDark) ExpenseRedDark else ExpenseRed,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val net = incomeVsExpensesReport.totalIncomeCents - incomeVsExpensesReport.totalExpenseCents
                    val netPositive = net >= 0
                    val netColor = if (netPositive) {
                        if (isDark) IncomeGreenDark else IncomeGreen
                    } else {
                        if (isDark) ExpenseRedDark else ExpenseRed
                    }
                    val netBg = if (netPositive) {
                        if (isDark) IncomeGreenDarkContainer else IncomeGreenLight
                    } else {
                        if (isDark) ExpenseRedDarkContainer else ExpenseRedLight
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = netBg),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingFlat,
                                    contentDescription = null,
                                    tint = netColor,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Net",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = netColor,
                                )
                            }
                            Text(
                                "${if (net < 0) "-" else ""}${CurrencyFormatter.format(kotlin.math.abs(net))}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = netColor,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDateFromPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = customDateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDateFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.customDateFrom.value = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.loadReports()
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
            initialSelectedDateMillis = customDateTo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDateToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        viewModel.customDateTo.value = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.loadReports()
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

@Composable
private fun SummaryCard(
    label: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                amount,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
