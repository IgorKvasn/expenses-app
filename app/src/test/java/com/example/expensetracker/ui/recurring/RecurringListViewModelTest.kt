package com.example.expensetracker.ui.recurring

import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import com.example.expensetracker.domain.model.Interval
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringListViewModelTest {

    private val recurringExpenseRepository = mockk<RecurringExpenseRepository>()
    private val incomeRepository = mockk<IncomeRepository>()
    private val categoryRepository = mockk<CategoryRepository> {
        every { getAll() } returns flowOf(emptyList())
    }
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RecurringListViewModel {
        return RecurringListViewModel(
            recurringExpenseRepository, incomeRepository, categoryRepository,
        )
    }

    private suspend fun RecurringListViewModel.awaitSummary(): RecurringSummary {
        // Trigger collection by subscribing, then get the value
        return summary.first { it != RecurringSummary() || true }
    }

    @Test
    fun `summary with no recurring items is zero`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(emptyList())
        every { incomeRepository.getRecurring() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        // Start collecting to trigger WhileSubscribed
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        assertEquals(0L, viewModel.summary.value.monthlyExpenseCents)
        assertEquals(0L, viewModel.summary.value.monthlyIncomeCents)
        assertEquals(0L, viewModel.summary.value.monthlyNetCents)
    }

    @Test
    fun `summary with monthly expenses sums correctly`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(
            listOf(
                RecurringExpenseEntity(
                    id = 1, amountCents = 150000, categoryId = 1,
                    interval = Interval.MONTHLY, startDate = "2026-01-01",
                ),
                RecurringExpenseEntity(
                    id = 2, amountCents = 50000, categoryId = 2,
                    interval = Interval.MONTHLY, startDate = "2026-01-01",
                ),
            ),
        )
        every { incomeRepository.getRecurring() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        assertEquals(200000L, viewModel.summary.value.monthlyExpenseCents)
    }

    @Test
    fun `summary with quarterly expense divides by 3`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(
            listOf(
                RecurringExpenseEntity(
                    id = 1, amountCents = 30000, categoryId = 1,
                    interval = Interval.QUARTERLY, startDate = "2026-01-01",
                ),
            ),
        )
        every { incomeRepository.getRecurring() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        assertEquals(10000L, viewModel.summary.value.monthlyExpenseCents)
    }

    @Test
    fun `summary with half-yearly expense divides by 6`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(
            listOf(
                RecurringExpenseEntity(
                    id = 1, amountCents = 60000, categoryId = 1,
                    interval = Interval.HALF_YEARLY, startDate = "2026-01-01",
                ),
            ),
        )
        every { incomeRepository.getRecurring() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        assertEquals(10000L, viewModel.summary.value.monthlyExpenseCents)
    }

    @Test
    fun `summary with monthly income sums correctly`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(emptyList())
        every { incomeRepository.getRecurring() } returns flowOf(
            listOf(
                IncomeEntity(
                    id = 1, amountCents = 500000, source = "Salary",
                    date = LocalDate.of(2026, 1, 1), isRecurring = true,
                    recurrenceInterval = Interval.MONTHLY, startDate = "2026-01-01",
                ),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        assertEquals(500000L, viewModel.summary.value.monthlyIncomeCents)
    }

    @Test
    fun `summary with quarterly income divides by 3`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(emptyList())
        every { incomeRepository.getRecurring() } returns flowOf(
            listOf(
                IncomeEntity(
                    id = 1, amountCents = 90000, source = "Bonus",
                    date = LocalDate.of(2026, 1, 1), isRecurring = true,
                    recurrenceInterval = Interval.QUARTERLY, startDate = "2026-01-01",
                ),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        assertEquals(30000L, viewModel.summary.value.monthlyIncomeCents)
    }

    @Test
    fun `summary with null recurrenceInterval defaults to monthly divisor`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(emptyList())
        every { incomeRepository.getRecurring() } returns flowOf(
            listOf(
                IncomeEntity(
                    id = 1, amountCents = 100000, source = "Test",
                    date = LocalDate.of(2026, 1, 1), isRecurring = true,
                    recurrenceInterval = null, startDate = "2026-01-01",
                ),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        assertEquals(100000L, viewModel.summary.value.monthlyIncomeCents)
    }

    @Test
    fun `summary net is income minus expenses`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(
            listOf(
                RecurringExpenseEntity(
                    id = 1, amountCents = 150000, categoryId = 1,
                    interval = Interval.MONTHLY, startDate = "2026-01-01",
                ),
            ),
        )
        every { incomeRepository.getRecurring() } returns flowOf(
            listOf(
                IncomeEntity(
                    id = 1, amountCents = 500000, source = "Salary",
                    date = LocalDate.of(2026, 1, 1), isRecurring = true,
                    recurrenceInterval = Interval.MONTHLY, startDate = "2026-01-01",
                ),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        assertEquals(150000L, viewModel.summary.value.monthlyExpenseCents)
        assertEquals(500000L, viewModel.summary.value.monthlyIncomeCents)
        assertEquals(350000L, viewModel.summary.value.monthlyNetCents)
    }

    @Test
    fun `summary with mixed intervals calculates correctly`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(
            listOf(
                RecurringExpenseEntity(
                    id = 1, amountCents = 120000, categoryId = 1,
                    interval = Interval.MONTHLY, startDate = "2026-01-01",
                ),
                RecurringExpenseEntity(
                    id = 2, amountCents = 30000, categoryId = 2,
                    interval = Interval.QUARTERLY, startDate = "2026-01-01",
                ),
                RecurringExpenseEntity(
                    id = 3, amountCents = 60000, categoryId = 3,
                    interval = Interval.HALF_YEARLY, startDate = "2026-01-01",
                ),
            ),
        )
        every { incomeRepository.getRecurring() } returns flowOf(
            listOf(
                IncomeEntity(
                    id = 1, amountCents = 500000, source = "Salary",
                    date = LocalDate.of(2026, 1, 1), isRecurring = true,
                    recurrenceInterval = Interval.MONTHLY, startDate = "2026-01-01",
                ),
                IncomeEntity(
                    id = 2, amountCents = 60000, source = "Dividends",
                    date = LocalDate.of(2026, 1, 1), isRecurring = true,
                    recurrenceInterval = Interval.HALF_YEARLY, startDate = "2026-01-01",
                ),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        // Expenses: 120000/1 + 30000/3 + 60000/6 = 120000 + 10000 + 10000 = 140000
        assertEquals(140000L, viewModel.summary.value.monthlyExpenseCents)
        // Income: 500000/1 + 60000/6 = 500000 + 10000 = 510000
        assertEquals(510000L, viewModel.summary.value.monthlyIncomeCents)
        // Net: 510000 - 140000 = 370000
        assertEquals(370000L, viewModel.summary.value.monthlyNetCents)
    }

    @Test
    fun `summary net is negative when expenses exceed income`() = runTest {
        every { recurringExpenseRepository.getAll() } returns flowOf(
            listOf(
                RecurringExpenseEntity(
                    id = 1, amountCents = 500000, categoryId = 1,
                    interval = Interval.MONTHLY, startDate = "2026-01-01",
                ),
            ),
        )
        every { incomeRepository.getRecurring() } returns flowOf(
            listOf(
                IncomeEntity(
                    id = 1, amountCents = 300000, source = "Part-time",
                    date = LocalDate.of(2026, 1, 1), isRecurring = true,
                    recurrenceInterval = Interval.MONTHLY, startDate = "2026-01-01",
                ),
            ),
        )

        val viewModel = createViewModel()
        backgroundScope.launch(testDispatcher) { viewModel.summary.collect {} }

        assertEquals(-200000L, viewModel.summary.value.monthlyNetCents)
    }
}
