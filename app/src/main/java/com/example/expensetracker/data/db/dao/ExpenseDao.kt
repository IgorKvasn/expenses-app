package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("""
        SELECT * FROM expenses
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:dateFrom IS NULL OR date >= :dateFrom)
          AND (:dateTo IS NULL OR date <= :dateTo)
          AND (:amountMin IS NULL OR amountCents >= :amountMin)
          AND (:amountMax IS NULL OR amountCents <= :amountMax)
          AND (:search IS NULL OR (note LIKE '%' || :search || '%' COLLATE NOCASE
               OR CAST(amountCents AS TEXT) LIKE '%' || :search || '%'))
        ORDER BY
            CASE WHEN :sortOrder = 'DATE_DESC' THEN date END DESC,
            CASE WHEN :sortOrder = 'DATE_ASC' THEN date END ASC,
            CASE WHEN :sortOrder = 'AMOUNT_DESC' THEN amountCents END DESC,
            CASE WHEN :sortOrder = 'AMOUNT_ASC' THEN amountCents END ASC
    """)
    fun getFiltered(
        categoryId: Long?,
        dateFrom: String?,
        dateTo: String?,
        amountMin: Long?,
        amountMax: Long?,
        search: String?,
        sortOrder: String,
    ): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("""
        SELECT e.categoryId, c.name AS categoryName, c.icon AS categoryIcon, SUM(e.amountCents) AS totalCents
        FROM expenses e
        JOIN categories c ON e.categoryId = c.id
        WHERE e.date >= :dateFrom AND e.date <= :dateTo
        GROUP BY e.categoryId
        ORDER BY totalCents DESC
    """)
    suspend fun getCategoryTotals(dateFrom: String, dateTo: String): List<CategoryTotal>

    @Query("""
        SELECT SUM(amountCents) FROM expenses
        WHERE date >= :dateFrom AND date <= :dateTo
    """)
    suspend fun getTotalInRange(dateFrom: String, dateTo: String): Long?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses")
    suspend fun getAllSuspend(): List<ExpenseEntity>

    @Insert
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

data class CategoryTotal(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String?,
    val totalCents: Long,
)
