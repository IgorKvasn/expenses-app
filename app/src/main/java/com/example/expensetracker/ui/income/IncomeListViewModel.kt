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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val selectedMonth = MutableStateFlow<YearMonth?>(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTotal: StateFlow<Long> = selectedMonth.flatMapLatest { month ->
        val filter = IncomeFilter(
            dateFrom = month?.atDay(1),
            dateTo = month?.atEndOfMonth(),
        )
        incomeRepository.getFiltered(filter).map { items -> items.sumOf { it.amountCents } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomeItems: StateFlow<List<IncomeEntity>> = combine(
        search, amountMin, amountMax, sortOrder, selectedMonth,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val month = values[4] as YearMonth?
        IncomeFilter(
            isRecurring = null,
            search = (values[0] as String).ifBlank { null },
            amountMinCents = amountStringToCents(values[1] as String),
            amountMaxCents = amountStringToCents(values[2] as String),
            sortOrder = values[3] as SortOrder,
            dateFrom = month?.atDay(1),
            dateTo = month?.atEndOfMonth(),
        )
    }.flatMapLatest { filter ->
        incomeRepository.getFiltered(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resetMonth() {
        selectedMonth.value = YearMonth.now()
    }

    fun clearFilters() {
        search.value = ""
        amountMin.value = ""
        amountMax.value = ""
        sortOrder.value = SortOrder.DATE_DESC
        selectedMonth.value = YearMonth.now()
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch { incomeRepository.delete(income) }
    }
}
