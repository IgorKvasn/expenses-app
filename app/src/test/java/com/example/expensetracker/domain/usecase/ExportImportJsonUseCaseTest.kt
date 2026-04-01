package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.db.dao.CategoryDao
import com.example.expensetracker.data.db.dao.ExpenseDao
import com.example.expensetracker.data.db.dao.IncomeDao
import com.example.expensetracker.data.db.dao.RecurringExpenseDao
import com.example.expensetracker.data.db.dao.RecurringExpenseGenerationDao
import com.example.expensetracker.data.db.dao.RecurringIncomeGenerationDao
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import com.example.expensetracker.data.db.entity.RecurringIncomeGenerationEntity
import com.example.expensetracker.domain.model.AppExportData
import com.example.expensetracker.domain.model.Interval
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ExportImportJsonUseCaseTest {

    private val categoryDao: CategoryDao = mockk(relaxed = true)
    private val expenseDao: ExpenseDao = mockk(relaxed = true)
    private val incomeDao: IncomeDao = mockk(relaxed = true)
    private val recurringExpenseDao: RecurringExpenseDao = mockk(relaxed = true)
    private val recurringExpenseGenerationDao: RecurringExpenseGenerationDao = mockk(relaxed = true)
    private val recurringIncomeGenerationDao: RecurringIncomeGenerationDao = mockk(relaxed = true)
    private val database: AppDatabase = mockk(relaxed = true)

    private lateinit var useCase: ExportImportJsonUseCase

    @Before
    fun setup() {
        every { database.categoryDao() } returns categoryDao
        every { database.expenseDao() } returns expenseDao
        every { database.incomeDao() } returns incomeDao
        every { database.recurringExpenseDao() } returns recurringExpenseDao
        every { database.recurringExpenseGenerationDao() } returns recurringExpenseGenerationDao
        every { database.recurringIncomeGenerationDao() } returns recurringIncomeGenerationDao

        useCase = ExportImportJsonUseCase(database).apply {
            transactionRunner = { block -> block() }
        }
    }

    private val sampleCategory = CategoryEntity(id = 1, name = "Food", icon = "restaurant", isDefault = true)
    private val sampleExpense = ExpenseEntity(
        id = 1, amountCents = 1250, categoryId = 1,
        date = LocalDate.of(2026, 3, 15), note = "Lunch",
    )
    private val sampleIncome = IncomeEntity(
        id = 1, amountCents = 300000, source = "Salary",
        date = LocalDate.of(2026, 3, 1), isRecurring = false,
    )
    private val sampleRecurringExpense = RecurringExpenseEntity(
        id = 1, amountCents = 50000, categoryId = 1,
        interval = Interval.MONTHLY, note = "Rent", startDate = "2026-01-01",
    )
    private val sampleExpenseGeneration = RecurringExpenseGenerationEntity(
        id = 1, recurringExpenseId = 1, generatedForMonth = "2026-03", expenseId = 1,
    )
    private val sampleIncomeGeneration = RecurringIncomeGenerationEntity(
        id = 1, recurringIncomeId = 1, generatedForMonth = "2026-03", incomeId = 1,
    )

    private fun setupDaosWithSampleData() {
        coEvery { categoryDao.getAllSuspend() } returns listOf(sampleCategory)
        coEvery { expenseDao.getAllSuspend() } returns listOf(sampleExpense)
        coEvery { incomeDao.getAllSuspend() } returns listOf(sampleIncome)
        coEvery { recurringExpenseDao.getAllSuspend() } returns listOf(sampleRecurringExpense)
        coEvery { recurringExpenseGenerationDao.getAllSuspend() } returns listOf(sampleExpenseGeneration)
        coEvery { recurringIncomeGenerationDao.getAllSuspend() } returns listOf(sampleIncomeGeneration)
    }

    @Test
    fun `export produces valid JSON with all fields`() = runTest {
        setupDaosWithSampleData()

        val jsonString = useCase.exportToJson()
        val parsed = Json.decodeFromString(AppExportData.serializer(), jsonString)

        assertEquals(1, parsed.schemaVersion)
        assertTrue(parsed.exportedAt.isNotEmpty())
        assertEquals(1, parsed.categories.size)
        assertEquals("Food", parsed.categories[0].name)
        assertEquals(1, parsed.expenses.size)
        assertEquals(1250L, parsed.expenses[0].amountCents)
        assertEquals("2026-03-15", parsed.expenses[0].date)
        assertEquals(1, parsed.income.size)
        assertEquals("Salary", parsed.income[0].source)
        assertEquals(1, parsed.recurringExpenses.size)
        assertEquals("MONTHLY", parsed.recurringExpenses[0].interval)
        assertEquals(1, parsed.recurringExpenseGenerations.size)
        assertEquals(1, parsed.recurringIncomeGenerations.size)
    }

    @Test
    fun `round trip produces consistent data`() = runTest {
        setupDaosWithSampleData()

        val exported = useCase.exportToJson()

        // Import should succeed without error
        useCase.importFromJson(exported)

        // Verify all tables were cleared and repopulated
        coVerify { categoryDao.deleteAll() }
        coVerify { expenseDao.deleteAll() }
        coVerify { incomeDao.deleteAll() }
        coVerify { recurringExpenseDao.deleteAll() }
        coVerify { recurringExpenseGenerationDao.deleteAll() }
        coVerify { recurringIncomeGenerationDao.deleteAll() }

        coVerify { categoryDao.insertAll(match { it.size == 1 && it[0].name == "Food" }) }
        coVerify { expenseDao.insertAll(match { it.size == 1 && it[0].amountCents == 1250L }) }
        coVerify { incomeDao.insertAll(match { it.size == 1 && it[0].source == "Salary" }) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import rejects unsupported schema version`() = runTest {
        val badJson = """{"schemaVersion":99,"exportedAt":"2026-01-01T00:00:00Z","categories":[],"expenses":[],"income":[],"recurringExpenses":[],"recurringExpenseGenerations":[],"recurringIncomeGenerations":[]}"""
        useCase.importFromJson(badJson)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import rejects expense with non-existent category`() = runTest {
        val badJson = """
        {
            "schemaVersion": 1,
            "exportedAt": "2026-01-01T00:00:00Z",
            "categories": [{"id": 1, "name": "Food", "icon": null, "isDefault": true}],
            "expenses": [{"id": 1, "amountCents": 100, "categoryId": 999, "date": "2026-01-01", "note": null, "recurringExpenseId": null}],
            "income": [],
            "recurringExpenses": [],
            "recurringExpenseGenerations": [],
            "recurringIncomeGenerations": []
        }
        """.trimIndent()
        useCase.importFromJson(badJson)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import rejects recurring expense generation with non-existent expense`() = runTest {
        val badJson = """
        {
            "schemaVersion": 1,
            "exportedAt": "2026-01-01T00:00:00Z",
            "categories": [{"id": 1, "name": "Food", "icon": null, "isDefault": true}],
            "expenses": [],
            "income": [],
            "recurringExpenses": [{"id": 1, "amountCents": 100, "categoryId": 1, "interval": "MONTHLY", "note": null, "startDate": "2026-01-01"}],
            "recurringExpenseGenerations": [{"id": 1, "recurringExpenseId": 1, "generatedForMonth": "2026-01", "expenseId": 999}],
            "recurringIncomeGenerations": []
        }
        """.trimIndent()
        useCase.importFromJson(badJson)
    }

    @Test
    fun `import deletes all existing data before inserting`() = runTest {
        setupDaosWithSampleData()
        val exported = useCase.exportToJson()

        useCase.importFromJson(exported)

        // Verify delete order: generations first
        coVerify(ordering = Ordering.ORDERED) {
            recurringExpenseGenerationDao.deleteAll()
            recurringIncomeGenerationDao.deleteAll()
            expenseDao.deleteAll()
            incomeDao.deleteAll()
            recurringExpenseDao.deleteAll()
            categoryDao.deleteAll()
        }
    }
}
