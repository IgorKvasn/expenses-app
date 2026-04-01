package com.example.expensetracker.domain.model

import java.time.LocalDate

data class IncomeFilter(
    val isRecurring: Boolean? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val amountMinCents: Long? = null,
    val amountMaxCents: Long? = null,
    val search: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
)
