package com.example.expensetracker.data.repository

import com.example.expensetracker.data.db.dao.RecurringExpenseDao
import com.example.expensetracker.data.db.dao.RecurringExpenseGenerationDao
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseRepository @Inject constructor(
    private val recurringExpenseDao: RecurringExpenseDao,
    private val generationDao: RecurringExpenseGenerationDao,
) {
    fun getAll(): Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.getAll()

    suspend fun getActive(): List<RecurringExpenseEntity> = recurringExpenseDao.getActive()

    suspend fun getById(id: Long): RecurringExpenseEntity? = recurringExpenseDao.getById(id)

    suspend fun insert(recurringExpense: RecurringExpenseEntity): Long =
        recurringExpenseDao.insert(recurringExpense)

    suspend fun update(recurringExpense: RecurringExpenseEntity) =
        recurringExpenseDao.update(recurringExpense)

    suspend fun delete(recurringExpense: RecurringExpenseEntity) =
        recurringExpenseDao.delete(recurringExpense)

    suspend fun isGeneratedForMonth(recurringExpenseId: Long, month: String): Boolean =
        generationDao.existsForMonth(recurringExpenseId, month)

    suspend fun recordGeneration(generation: RecurringExpenseGenerationEntity): Long =
        generationDao.insert(generation)
}
