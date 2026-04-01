package com.example.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.domain.usecase.GenerateRecurringExpensesUseCase
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class AddEditRecurringExpenseViewModel @Inject constructor(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val generateRecurringExpenses: GenerateRecurringExpensesUseCase,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val amount = MutableStateFlow("")
    val categoryId = MutableStateFlow<Long?>(null)
    val startDate = MutableStateFlow(LocalDate.now())
    val interval = MutableStateFlow(Interval.MONTHLY)
    val note = MutableStateFlow("")

    val amountError = MutableStateFlow<String?>(null)
    val categoryError = MutableStateFlow<String?>(null)

    private var editingId: Long? = null

    fun loadRecurringExpense(id: Long) {
        viewModelScope.launch {
            val item = recurringExpenseRepository.getById(id) ?: return@launch
            editingId = item.id
            amount.value = centsToAmountString(item.amountCents)
            categoryId.value = item.categoryId
            startDate.value = LocalDate.parse(item.startDate)
            interval.value = item.interval
            note.value = item.note ?: ""
        }
    }

    fun delete(onComplete: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch {
            val entity = recurringExpenseRepository.getById(id) ?: return@launch
            recurringExpenseRepository.delete(entity)
            onComplete()
        }
    }

    fun save(onComplete: () -> Unit) {
        val cents = amountStringToCents(amount.value)
        val catId = categoryId.value

        amountError.value = if (cents == null) "Enter a valid amount" else null
        categoryError.value = if (catId == null) "Select a category" else null
        if (cents == null || catId == null) return

        viewModelScope.launch {
            val entity = RecurringExpenseEntity(
                id = editingId ?: 0,
                amountCents = cents,
                categoryId = catId,
                interval = interval.value,
                note = note.value.ifBlank { null },
                startDate = startDate.value.toString(),
            )
            if (editingId != null) {
                recurringExpenseRepository.update(entity)
            } else {
                recurringExpenseRepository.insert(entity)
            }
            generateRecurringExpenses(YearMonth.now())
            onComplete()
        }
    }
}
