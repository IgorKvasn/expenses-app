package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringIncomeGenerationEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.Interval
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Month-by-month simulation tests for recurring income.
 * These tests simulate the app being opened each month and verify that
 * the correct income entries are generated cumulatively.
 */
class RecurringIncomeMonthByMonthTest {

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

    private fun setupStatefulMocks(
        templates: List<IncomeEntity>,
    ): MutableList<IncomeEntity> {
        coEvery { incomeRepository.getAllRecurringSuspend() } returns templates

        val generatedMonths = mutableMapOf<Long, MutableSet<String>>()
        coEvery { incomeRepository.isGeneratedForMonth(any(), any()) } answers {
            generatedMonths.getOrDefault(firstArg(), mutableSetOf()).contains(secondArg())
        }
        coEvery { incomeRepository.recordGeneration(any()) } answers {
            val gen = firstArg<RecurringIncomeGenerationEntity>()
            generatedMonths.getOrPut(gen.recurringIncomeId) { mutableSetOf() }
                .add(gen.generatedForMonth)
            1L
        }

        val allIncome = mutableListOf<IncomeEntity>()
        coEvery { incomeRepository.insert(any()) } answers {
            allIncome.add(firstArg())
            allIncome.size.toLong()
        }
        return allIncome
    }

    // --- Monthly: month-by-month simulation ---

    @Test
    fun `monthly income - generate month by month for full year`() = runTest {
        val template = recurringTemplate(startDate = "2026-01-15")
        val allIncome = setupStatefulMocks(listOf(template))

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        assertEquals(12, allIncome.size)
        for (month in 1..12) {
            val income = allIncome[month - 1]
            assertEquals(LocalDate.of(2026, month, 15), income.date)
            assertEquals(300000L, income.amountCents)
            assertEquals("Salary", income.source)
            assertFalse(income.isRecurring)
            assertEquals(1L, income.recurringIncomeId)
        }
    }

