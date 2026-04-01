package com.example.expensetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.db.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("""
        SELECT * FROM income
        WHERE (:isRecurring IS NULL OR isRecurring = :isRecurring)
          AND (:dateFrom IS NULL OR date >= :dateFrom)
          AND (:dateTo IS NULL OR date <= :dateTo)
          AND (:amountMin IS NULL OR amountCents >= :amountMin)
          AND (:amountMax IS NULL OR amountCents <= :amountMax)
          AND (:search IS NULL OR (source LIKE '%' || :search || '%' COLLATE NOCASE
               OR note LIKE '%' || :search || '%' COLLATE NOCASE
               OR CAST(amountCents AS TEXT) LIKE '%' || :search || '%'))
        ORDER BY
            CASE WHEN :sortOrder = 'DATE_DESC' THEN date END DESC,
            CASE WHEN :sortOrder = 'DATE_ASC' THEN date END ASC,
            CASE WHEN :sortOrder = 'AMOUNT_DESC' THEN amountCents END DESC,
            CASE WHEN :sortOrder = 'AMOUNT_ASC' THEN amountCents END ASC
    """)
    fun getFiltered(
        isRecurring: Boolean?,
        dateFrom: String?,
        dateTo: String?,
        amountMin: Long?,
        amountMax: Long?,
        search: String?,
        sortOrder: String,
    ): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income WHERE isRecurring = 1 ORDER BY source ASC")
    fun getRecurring(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income WHERE isRecurring = 1")
    suspend fun getAllRecurringSuspend(): List<IncomeEntity>

    @Query("SELECT * FROM income WHERE id = :id")
    suspend fun getById(id: Long): IncomeEntity?

    @Query("""
        SELECT SUM(amountCents) FROM income
        WHERE date >= :dateFrom AND date <= :dateTo
    """)
    suspend fun getTotalInRange(dateFrom: String, dateTo: String): Long?

    @Insert
    suspend fun insert(income: IncomeEntity): Long

    @Update
    suspend fun update(income: IncomeEntity)

    @Delete
    suspend fun delete(income: IncomeEntity)

    @Query("SELECT * FROM income")
    suspend fun getAllSuspend(): List<IncomeEntity>

    @Insert
    suspend fun insertAll(income: List<IncomeEntity>)

    @Query("DELETE FROM income")
    suspend fun deleteAll()
}
