package com.example.expensetracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_income_generations",
    foreignKeys = [
        ForeignKey(
            entity = IncomeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringIncomeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IncomeEntity::class,
            parentColumns = ["id"],
            childColumns = ["incomeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["recurringIncomeId", "generatedForMonth"], unique = true),
        Index(value = ["incomeId"]),
    ],
)
data class RecurringIncomeGenerationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recurringIncomeId: Long,
    val generatedForMonth: String,
    val incomeId: Long,
)
