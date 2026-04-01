package com.example.expensetracker.ui.expenses

import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.ExpenseRepository
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
class AddEditExpenseViewModelTest {

    private val expenseRepository = mockk<ExpenseRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository> {
        every { getAll() } returns flowOf(emptyList())
    }
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: AddEditExpenseViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AddEditExpenseViewModel(expenseRepository, categoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields`() {
        assertEquals("", viewModel.amount.value)
        assertNull(viewModel.categoryId.value)
        assertNull(viewModel.amountError.value)
        assertNull(viewModel.categoryError.value)
    }

    @Test
    fun `loadExpense populates fields`() = runTest {
        val entity = ExpenseEntity(
            id = 5,
            amountCents = 1250,
            categoryId = 3,
            date = LocalDate.of(2026, 3, 15),
            note = "Lunch",
        )
        coEvery { expenseRepository.getById(5) } returns entity

        viewModel.loadExpense(5)

        assertEquals("12.50", viewModel.amount.value)
        assertEquals(3L, viewModel.categoryId.value)
        assertEquals(LocalDate.of(2026, 3, 15), viewModel.date.value)
        assertEquals("Lunch", viewModel.note.value)
    }

    @Test
    fun `save inserts new entity`() = runTest {
        viewModel.amount.value = "25.50"
        viewModel.categoryId.value = 2L
        viewModel.date.value = LocalDate.of(2026, 4, 1)
        viewModel.note.value = "Groceries"

        var completed = false
        viewModel.save { completed = true }

        val entitySlot = slot<ExpenseEntity>()
        coVerify { expenseRepository.insert(capture(entitySlot)) }
        assertEquals(0L, entitySlot.captured.id)
        assertEquals(2550L, entitySlot.captured.amountCents)
        assertEquals(2L, entitySlot.captured.categoryId)
        assertEquals(LocalDate.of(2026, 4, 1), entitySlot.captured.date)
        assertEquals("Groceries", entitySlot.captured.note)
        assertTrue(completed)
    }

    @Test
    fun `save updates existing entity when editing`() = runTest {
        val existing = ExpenseEntity(
            id = 10,
            amountCents = 5000,
            categoryId = 1,
            date = LocalDate.of(2026, 1, 1),
        )
        coEvery { expenseRepository.getById(10) } returns existing

        viewModel.loadExpense(10)
        viewModel.amount.value = "75"
        viewModel.categoryId.value = 2L

        var completed = false
        viewModel.save { completed = true }

        val entitySlot = slot<ExpenseEntity>()
        coVerify { expenseRepository.update(capture(entitySlot)) }
        assertEquals(10L, entitySlot.captured.id)
        assertEquals(7500L, entitySlot.captured.amountCents)
        assertEquals(2L, entitySlot.captured.categoryId)
        assertTrue(completed)
    }

    @Test
    fun `save sets amount error with invalid amount`() = runTest {
        viewModel.amount.value = ""
        viewModel.categoryId.value = 1L

        var completed = false
        viewModel.save { completed = true }

        assertEquals("Enter a valid amount", viewModel.amountError.value)
        assertNull(viewModel.categoryError.value)
        coVerify(exactly = 0) { expenseRepository.insert(any()) }
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
        coVerify(exactly = 0) { expenseRepository.insert(any()) }
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
    fun `blank note is saved as null`() = runTest {
        viewModel.amount.value = "10"
        viewModel.categoryId.value = 1L
        viewModel.note.value = "   "

        viewModel.save {}

        val entitySlot = slot<ExpenseEntity>()
        coVerify { expenseRepository.insert(capture(entitySlot)) }
        assertNull(entitySlot.captured.note)
    }

    @Test
    fun `delete removes loaded expense and calls onComplete`() = runTest {
        val entity = ExpenseEntity(
            id = 10,
            amountCents = 5000,
            categoryId = 1,
            date = LocalDate.of(2026, 1, 1),
        )
        coEvery { expenseRepository.getById(10) } returns entity

        viewModel.loadExpense(10)

        var completed = false
        viewModel.delete { completed = true }

        coVerify { expenseRepository.delete(entity) }
        assertTrue(completed)
    }

    @Test
    fun `delete does nothing without loaded expense`() = runTest {
        var completed = false
        viewModel.delete { completed = true }

        coVerify(exactly = 0) { expenseRepository.delete(any()) }
        assert(!completed)
    }

    @Test
    fun `loadExpense with nonexistent id does nothing`() = runTest {
        coEvery { expenseRepository.getById(999) } returns null

        viewModel.loadExpense(999)

        assertEquals("", viewModel.amount.value)
        assertNull(viewModel.categoryId.value)
    }
}
