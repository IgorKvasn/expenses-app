package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.IncomeVsExpensesReport
import com.example.expensetracker.domain.model.MonthlyBalance
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class IncomeVsExpensesReportUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
) {
    suspend operator fun invoke(from: LocalDate, to: LocalDate): IncomeVsExpensesReport {
        val items = mutableListOf<MonthlyBalance>()
        var month = YearMonth.from(from)
        val endMonth = YearMonth.from(to)

        while (!month.isAfter(endMonth)) {
            val monthStart = maxOf(month.atDay(1), from)
            val monthEnd = minOf(month.atEndOfMonth(), to)
            val income = incomeRepository.getTotalInRange(monthStart, monthEnd)
            val expenses = expenseRepository.getTotalInRange(monthStart, monthEnd)
            val label = month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            items.add(MonthlyBalance(label = label, incomeCents = income, expenseCents = expenses))
            month = month.plusMonths(1)
        }

        return IncomeVsExpensesReport(
            items = items,
            totalIncomeCents = items.sumOf { it.incomeCents },
            totalExpenseCents = items.sumOf { it.expenseCents },
        )
    }
}
