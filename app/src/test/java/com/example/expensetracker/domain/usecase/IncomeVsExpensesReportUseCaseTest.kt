package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.IncomeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class IncomeVsExpensesReportUseCaseTest {

    private val expenseRepository = mockk<ExpenseRepository>()
    private val incomeRepository = mockk<IncomeRepository>()
    private val useCase = IncomeVsExpensesReportUseCase(expenseRepository, incomeRepository)

    @Test
    fun `generates monthly buckets for a quarter`() = runTest {
        coEvery { incomeRepository.getTotalInRange(any(), any()) } returns 200000L
        coEvery { expenseRepository.getTotalInRange(any(), any()) } returns 150000L

        val report = useCase(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
        )

        assertEquals(3, report.items.size)
        assertEquals("Jan", report.items[0].label)
        assertEquals("Feb", report.items[1].label)
        assertEquals("Mar", report.items[2].label)
        assertEquals(600000L, report.totalIncomeCents)
        assertEquals(450000L, report.totalExpenseCents)
    }
}
