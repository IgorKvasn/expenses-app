package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import java.time.YearMonth
import javax.inject.Inject

class GenerateRecurringExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
) {
    suspend operator fun invoke(currentMonth: YearMonth) {
        val activeRecurring = recurringExpenseRepository.getActive()
        for (recurring in activeRecurring) {
            val startMonth = YearMonth.parse(recurring.startMonth)
            val monthsToGenerate = generateDueMonths(recurring, startMonth, currentMonth)
            for (month in monthsToGenerate) {
                val monthString = month.toString()
                if (recurringExpenseRepository.isGeneratedForMonth(recurring.id, monthString)) {
                    continue
                }
                val day = minOf(recurring.dayOfMonth, month.lengthOfMonth())
                val date = month.atDay(day)
                val expenseId = expenseRepository.insert(
                    ExpenseEntity(
                        amountCents = recurring.amountCents,
                        categoryId = recurring.categoryId,
                        date = date,
                        note = recurring.note,
                        recurringExpenseId = recurring.id,
                    )
                )
                recurringExpenseRepository.recordGeneration(
                    RecurringExpenseGenerationEntity(
                        recurringExpenseId = recurring.id,
                        generatedForMonth = monthString,
                        expenseId = expenseId,
                    )
                )
            }
        }
    }

    private fun generateDueMonths(
        recurring: RecurringExpenseEntity,
        startMonth: YearMonth,
        currentMonth: YearMonth,
    ): List<YearMonth> {
        val months = mutableListOf<YearMonth>()
        var month = startMonth
        while (!month.isAfter(currentMonth)) {
            months.add(month)
            month = month.plusMonths(recurring.interval.months.toLong())
        }
        return months
    }
}
