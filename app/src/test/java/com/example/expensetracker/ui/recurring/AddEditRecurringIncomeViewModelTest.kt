package com.example.expensetracker.ui.recurring

import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.repository.IncomeRepository
import com.example.expensetracker.domain.model.Interval
import com.example.expensetracker.domain.usecase.GenerateRecurringIncomeUseCase
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
class AddEditRecurringIncomeViewModelTest {

    private val incomeRepository = mockk<IncomeRepository>(relaxed = true)
    private val generateRecurringIncome = mockk<GenerateRecurringIncomeUseCase>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: AddEditRecurringIncomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AddEditRecurringIncomeViewModel(incomeRepository, generateRecurringIncome)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields and monthly interval`() {
        assertEquals("", viewModel.amount.value)
        assertEquals("", viewModel.source.value)
        assertEquals(Interval.MONTHLY, viewModel.recurrenceInterval.value)
        assertEquals("", viewModel.note.value)
    }

    @Test
    fun `loadIncome populates fields from existing entity`() = runTest {
        val entity = IncomeEntity(
            id = 7,
            amountCents = 300000,
            source = "Salary",
            date = LocalDate.of(2026, 1, 1),
            note = "Monthly salary",
            isRecurring = true,
            recurrenceInterval = Interval.MONTHLY,
            startDate = "2026-01-01",
        )
        coEvery { incomeRepository.getById(7) } returns entity

        viewModel.loadIncome(7)

        assertEquals("3000", viewModel.amount.value)
        assertEquals("Salary", viewModel.source.value)
        assertEquals(Interval.MONTHLY, viewModel.recurrenceInterval.value)
        assertEquals(LocalDate.of(2026, 1, 1), viewModel.startDate.value)
        assertEquals("Monthly salary", viewModel.note.value)
    }

    @Test
    fun `loadIncome with quarterly interval`() = runTest {
        val entity = IncomeEntity(
            id = 8,
            amountCents = 50000,
            source = "Freelance",
            date = LocalDate.of(2026, 3, 15),
            isRecurring = true,
            recurrenceInterval = Interval.QUARTERLY,
            startDate = "2026-03-15",
        )
        coEvery { incomeRepository.getById(8) } returns entity

        viewModel.loadIncome(8)

        assertEquals(Interval.QUARTERLY, viewModel.recurrenceInterval.value)
        assertEquals(LocalDate.of(2026, 3, 15), viewModel.startDate.value)
    }

    @Test
    fun `loadIncome with null recurrenceInterval defaults to monthly`() = runTest {
        val entity = IncomeEntity(
            id = 9,
            amountCents = 10000,
            source = "Side gig",
            date = LocalDate.of(2026, 2, 1),
            isRecurring = true,
            recurrenceInterval = null,
            startDate = "2026-02-01",
        )
        coEvery { incomeRepository.getById(9) } returns entity

        viewModel.loadIncome(9)

        assertEquals(Interval.MONTHLY, viewModel.recurrenceInterval.value)
    }

    @Test
    fun `save inserts new recurring income entity`() = runTest {
        viewModel.amount.value = "1500"
        viewModel.source.value = "Consulting"
        viewModel.startDate.value = LocalDate.of(2026, 5, 1)
        viewModel.recurrenceInterval.value = Interval.HALF_YEARLY
        viewModel.note.value = "Bi-annual retainer"

        var completed = false
        viewModel.save { completed = true }

        val entitySlot = slot<IncomeEntity>()
        coVerify { incomeRepository.insert(capture(entitySlot)) }
        assertEquals(0L, entitySlot.captured.id)
        assertEquals(150000L, entitySlot.captured.amountCents)
        assertEquals("Consulting", entitySlot.captured.source)
        assertEquals(LocalDate.of(2026, 5, 1), entitySlot.captured.date)
        assertTrue(entitySlot.captured.isRecurring)
        assertEquals(Interval.HALF_YEARLY, entitySlot.captured.recurrenceInterval)
        assertEquals("2026-05-01", entitySlot.captured.startDate)
        assertEquals("Bi-annual retainer", entitySlot.captured.note)
        assertTrue(completed)
    }

    @Test
    fun `save updates existing entity when editing`() = runTest {
        val existing = IncomeEntity(
            id = 12,
            amountCents = 200000,
            source = "Salary",
            date = LocalDate.of(2026, 1, 1),
            isRecurring = true,
            recurrenceInterval = Interval.MONTHLY,
            startDate = "2026-01-01",
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
    fun `delete removes loaded recurring income and calls onComplete`() = runTest {
        val entity = IncomeEntity(
            id = 12,
            amountCents = 200000,
            source = "Salary",
            date = LocalDate.of(2026, 1, 1),
            isRecurring = true,
            recurrenceInterval = Interval.MONTHLY,
            startDate = "2026-01-01",
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
