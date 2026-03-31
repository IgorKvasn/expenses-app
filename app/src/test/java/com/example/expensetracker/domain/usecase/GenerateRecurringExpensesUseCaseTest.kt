package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import com.example.expensetracker.domain.model.Interval
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GenerateRecurringExpensesUseCaseTest {

    private val expenseRepository = mockk<ExpenseRepository>(relaxed = true)
    private val recurringExpenseRepository = mockk<RecurringExpenseRepository>(relaxed = true)
    private val useCase = GenerateRecurringExpensesUseCase(expenseRepository, recurringExpenseRepository)

    @Test
    fun `generates monthly expense for current month`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1,
            amountCents = 50000,
            categoryId = 1,
            dayOfMonth = 15,
            interval = Interval.MONTHLY,
            isActive = true,
            startMonth = "2026-01",
        )
        coEvery { recurringExpenseRepository.getActive() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        val currentMonth = YearMonth.of(2026, 3)
        useCase(currentMonth)

        val expenseSlot = slot<ExpenseEntity>()
        coVerify(atLeast = 1) { expenseRepository.insert(capture(expenseSlot)) }
        assertEquals(50000L, expenseSlot.captured.amountCents)
        assertEquals(1L, expenseSlot.captured.recurringExpenseId)
    }

    @Test
    fun `skips already generated months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1,
            amountCents = 50000,
            categoryId = 1,
            dayOfMonth = 15,
            interval = Interval.MONTHLY,
            isActive = true,
            startMonth = "2026-01",
        )
        coEvery { recurringExpenseRepository.getActive() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-03") } returns true
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-01") } returns true
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-02") } returns true

        useCase(YearMonth.of(2026, 3))

        coVerify(exactly = 0) { expenseRepository.insert(any()) }
    }

    @Test
    fun `quarterly generates only on correct months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 2,
            amountCents = 100000,
            categoryId = 2,
            dayOfMonth = 1,
            interval = Interval.QUARTERLY,
            isActive = true,
            startMonth = "2026-01",
        )
        coEvery { recurringExpenseRepository.getActive() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 6))

        // Should generate for 2026-01 and 2026-04 (quarterly from Jan)
        coVerify(exactly = 2) { expenseRepository.insert(any()) }
    }

    @Test
    fun `clamps day of month for short months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 3,
            amountCents = 30000,
            categoryId = 1,
            dayOfMonth = 31,
            interval = Interval.MONTHLY,
            isActive = true,
            startMonth = "2026-02",
        )
        coEvery { recurringExpenseRepository.getActive() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 2))

        val expenseSlot = slot<ExpenseEntity>()
        coVerify { expenseRepository.insert(capture(expenseSlot)) }
        assertEquals(LocalDate.of(2026, 2, 28), expenseSlot.captured.date)
    }
}
