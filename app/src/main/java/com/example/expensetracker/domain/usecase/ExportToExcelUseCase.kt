package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.example.expensetracker.ui.components.DateFormatter
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ExportToExcelUseCase @Inject constructor() {

    operator fun invoke(
        expenses: List<ExpenseEntity>,
        income: List<IncomeEntity>,
        categories: Map<Long, CategoryEntity>,
        outputFile: File,
    ) {
        val workbook = XSSFWorkbook()
        val headerStyle = createHeaderStyle(workbook)

        writeExpenseSheet(workbook, headerStyle, expenses, categories)
        writeIncomeSheet(workbook, headerStyle, income)

        FileOutputStream(outputFile).use { workbook.write(it) }
        workbook.close()
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        style.setFont(font)
        return style
    }

    private fun writeExpenseSheet(
        workbook: XSSFWorkbook,
        headerStyle: XSSFCellStyle,
        expenses: List<ExpenseEntity>,
        categories: Map<Long, CategoryEntity>,
    ) {
        val sheet = workbook.createSheet("Expenses")
        val headers = listOf("Date", "Amount (€)", "Category", "Note", "Recurring", "Created")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, header ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        expenses.forEachIndexed { index, expense ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(DateFormatter.format(expense.date))
            row.createCell(1).setCellValue(expense.amountCents / 100.0)
            row.createCell(2).setCellValue(categories[expense.categoryId]?.name ?: "Unknown")
            row.createCell(3).setCellValue(expense.note ?: "")
            row.createCell(4).setCellValue(if (expense.recurringExpenseId != null) "Yes" else "No")
            row.createCell(5).setCellValue(expense.createdAt.toString())
        }

        sheet.setColumnWidth(0, 12 * 256)
        sheet.setColumnWidth(1, 12 * 256)
        sheet.setColumnWidth(2, 20 * 256)
        sheet.setColumnWidth(3, 30 * 256)
        sheet.setColumnWidth(4, 10 * 256)
        sheet.setColumnWidth(5, 22 * 256)
    }

    private fun writeIncomeSheet(
        workbook: XSSFWorkbook,
        headerStyle: XSSFCellStyle,
        income: List<IncomeEntity>,
    ) {
        val sheet = workbook.createSheet("Income")
        val headers = listOf("Date", "Amount (€)", "Source", "Note", "Recurring", "Interval", "Created")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, header ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        income.forEachIndexed { index, item ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(DateFormatter.format(item.date))
            row.createCell(1).setCellValue(item.amountCents / 100.0)
            row.createCell(2).setCellValue(item.source)
            row.createCell(3).setCellValue(item.note ?: "")
            row.createCell(4).setCellValue(if (item.recurringIncomeId != null) "Yes" else "No")
            row.createCell(5).setCellValue(item.recurrenceInterval?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "")
            row.createCell(6).setCellValue(item.createdAt.toString())
        }

        sheet.setColumnWidth(0, 12 * 256)
        sheet.setColumnWidth(1, 12 * 256)
        sheet.setColumnWidth(2, 20 * 256)
        sheet.setColumnWidth(3, 30 * 256)
        sheet.setColumnWidth(4, 10 * 256)
        sheet.setColumnWidth(5, 14 * 256)
        sheet.setColumnWidth(6, 22 * 256)
    }
}
