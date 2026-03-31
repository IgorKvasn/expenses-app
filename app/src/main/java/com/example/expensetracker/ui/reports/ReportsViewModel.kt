package com.example.expensetracker.ui.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.CategoryReport
import com.example.expensetracker.domain.model.ExpenseFilter
import com.example.expensetracker.domain.model.IncomeFilter
import com.example.expensetracker.domain.model.IncomeVsExpensesReport
import com.example.expensetracker.domain.usecase.CategoryReportUseCase
import com.example.expensetracker.domain.usecase.ExportToExcelUseCase
import com.example.expensetracker.domain.usecase.IncomeVsExpensesReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class ReportPeriod { MONTH, QUARTER, YEAR, CUSTOM }
enum class ReportTab { CATEGORY, INCOME_VS_EXPENSES }

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val categoryReportUseCase: CategoryReportUseCase,
    private val incomeVsExpensesReportUseCase: IncomeVsExpensesReportUseCase,
    private val exportToExcelUseCase: ExportToExcelUseCase,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)
    val selectedTab = MutableStateFlow(ReportTab.CATEGORY)
    val customDateFrom = MutableStateFlow(YearMonth.now().atDay(1))
    val customDateTo = MutableStateFlow(YearMonth.now().atEndOfMonth())

    private val _categoryReport = MutableStateFlow(CategoryReport(emptyList(), 0L))
    val categoryReport: StateFlow<CategoryReport> = _categoryReport

    private val _incomeVsExpensesReport = MutableStateFlow(IncomeVsExpensesReport(emptyList(), 0L, 0L))
    val incomeVsExpensesReport: StateFlow<IncomeVsExpensesReport> = _incomeVsExpensesReport

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            val (from, to) = getDateRange()
            _categoryReport.value = categoryReportUseCase(from, to)
            _incomeVsExpensesReport.value = incomeVsExpensesReportUseCase(from, to)
        }
    }

    fun exportToExcel(context: Context) {
        viewModelScope.launch {
            val (from, to) = getDateRange()
            val expenses = expenseRepository.getFiltered(
                ExpenseFilter(dateFrom = from, dateTo = to)
            ).first()
            val income = incomeRepository.getFiltered(
                IncomeFilter(dateFrom = from, dateTo = to)
            ).first()
            val categories = categoryRepository.getAll().first().associateBy { it.id }

            val file = File(context.cacheDir, "expense_report.xlsx")
            exportToExcelUseCase(expenses, income, categories, file)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Report"))
        }
    }

    private fun getDateRange(): Pair<LocalDate, LocalDate> {
        val now = YearMonth.now()
        return when (selectedPeriod.value) {
            ReportPeriod.MONTH -> now.atDay(1) to now.atEndOfMonth()
            ReportPeriod.QUARTER -> {
                val quarterStart = now.withMonth(((now.monthValue - 1) / 3) * 3 + 1)
                quarterStart.atDay(1) to quarterStart.plusMonths(2).atEndOfMonth()
            }
            ReportPeriod.YEAR -> LocalDate.of(now.year, 1, 1) to LocalDate.of(now.year, 12, 31)
            ReportPeriod.CUSTOM -> customDateFrom.value to customDateTo.value
        }
    }
}