    @Test
    fun `monthly income - skipping months still catches up`() = runTest {
        val template = recurringTemplate(startDate = "2026-01-01")
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 1))
        assertEquals(1, allIncome.size)

        useCase(YearMonth.of(2026, 6))
        assertEquals(6, allIncome.size)
        assertEquals(LocalDate.of(2026, 6, 1), allIncome[5].date)
    }

    @Test
    fun `monthly income - calling twice in same month does not duplicate`() = runTest {
        val template = recurringTemplate(startDate = "2026-03-01")
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 3))
        useCase(YearMonth.of(2026, 3))
        useCase(YearMonth.of(2026, 3))

        assertEquals(1, allIncome.size)
    }

    // --- Quarterly: month-by-month simulation ---

    @Test
    fun `quarterly income - generate month by month for full year`() = runTest {
        val template = recurringTemplate(
            interval = Interval.QUARTERLY, startDate = "2026-01-10",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        assertEquals(4, allIncome.size)
        assertEquals(LocalDate.of(2026, 1, 10), allIncome[0].date)
        assertEquals(LocalDate.of(2026, 4, 10), allIncome[1].date)
        assertEquals(LocalDate.of(2026, 7, 10), allIncome[2].date)
        assertEquals(LocalDate.of(2026, 10, 10), allIncome[3].date)
    }

    @Test
    fun `quarterly income - skipping months catches up correctly`() = runTest {
        val template = recurringTemplate(
            interval = Interval.QUARTERLY, startDate = "2026-02-01",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 2))
        assertEquals(1, allIncome.size)

        useCase(YearMonth.of(2026, 11))
        assertEquals(4, allIncome.size)
        assertEquals(LocalDate.of(2026, 2, 1), allIncome[0].date)
        assertEquals(LocalDate.of(2026, 5, 1), allIncome[1].date)
        assertEquals(LocalDate.of(2026, 8, 1), allIncome[2].date)
        assertEquals(LocalDate.of(2026, 11, 1), allIncome[3].date)
    }

    @Test
    fun `quarterly income - non-quarter months produce no new income`() = runTest {
        val template = recurringTemplate(
            interval = Interval.QUARTERLY, startDate = "2026-01-01",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 1))
        assertEquals(1, allIncome.size)

        useCase(YearMonth.of(2026, 2))
        assertEquals(1, allIncome.size)

        useCase(YearMonth.of(2026, 3))
        assertEquals(1, allIncome.size)

        useCase(YearMonth.of(2026, 4))
        assertEquals(2, allIncome.size)
    }

    // --- Half-yearly: month-by-month simulation ---

    @Test
    fun `half-yearly income - generate month by month for full year`() = runTest {
        val template = recurringTemplate(
            interval = Interval.HALF_YEARLY, startDate = "2026-03-20",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        assertEquals(2, allIncome.size)
        assertEquals(LocalDate.of(2026, 3, 20), allIncome[0].date)
        assertEquals(LocalDate.of(2026, 9, 20), allIncome[1].date)
    }

    @Test
    fun `half-yearly income - spans across years`() = runTest {
        val template = recurringTemplate(
            interval = Interval.HALF_YEARLY, startDate = "2025-10-01",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 4))

        assertEquals(2, allIncome.size)
        assertEquals(LocalDate.of(2025, 10, 1), allIncome[0].date)
        assertEquals(LocalDate.of(2026, 4, 1), allIncome[1].date)
    }

    // --- Day-of-month clamping ---

    @Test
    fun `monthly income on 31st - clamps correctly through all months`() = runTest {
        val template = recurringTemplate(startDate = "2026-01-31")
        val allIncome = setupStatefulMocks(listOf(template))

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        assertEquals(12, allIncome.size)
        assertEquals(31, allIncome[0].date.dayOfMonth)  // Jan
        assertEquals(28, allIncome[1].date.dayOfMonth)  // Feb (non-leap)
        assertEquals(31, allIncome[2].date.dayOfMonth)  // Mar
        assertEquals(30, allIncome[3].date.dayOfMonth)  // Apr
        assertEquals(31, allIncome[4].date.dayOfMonth)  // May
        assertEquals(30, allIncome[5].date.dayOfMonth)  // Jun
        assertEquals(31, allIncome[6].date.dayOfMonth)  // Jul
        assertEquals(31, allIncome[7].date.dayOfMonth)  // Aug
        assertEquals(30, allIncome[8].date.dayOfMonth)  // Sep
        assertEquals(31, allIncome[9].date.dayOfMonth)  // Oct
        assertEquals(30, allIncome[10].date.dayOfMonth) // Nov
        assertEquals(31, allIncome[11].date.dayOfMonth) // Dec
    }

    @Test
    fun `monthly income on 30th - clamps only for February`() = runTest {
        val template = recurringTemplate(startDate = "2026-01-30")
        val allIncome = setupStatefulMocks(listOf(template))

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        assertEquals(12, allIncome.size)
        assertEquals(30, allIncome[0].date.dayOfMonth)  // Jan
        assertEquals(28, allIncome[1].date.dayOfMonth)  // Feb (clamped)
        for (i in 2..11) {
            assertEquals(30, allIncome[i].date.dayOfMonth) // All other months have >= 30 days
        }
    }

    @Test
    fun `monthly income on 29th - February in leap year has 29`() = runTest {
        val template = recurringTemplate(startDate = "2028-01-29")
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2028, 2))

        assertEquals(2, allIncome.size)
        assertEquals(LocalDate.of(2028, 1, 29), allIncome[0].date)
        assertEquals(LocalDate.of(2028, 2, 29), allIncome[1].date)
    }

    @Test
    fun `monthly income on 29th - February in non-leap year clamps to 28`() = runTest {
        val template = recurringTemplate(startDate = "2026-01-29")
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 2))

        assertEquals(2, allIncome.size)
        assertEquals(LocalDate.of(2026, 2, 28), allIncome[1].date)
    }

    // --- Multiple recurring incomes, month-by-month ---

    @Test
    fun `multiple incomes with different intervals - month by month`() = runTest {
        val salary = recurringTemplate(
            id = 1, source = "Salary", amountCents = 500000,
            interval = Interval.MONTHLY, startDate = "2026-01-01",
        )
        val bonus = recurringTemplate(
            id = 2, source = "Bonus", amountCents = 200000,
            interval = Interval.QUARTERLY, startDate = "2026-01-15",
        )
        val dividends = recurringTemplate(
            id = 3, source = "Dividends", amountCents = 100000,
            interval = Interval.HALF_YEARLY, startDate = "2026-01-20",
        )
        val allIncome = setupStatefulMocks(listOf(salary, bonus, dividends))

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        val salaryIncome = allIncome.filter { it.recurringIncomeId == 1L }
        val bonusIncome = allIncome.filter { it.recurringIncomeId == 2L }
        val dividendIncome = allIncome.filter { it.recurringIncomeId == 3L }

        assertEquals(12, salaryIncome.size)
        assertEquals(4, bonusIncome.size)
        assertEquals(2, dividendIncome.size)

        // Verify bonus months
        assertEquals(1, bonusIncome[0].date.monthValue)
        assertEquals(4, bonusIncome[1].date.monthValue)
        assertEquals(7, bonusIncome[2].date.monthValue)
        assertEquals(10, bonusIncome[3].date.monthValue)

        // Verify dividend months
        assertEquals(1, dividendIncome[0].date.monthValue)
        assertEquals(7, dividendIncome[1].date.monthValue)
    }

    @Test
    fun `multiple incomes with different start dates - staggered generation`() = runTest {
        val salary = recurringTemplate(id = 1, source = "Salary", startDate = "2026-01-01")
        val freelance = recurringTemplate(id = 2, source = "Freelance", startDate = "2026-04-01")
        val allIncome = setupStatefulMocks(listOf(salary, freelance))

        for (month in 1..6) {
            useCase(YearMonth.of(2026, month))
        }

        val salaryIncome = allIncome.filter { it.recurringIncomeId == 1L }
        val freelanceIncome = allIncome.filter { it.recurringIncomeId == 2L }

        assertEquals(6, salaryIncome.size) // Jan-Jun
        assertEquals(3, freelanceIncome.size) // Apr-Jun
    }

    // --- Year boundary tests ---

    @Test
    fun `monthly income spanning year boundary`() = runTest {
        val template = recurringTemplate(startDate = "2025-11-01")
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2025, 11))
        useCase(YearMonth.of(2025, 12))
        useCase(YearMonth.of(2026, 1))
        useCase(YearMonth.of(2026, 2))

        assertEquals(4, allIncome.size)
        assertEquals(LocalDate.of(2025, 11, 1), allIncome[0].date)
        assertEquals(LocalDate.of(2025, 12, 1), allIncome[1].date)
        assertEquals(LocalDate.of(2026, 1, 1), allIncome[2].date)
        assertEquals(LocalDate.of(2026, 2, 1), allIncome[3].date)
    }

    @Test
    fun `quarterly income spanning year boundary`() = runTest {
        val template = recurringTemplate(
            interval = Interval.QUARTERLY, startDate = "2025-10-01",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 7))

        assertEquals(4, allIncome.size)
        assertEquals(LocalDate.of(2025, 10, 1), allIncome[0].date)
        assertEquals(LocalDate.of(2026, 1, 1), allIncome[1].date)
        assertEquals(LocalDate.of(2026, 4, 1), allIncome[2].date)
        assertEquals(LocalDate.of(2026, 7, 1), allIncome[3].date)
    }

    // --- Generated income properties ---

    @Test
    fun `generated income is always non-recurring with no interval`() = runTest {
        val template = recurringTemplate(
            interval = Interval.QUARTERLY, startDate = "2026-01-01",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        allIncome.forEach { income ->
            assertFalse(income.isRecurring)
            assertNull(income.recurrenceInterval)
            assertNull(income.startDate)
            assertEquals(1L, income.recurringIncomeId)
        }
    }

    @Test
    fun `generated income preserves source and note`() = runTest {
        val template = recurringTemplate(
            source = "Freelance Consulting",
            note = "Retainer fee",
            startDate = "2026-01-01",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 3))

        assertEquals(3, allIncome.size)
        allIncome.forEach { income ->
            assertEquals("Freelance Consulting", income.source)
            assertEquals("Retainer fee", income.note)
            assertEquals(300000L, income.amountCents)
        }
    }

    @Test
    fun `generated income preserves null note`() = runTest {
        val template = recurringTemplate(
            source = "Salary", note = null, startDate = "2026-01-01",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 3))

        allIncome.forEach { assertNull(it.note) }
    }

    // --- Generation tracking records ---

    @Test
    fun `generation records have correct month strings`() = runTest {
        val template = recurringTemplate(
            id = 5, interval = Interval.QUARTERLY, startDate = "2026-01-01",
        )
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)
        coEvery { incomeRepository.isGeneratedForMonth(any(), any()) } returns false

        var insertCounter = 0L
        coEvery { incomeRepository.insert(any()) } answers { ++insertCounter }

        val generations = mutableListOf<RecurringIncomeGenerationEntity>()
        coEvery { incomeRepository.recordGeneration(any()) } answers {
            generations.add(firstArg())
            1L
        }

        useCase(YearMonth.of(2026, 7))

        assertEquals(3, generations.size)
        assertEquals("2026-01", generations[0].generatedForMonth)
        assertEquals("2026-04", generations[1].generatedForMonth)
        assertEquals("2026-07", generations[2].generatedForMonth)

        generations.forEach { assertEquals(5L, it.recurringIncomeId) }
        assertEquals(1L, generations[0].incomeId)
        assertEquals(2L, generations[1].incomeId)
        assertEquals(3L, generations[2].incomeId)
    }

    // --- Partial generation ---

    @Test
    fun `only generates missing months when some already exist`() = runTest {
        val template = recurringTemplate(startDate = "2026-01-01")
        coEvery { incomeRepository.getAllRecurringSuspend() } returns listOf(template)

        coEvery { incomeRepository.isGeneratedForMonth(1, "2026-01") } returns true
        coEvery { incomeRepository.isGeneratedForMonth(1, "2026-02") } returns true
        coEvery { incomeRepository.isGeneratedForMonth(1, "2026-03") } returns false
        coEvery { incomeRepository.isGeneratedForMonth(1, "2026-04") } returns false
        coEvery { incomeRepository.isGeneratedForMonth(1, "2026-05") } returns false
        coEvery { incomeRepository.isGeneratedForMonth(1, "2026-06") } returns false
        coEvery { incomeRepository.recordGeneration(any()) } returns 1L

        val allIncome = mutableListOf<IncomeEntity>()
        coEvery { incomeRepository.insert(any()) } answers {
            allIncome.add(firstArg())
            allIncome.size.toLong()
        }

        useCase(YearMonth.of(2026, 6))

        assertEquals(4, allIncome.size)
        assertEquals(LocalDate.of(2026, 3, 1), allIncome[0].date)
        assertEquals(LocalDate.of(2026, 4, 1), allIncome[1].date)
        assertEquals(LocalDate.of(2026, 5, 1), allIncome[2].date)
        assertEquals(LocalDate.of(2026, 6, 1), allIncome[3].date)
    }

    // --- Edge cases for null fields ---

    @Test
    fun `template with null startDate and null interval are both skipped`() = runTest {
        val noStartDate = IncomeEntity(
            id = 1, amountCents = 10000, source = "A",
            date = LocalDate.of(2026, 1, 1), isRecurring = true,
            recurrenceInterval = Interval.MONTHLY, startDate = null,
        )
        val noInterval = IncomeEntity(
            id = 2, amountCents = 20000, source = "B",
            date = LocalDate.of(2026, 1, 1), isRecurring = true,
            recurrenceInterval = null, startDate = "2026-01-01",
        )
        val valid = recurringTemplate(id = 3, source = "Valid", startDate = "2026-01-01")

        val allIncome = setupStatefulMocks(listOf(noStartDate, noInterval, valid))

        useCase(YearMonth.of(2026, 3))

        // Only the valid template should generate income
        assertEquals(3, allIncome.size)
        allIncome.forEach { assertEquals("Valid", it.source) }
    }

    // --- Long backfill ---

    @Test
    fun `backfill 2 years of monthly income at once`() = runTest {
        val template = recurringTemplate(startDate = "2025-01-01")
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 12))

        assertEquals(24, allIncome.size)
    }

    // --- Start date is after current month ---

    @Test
    fun `no generation when current month is before all start dates`() = runTest {
        val template = recurringTemplate(startDate = "2027-01-01")
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 12))

        assertEquals(0, allIncome.size)
    }

    // --- Quarterly with Feb 28 start (edge case for short months) ---

    @Test
    fun `quarterly starting Feb 28 - day preserved in months with more days`() = runTest {
        val template = recurringTemplate(
            interval = Interval.QUARTERLY, startDate = "2026-02-28",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 11))

        // Feb, May, Aug, Nov
        assertEquals(4, allIncome.size)
        assertEquals(LocalDate.of(2026, 2, 28), allIncome[0].date)
        assertEquals(LocalDate.of(2026, 5, 28), allIncome[1].date)
        assertEquals(LocalDate.of(2026, 8, 28), allIncome[2].date)
        assertEquals(LocalDate.of(2026, 11, 28), allIncome[3].date)
    }

    // --- Half-yearly with day clamping ---

    @Test
    fun `half-yearly starting Jan 31 - clamps in July (31 days) and then Jan again`() = runTest {
        val template = recurringTemplate(
            interval = Interval.HALF_YEARLY, startDate = "2025-08-31",
        )
        val allIncome = setupStatefulMocks(listOf(template))

        useCase(YearMonth.of(2026, 8))

        // Aug 2025, Feb 2026, Aug 2026
        assertEquals(3, allIncome.size)
        assertEquals(LocalDate.of(2025, 8, 31), allIncome[0].date)
        assertEquals(LocalDate.of(2026, 2, 28), allIncome[1].date) // Clamped
        assertEquals(LocalDate.of(2026, 8, 31), allIncome[2].date)
    }
}
