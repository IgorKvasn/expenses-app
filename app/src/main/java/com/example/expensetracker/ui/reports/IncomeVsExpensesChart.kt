package com.example.expensetracker.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.example.expensetracker.domain.model.IncomeVsExpensesReport
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen

@Composable
fun IncomeVsExpensesChart(
    report: IncomeVsExpensesReport,
    modifier: Modifier = Modifier,
) {
    if (report.items.isEmpty()) return

    val monthLabelKey = remember { ExtraStore.Key<List<String>>() }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(report) {
        modelProducer.runTransaction {
            columnSeries {
                series(report.items.map { it.incomeCents / 100.0 })
                series(report.items.map { it.expenseCents / 100.0 })
            }
            extras { it[monthLabelKey] = report.items.map { item -> item.label } }
        }
    }

    val monthFormatter = remember(monthLabelKey) {
        CartesianValueFormatter { context, x, _ ->
            context.model.extraStore[monthLabelKey].getOrElse(x.toInt()) { "" }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(fill = Fill(IncomeGreen.toArgb()), thickness = 16.dp),
                    rememberLineComponent(fill = Fill(ExpenseRed.toArgb()), thickness = 16.dp),
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = monthFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}
