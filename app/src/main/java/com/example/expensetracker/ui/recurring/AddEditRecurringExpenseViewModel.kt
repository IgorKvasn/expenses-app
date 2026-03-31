package com.example.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class AddEditRecurringExpenseViewModel @Inject constructor(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val amount = MutableStateFlow("")
    val categoryId = MutableStateFlow<Long?>(null)
    val dayOfMonth = MutableStateFlow("1")
    val interval = MutableStateFlow(Interval.MONTHLY)
    val note = MutableStateFlow("")

    private var editingId: Long? = null

    fun loadRecurringExpense(id: Long) {
        viewModelScope.launch {
            val item = recurringExpenseRepository.getById(id) ?: return@launch
            editingId = item.id
            amount.value = centsToAmountString(item.amountCents)
            categoryId.value = item.categoryId
            dayOfMonth.value = item.dayOfMonth.toString()
            interval.value = item.interval
            note.value = item.note ?: ""
        }
    }

    fun save(onComplete: () -> Unit) {
        val cents = amountStringToCents(amount.value) ?: return
        val catId = categoryId.value ?: return
        val day = dayOfMonth.value.toIntOrNull()?.coerceIn(1, 31) ?: return

        viewModelScope.launch {
            val entity = RecurringExpenseEntity(
                id = editingId ?: 0,
                amountCents = cents,
                categoryId = catId,
                dayOfMonth = day,
                interval = interval.value,
                note = note.value.ifBlank { null },
                isActive = true,
                startMonth = YearMonth.now().toString(),
            )
            if (editingId != null) {
                recurringExpenseRepository.update(entity)
            } else {
                recurringExpenseRepository.insert(entity)
            }
            onComplete()
        }
    }
}
