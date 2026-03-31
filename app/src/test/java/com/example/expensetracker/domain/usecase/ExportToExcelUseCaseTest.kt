package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import kotlinx.coroutines.test.runTest
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate

class ExportToExcelUseCaseTest {

    private val useCase = ExportToExcelUseCase()

    @Test
    fun `generates xlsx with expense and income sheets`() = runTest {
        val expenses = listOf(
            ExpenseEntity(id = 1, amountCents = 1250, categoryId = 1, date = LocalDate.of(2026, 3, 15), note = "Lunch"),
        )
        val income = listOf(
            IncomeEntity(id = 1, amountCents = 300000, source = "Salary", date = LocalDate.of(2026, 3, 1)),
        )
        val categories = mapOf(1L to CategoryEntity(id = 1, name = "Food"))

        val file = File.createTempFile("test_export", ".xlsx")
        try {
            useCase(expenses, income, categories, file)

            val workbook = XSSFWorkbook(FileInputStream(file))
            assertEquals(2, workbook.numberOfSheets)
            assertEquals("Expenses", workbook.getSheetAt(0).sheetName)
            assertEquals("Income", workbook.getSheetAt(1).sheetName)

            val expenseSheet = workbook.getSheetAt(0)
            assertEquals("Date", expenseSheet.getRow(0).getCell(0).stringCellValue)
            assertEquals("2026-03-15", expenseSheet.getRow(1).getCell(0).stringCellValue)
            assertEquals(12.50, expenseSheet.getRow(1).getCell(1).numericCellValue, 0.01)
            assertEquals("Food", expenseSheet.getRow(1).getCell(2).stringCellValue)

            val incomeSheet = workbook.getSheetAt(1)
            assertEquals("Salary", incomeSheet.getRow(1).getCell(2).stringCellValue)

            workbook.close()
        } finally {
            file.delete()
        }
    }
}
