package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.db.entity.CategoryEntity
import com.example.expensetracker.data.db.entity.ExpenseEntity
import com.example.expensetracker.data.db.entity.IncomeEntity
import kotlinx.coroutines.test.runTest
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        withTempFile { file ->
            useCase(expenses, income, categories, file)

            readWorkbook(file) { workbook ->
                assertEquals(2, workbook.numberOfSheets)
                assertEquals("Expenses", workbook.getSheetAt(0).sheetName)
                assertEquals("Income", workbook.getSheetAt(1).sheetName)

                val expenseSheet = workbook.getSheetAt(0)
                assertEquals("Date", expenseSheet.getRow(0).getCell(0).stringCellValue)
                assertEquals("15 Mar 2026", expenseSheet.getRow(1).getCell(0).stringCellValue)
                assertEquals(12.50, expenseSheet.getRow(1).getCell(1).numericCellValue, 0.01)
                assertEquals("Food", expenseSheet.getRow(1).getCell(2).stringCellValue)

                val incomeSheet = workbook.getSheetAt(1)
                assertEquals("Salary", incomeSheet.getRow(1).getCell(2).stringCellValue)
            }
        }
    }

    @Test
    fun `empty lists produce header-only sheets`() = runTest {
        withTempFile { file ->
            useCase(emptyList(), emptyList(), emptyMap(), file)

            readWorkbook(file) { workbook ->
                val expenseSheet = workbook.getSheetAt(0)
                val incomeSheet = workbook.getSheetAt(1)

                assertEquals(0, expenseSheet.lastRowNum)
                assertEquals(0, incomeSheet.lastRowNum)
                assertEquals("Date", expenseSheet.getRow(0).getCell(0).stringCellValue)
                assertEquals("Date", incomeSheet.getRow(0).getCell(0).stringCellValue)
            }
        }
    }

    @Test
    fun `multiple expenses are written in order`() = runTest {
        val expenses = listOf(
            ExpenseEntity(id = 1, amountCents = 1000, categoryId = 1, date = LocalDate.of(2026, 1, 10)),
            ExpenseEntity(id = 2, amountCents = 2500, categoryId = 2, date = LocalDate.of(2026, 1, 20)),
            ExpenseEntity(id = 3, amountCents = 500, categoryId = 1, date = LocalDate.of(2026, 2, 5)),
        )
        val categories = mapOf(
            1L to CategoryEntity(id = 1, name = "Food"),
            2L to CategoryEntity(id = 2, name = "Transport"),
        )

        withTempFile { file ->
            useCase(expenses, emptyList(), categories, file)

            readWorkbook(file) { workbook ->
                val sheet = workbook.getSheetAt(0)
                assertEquals(3, sheet.lastRowNum)
                assertEquals(10.0, sheet.getRow(1).getCell(1).numericCellValue, 0.01)
                assertEquals(25.0, sheet.getRow(2).getCell(1).numericCellValue, 0.01)
                assertEquals(5.0, sheet.getRow(3).getCell(1).numericCellValue, 0.01)
                assertEquals("Food", sheet.getRow(1).getCell(2).stringCellValue)
                assertEquals("Transport", sheet.getRow(2).getCell(2).stringCellValue)
            }
        }
    }

    @Test
    fun `unknown category shows Unknown`() = runTest {
        val expenses = listOf(
            ExpenseEntity(id = 1, amountCents = 1000, categoryId = 99, date = LocalDate.of(2026, 1, 1)),
        )

        withTempFile { file ->
            useCase(expenses, emptyList(), emptyMap(), file)

            readWorkbook(file) { workbook ->
                val sheet = workbook.getSheetAt(0)
                assertEquals("Unknown", sheet.getRow(1).getCell(2).stringCellValue)
            }
        }
    }

    @Test
    fun `null notes are written as empty strings`() = runTest {
        val expenses = listOf(
            ExpenseEntity(id = 1, amountCents = 1000, categoryId = 1, date = LocalDate.of(2026, 1, 1), note = null),
        )
        val income = listOf(
            IncomeEntity(id = 1, amountCents = 5000, source = "Gift", date = LocalDate.of(2026, 1, 1), note = null),
        )
        val categories = mapOf(1L to CategoryEntity(id = 1, name = "Food"))

        withTempFile { file ->
            useCase(expenses, income, categories, file)

            readWorkbook(file) { workbook ->
                assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(3).stringCellValue)
                assertEquals("", workbook.getSheetAt(1).getRow(1).getCell(3).stringCellValue)
            }
        }
    }

    @Test
    fun `income sheet contains all columns`() = runTest {
        val income = listOf(
            IncomeEntity(id = 1, amountCents = 150000, source = "Freelance", date = LocalDate.of(2026, 6, 15), note = "June invoice"),
        )

        withTempFile { file ->
            useCase(emptyList(), income, emptyMap(), file)

            readWorkbook(file) { workbook ->
                val sheet = workbook.getSheetAt(1)
                val headerRow = sheet.getRow(0)
                assertEquals("Date", headerRow.getCell(0).stringCellValue)
                assertEquals("Amount (€)", headerRow.getCell(1).stringCellValue)
                assertEquals("Source", headerRow.getCell(2).stringCellValue)
                assertEquals("Note", headerRow.getCell(3).stringCellValue)

                val dataRow = sheet.getRow(1)
                assertEquals("15 Jun 2026", dataRow.getCell(0).stringCellValue)
                assertEquals(1500.0, dataRow.getCell(1).numericCellValue, 0.01)
                assertEquals("Freelance", dataRow.getCell(2).stringCellValue)
                assertEquals("June invoice", dataRow.getCell(3).stringCellValue)
            }
        }
    }

    @Test
    fun `expense sheet headers are bold`() = runTest {
        withTempFile { file ->
            useCase(emptyList(), emptyList(), emptyMap(), file)

            readWorkbook(file) { workbook ->
                val headerCell = workbook.getSheetAt(0).getRow(0).getCell(0)
                val fontIndex = headerCell.cellStyle.fontIndex
                val font = workbook.getFontAt(fontIndex)
                assertTrue(font.bold)
            }
        }
    }

    @Test
    fun `output file is a valid xlsx`() = runTest {
        withTempFile { file ->
            useCase(emptyList(), emptyList(), emptyMap(), file)
            assertTrue(file.length() > 0)
            readWorkbook(file) { workbook ->
                assertEquals(2, workbook.numberOfSheets)
            }
        }
    }

    private fun withTempFile(block: (File) -> Unit) {
        val file = File.createTempFile("test_export", ".xlsx")
        try {
            block(file)
        } finally {
            file.delete()
        }
    }

    private fun readWorkbook(file: File, block: (XSSFWorkbook) -> Unit) {
        val workbook = XSSFWorkbook(FileInputStream(file))
        try {
            block(workbook)
        } finally {
            workbook.close()
        }
    }
}
