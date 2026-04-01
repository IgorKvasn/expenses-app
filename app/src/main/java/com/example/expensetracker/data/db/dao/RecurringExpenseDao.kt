package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.db.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY startDate ASC")
    fun getAll(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getAllSuspend(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getById(id: Long): RecurringExpenseEntity?

    @Insert
    suspend fun insert(recurringExpense: RecurringExpenseEntity): Long

    @Update
    suspend fun update(recurringExpense: RecurringExpenseEntity)

    @Delete
    suspend fun delete(recurringExpense: RecurringExpenseEntity)

    @Insert
    suspend fun insertAll(recurringExpenses: List<RecurringExpenseEntity>)

    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAll()
}
