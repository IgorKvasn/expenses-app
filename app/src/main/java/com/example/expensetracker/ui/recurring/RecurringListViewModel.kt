package com.example.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val recurringExpenses: StateFlow<List<RecurringExpenseEntity>> =
        recurringExpenseRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleActive(item: RecurringExpenseEntity) {
        viewModelScope.launch {
            recurringExpenseRepository.update(item.copy(isActive = !item.isActive))
        }
    }

    fun delete(item: RecurringExpenseEntity) {
        viewModelScope.launch {
            recurringExpenseRepository.delete(item)
        }
    }
}
