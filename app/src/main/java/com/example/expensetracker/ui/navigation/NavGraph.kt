package com.example.expensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensetracker.ui.expenses.ExpenseListScreen
import com.example.expensetracker.ui.expenses.AddEditExpenseScreen
import com.example.expensetracker.ui.income.IncomeListScreen
import com.example.expensetracker.ui.income.AddEditIncomeScreen
import com.example.expensetracker.ui.recurring.RecurringListScreen
import com.example.expensetracker.ui.recurring.AddEditRecurringExpenseScreen
import com.example.expensetracker.ui.recurring.AddEditRecurringIncomeScreen
import com.example.expensetracker.ui.reports.ReportsScreen
import com.example.expensetracker.ui.categories.CategoryManagementScreen
import com.example.expensetracker.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(navigateToAddExpense: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val mainScreenRoutes = bottomNavItems.map { it.screen.route } + drawerNavItems.map { it.screen.route }
    val isMainScreen = currentRoute in mainScreenRoutes
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (navigateToAddExpense) {
        LaunchedEffect(Unit) {
            navController.navigate(Screen.AddEditExpense.createRoute())
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Expense Tracker",
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                )
                drawerNavItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != item.screen.route) {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.ExpenseList.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                if (isMainScreen) {
                    TopAppBar(
                        title = {
                            val label = bottomNavItems.find { it.screen.route == currentRoute }?.label
                                ?: drawerNavItems.find { it.screen.route == currentRoute }?.label
                                ?: ""
                            Text(label)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        },
                    )
                }
            },
            bottomBar = { BottomNavBar(navController) },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.ExpenseList.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(animationSpec = tween(350)) },
                exitTransition = { fadeOut(animationSpec = tween(350)) },
                popEnterTransition = { fadeIn(animationSpec = tween(350)) },
                popExitTransition = { fadeOut(animationSpec = tween(350)) },
            ) {
                composable(Screen.ExpenseList.route) {
                    ExpenseListScreen(
                        onAddExpense = { navController.navigate(Screen.AddEditExpense.createRoute()) },
                        onEditExpense = { id -> navController.navigate(Screen.AddEditExpense.createRoute(id)) },
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
                        onAddRecurringIncome = { navController.navigate(Screen.AddEditRecurringIncome.createRoute()) },
                        onEditRecurringIncome = { id -> navController.navigate(Screen.AddEditRecurringIncome.createRoute(id)) },
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
                composable(
                    route = Screen.AddEditRecurringIncome.route,
                    arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("id")?.takeIf { it != -1L }
                    AddEditRecurringIncomeScreen(
                        recurringIncomeId = id,
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
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }
    }
}
