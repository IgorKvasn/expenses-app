package com.example.expensetracker.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newCategoryName = MutableStateFlow("")

    fun addCategory() {
        val name = newCategoryName.value.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.insert(CategoryEntity(name = name))
            newCategoryName.value = ""
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (category.isDefault) return
        viewModelScope.launch {
            categoryRepository.delete(category)
        }
    }

    fun renameCategory(category: CategoryEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            categoryRepository.update(category.copy(name = newName.trim()))
        }
    }
}
