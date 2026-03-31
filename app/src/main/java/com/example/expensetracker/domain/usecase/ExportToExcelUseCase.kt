package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
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

    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        style.setFont(font)
        return style
    }

    private fun writeExpenseSheet(
        workbook: XSSFWorkbook,
        headerStyle: CellStyle,
        expenses: List<ExpenseEntity>,
        categories: Map<Long, CategoryEntity>,
    ) {
        val sheet = workbook.createSheet("Expenses")
        val headers = listOf("Date", "Amount (€)", "Category", "Note")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, header ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        expenses.forEachIndexed { index, expense ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(expense.date.toString())
            row.createCell(1).setCellValue(expense.amountCents / 100.0)
            row.createCell(2).setCellValue(categories[expense.categoryId]?.name ?: "Unknown")
            row.createCell(3).setCellValue(expense.note ?: "")
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }

    private fun writeIncomeSheet(
        workbook: XSSFWorkbook,
        headerStyle: CellStyle,
        income: List<IncomeEntity>,
    ) {
        val sheet = workbook.createSheet("Income")
        val headers = listOf("Date", "Amount (€)", "Source", "Note")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, header ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        income.forEachIndexed { index, item ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(item.date.toString())
            row.createCell(1).setCellValue(item.amountCents / 100.0)
            row.createCell(2).setCellValue(item.source)
            row.createCell(3).setCellValue(item.note ?: "")
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }
    }
}
