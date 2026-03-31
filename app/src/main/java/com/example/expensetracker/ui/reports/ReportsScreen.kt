package com.example.expensetracker.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.ui.components.CurrencyFormatter
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                actions = {
                    IconButton(onClick = { viewModel.exportToExcel(context) }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Export")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                    )
                }
            }

            TabRow(selectedTabIndex = ReportTab.entries.indexOf(selectedTab)) {
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
                    Text(
                        "Total: ${CurrencyFormatter.format(categoryReport.totalCents)}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    categoryReport.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(item.categoryName)
                            Text("${CurrencyFormatter.format(item.totalCents)} (${"%.1f".format(item.percentage)}%)")
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Income:", color = IncomeGreen)
                        Text(CurrencyFormatter.format(incomeVsExpensesReport.totalIncomeCents), color = IncomeGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Expenses:", color = ExpenseRed)
                        Text(CurrencyFormatter.format(incomeVsExpensesReport.totalExpenseCents), color = ExpenseRed)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val net = incomeVsExpensesReport.totalIncomeCents - incomeVsExpensesReport.totalExpenseCents
                        Text("Net:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            CurrencyFormatter.format(kotlin.math.abs(net)),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (net >= 0) IncomeGreen else ExpenseRed,
                        )
                    }
                }
            }
        }
    }
}
