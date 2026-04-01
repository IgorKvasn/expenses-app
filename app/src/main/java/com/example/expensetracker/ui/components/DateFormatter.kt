package com.example.expensetracker.ui.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateFormatter {
    private val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")

    fun format(date: LocalDate): String = date.format(formatter)

    fun format(isoDate: String): String = format(LocalDate.parse(isoDate))
}
