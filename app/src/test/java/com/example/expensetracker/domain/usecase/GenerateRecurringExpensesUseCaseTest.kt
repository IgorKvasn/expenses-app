package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.AppDatabase
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
    private val database = mockk<AppDatabase>(relaxed = true)
    private val useCase = GenerateRecurringExpensesUseCase(expenseRepository, recurringExpenseRepository, database).apply {
        transactionRunner = { block -> block() }
    }

    @Test
    fun `generates monthly expense for current month`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1,
            amountCents = 50000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-01-15",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        val currentMonth = YearMonth.of(2026, 3)
        useCase(currentMonth)

        val capturedExpenses = mutableListOf<ExpenseEntity>()
        coVerify(atLeast = 1) { expenseRepository.insert(capture(capturedExpenses)) }
        assert(capturedExpenses.all { it.amountCents == 50000L })
        assert(capturedExpenses.all { it.recurringExpenseId == 1L })
    }

    @Test
    fun `skips already generated months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1,
            amountCents = 50000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-01-15",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-01") } returns true
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-02") } returns true
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-03") } returns true

        useCase(YearMonth.of(2026, 3))

        coVerify(exactly = 0) { expenseRepository.insert(any()) }
    }

    @Test
    fun `quarterly generates only on correct months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 2,
            amountCents = 100000,
            categoryId = 2,
            interval = Interval.QUARTERLY,
            startDate = "2026-01-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
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
            interval = Interval.MONTHLY,
            startDate = "2026-01-31",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 2))

        val capturedExpenses = mutableListOf<ExpenseEntity>()
        coVerify(atLeast = 1) { expenseRepository.insert(capture(capturedExpenses)) }
        // Jan 31 should generate as-is, Feb should clamp to 28
        val febExpense = capturedExpenses.find { it.date.monthValue == 2 }!!
        assertEquals(LocalDate.of(2026, 2, 28), febExpense.date)
    }

    @Test
    fun `generates correct dates for monthly expenses`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1,
            amountCents = 10000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-03-10",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 5))

        val capturedExpenses = mutableListOf<ExpenseEntity>()
        coVerify(exactly = 3) { expenseRepository.insert(capture(capturedExpenses)) }
        assertEquals(LocalDate.of(2026, 3, 10), capturedExpenses[0].date)
        assertEquals(LocalDate.of(2026, 4, 10), capturedExpenses[1].date)
        assertEquals(LocalDate.of(2026, 5, 10), capturedExpenses[2].date)
    }

    @Test
    fun `half-yearly generates every 6 months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 4,
            amountCents = 200000,
            categoryId = 1,
            interval = Interval.HALF_YEARLY,
            startDate = "2026-01-15",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 12))

        val capturedExpenses = mutableListOf<ExpenseEntity>()
        coVerify(exactly = 2) { expenseRepository.insert(capture(capturedExpenses)) }
        assertEquals(LocalDate.of(2026, 1, 15), capturedExpenses[0].date)
        assertEquals(LocalDate.of(2026, 7, 15), capturedExpenses[1].date)
    }

    @Test
    fun `does not generate before start date month`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 5,
            amountCents = 10000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-06-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 4))

        coVerify(exactly = 0) { expenseRepository.insert(any()) }
    }

    @Test
    fun `records generation in RecurringExpenseGeneration table`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1,
            amountCents = 50000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-03-15",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 42L

        useCase(YearMonth.of(2026, 3))

        val generationSlot = slot<RecurringExpenseGenerationEntity>()
        coVerify { recurringExpenseRepository.recordGeneration(capture(generationSlot)) }
        assertEquals(1L, generationSlot.captured.recurringExpenseId)
        assertEquals("2026-03", generationSlot.captured.generatedForMonth)
        assertEquals(42L, generationSlot.captured.expenseId)
    }

    @Test
    fun `handles multiple recurring expenses`() = runTest {
        val recurring1 = RecurringExpenseEntity(
            id = 1,
            amountCents = 10000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-03-01",
        )
        val recurring2 = RecurringExpenseEntity(
            id = 2,
            amountCents = 20000,
            categoryId = 2,
            interval = Interval.QUARTERLY,
            startDate = "2026-03-15",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring1, recurring2)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 3))

        // Both should generate for March
        coVerify(exactly = 2) { expenseRepository.insert(any()) }
    }

    @Test
    fun `quarterly with February start generates on correct months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 6,
            amountCents = 50000,
            categoryId = 1,
            interval = Interval.QUARTERLY,
            startDate = "2026-02-28",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 8))

        val capturedExpenses = mutableListOf<ExpenseEntity>()
        // Feb, May, Aug = 3 quarters
        coVerify(exactly = 3) { expenseRepository.insert(capture(capturedExpenses)) }
        assertEquals(LocalDate.of(2026, 2, 28), capturedExpenses[0].date)
        assertEquals(LocalDate.of(2026, 5, 28), capturedExpenses[1].date)
        assertEquals(LocalDate.of(2026, 8, 28), capturedExpenses[2].date)
    }

    @Test
    fun `no recurring expenses does nothing`() = runTest {
        coEvery { recurringExpenseRepository.getAllSuspend() } returns emptyList()

        useCase(YearMonth.of(2026, 3))

        coVerify(exactly = 0) { expenseRepository.insert(any()) }
    }

    @Test
    fun `preserves note and category from template`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1,
            amountCents = 15000,
            categoryId = 7,
            interval = Interval.MONTHLY,
            note = "Netflix subscription",
            startDate = "2026-04-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 4))

        val expenseSlot = slot<ExpenseEntity>()
        coVerify { expenseRepository.insert(capture(expenseSlot)) }
        assertEquals(15000L, expenseSlot.captured.amountCents)
        assertEquals(7L, expenseSlot.captured.categoryId)
        assertEquals("Netflix subscription", expenseSlot.captured.note)
        assertEquals(1L, expenseSlot.captured.recurringExpenseId)
    }
}
