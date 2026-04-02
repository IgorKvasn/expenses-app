package com.example.expensetracker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppExportData(
    val schemaVersion: Int,
    val exportedAt: String,
    val categories: List<ExportCategory>,
    val expenses: List<ExportExpense>,
    val income: List<ExportIncome>,
    val recurringExpenses: List<ExportRecurringExpense>,
    val recurringExpenseGenerations: List<ExportRecurringExpenseGeneration>,
    val recurringIncomeGenerations: List<ExportRecurringIncomeGeneration>,
)

@Serializable
data class ExportCategory(
    val id: Long,
    val name: String,
    val icon: String? = null,
    val isDefault: Boolean,
)

@Serializable
data class ExportExpense(
    val id: Long,
    val amountCents: Long,
    val categoryId: Long,
    val date: String,
    val note: String? = null,
    val recurringExpenseId: Long? = null,
    val createdAt: String? = null,
)

@Serializable
data class ExportIncome(
    val id: Long,
    val amountCents: Long,
    val source: String,
    val date: String,
    val note: String? = null,
    val isRecurring: Boolean,
    val recurrenceInterval: String? = null,
    val startDate: String? = null,
    val recurringIncomeId: Long? = null,
    val createdAt: String? = null,
)

@Serializable
data class ExportRecurringExpense(
    val id: Long,
    val amountCents: Long,
    val categoryId: Long,
    val interval: String,
    val note: String? = null,
    val startDate: String,
)

@Serializable
data class ExportRecurringExpenseGeneration(
    val id: Long,
    val recurringExpenseId: Long,
    val generatedForMonth: String,
    val expenseId: Long,
)

@Serializable
data class ExportRecurringIncomeGeneration(
    val id: Long,
    val recurringIncomeId: Long,
    val generatedForMonth: String,
    val incomeId: Long,
)
