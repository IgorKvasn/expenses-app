package com.example.expensetracker.domain.usecase

import androidx.room.withTransaction
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringIncomeGenerationEntity
import com.example.expensetracker.data.repository.IncomeRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class GenerateRecurringIncomeUseCase @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val database: AppDatabase,
) {
    var transactionRunner: suspend (suspend () -> Unit) -> Unit = { block ->
        database.withTransaction { block() }
    }

    suspend operator fun invoke(currentMonth: YearMonth) {
        val recurringItems = incomeRepository.getAllRecurringSuspend()
        for (template in recurringItems) {
            val startDateStr = template.startDate ?: continue
            val interval = template.recurrenceInterval ?: continue
            val startDate = LocalDate.parse(startDateStr)
            val startMonth = YearMonth.from(startDate)
            val dayOfMonth = startDate.dayOfMonth

            var month = startMonth
            while (!month.isAfter(currentMonth)) {
                val monthString = month.toString()
                transactionRunner {
                    if (!incomeRepository.isGeneratedForMonth(template.id, monthString)) {
                        val day = minOf(dayOfMonth, month.lengthOfMonth())
                        val date = month.atDay(day)
                        val incomeId = incomeRepository.insert(
                            IncomeEntity(
                                amountCents = template.amountCents,
                                source = template.source,
                                date = date,
                                note = template.note,
                                isRecurring = false,
                                recurringIncomeId = template.id,
                            ),
                        )
                        incomeRepository.recordGeneration(
                            RecurringIncomeGenerationEntity(
                                recurringIncomeId = template.id,
                                generatedForMonth = monthString,
                                incomeId = incomeId,
                            ),
                        )
                    }
                }
                month = month.plusMonths(interval.months.toLong())
            }
        }
    }
}
