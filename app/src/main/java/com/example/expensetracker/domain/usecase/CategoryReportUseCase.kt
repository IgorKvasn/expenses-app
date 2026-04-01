package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.domain.model.CategoryReport
import com.example.expensetracker.domain.model.CategorySpending
import java.time.LocalDate
import javax.inject.Inject

class CategoryReportUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(from: LocalDate, to: LocalDate): CategoryReport {
        val totals = expenseRepository.getCategoryTotals(from, to)
        val grandTotal = totals.sumOf { it.totalCents }
        val items = totals.map { ct ->
            CategorySpending(
                categoryId = ct.categoryId,
                categoryName = ct.categoryName,
                categoryIcon = ct.categoryIcon,
                totalCents = ct.totalCents,
                percentage = if (grandTotal > 0) ct.totalCents.toDouble() / grandTotal * 100.0 else 0.0,
            )
        }
        return CategoryReport(items = items, totalCents = grandTotal)
    }
}
