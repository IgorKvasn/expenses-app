package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.expensetracker.data.db.entity.RecurringIncomeGenerationEntity

@Dao
interface RecurringIncomeGenerationDao {
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM recurring_income_generations
            WHERE recurringIncomeId = :recurringIncomeId AND generatedForMonth = :month
        )
    """)
    suspend fun existsForMonth(recurringIncomeId: Long, month: String): Boolean

    @Insert
    suspend fun insert(generation: RecurringIncomeGenerationEntity): Long

    @Query("SELECT * FROM recurring_income_generations")
    suspend fun getAllSuspend(): List<RecurringIncomeGenerationEntity>

    @Insert
    suspend fun insertAll(generations: List<RecurringIncomeGenerationEntity>)

    @Query("DELETE FROM recurring_income_generations")
    suspend fun deleteAll()
}
