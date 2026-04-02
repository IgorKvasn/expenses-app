package com.example.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RecurringSummary(
    val monthlyExpenseCents: Long = 0,
    val monthlyIncomeCents: Long = 0,
) {
    val monthlyNetCents: Long get() = monthlyIncomeCents - monthlyExpenseCents
}

@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val incomeRepository: IncomeRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val recurringExpenses: StateFlow<List<RecurringExpenseEntity>> =
        recurringExpenseRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringIncome: StateFlow<List<IncomeEntity>> =
        incomeRepository.getRecurring()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<RecurringSummary> =
        combine(recurringExpenses, recurringIncome) { expenses, income ->
            RecurringSummary(
                monthlyExpenseCents = expenses.sumOf { it.amountCents * 1L / it.interval.months },
                monthlyIncomeCents = income.sumOf {
                    it.amountCents * 1L / (it.recurrenceInterval?.months ?: 1)
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecurringSummary())

}
