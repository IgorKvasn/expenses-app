package com.example.expensetracker.domain.model

import java.time.LocalDate

data class ExpenseFilter(
    val categoryId: Long? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val amountMinCents: Long? = null,
    val amountMaxCents: Long? = null,
    val noteSearch: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
)
