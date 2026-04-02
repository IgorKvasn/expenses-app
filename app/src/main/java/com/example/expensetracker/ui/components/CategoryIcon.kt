package com.example.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private val iconMap = mapOf(
    "home" to Icons.Filled.Home,
    "shopping_cart" to Icons.Filled.ShoppingCart,
    "restaurant" to Icons.Filled.Restaurant,
    "directions_car" to Icons.Filled.DirectionsCar,
    "bolt" to Icons.Filled.Bolt,
    "local_hospital" to Icons.Filled.LocalHospital,
    "movie" to Icons.Filled.Movie,
    "checkroom" to Icons.Filled.Checkroom,
    "school" to Icons.Filled.School,
    "celebration" to Icons.Filled.Celebration,
    "account_balance" to Icons.Filled.AccountBalance,
    "savings" to Icons.Filled.Savings,
    "more_horiz" to Icons.Filled.MoreHoriz,
)

fun categoryIconFor(iconName: String?): ImageVector =
    iconName?.let { iconMap[it] } ?: Icons.Filled.MoreHoriz

@Composable
fun CategoryIcon(
    iconName: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = categoryIconFor(iconName),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
