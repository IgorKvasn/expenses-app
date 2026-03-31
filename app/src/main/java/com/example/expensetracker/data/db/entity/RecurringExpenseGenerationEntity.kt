package com.example.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_expense_generations",
    foreignKeys = [
        ForeignKey(
            entity = RecurringExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringExpenseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["recurringExpenseId", "generatedForMonth"], unique = true),
        Index(value = ["expenseId"]),
    ],
)
data class RecurringExpenseGenerationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recurringExpenseId: Long,
    val generatedForMonth: String,
    val expenseId: Long,
)
