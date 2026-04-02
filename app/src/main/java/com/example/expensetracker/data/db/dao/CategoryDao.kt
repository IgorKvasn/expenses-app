package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("""
        SELECT c.* FROM categories c
        LEFT JOIN expenses e ON c.id = e.categoryId
        GROUP BY c.id
        ORDER BY COUNT(e.id) DESC, c.name ASC
    """)
    fun getAllOrderedByExpenseUsage(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllSuspend(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
