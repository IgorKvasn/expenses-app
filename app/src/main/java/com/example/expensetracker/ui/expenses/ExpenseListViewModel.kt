package com.example.expensetracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.domain.model.ExpenseFilter
import com.example.expensetracker.domain.model.SortOrder
import com.example.expensetracker.ui.components.amountStringToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCategoryId = MutableStateFlow<Long?>(null)
    val noteSearch = MutableStateFlow("")
    val amountMin = MutableStateFlow("")
    val amountMax = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val dateFrom = MutableStateFlow<LocalDate?>(YearMonth.now().atDay(1))
    val dateTo = MutableStateFlow<LocalDate?>(YearMonth.now().atEndOfMonth())

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<ExpenseEntity>> = combine(
        selectedCategoryId, noteSearch, amountMin, amountMax, sortOrder, dateFrom, dateTo,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ExpenseFilter(
            categoryId = values[0] as Long?,
            noteSearch = (values[1] as String).ifBlank { null },
            amountMinCents = amountStringToCents(values[2] as String),
            amountMaxCents = amountStringToCents(values[3] as String),
            sortOrder = values[4] as SortOrder,
            dateFrom = values[5] as LocalDate?,
            dateTo = values[6] as LocalDate?,
        )
    }.flatMapLatest { filter ->
        expenseRepository.getFiltered(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseRepository.delete(expense)
        }
    }
}
