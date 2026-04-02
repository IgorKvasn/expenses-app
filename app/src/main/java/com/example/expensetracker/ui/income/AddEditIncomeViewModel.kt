package com.example.expensetracker.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddEditIncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    val amount = MutableStateFlow("")
    val source = MutableStateFlow("")
    val date = MutableStateFlow(LocalDate.now())
    val note = MutableStateFlow("")

    val amountError = MutableStateFlow<String?>(null)
    val sourceError = MutableStateFlow<String?>(null)

    private var editingIncomeId: Long? = null
    private var editingCreatedAt: Instant? = null

    fun loadIncome(id: Long) {
        viewModelScope.launch {
            val income = incomeRepository.getById(id) ?: return@launch
            editingIncomeId = income.id
            editingCreatedAt = income.createdAt
            amount.value = centsToAmountString(income.amountCents)
            source.value = income.source
            date.value = income.date
            note.value = income.note ?: ""
        }
    }

    fun delete(onComplete: () -> Unit) {
        val id = editingIncomeId ?: return
        viewModelScope.launch {
            val entity = incomeRepository.getById(id) ?: return@launch
            incomeRepository.delete(entity)
            onComplete()
        }
    }

    fun save(onComplete: () -> Unit) {
        val cents = amountStringToCents(amount.value)
        val sourceBlank = source.value.isBlank()

        amountError.value = if (cents == null) "Enter a valid amount" else null
        sourceError.value = if (sourceBlank) "Enter a source" else null
        if (cents == null || sourceBlank) return

        viewModelScope.launch {
            val entity = IncomeEntity(
                id = editingIncomeId ?: 0,
                amountCents = cents,
                source = source.value,
                date = date.value,
                note = note.value.ifBlank { null },
                isRecurring = false,
                recurrenceInterval = null,
                createdAt = editingCreatedAt ?: Instant.now(),
            )
            if (editingIncomeId != null) {
                incomeRepository.update(entity)
            } else {
                incomeRepository.insert(entity)
            }
            onComplete()
        }
    }
}
