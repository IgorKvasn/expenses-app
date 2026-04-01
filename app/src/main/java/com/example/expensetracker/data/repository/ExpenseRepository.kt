package com.example.expensetracker.data.repository

import com.example.expensetracker.data.db.dao.CategoryTotal
import com.example.expensetracker.data.db.dao.ExpenseDao
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.domain.model.ExpenseFilter
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
) {
    fun getFiltered(filter: ExpenseFilter): Flow<List<ExpenseEntity>> =
        expenseDao.getFiltered(
            categoryId = filter.categoryId,
            dateFrom = filter.dateFrom?.toString(),
            dateTo = filter.dateTo?.toString(),
            amountMin = filter.amountMinCents,
            amountMax = filter.amountMaxCents,
            search = filter.search,
            sortOrder = filter.sortOrder.name,
        )

    suspend fun getById(id: Long): ExpenseEntity? = expenseDao.getById(id)

    suspend fun getCategoryTotals(from: LocalDate, to: LocalDate): List<CategoryTotal> =
        expenseDao.getCategoryTotals(from.toString(), to.toString())

    suspend fun getTotalInRange(from: LocalDate, to: LocalDate): Long =
        expenseDao.getTotalInRange(from.toString(), to.toString()) ?: 0L

    suspend fun insert(expense: ExpenseEntity): Long = expenseDao.insert(expense)

    suspend fun update(expense: ExpenseEntity) = expenseDao.update(expense)

    suspend fun delete(expense: ExpenseEntity) = expenseDao.delete(expense)
}
