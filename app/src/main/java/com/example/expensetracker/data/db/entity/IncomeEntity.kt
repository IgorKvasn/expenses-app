package com.example.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.expensetracker.domain.model.Interval
import java.time.LocalDate

@Entity(
    tableName = "income",
    indices = [Index(value = ["date"])],
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val source: String,
    val date: LocalDate,
    val note: String? = null,
    val isRecurring: Boolean = false,
    val recurrenceInterval: Interval? = null,
    val startDate: String? = null,
    val recurringIncomeId: Long? = null,
)
