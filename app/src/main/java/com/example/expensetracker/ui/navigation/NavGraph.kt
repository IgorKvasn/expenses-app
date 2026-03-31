package com.example.expensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensetracker.ui.expenses.ExpenseListScreen
import com.example.expensetracker.ui.expenses.AddEditExpenseScreen
import com.example.expensetracker.ui.income.IncomeListScreen
import com.example.expensetracker.ui.income.AddEditIncomeScreen
import com.example.expensetracker.ui.recurring.RecurringListScreen
import com.example.expensetracker.ui.recurring.AddEditRecurringExpenseScreen
import com.example.expensetracker.ui.reports.ReportsScreen
import com.example.expensetracker.ui.categories.CategoryManagementScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ExpenseList.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.ExpenseList.route) {
                ExpenseListScreen(
                    onAddExpense = { navController.navigate(Screen.AddEditExpense.createRoute()) },
                    onEditExpense = { id -> navController.navigate(Screen.AddEditExpense.createRoute(id)) },
                    onManageCategories = { navController.navigate(Screen.CategoryManagement.route) },
                )
            }
            composable(
                route = Screen.AddEditExpense.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id")?.takeIf { it != -1L }
                AddEditExpenseScreen(
                    expenseId = id,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.IncomeList.route) {
                IncomeListScreen(
                    onAddIncome = { navController.navigate(Screen.AddEditIncome.createRoute()) },
                    onEditIncome = { id -> navController.navigate(Screen.AddEditIncome.createRoute(id)) },
                )
            }
            composable(
                route = Screen.AddEditIncome.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id")?.takeIf { it != -1L }
                AddEditIncomeScreen(
                    incomeId = id,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.RecurringList.route) {
                RecurringListScreen(
                    onAddRecurring = { navController.navigate(Screen.AddEditRecurringExpense.createRoute()) },
                    onEditRecurring = { id -> navController.navigate(Screen.AddEditRecurringExpense.createRoute(id)) },
                )
            }
            composable(
                route = Screen.AddEditRecurringExpense.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id")?.takeIf { it != -1L }
                AddEditRecurringExpenseScreen(
                    recurringExpenseId = id,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen()
            }
            composable(Screen.CategoryManagement.route) {
                CategoryManagementScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
