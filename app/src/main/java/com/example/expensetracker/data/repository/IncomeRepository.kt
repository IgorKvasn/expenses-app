package com.example.expensetracker.data.repository

import com.example.expensetracker.data.db.dao.IncomeDao
import com.example.expensetracker.data.db.dao.RecurringIncomeGenerationDao
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.data.db.entity.RecurringIncomeGenerationEntity
import com.example.expensetracker.domain.model.IncomeFilter
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao,
    private val generationDao: RecurringIncomeGenerationDao,
) {
    fun getRecurring(): Flow<List<IncomeEntity>> = incomeDao.getRecurring()

    suspend fun getAllRecurringSuspend(): List<IncomeEntity> = incomeDao.getAllRecurringSuspend()

    fun getFiltered(filter: IncomeFilter): Flow<List<IncomeEntity>> =
        incomeDao.getFiltered(
            isRecurring = filter.isRecurring,
            dateFrom = filter.dateFrom?.toString(),
            dateTo = filter.dateTo?.toString(),
            amountMin = filter.amountMinCents,
            amountMax = filter.amountMaxCents,
            search = filter.search,
            sortOrder = filter.sortOrder.name,
        )

    suspend fun getById(id: Long): IncomeEntity? = incomeDao.getById(id)

    suspend fun getTotalInRange(from: LocalDate, to: LocalDate): Long =
        incomeDao.getTotalInRange(from.toString(), to.toString()) ?: 0L

    suspend fun insert(income: IncomeEntity): Long = incomeDao.insert(income)

    suspend fun update(income: IncomeEntity) = incomeDao.update(income)

    suspend fun delete(income: IncomeEntity) = incomeDao.delete(income)

    suspend fun isGeneratedForMonth(recurringIncomeId: Long, month: String): Boolean =
        generationDao.existsForMonth(recurringIncomeId, month)

    suspend fun recordGeneration(generation: RecurringIncomeGenerationEntity): Long =
        generationDao.insert(generation)
}
