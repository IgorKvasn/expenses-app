package com.example.expensetracker.domain.model

enum class Interval(val months: Int, val displayName: String) {
    MONTHLY(1, "Monthly"),
    QUARTERLY(3, "Quarterly"),
    HALF_YEARLY(6, "Half-yearly"),
}
