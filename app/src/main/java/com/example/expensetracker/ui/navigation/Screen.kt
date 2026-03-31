package com.example.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object ExpenseList : Screen("expenses")
    data object AddEditExpense : Screen("expenses/edit?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "expenses/edit?id=$id" else "expenses/edit"
    }
    data object IncomeList : Screen("income")
    data object AddEditIncome : Screen("income/edit?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "income/edit?id=$id" else "income/edit"
    }
    data object RecurringList : Screen("recurring")
    data object AddEditRecurringExpense : Screen("recurring/edit?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "recurring/edit?id=$id" else "recurring/edit"
    }
    data object Reports : Screen("reports")
    data object CategoryManagement : Screen("categories")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.ExpenseList, "Expenses", Icons.Filled.AccountBalanceWallet),
    BottomNavItem(Screen.IncomeList, "Income", Icons.Filled.AttachMoney),
    BottomNavItem(Screen.RecurringList, "Recurring", Icons.Filled.Repeat),
    BottomNavItem(Screen.Reports, "Reports", Icons.Filled.Assessment),
)
