package com.example.expensetracker.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.expensetracker.domain.model.CategoryReport

@Composable
fun CategoryBarChart(
    report: CategoryReport,
    modifier: Modifier = Modifier,
) {
    if (report.items.isEmpty()) return

    val labelListKey = remember { ExtraStore.Key<List<String>>() }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(report) {
        modelProducer.runTransaction {
            columnSeries {
                series(report.items.map { it.totalCents / 100.0 })
            }
            extras { it[labelListKey] = report.items.map { item -> item.categoryName } }
        }
    }

    val labelFormatter = remember(labelListKey) {
        CartesianValueFormatter { context, x, _ ->
            context.model.extraStore[labelListKey].getOrElse(x.toInt()) { "" }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(fill = Fill(Color(0xFF6650a4).toArgb()), thickness = 24.dp),
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = labelFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}
