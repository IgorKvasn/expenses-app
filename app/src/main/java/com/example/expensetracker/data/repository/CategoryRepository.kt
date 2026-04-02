package com.example.expensetracker.data.repository

import com.example.expensetracker.data.db.dao.CategoryDao
import com.example.expensetracker.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun getAll(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    fun getAllOrderedByExpenseUsage(): Flow<List<CategoryEntity>> =
        categoryDao.getAllOrderedByExpenseUsage()

    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun insert(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun update(category: CategoryEntity) = categoryDao.update(category)

    suspend fun delete(category: CategoryEntity) = categoryDao.delete(category)
}
