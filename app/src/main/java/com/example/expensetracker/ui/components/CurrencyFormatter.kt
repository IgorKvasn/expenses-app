package com.example.expensetracker.ui.components

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val euroFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY).apply {
        currency = java.util.Currency.getInstance("EUR")
    }

    fun format(cents: Long): String = euroFormat.format(cents / 100.0)
}
