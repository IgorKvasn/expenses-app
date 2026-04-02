package com.example.expensetracker.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetracker.data.db.dao.CategoryDao
import com.example.expensetracker.data.db.dao.ExpenseDao
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CategoryDeleteRestrictionTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        categoryDao = db.categoryDao()
        expenseDao = db.expenseDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun deletingDefaultCategoryWithExpensesIsRejected() = runTest {
        val categoryId = categoryDao.insert(
            CategoryEntity(name = "Food", icon = "restaurant", isDefault = true),
        )
        expenseDao.insert(expense(categoryId))

        val threw = runCatching { categoryDao.delete(categoryDao.getById(categoryId)!!) }.isFailure
        assertTrue("Expected RESTRICT to prevent deletion", threw)

        val expenses = expenseDao.getAllSuspend()
        assertEquals(1, expenses.size)
        assertEquals(categoryId, expenses[0].categoryId)
    }

    @Test
    fun deletingCustomCategoryWithExpensesIsRejected() = runTest {
        val categoryId = categoryDao.insert(
            CategoryEntity(name = "My Custom", isDefault = false),
        )
        expenseDao.insert(expense(categoryId))
        expenseDao.insert(expense(categoryId, amountCents = 5000))

        val threw = runCatching { categoryDao.delete(categoryDao.getById(categoryId)!!) }.isFailure
        assertTrue("Expected RESTRICT to prevent deletion", threw)

        val expenses = expenseDao.getAllSuspend()
        assertEquals(2, expenses.size)
        assertTrue(expenses.all { it.categoryId == categoryId })
    }

    @Test
    fun expensesRetainCategoryAfterFailedDeletion() = runTest {
        val categoryId = categoryDao.insert(
            CategoryEntity(name = "Subscriptions", isDefault = false),
        )
        val expenseId = expenseDao.insert(expense(categoryId, note = "Netflix"))

        runCatching { categoryDao.delete(categoryDao.getById(categoryId)!!) }

        val loaded = expenseDao.getById(expenseId)!!
        assertEquals(categoryId, loaded.categoryId)
        assertEquals("Netflix", loaded.note)
    }

    @Test
    fun categoryWithExpensesStillAppearsInFilteredList() = runTest {
        val foodId = categoryDao.insert(CategoryEntity(name = "Food", isDefault = true))
        val customId = categoryDao.insert(CategoryEntity(name = "Hobbies", isDefault = false))
        expenseDao.insert(expense(foodId, amountCents = 1000))
        expenseDao.insert(expense(customId, amountCents = 2000))
        expenseDao.insert(expense(customId, amountCents = 3000))

        runCatching { categoryDao.delete(categoryDao.getById(customId)!!) }

        val filtered = expenseDao.getFiltered(
            categoryId = customId,
            dateFrom = null,
            dateTo = null,
            amountMin = null,
            amountMax = null,
            search = null,
            sortOrder = "DATE_DESC",
        ).first()

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.categoryId == customId })
    }

    @Test
    fun categoryTotalsIncludeCategoryAfterFailedDeletion() = runTest {
        val foodId = categoryDao.insert(CategoryEntity(name = "Food", icon = "restaurant", isDefault = true))
        val customId = categoryDao.insert(CategoryEntity(name = "Gym", isDefault = false))
        val date = LocalDate.of(2026, 4, 1)
        expenseDao.insert(expense(foodId, amountCents = 1000, date = date))
        expenseDao.insert(expense(customId, amountCents = 2000, date = date))
        expenseDao.insert(expense(customId, amountCents = 3000, date = date))

        runCatching { categoryDao.delete(categoryDao.getById(customId)!!) }

        val totals = expenseDao.getCategoryTotals("2026-04-01", "2026-04-30")
        assertEquals(2, totals.size)

        val gymTotal = totals.first { it.categoryName == "Gym" }
        assertEquals(5000L, gymTotal.totalCents)
        assertEquals(customId, gymTotal.categoryId)

        val foodTotal = totals.first { it.categoryName == "Food" }
        assertEquals(1000L, foodTotal.totalCents)
    }

    @Test
    fun deletingCategoryWithoutExpensesSucceeds() = runTest {
        val categoryId = categoryDao.insert(
            CategoryEntity(name = "Unused", isDefault = false),
        )

        categoryDao.delete(categoryDao.getById(categoryId)!!)

        val categories = categoryDao.getAllSuspend()
        assertTrue(categories.none { it.id == categoryId })
    }

    @Test
    fun multipleExpensesAcrossCategoriesSurviveFailedDeletion() = runTest {
        val defaultId = categoryDao.insert(CategoryEntity(name = "Transport", icon = "directions_car", isDefault = true))
        val custom1Id = categoryDao.insert(CategoryEntity(name = "Coffee", isDefault = false))
        val custom2Id = categoryDao.insert(CategoryEntity(name = "Books", isDefault = false))

        expenseDao.insert(expense(defaultId, amountCents = 1000))
        expenseDao.insert(expense(custom1Id, amountCents = 2000))
        expenseDao.insert(expense(custom1Id, amountCents = 3000))
        expenseDao.insert(expense(custom2Id, amountCents = 4000))

        runCatching { categoryDao.delete(categoryDao.getById(custom1Id)!!) }
        runCatching { categoryDao.delete(categoryDao.getById(defaultId)!!) }

        val allExpenses = expenseDao.getAllSuspend()
        assertEquals(4, allExpenses.size)
        assertEquals(1, allExpenses.count { it.categoryId == defaultId })
        assertEquals(2, allExpenses.count { it.categoryId == custom1Id })
        assertEquals(1, allExpenses.count { it.categoryId == custom2Id })

        val allCategories = categoryDao.getAllSuspend()
        assertEquals(3, allCategories.size)
    }

    private fun expense(
        categoryId: Long,
        amountCents: Long = 2500,
        date: LocalDate = LocalDate.of(2026, 4, 1),
        note: String? = null,
    ) = ExpenseEntity(
        amountCents = amountCents,
        categoryId = categoryId,
        date = date,
        note = note,
        createdAt = Instant.now(),
    )
}
