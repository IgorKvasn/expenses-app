package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.dao.CategoryTotal
import com.example.expensetracker.data.repository.ExpenseRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CategoryReportUseCaseTest {

    private val expenseRepository = mockk<ExpenseRepository>()
    private val useCase = CategoryReportUseCase(expenseRepository)

    @Test
    fun `calculates percentages correctly`() = runTest {
        coEvery {
            expenseRepository.getCategoryTotals(any(), any())
        } returns listOf(
            CategoryTotal(1, "Food", 60000),
            CategoryTotal(2, "Transport", 40000),
        )

        val report = useCase(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
        )

        assertEquals(100000L, report.totalCents)
        assertEquals(2, report.items.size)
        assertEquals(60.0, report.items[0].percentage, 0.01)
        assertEquals(40.0, report.items[1].percentage, 0.01)
    }

    @Test
    fun `returns empty report when no expenses`() = runTest {
        coEvery { expenseRepository.getCategoryTotals(any(), any()) } returns emptyList()

        val report = useCase(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
        )

        assertEquals(0L, report.totalCents)
        assertEquals(0, report.items.size)
    }
}
