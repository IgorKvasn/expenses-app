package com.example.expensetracker.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
    val isRecurring = MutableStateFlow(false)
    val recurrenceInterval = MutableStateFlow(Interval.MONTHLY)

    private var editingIncomeId: Long? = null

    fun loadIncome(id: Long) {
        viewModelScope.launch {
            val income = incomeRepository.getById(id) ?: return@launch
            editingIncomeId = income.id
            amount.value = centsToAmountString(income.amountCents)
            source.value = income.source
            date.value = income.date
            note.value = income.note ?: ""
            isRecurring.value = income.isRecurring
            recurrenceInterval.value = income.recurrenceInterval ?: Interval.MONTHLY
        }
    }

    fun save(onComplete: () -> Unit) {
        val cents = amountStringToCents(amount.value) ?: return
        if (source.value.isBlank()) return

        viewModelScope.launch {
            val entity = IncomeEntity(
                id = editingIncomeId ?: 0,
                amountCents = cents,
                source = source.value,
                date = date.value,
                note = note.value.ifBlank { null },
                isRecurring = isRecurring.value,
                recurrenceInterval = if (isRecurring.value) recurrenceInterval.value else null,
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
