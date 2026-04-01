package com.example.expensetracker.domain.usecase

import androidx.room.withTransaction
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import com.example.expensetracker.data.db.entity.RecurringIncomeGenerationEntity
import com.example.expensetracker.domain.model.AppExportData
import com.example.expensetracker.domain.model.ExportCategory
import com.example.expensetracker.domain.model.ExportExpense
import com.example.expensetracker.domain.model.ExportIncome
import com.example.expensetracker.domain.model.ExportRecurringExpense
import com.example.expensetracker.domain.model.ExportRecurringExpenseGeneration
import com.example.expensetracker.domain.model.ExportRecurringIncomeGeneration
import com.example.expensetracker.domain.model.Interval
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportImportJsonUseCase @Inject constructor(
    private val database: AppDatabase,
) {
    var transactionRunner: suspend (suspend () -> Unit) -> Unit = { block ->
        database.withTransaction { block() }
    }

    private val json = Json { prettyPrint = true }

    suspend fun exportToJson(): String {
        val categories = database.categoryDao().getAllSuspend()
        val expenses = database.expenseDao().getAllSuspend()
        val income = database.incomeDao().getAllSuspend()
        val recurringExpenses = database.recurringExpenseDao().getAllSuspend()
        val recurringExpenseGenerations = database.recurringExpenseGenerationDao().getAllSuspend()
        val recurringIncomeGenerations = database.recurringIncomeGenerationDao().getAllSuspend()

        val exportData = AppExportData(
            schemaVersion = SCHEMA_VERSION,
            exportedAt = Instant.now().toString(),
            categories = categories.map { it.toExport() },
            expenses = expenses.map { it.toExport() },
            income = income.map { it.toExport() },
            recurringExpenses = recurringExpenses.map { it.toExport() },
            recurringExpenseGenerations = recurringExpenseGenerations.map { it.toExport() },
            recurringIncomeGenerations = recurringIncomeGenerations.map { it.toExport() },
        )

        return json.encodeToString(AppExportData.serializer(), exportData)
    }

    suspend fun importFromJson(jsonString: String) {
        val data = json.decodeFromString(AppExportData.serializer(), jsonString)

        require(data.schemaVersion == SCHEMA_VERSION) {
            "Unsupported schema version ${data.schemaVersion}. Expected $SCHEMA_VERSION."
        }

        validateForeignKeys(data)

        val categories = data.categories.map { it.toEntity() }
        val expenses = data.expenses.map { it.toEntity() }
        val income = data.income.map { it.toEntity() }
        val recurringExpenses = data.recurringExpenses.map { it.toEntity() }
        val recurringExpenseGenerations = data.recurringExpenseGenerations.map { it.toEntity() }
        val recurringIncomeGenerations = data.recurringIncomeGenerations.map { it.toEntity() }

        transactionRunner {
            // Delete in FK-safe order: generations first, then dependents, then categories
            database.recurringExpenseGenerationDao().deleteAll()
            database.recurringIncomeGenerationDao().deleteAll()
            database.expenseDao().deleteAll()
            database.incomeDao().deleteAll()
            database.recurringExpenseDao().deleteAll()
            database.categoryDao().deleteAll()

            // Insert in FK-safe order: categories first, then main tables, then generations
            database.categoryDao().insertAll(categories)
            database.recurringExpenseDao().insertAll(recurringExpenses)
            database.expenseDao().insertAll(expenses)
            database.incomeDao().insertAll(income)
            database.recurringExpenseGenerationDao().insertAll(recurringExpenseGenerations)
            database.recurringIncomeGenerationDao().insertAll(recurringIncomeGenerations)
        }
    }

    private fun validateForeignKeys(data: AppExportData) {
        val categoryIds = data.categories.map { it.id }.toSet()
        val expenseIds = data.expenses.map { it.id }.toSet()
        val incomeIds = data.income.map { it.id }.toSet()
        val recurringExpenseIds = data.recurringExpenses.map { it.id }.toSet()

        for (expense in data.expenses) {
            require(expense.categoryId in categoryIds) {
                "Expense ${expense.id} references non-existent category ${expense.categoryId}"
            }
            expense.recurringExpenseId?.let { recurringId ->
                require(recurringId in recurringExpenseIds) {
                    "Expense ${expense.id} references non-existent recurring expense $recurringId"
                }
            }
        }

        for (recurringExpense in data.recurringExpenses) {
            require(recurringExpense.categoryId in categoryIds) {
                "Recurring expense ${recurringExpense.id} references non-existent category ${recurringExpense.categoryId}"
            }
        }

        for (generation in data.recurringExpenseGenerations) {
            require(generation.recurringExpenseId in recurringExpenseIds) {
                "Recurring expense generation ${generation.id} references non-existent recurring expense ${generation.recurringExpenseId}"
            }
            require(generation.expenseId in expenseIds) {
                "Recurring expense generation ${generation.id} references non-existent expense ${generation.expenseId}"
            }
        }

        for (generation in data.recurringIncomeGenerations) {
            require(generation.recurringIncomeId in incomeIds) {
                "Recurring income generation ${generation.id} references non-existent income ${generation.recurringIncomeId}"
            }
            require(generation.incomeId in incomeIds) {
                "Recurring income generation ${generation.id} references non-existent income ${generation.incomeId}"
            }
        }
    }

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

private fun CategoryEntity.toExport() = ExportCategory(
    id = id, name = name, icon = icon, isDefault = isDefault,
)

private fun ExpenseEntity.toExport() = ExportExpense(
    id = id, amountCents = amountCents, categoryId = categoryId,
    date = date.toString(), note = note, recurringExpenseId = recurringExpenseId,
)

private fun IncomeEntity.toExport() = ExportIncome(
    id = id, amountCents = amountCents, source = source,
    date = date.toString(), note = note, isRecurring = isRecurring,
    recurrenceInterval = recurrenceInterval?.name, startDate = startDate,
    recurringIncomeId = recurringIncomeId,
)

private fun RecurringExpenseEntity.toExport() = ExportRecurringExpense(
    id = id, amountCents = amountCents, categoryId = categoryId,
    interval = interval.name, note = note, startDate = startDate,
)

private fun RecurringExpenseGenerationEntity.toExport() = ExportRecurringExpenseGeneration(
    id = id, recurringExpenseId = recurringExpenseId,
    generatedForMonth = generatedForMonth, expenseId = expenseId,
)

private fun RecurringIncomeGenerationEntity.toExport() = ExportRecurringIncomeGeneration(
    id = id, recurringIncomeId = recurringIncomeId,
    generatedForMonth = generatedForMonth, incomeId = incomeId,
)

private fun ExportCategory.toEntity() = CategoryEntity(
    id = id, name = name, icon = icon, isDefault = isDefault,
)

private fun ExportExpense.toEntity() = ExpenseEntity(
    id = id, amountCents = amountCents, categoryId = categoryId,
    date = LocalDate.parse(date), note = note, recurringExpenseId = recurringExpenseId,
)

private fun ExportIncome.toEntity() = IncomeEntity(
    id = id, amountCents = amountCents, source = source,
    date = LocalDate.parse(date), note = note, isRecurring = isRecurring,
    recurrenceInterval = recurrenceInterval?.let { Interval.valueOf(it) },
    startDate = startDate, recurringIncomeId = recurringIncomeId,
)

private fun ExportRecurringExpense.toEntity() = RecurringExpenseEntity(
    id = id, amountCents = amountCents, categoryId = categoryId,
    interval = Interval.valueOf(interval), note = note, startDate = startDate,
)

private fun ExportRecurringExpenseGeneration.toEntity() = RecurringExpenseGenerationEntity(
    id = id, recurringExpenseId = recurringExpenseId,
    generatedForMonth = generatedForMonth, expenseId = expenseId,
)

private fun ExportRecurringIncomeGeneration.toEntity() = RecurringIncomeGenerationEntity(
    id = id, recurringIncomeId = recurringIncomeId,
    generatedForMonth = generatedForMonth, incomeId = incomeId,
)
