package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.expensetracker.data.db.entity.RecurringExpenseGenerationEntity

@Dao
interface RecurringExpenseGenerationDao {
    @Query("""
        SELECT * FROM recurring_expense_generations
        WHERE recurringExpenseId = :recurringExpenseId
    """)
    suspend fun getByRecurringExpenseId(recurringExpenseId: Long): List<RecurringExpenseGenerationEntity>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM recurring_expense_generations
            WHERE recurringExpenseId = :recurringExpenseId AND generatedForMonth = :month
        )
    """)
    suspend fun existsForMonth(recurringExpenseId: Long, month: String): Boolean

    @Insert
    suspend fun insert(generation: RecurringExpenseGenerationEntity): Long
}
