package com.example.expensetracker.ui.income

import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.repository.IncomeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditIncomeViewModelTest {

    private val incomeRepository = mockk<IncomeRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: AddEditIncomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AddEditIncomeViewModel(incomeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields`() {
        assertEquals("", viewModel.amount.value)
        assertEquals("", viewModel.source.value)
        assertNull(viewModel.amountError.value)
        assertNull(viewModel.sourceError.value)
    }

    @Test
    fun `loadIncome populates fields`() = runTest {
        val entity = IncomeEntity(
            id = 7,
            amountCents = 300000,
            source = "Salary",
            date = LocalDate.of(2026, 1, 15),
            note = "Monthly salary",
            isRecurring = false,
        )
        coEvery { incomeRepository.getById(7) } returns entity

        viewModel.loadIncome(7)

        assertEquals("3000", viewModel.amount.value)
        assertEquals("Salary", viewModel.source.value)
        assertEquals(LocalDate.of(2026, 1, 15), viewModel.date.value)
        assertEquals("Monthly salary", viewModel.note.value)
    }

    @Test
    fun `save inserts new income entity`() = runTest {
        viewModel.amount.value = "1500"
        viewModel.source.value = "Freelance"
        viewModel.date.value = LocalDate.of(2026, 4, 1)
        viewModel.note.value = "Project payment"

        var completed = false
        viewModel.save { completed = true }

        val entitySlot = slot<IncomeEntity>()
        coVerify { incomeRepository.insert(capture(entitySlot)) }
        assertEquals(0L, entitySlot.captured.id)
        assertEquals(150000L, entitySlot.captured.amountCents)
        assertEquals("Freelance", entitySlot.captured.source)
        assertEquals(LocalDate.of(2026, 4, 1), entitySlot.captured.date)
        assertFalse(entitySlot.captured.isRecurring)
        assertEquals("Project payment", entitySlot.captured.note)
        assertTrue(completed)
    }

    @Test
    fun `save updates existing entity when editing`() = runTest {
        val existing = IncomeEntity(
            id = 12,
            amountCents = 200000,
            source = "Salary",
            date = LocalDate.of(2026, 1, 1),
            isRecurring = false,
        )
        coEvery { incomeRepository.getById(12) } returns existing

        viewModel.loadIncome(12)
        viewModel.amount.value = "2500"
        viewModel.source.value = "New Salary"

        var completed = false
        viewModel.save { completed = true }

        val entitySlot = slot<IncomeEntity>()
        coVerify { incomeRepository.update(capture(entitySlot)) }
        assertEquals(12L, entitySlot.captured.id)
        assertEquals(250000L, entitySlot.captured.amountCents)
        assertEquals("New Salary", entitySlot.captured.source)
        assertTrue(completed)
    }

    @Test
    fun `save sets amount error with empty amount`() = runTest {
        viewModel.amount.value = ""
        viewModel.source.value = "Salary"

        var completed = false
        viewModel.save { completed = true }

        assertEquals("Enter a valid amount", viewModel.amountError.value)
        assertNull(viewModel.sourceError.value)
        coVerify(exactly = 0) { incomeRepository.insert(any()) }
        assert(!completed)
    }

    @Test
    fun `save sets source error with blank source`() = runTest {
        viewModel.amount.value = "100"
        viewModel.source.value = "   "

        var completed = false
        viewModel.save { completed = true }

        assertNull(viewModel.amountError.value)
        assertEquals("Enter a source", viewModel.sourceError.value)
        coVerify(exactly = 0) { incomeRepository.insert(any()) }
        assert(!completed)
    }

    @Test
    fun `save sets both errors when both fields invalid`() = runTest {
        viewModel.amount.value = ""
        viewModel.source.value = ""

        viewModel.save {}

        assertEquals("Enter a valid amount", viewModel.amountError.value)
        assertEquals("Enter a source", viewModel.sourceError.value)
    }

    @Test
    fun `successful save clears errors`() = runTest {
        viewModel.amount.value = ""
        viewModel.source.value = ""
        viewModel.save {}
        assertEquals("Enter a valid amount", viewModel.amountError.value)
        assertEquals("Enter a source", viewModel.sourceError.value)

        viewModel.amount.value = "100"
        viewModel.source.value = "Salary"
        viewModel.save {}

        assertNull(viewModel.amountError.value)
        assertNull(viewModel.sourceError.value)
    }

    @Test
    fun `blank note is saved as null`() = runTest {
        viewModel.amount.value = "100"
        viewModel.source.value = "Test"
        viewModel.note.value = ""

        viewModel.save {}

        val entitySlot = slot<IncomeEntity>()
        coVerify { incomeRepository.insert(capture(entitySlot)) }
        assertNull(entitySlot.captured.note)
    }

    @Test
    fun `delete removes loaded income and calls onComplete`() = runTest {
        val entity = IncomeEntity(
            id = 12,
            amountCents = 200000,
            source = "Salary",
            date = LocalDate.of(2026, 1, 1),
            isRecurring = false,
        )
        coEvery { incomeRepository.getById(12) } returns entity

        viewModel.loadIncome(12)

        var completed = false
        viewModel.delete { completed = true }

        coVerify { incomeRepository.delete(entity) }
        assertTrue(completed)
    }

    @Test
    fun `delete does nothing without loaded income`() = runTest {
        var completed = false
        viewModel.delete { completed = true }

        coVerify(exactly = 0) { incomeRepository.delete(any()) }
        assertFalse(completed)
    }

    @Test
    fun `loadIncome with nonexistent id does nothing`() = runTest {
        coEvery { incomeRepository.getById(999) } returns null

        viewModel.loadIncome(999)

        assertEquals("", viewModel.amount.value)
        assertEquals("", viewModel.source.value)
    }
}
