package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringIncomeGenerationEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.Interval
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GenerateRecurringIncomeUseCaseTest {

    private val incomeRepository = mockk<IncomeRepository>(relaxed = true)
    private val database = mockk<AppDatabase>(relaxed = true)
    private val useCase = GenerateRecurringIncomeUseCase(incomeRepository, database).apply {
        transactionRunner = { block -> block() }
    }

    private fun recurringTemplate(
        id: Long = 1,
        amountCents: Long = 300000,
        source: String = "Salary",
        interval: Interval = Interval.MONTHLY,
        startDate: String = "2026-01-01",
        note: String? = null,
    ) = IncomeEntity(
        id = id,
        amountCents = amountCents,
        source = source,
        date = LocalDate.parse(startDate),
        isRecurring = true,
        recurrenceInterval = interval,
        startDate = startDate,
        note = note,
    )

    @Test
    fun `generates monthly income for current month`() = runTest {
        val template = recurringTemplate(startDate = "2026-01-15")
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)
        coEvery { incomeRepository.isGeneratedForMonth(1, any()) } returns false
        coEvery { incomeRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 3))

        val capturedIncome = mutableListOf<IncomeEntity>()
        coVerify(exactly = 3) { incomeRepository.insert(capture(capturedIncome)) }
        assertEquals(LocalDate.of(2026, 1, 15), capturedIncome[0].date)
        assertEquals(LocalDate.of(2026, 2, 15), capturedIncome[1].date)
        assertEquals(LocalDate.of(2026, 3, 15), capturedIncome[2].date)
    }

    @Test
    fun `generated income is not recurring`() = runTest {
        val template = recurringTemplate(startDate = "2026-03-01")
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)
        coEvery { incomeRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { incomeRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 3))

        val incomeSlot = slot<IncomeEntity>()
        coVerify { incomeRepository.insert(capture(incomeSlot)) }
        assertFalse(incomeSlot.captured.isRecurring)
        assertNull(incomeSlot.captured.recurrenceInterval)
        assertEquals(1L, incomeSlot.captured.recurringIncomeId)
    }

    @Test
    fun `skips already generated months`() = runTest {
        val template = recurringTemplate(startDate = "2026-01-01")
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)
        coEvery { incomeRepository.isGeneratedForMonth(1, "2026-01") } returns true
        coEvery { incomeRepository.isGeneratedForMonth(1, "2026-02") } returns true
        coEvery { incomeRepository.isGeneratedForMonth(1, "2026-03") } returns true

        useCase(YearMonth.of(2026, 3))

        coVerify(exactly = 0) { incomeRepository.insert(any()) }
    }

    @Test
    fun `quarterly generates only on correct months`() = runTest {
        val template = recurringTemplate(
            id = 2,
            interval = Interval.QUARTERLY,
            startDate = "2026-01-01",
        )
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)
        coEvery { incomeRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { incomeRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 6))

        // Jan and Apr (quarterly from Jan, up to and including Jun — next would be Jul)
        coVerify(exactly = 2) { incomeRepository.insert(any()) }
    }

    @Test
    fun `half-yearly generates every 6 months`() = runTest {
        val template = recurringTemplate(
            id = 3,
            interval = Interval.HALF_YEARLY,
            startDate = "2026-01-15",
        )
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)
        coEvery { incomeRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { incomeRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 12))

        val capturedIncome = mutableListOf<IncomeEntity>()
        coVerify(exactly = 2) { incomeRepository.insert(capture(capturedIncome)) }
        assertEquals(LocalDate.of(2026, 1, 15), capturedIncome[0].date)
        assertEquals(LocalDate.of(2026, 7, 15), capturedIncome[1].date)
    }

    @Test
    fun `clamps day of month for short months`() = runTest {
        val template = recurringTemplate(startDate = "2026-01-31")
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)
        coEvery { incomeRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { incomeRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 2))

        val capturedIncome = mutableListOf<IncomeEntity>()
        coVerify(atLeast = 1) { incomeRepository.insert(capture(capturedIncome)) }
        val febIncome = capturedIncome.find { it.date.monthValue == 2 }!!
        assertEquals(LocalDate.of(2026, 2, 28), febIncome.date)
    }

    @Test
    fun `does not generate before start date month`() = runTest {
        val template = recurringTemplate(startDate = "2026-06-01")
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)

        useCase(YearMonth.of(2026, 4))

        coVerify(exactly = 0) { incomeRepository.insert(any()) }
    }

    @Test
    fun `records generation tracking entry`() = runTest {
        val template = recurringTemplate(startDate = "2026-03-15")
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)
        coEvery { incomeRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { incomeRepository.insert(any()) } returns 42L

        useCase(YearMonth.of(2026, 3))

        val generationSlot = slot<RecurringIncomeGenerationEntity>()
        coVerify { incomeRepository.recordGeneration(capture(generationSlot)) }
        assertEquals(1L, generationSlot.captured.recurringIncomeId)
        assertEquals("2026-03", generationSlot.captured.generatedForMonth)
        assertEquals(42L, generationSlot.captured.incomeId)
    }

    @Test
    fun `preserves source and note from template`() = runTest {
        val template = recurringTemplate(
            source = "Freelance",
            note = "Monthly retainer",
            startDate = "2026-04-01",
        )
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)
        coEvery { incomeRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { incomeRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 4))

        val incomeSlot = slot<IncomeEntity>()
        coVerify { incomeRepository.insert(capture(incomeSlot)) }
        assertEquals("Freelance", incomeSlot.captured.source)
        assertEquals("Monthly retainer", incomeSlot.captured.note)
        assertEquals(300000L, incomeSlot.captured.amountCents)
    }

    @Test
    fun `skips template with null startDate`() = runTest {
        val template = IncomeEntity(
            id = 1,
            amountCents = 10000,
            source = "Test",
            date = LocalDate.of(2026, 1, 1),
            isRecurring = true,
            recurrenceInterval = Interval.MONTHLY,
            startDate = null,
        )
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)

        useCase(YearMonth.of(2026, 3))

        coVerify(exactly = 0) { incomeRepository.insert(any()) }
    }

    @Test
    fun `skips template with null recurrenceInterval`() = runTest {
        val template = IncomeEntity(
            id = 1,
            amountCents = 10000,
            source = "Test",
            date = LocalDate.of(2026, 1, 1),
            isRecurring = true,
            recurrenceInterval = null,
            startDate = "2026-01-01",
        )
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)

        useCase(YearMonth.of(2026, 3))

        coVerify(exactly = 0) { incomeRepository.insert(any()) }
    }

    @Test
    fun `no recurring income does nothing`() = runTest {
        coEvery { incomeRepository.getAllRecurringSuspend() } returns emptyList()

        useCase(YearMonth.of(2026, 3))

        coVerify(exactly = 0) { incomeRepository.insert(any()) }
    }

    @Test
    fun `handles multiple recurring income templates`() = runTest {
        val salary = recurringTemplate(id = 1, source = "Salary", startDate = "2026-03-01")
        val freelance = recurringTemplate(id = 2, source = "Freelance", startDate = "2026-03-15")
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(salary, freelance)
        coEvery { incomeRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { incomeRepository.insert(any()) } returns 100L

        useCase(YearMonth.of(2026, 3))

        coVerify(exactly = 2) { incomeRepository.insert(any()) }
    }
}
