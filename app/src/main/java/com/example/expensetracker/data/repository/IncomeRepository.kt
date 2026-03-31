package com.example.expensetracker.data.repository

import com.example.expensetracker.data.db.dao.IncomeDao
import com.example.expensetracker.data.db.entity.IncomeEntity
import com.example.expensetracker.domain.model.IncomeFilter
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao,
) {
    fun getFiltered(filter: IncomeFilter): Flow<List<IncomeEntity>> =
        incomeDao.getFiltered(
            sourceSearch = filter.sourceSearch,
            dateFrom = filter.dateFrom?.toString(),
            dateTo = filter.dateTo?.toString(),
            amountMin = filter.amountMinCents,
            amountMax = filter.amountMaxCents,
            noteSearch = filter.noteSearch,
            sortOrder = filter.sortOrder.name,
        )

    suspend fun getById(id: Long): IncomeEntity? = incomeDao.getById(id)

    suspend fun getTotalInRange(from: LocalDate, to: LocalDate): Long =
        incomeDao.getTotalInRange(from.toString(), to.toString()) ?: 0L

    suspend fun insert(income: IncomeEntity): Long = incomeDao.insert(income)

    suspend fun update(income: IncomeEntity) = incomeDao.update(income)

    suspend fun delete(income: IncomeEntity) = incomeDao.delete(income)
}
