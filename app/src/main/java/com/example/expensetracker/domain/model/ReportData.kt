package com.example.expensetracker.domain.model

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val totalCents: Long,
    val percentage: Double,
)

data class CategoryReport(
    val items: List<CategorySpending>,
    val totalCents: Long,
)

data class MonthlyBalance(
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long,
)

data class IncomeVsExpensesReport(
    val items: List<MonthlyBalance>,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
)
