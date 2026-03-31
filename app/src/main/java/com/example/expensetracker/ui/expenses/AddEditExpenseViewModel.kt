package com.example.expensetracker.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val amount = MutableStateFlow("")
    val categoryId = MutableStateFlow<Long?>(null)
    val date = MutableStateFlow(LocalDate.now())
    val note = MutableStateFlow("")

    private var editingExpenseId: Long? = null

    fun loadExpense(id: Long) {
        viewModelScope.launch {
            val expense = expenseRepository.getById(id) ?: return@launch
            editingExpenseId = expense.id
            amount.value = centsToAmountString(expense.amountCents)
            categoryId.value = expense.categoryId
            date.value = expense.date
            note.value = expense.note ?: ""
        }
    }

    fun save(onComplete: () -> Unit) {
        val cents = amountStringToCents(amount.value) ?: return
        val catId = categoryId.value ?: return

        viewModelScope.launch {
            val entity = ExpenseEntity(
                id = editingExpenseId ?: 0,
                amountCents = cents,
                categoryId = catId,
                date = date.value,
                note = note.value.ifBlank { null },
            )
            if (editingExpenseId != null) {
                expenseRepository.update(entity)
            } else {
                expenseRepository.insert(entity)
            }
            onComplete()
        }
    }
}
