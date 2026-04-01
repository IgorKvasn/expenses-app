package com.example.expensetracker.domain.usecase

import androidx.room.withTransaction
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class GenerateRecurringExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val database: AppDatabase,
) {
    var transactionRunner: suspend (suspend () -> Unit) -> Unit = { block ->
        database.withTransaction { block() }
    }

    suspend operator fun invoke(currentMonth: YearMonth) {
        val activeRecurring = recurringExpenseRepository.getAllSuspend()
        for (recurring in activeRecurring) {
            val startDate = LocalDate.parse(recurring.startDate)
            val startMonth = YearMonth.from(startDate)
            val dayOfMonth = startDate.dayOfMonth
            val monthsToGenerate = generateDueMonths(recurring, startMonth, currentMonth)
            for (month in monthsToGenerate) {
                val monthString = month.toString()
                transactionRunner {
                    if (recurringExpenseRepository.isGeneratedForMonth(recurring.id, monthString)) {
                        return@transactionRunner
                    }
                    val day = minOf(dayOfMonth, month.lengthOfMonth())
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
