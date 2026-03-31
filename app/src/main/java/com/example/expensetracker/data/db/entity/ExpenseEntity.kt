package com.example.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = RecurringExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringExpenseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["recurringExpenseId"]),
        Index(value = ["date"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val categoryId: Long,
    val date: LocalDate,
    val note: String? = null,
    val recurringExpenseId: Long? = null,
)
