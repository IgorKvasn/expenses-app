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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Month-by-month simulation tests for recurring expenses.
 * These tests simulate the app being opened each month and verify that
 * the correct expenses are generated cumulatively.
 */
class RecurringExpensesMonthByMonthTest {

    private val expenseRepository = mockk<ExpenseRepository>(relaxed = true)
    private val recurringExpenseRepository = mockk<RecurringExpenseRepository>(relaxed = true)
    private val database = mockk<AppDatabase>(relaxed = true)
    private val useCase = GenerateRecurringExpensesUseCase(
        expenseRepository, recurringExpenseRepository, database,
    ).apply {
        transactionRunner = { block -> block() }
    }

    // --- Monthly: month-by-month simulation ---

    @Test
    fun `monthly expense - generate month by month for full year`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-15",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { expenseRepository.insert(any()) } returns 100L

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        // Simulate opening the app each month, Jan through Dec
        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        assertEquals(12, allExpenses.size)
        for (month in 1..12) {
            val expense = allExpenses[month - 1]
            assertEquals(LocalDate.of(2026, month, 15), expense.date)
            assertEquals(10000L, expense.amountCents)
            assertEquals(1L, expense.categoryId)
            assertEquals(1L, expense.recurringExpenseId)
        }
    }

    @Test
    fun `monthly expense - skipping months still catches up`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 5000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        // User only opens app in January and then in June
        useCase(YearMonth.of(2026, 1))
        assertEquals(1, allExpenses.size)

        useCase(YearMonth.of(2026, 6))
        // Should backfill Feb, Mar, Apr, May, Jun = 5 more
        assertEquals(6, allExpenses.size)
        assertEquals(LocalDate.of(2026, 1, 1), allExpenses[0].date)
        assertEquals(LocalDate.of(2026, 2, 1), allExpenses[1].date)
        assertEquals(LocalDate.of(2026, 3, 1), allExpenses[2].date)
        assertEquals(LocalDate.of(2026, 4, 1), allExpenses[3].date)
        assertEquals(LocalDate.of(2026, 5, 1), allExpenses[4].date)
        assertEquals(LocalDate.of(2026, 6, 1), allExpenses[5].date)
    }

    @Test
    fun `monthly expense - calling twice in same month does not duplicate`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-03-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2026, 3))
        useCase(YearMonth.of(2026, 3))
        useCase(YearMonth.of(2026, 3))

        assertEquals(1, allExpenses.size)
    }

    // --- Quarterly: month-by-month simulation ---

    @Test
    fun `quarterly expense - generate month by month for full year`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 30000, categoryId = 2,
            interval = Interval.QUARTERLY, startDate = "2026-01-10",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        // Jan, Apr, Jul, Oct = 4 quarters
        assertEquals(4, allExpenses.size)
        assertEquals(LocalDate.of(2026, 1, 10), allExpenses[0].date)
        assertEquals(LocalDate.of(2026, 4, 10), allExpenses[1].date)
        assertEquals(LocalDate.of(2026, 7, 10), allExpenses[2].date)
        assertEquals(LocalDate.of(2026, 10, 10), allExpenses[3].date)
    }

    @Test
    fun `quarterly expense - skipping months catches up correctly`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 50000, categoryId = 1,
            interval = Interval.QUARTERLY, startDate = "2026-02-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        // Only open in Feb, then skip to Nov
        useCase(YearMonth.of(2026, 2))
        assertEquals(1, allExpenses.size) // Feb

        useCase(YearMonth.of(2026, 11))
        // Should backfill: May, Aug, Nov = 3 more
        assertEquals(4, allExpenses.size)
        assertEquals(LocalDate.of(2026, 2, 1), allExpenses[0].date)
        assertEquals(LocalDate.of(2026, 5, 1), allExpenses[1].date)
        assertEquals(LocalDate.of(2026, 8, 1), allExpenses[2].date)
        assertEquals(LocalDate.of(2026, 11, 1), allExpenses[3].date)
    }

    @Test
    fun `quarterly expense - non-quarter months produce no new expenses`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.QUARTERLY, startDate = "2026-01-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2026, 1))
        assertEquals(1, allExpenses.size)

        // Opening in Feb and Mar should not generate anything new
        useCase(YearMonth.of(2026, 2))
        assertEquals(1, allExpenses.size)

        useCase(YearMonth.of(2026, 3))
        assertEquals(1, allExpenses.size)

        // April should generate the next one
        useCase(YearMonth.of(2026, 4))
        assertEquals(2, allExpenses.size)
    }

    // --- Half-yearly: month-by-month simulation ---

    @Test
    fun `half-yearly expense - generate month by month for full year`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 120000, categoryId = 3,
            interval = Interval.HALF_YEARLY, startDate = "2026-03-20",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        // Start in March: Mar and Sep
        assertEquals(2, allExpenses.size)
        assertEquals(LocalDate.of(2026, 3, 20), allExpenses[0].date)
        assertEquals(LocalDate.of(2026, 9, 20), allExpenses[1].date)
    }

    @Test
    fun `half-yearly expense - spans across years`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 100000, categoryId = 1,
            interval = Interval.HALF_YEARLY, startDate = "2025-10-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        // Call for April 2026 — should backfill Oct 2025 and Apr 2026
        useCase(YearMonth.of(2026, 4))

        assertEquals(2, allExpenses.size)
        assertEquals(LocalDate.of(2025, 10, 1), allExpenses[0].date)
        assertEquals(LocalDate.of(2026, 4, 1), allExpenses[1].date)
    }

    // --- Day-of-month clamping across months ---

    @Test
    fun `monthly expense on 31st - clamps correctly through all months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-31",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        assertEquals(12, allExpenses.size)
        // Verify clamping for each month
        assertEquals(31, allExpenses[0].date.dayOfMonth)  // Jan 31
        assertEquals(28, allExpenses[1].date.dayOfMonth)  // Feb 28 (2026 is not leap)
        assertEquals(31, allExpenses[2].date.dayOfMonth)  // Mar 31
        assertEquals(30, allExpenses[3].date.dayOfMonth)  // Apr 30
        assertEquals(31, allExpenses[4].date.dayOfMonth)  // May 31
        assertEquals(30, allExpenses[5].date.dayOfMonth)  // Jun 30
        assertEquals(31, allExpenses[6].date.dayOfMonth)  // Jul 31
        assertEquals(31, allExpenses[7].date.dayOfMonth)  // Aug 31
        assertEquals(30, allExpenses[8].date.dayOfMonth)  // Sep 30
        assertEquals(31, allExpenses[9].date.dayOfMonth)  // Oct 31
        assertEquals(30, allExpenses[10].date.dayOfMonth) // Nov 30
        assertEquals(31, allExpenses[11].date.dayOfMonth) // Dec 31
    }

    @Test
    fun `monthly expense on 30th - clamps only for February`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-30",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        assertEquals(12, allExpenses.size)
        assertEquals(30, allExpenses[0].date.dayOfMonth)  // Jan 30
        assertEquals(28, allExpenses[1].date.dayOfMonth)  // Feb 28 (clamped)
        assertEquals(30, allExpenses[2].date.dayOfMonth)  // Mar 30
        // Apr-Dec all have at least 30 days
        for (i in 3..11) {
            assertEquals(30, allExpenses[i].date.dayOfMonth)
        }
    }

    @Test
    fun `monthly expense on 29th - February in leap year has 29`() = runTest {
        // 2028 is a leap year
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2028-01-29",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2028, 2))

        assertEquals(2, allExpenses.size)
        assertEquals(LocalDate.of(2028, 1, 29), allExpenses[0].date)
        assertEquals(LocalDate.of(2028, 2, 29), allExpenses[1].date) // Leap year, no clamping
    }

    @Test
    fun `monthly expense on 29th - February in non-leap year clamps to 28`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-29",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2026, 2))

        assertEquals(2, allExpenses.size)
        assertEquals(LocalDate.of(2026, 2, 28), allExpenses[1].date) // Clamped
    }

    // --- Multiple recurring expenses, month-by-month ---

    @Test
    fun `multiple expenses with different intervals - month by month`() = runTest {
        val monthlyRent = RecurringExpenseEntity(
            id = 1, amountCents = 150000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-01",
        )
        val quarterlyInsurance = RecurringExpenseEntity(
            id = 2, amountCents = 50000, categoryId = 2,
            interval = Interval.QUARTERLY, startDate = "2026-01-15",
        )
        val halfYearlyMaintenance = RecurringExpenseEntity(
            id = 3, amountCents = 80000, categoryId = 3,
            interval = Interval.HALF_YEARLY, startDate = "2026-01-20",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(
            monthlyRent, quarterlyInsurance, halfYearlyMaintenance,
        )

        val generatedMonths = mutableMapOf<Long, MutableSet<String>>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } answers {
            val id = firstArg<Long>()
            val month = secondArg<String>()
            generatedMonths.getOrDefault(id, mutableSetOf()).contains(month)
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            val gen = firstArg<RecurringExpenseGenerationEntity>()
            generatedMonths.getOrPut(gen.recurringExpenseId) { mutableSetOf() }
                .add(gen.generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        for (month in 1..12) {
            useCase(YearMonth.of(2026, month))
        }

        val rentExpenses = allExpenses.filter { it.recurringExpenseId == 1L }
        val insuranceExpenses = allExpenses.filter { it.recurringExpenseId == 2L }
        val maintenanceExpenses = allExpenses.filter { it.recurringExpenseId == 3L }

        assertEquals(12, rentExpenses.size)       // Every month
        assertEquals(4, insuranceExpenses.size)    // Jan, Apr, Jul, Oct
        assertEquals(2, maintenanceExpenses.size)  // Jan, Jul

        // Verify insurance months
        assertEquals(1, insuranceExpenses[0].date.monthValue)
        assertEquals(4, insuranceExpenses[1].date.monthValue)
        assertEquals(7, insuranceExpenses[2].date.monthValue)
        assertEquals(10, insuranceExpenses[3].date.monthValue)

        // Verify maintenance months
        assertEquals(1, maintenanceExpenses[0].date.monthValue)
        assertEquals(7, maintenanceExpenses[1].date.monthValue)
    }

    @Test
    fun `multiple expenses with different start dates - staggered generation`() = runTest {
        val expense1 = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-01",
        )
        val expense2 = RecurringExpenseEntity(
            id = 2, amountCents = 20000, categoryId = 2,
            interval = Interval.MONTHLY, startDate = "2026-03-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(expense1, expense2)

        val generatedMonths = mutableMapOf<Long, MutableSet<String>>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } answers {
            generatedMonths.getOrDefault(firstArg(), mutableSetOf()).contains(secondArg())
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            val gen = firstArg<RecurringExpenseGenerationEntity>()
            generatedMonths.getOrPut(gen.recurringExpenseId) { mutableSetOf() }
                .add(gen.generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        for (month in 1..6) {
            useCase(YearMonth.of(2026, month))
        }

        val exp1 = allExpenses.filter { it.recurringExpenseId == 1L }
        val exp2 = allExpenses.filter { it.recurringExpenseId == 2L }

        assertEquals(6, exp1.size) // Jan-Jun
        assertEquals(4, exp2.size) // Mar-Jun (started in March)
    }

    // --- Year boundary tests ---

    @Test
    fun `monthly expense spanning year boundary`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2025-11-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        // Nov and Dec 2025
        useCase(YearMonth.of(2025, 11))
        useCase(YearMonth.of(2025, 12))
        // Jan and Feb 2026
        useCase(YearMonth.of(2026, 1))
        useCase(YearMonth.of(2026, 2))

        assertEquals(4, allExpenses.size)
        assertEquals(LocalDate.of(2025, 11, 1), allExpenses[0].date)
        assertEquals(LocalDate.of(2025, 12, 1), allExpenses[1].date)
        assertEquals(LocalDate.of(2026, 1, 1), allExpenses[2].date)
        assertEquals(LocalDate.of(2026, 2, 1), allExpenses[3].date)
    }

    @Test
    fun `quarterly expense spanning year boundary`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 30000, categoryId = 1,
            interval = Interval.QUARTERLY, startDate = "2025-10-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        val generatedMonths = mutableSetOf<String>()
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, any()) } answers {
            secondArg<String>() in generatedMonths
        }
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generatedMonths.add(firstArg<RecurringExpenseGenerationEntity>().generatedForMonth)
            1L
        }

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2026, 7))

        // Oct 2025, Jan 2026, Apr 2026, Jul 2026
        assertEquals(4, allExpenses.size)
        assertEquals(LocalDate.of(2025, 10, 1), allExpenses[0].date)
        assertEquals(LocalDate.of(2026, 1, 1), allExpenses[1].date)
        assertEquals(LocalDate.of(2026, 4, 1), allExpenses[2].date)
        assertEquals(LocalDate.of(2026, 7, 1), allExpenses[3].date)
    }

    // --- Edge case: start date is exactly current month ---

    @Test
    fun `generates for start month when current month equals start month`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-06-15",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { expenseRepository.insert(any()) } returns 1L

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2026, 6))

        assertEquals(1, allExpenses.size)
        assertEquals(LocalDate.of(2026, 6, 15), allExpenses[0].date)
    }

    // --- Long backfill scenario ---

    @Test
    fun `backfill 2 years of monthly expenses at once`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2025-01-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { recurringExpenseRepository.recordGeneration(any()) } returns 1L

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2026, 12))

        // Jan 2025 through Dec 2026 = 24 months
        assertEquals(24, allExpenses.size)
    }

    // --- Preserving note field ---

    @Test
    fun `null note is preserved across months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-01", note = null,
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { recurringExpenseRepository.recordGeneration(any()) } returns 1L

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2026, 3))

        assertEquals(3, allExpenses.size)
        allExpenses.forEach { assertEquals(null, it.note) }
    }

    @Test
    fun `non-null note is preserved across months`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-01", note = "Rent",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false
        coEvery { recurringExpenseRepository.recordGeneration(any()) } returns 1L

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2026, 3))

        assertEquals(3, allExpenses.size)
        allExpenses.forEach { assertEquals("Rent", it.note) }
    }

    // --- Generation tracking records ---

    @Test
    fun `generation records have correct month strings`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 5, amountCents = 10000, categoryId = 1,
            interval = Interval.QUARTERLY, startDate = "2026-01-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)
        coEvery { recurringExpenseRepository.isGeneratedForMonth(any(), any()) } returns false

        var insertCounter = 0L
        coEvery { expenseRepository.insert(any()) } answers { ++insertCounter }

        val generations = mutableListOf<RecurringExpenseGenerationEntity>()
        coEvery { recurringExpenseRepository.recordGeneration(any()) } answers {
            generations.add(firstArg())
            1L
        }

        useCase(YearMonth.of(2026, 7))

        assertEquals(3, generations.size)
        assertEquals("2026-01", generations[0].generatedForMonth)
        assertEquals("2026-04", generations[1].generatedForMonth)
        assertEquals("2026-07", generations[2].generatedForMonth)

        // Verify each generation links to the correct recurring expense
        generations.forEach { assertEquals(5L, it.recurringExpenseId) }

        // Verify expense IDs are sequential
        assertEquals(1L, generations[0].expenseId)
        assertEquals(2L, generations[1].expenseId)
        assertEquals(3L, generations[2].expenseId)
    }

    // --- Partial generation (some months already done) ---

    @Test
    fun `only generates missing months when some already exist`() = runTest {
        val recurring = RecurringExpenseEntity(
            id = 1, amountCents = 10000, categoryId = 1,
            interval = Interval.MONTHLY, startDate = "2026-01-01",
        )
        coEvery { recurringExpenseRepository.getAllSuspend() } returns listOf(recurring)

        // Jan and Feb already generated, Mar-Jun not
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-01") } returns true
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-02") } returns true
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-03") } returns false
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-04") } returns false
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-05") } returns false
        coEvery { recurringExpenseRepository.isGeneratedForMonth(1, "2026-06") } returns false
        coEvery { recurringExpenseRepository.recordGeneration(any()) } returns 1L

        val allExpenses = mutableListOf<ExpenseEntity>()
        coEvery { expenseRepository.insert(any()) } answers {
            allExpenses.add(firstArg())
            allExpenses.size.toLong()
        }

        useCase(YearMonth.of(2026, 6))

        assertEquals(4, allExpenses.size) // Mar, Apr, May, Jun
        assertEquals(LocalDate.of(2026, 3, 1), allExpenses[0].date)
        assertEquals(LocalDate.of(2026, 4, 1), allExpenses[1].date)
        assertEquals(LocalDate.of(2026, 5, 1), allExpenses[2].date)
        assertEquals(LocalDate.of(2026, 6, 1), allExpenses[3].date)
    }
}
