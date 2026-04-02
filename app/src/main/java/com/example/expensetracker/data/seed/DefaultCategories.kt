package com.example.expensetracker.data.seed

import com.example.expensetracker.data.db.entity.CategoryEntity

val defaultCategories = listOf(
    CategoryEntity(name = "Housing", icon = "home", isDefault = true),
    CategoryEntity(name = "Groceries", icon = "shopping_cart", isDefault = true),
    CategoryEntity(name = "Restaurant", icon = "restaurant", isDefault = true),
    CategoryEntity(name = "Transport", icon = "directions_car", isDefault = true),
    CategoryEntity(name = "Utilities", icon = "bolt", isDefault = true),
    CategoryEntity(name = "Medical", icon = "local_hospital", isDefault = true),
    CategoryEntity(name = "Entertainment", icon = "movie", isDefault = true),
    CategoryEntity(name = "Clothing", icon = "checkroom", isDefault = true),
    CategoryEntity(name = "Education", icon = "school", isDefault = true),
    CategoryEntity(name = "Fun", icon = "celebration", isDefault = true),
    CategoryEntity(name = "Loan", icon = "account_balance", isDefault = true),
    CategoryEntity(name = "Savings & Investments", icon = "savings", isDefault = true),
    CategoryEntity(name = "Other", icon = "more_horiz", isDefault = true),
)
