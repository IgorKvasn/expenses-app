package com.example.expensetracker.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.IncomeFilter
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
class IncomeListViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    val search = MutableStateFlow("")
    val amountMin = MutableStateFlow("")
    val amountMax = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val dateFrom = MutableStateFlow<LocalDate?>(YearMonth.now().atDay(1))
    val dateTo = MutableStateFlow<LocalDate?>(YearMonth.now().atEndOfMonth())

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomeItems: StateFlow<List<IncomeEntity>> = combine(
        search, amountMin, amountMax, sortOrder, dateFrom, dateTo,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        IncomeFilter(
            isRecurring = null,
            search = (values[0] as String).ifBlank { null },
            amountMinCents = amountStringToCents(values[1] as String),
            amountMaxCents = amountStringToCents(values[2] as String),
            sortOrder = values[3] as SortOrder,
            dateFrom = values[4] as LocalDate?,
            dateTo = values[5] as LocalDate?,
        )
    }.flatMapLatest { filter ->
        incomeRepository.getFiltered(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch { incomeRepository.delete(income) }
    }
}
