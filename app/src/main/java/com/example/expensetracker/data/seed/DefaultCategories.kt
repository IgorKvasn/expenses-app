package com.example.expensetracker.data.seed

import com.example.expensetracker.data.db.entity.CategoryEntity

val defaultCategories = listOf(
    CategoryEntity(name = "Housing", icon = "home", isDefault = true),
    CategoryEntity(name = "Food & Groceries", icon = "restaurant", isDefault = true),
    CategoryEntity(name = "Transport", icon = "directions_car", isDefault = true),
    CategoryEntity(name = "Utilities", icon = "bolt", isDefault = true),
    CategoryEntity(name = "Healthcare", icon = "local_hospital", isDefault = true),
    CategoryEntity(name = "Entertainment", icon = "movie", isDefault = true),
    CategoryEntity(name = "Clothing", icon = "checkroom", isDefault = true),
    CategoryEntity(name = "Education", icon = "school", isDefault = true),
    CategoryEntity(name = "Savings & Investments", icon = "savings", isDefault = true),
    CategoryEntity(name = "Other", icon = "more_horiz", isDefault = true),
)
