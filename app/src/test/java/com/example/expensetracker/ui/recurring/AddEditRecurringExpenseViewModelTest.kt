package com.example.expensetracker.ui.recurring

import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.RecurringExpenseRepository
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.domain.usecase.GenerateRecurringExpensesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditRecurringExpenseViewModelTest {

    private val recurringExpenseRepository = mockk<RecurringExpenseRepository>(relaxed = true)
    private val generateRecurringExpenses = mockk<GenerateRecurringExpensesUseCase>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository> {
        every { getAll() } returns flowOf(emptyList())
    }
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: AddEditRecurringExpenseViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AddEditRecurringExpenseViewModel(
            recurringExpenseRepository, generateRecurringExpenses, categoryRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty amount and monthly interval`() {
        assertEquals("", viewModel.amount.value)
        assertNull(viewModel.categoryId.value)
        assertEquals(Interval.MONTHLY, viewModel.interval.value)
        assertEquals("", viewModel.note.value)
    }

    @Test
    fun `loadRecurringExpense populates fields`() = runTest {
        val entity = RecurringExpenseEntity(
            id = 5,
            amountCents = 15000,
            categoryId = 3,
            interval = Interval.QUARTERLY,
            note = "Insurance",
            startDate = "2026-06-15",
        )
        coEvery { recurringExpenseRepository.getById(5) } returns entity

        viewModel.loadRecurringExpense(5)

        assertEquals("150", viewModel.amount.value)
        assertEquals(3L, viewModel.categoryId.value)
        assertEquals(Interval.QUARTERLY, viewModel.interval.value)
        assertEquals(LocalDate.of(2026, 6, 15), viewModel.startDate.value)
        assertEquals("Insurance", viewModel.note.value)
    }

    @Test
    fun `save inserts new entity when not editing`() = runTest {
        viewModel.amount.value = "25.50"
        viewModel.categoryId.value = 2L
        viewModel.startDate.value = LocalDate.of(2026, 4, 1)
        viewModel.interval.value = Interval.HALF_YEARLY
        viewModel.note.value = "Gym membership"

        var completed = false
        viewModel.save { completed = true }

        val entitySlot = slot<RecurringExpenseEntity>()
        coVerify { recurringExpenseRepository.insert(capture(entitySlot)) }
        assertEquals(0L, entitySlot.captured.id)
        assertEquals(2550L, entitySlot.captured.amountCents)
        assertEquals(2L, entitySlot.captured.categoryId)
        assertEquals(Interval.HALF_YEARLY, entitySlot.captured.interval)
        assertEquals("2026-04-01", entitySlot.captured.startDate)
        assertEquals("Gym membership", entitySlot.captured.note)
        assert(completed)
    }

    @Test
    fun `save updates existing entity when editing`() = runTest {
        val existing = RecurringExpenseEntity(
            id = 10,
            amountCents = 5000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-01-01",
        )
        coEvery { recurringExpenseRepository.getById(10) } returns existing

        viewModel.loadRecurringExpense(10)
        viewModel.amount.value = "75"
        viewModel.interval.value = Interval.QUARTERLY

        var completed = false
        viewModel.save { completed = true }

        val entitySlot = slot<RecurringExpenseEntity>()
        coVerify { recurringExpenseRepository.update(capture(entitySlot)) }
        assertEquals(10L, entitySlot.captured.id)
        assertEquals(7500L, entitySlot.captured.amountCents)
        assertEquals(Interval.QUARTERLY, entitySlot.captured.interval)
        assert(completed)
    }

    @Test
    fun `save sets amount error with invalid amount`() = runTest {
        viewModel.amount.value = ""
        viewModel.categoryId.value = 1L

        var completed = false
        viewModel.save { completed = true }

        assertEquals("Enter a valid amount", viewModel.amountError.value)
        assertNull(viewModel.categoryError.value)
        coVerify(exactly = 0) { recurringExpenseRepository.insert(any()) }
        assert(!completed)
    }

    @Test
    fun `save sets category error without category`() = runTest {
        viewModel.amount.value = "10"
        viewModel.categoryId.value = null

        var completed = false
        viewModel.save { completed = true }

        assertNull(viewModel.amountError.value)
        assertEquals("Select a category", viewModel.categoryError.value)
        coVerify(exactly = 0) { recurringExpenseRepository.insert(any()) }
        assert(!completed)
    }

    @Test
    fun `save sets both errors when both fields invalid`() = runTest {
        viewModel.amount.value = ""
        viewModel.categoryId.value = null

        viewModel.save {}

        assertEquals("Enter a valid amount", viewModel.amountError.value)
        assertEquals("Select a category", viewModel.categoryError.value)
    }

    @Test
    fun `successful save clears errors`() = runTest {
        viewModel.amount.value = ""
        viewModel.categoryId.value = null
        viewModel.save {}
        assertEquals("Enter a valid amount", viewModel.amountError.value)
        assertEquals("Select a category", viewModel.categoryError.value)

        viewModel.amount.value = "10"
        viewModel.categoryId.value = 1L
        viewModel.save {}

        assertNull(viewModel.amountError.value)
        assertNull(viewModel.categoryError.value)
    }

    @Test
    fun `delete removes loaded recurring expense and calls onComplete`() = runTest {
        val entity = RecurringExpenseEntity(
            id = 10,
            amountCents = 5000,
            categoryId = 1,
            interval = Interval.MONTHLY,
            startDate = "2026-01-01",
        )
        coEvery { recurringExpenseRepository.getById(10) } returns entity

        viewModel.loadRecurringExpense(10)

        var completed = false
        viewModel.delete { completed = true }

        coVerify { recurringExpenseRepository.delete(entity) }
        assertTrue(completed)
    }

    @Test
    fun `delete does nothing without loaded expense`() = runTest {
        var completed = false
        viewModel.delete { completed = true }

        coVerify(exactly = 0) { recurringExpenseRepository.delete(any()) }
        assert(!completed)
    }

    @Test
    fun `blank note is saved as null`() = runTest {
        viewModel.amount.value = "10"
        viewModel.categoryId.value = 1L
        viewModel.note.value = "   "

        viewModel.save {}

        val entitySlot = slot<RecurringExpenseEntity>()
        coVerify { recurringExpenseRepository.insert(capture(entitySlot)) }
        assertNull(entitySlot.captured.note)
    }
}
