package com.example.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.domain.usecase.GenerateRecurringIncomeUseCase
import com.example.expensetracker.ui.components.amountStringToCents
import com.example.expensetracker.ui.components.centsToAmountString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class AddEditRecurringIncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val generateRecurringIncome: GenerateRecurringIncomeUseCase,
) : ViewModel() {

    val amount = MutableStateFlow("")
    val source = MutableStateFlow("")
    val recurrenceInterval = MutableStateFlow(Interval.MONTHLY)
    val startDate = MutableStateFlow(LocalDate.now())
    val note = MutableStateFlow("")

    val amountError = MutableStateFlow<String?>(null)
    val sourceError = MutableStateFlow<String?>(null)

    private var editingIncomeId: Long? = null

    fun loadIncome(id: Long) {
        viewModelScope.launch {
            val income = incomeRepository.getById(id) ?: return@launch
            editingIncomeId = income.id
            amount.value = centsToAmountString(income.amountCents)
            source.value = income.source
            recurrenceInterval.value = income.recurrenceInterval ?: Interval.MONTHLY
            if (income.startDate != null) {
                startDate.value = LocalDate.parse(income.startDate)
            }
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
                date = startDate.value,
                note = note.value.ifBlank { null },
                isRecurring = true,
                recurrenceInterval = recurrenceInterval.value,
                startDate = startDate.value.toString(),
            )
            if (editingIncomeId != null) {
                incomeRepository.update(entity)
            } else {
                incomeRepository.insert(entity)
            }
            generateRecurringIncome(YearMonth.now())
            onComplete()
        }
    }
}
