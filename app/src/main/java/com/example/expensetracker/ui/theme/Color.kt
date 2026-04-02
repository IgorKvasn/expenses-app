package com.example.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

// Primary blue palette
val PrimaryLight = Color(0xFF1565C0)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD4E3FF)
val OnPrimaryContainerLight = Color(0xFF001B3D)

val PrimaryDark = Color(0xFF9ECAFF)
val OnPrimaryDark = Color(0xFF003062)
val PrimaryContainerDark = Color(0xFF00468A)
val OnPrimaryContainerDark = Color(0xFFD4E3FF)

// Secondary slate-blue palette
val SecondaryLight = Color(0xFF555F71)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD9E3F8)
val OnSecondaryContainerLight = Color(0xFF121C2B)

val SecondaryDark = Color(0xFFBDC7DC)
val OnSecondaryDark = Color(0xFF273141)
val SecondaryContainerDark = Color(0xFF3E4858)
val OnSecondaryContainerDark = Color(0xFFD9E3F8)

// Tertiary accent (warm indigo)
val TertiaryLight = Color(0xFF6E5676)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFF8D8FF)
val OnTertiaryContainerLight = Color(0xFF271430)

val TertiaryDark = Color(0xFFDBBDE2)
val OnTertiaryDark = Color(0xFF3E2846)
val TertiaryContainerDark = Color(0xFF553F5D)
val OnTertiaryContainerDark = Color(0xFFF8D8FF)

// Surfaces
val SurfaceLight = Color(0xFFF8F9FF)
val OnSurfaceLight = Color(0xFF191C20)
val SurfaceVariantLight = Color(0xFFDFE2EB)
val OnSurfaceVariantLight = Color(0xFF43474E)
val SurfaceContainerLight = Color(0xFFEDEFF5)
val SurfaceContainerHighLight = Color(0xFFE7E9F0)

val SurfaceDark = Color(0xFF111318)
val OnSurfaceDark = Color(0xFFE1E2E9)
val SurfaceVariantDark = Color(0xFF43474E)
val OnSurfaceVariantDark = Color(0xFFC3C6CF)
val SurfaceContainerDark = Color(0xFF1D1F24)
val SurfaceContainerHighDark = Color(0xFF282A2F)

// Error
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)

// Semantic colors for income/expense
val IncomeGreen = Color(0xFF2E7D32)
val IncomeGreenLight = Color(0xFFE8F5E9)
val IncomeGreenDark = Color(0xFF66BB6A)
val IncomeGreenDarkContainer = Color(0xFF1B3A1D)

val ExpenseRed = Color(0xFFC62828)
val ExpenseRedLight = Color(0xFFFFEBEE)
val ExpenseRedDark = Color(0xFFEF5350)
val ExpenseRedDarkContainer = Color(0xFF3D1214)

// Fixed colors for category bar chart
val CategoryColors = mapOf(
    "Housing" to Color(0xFF1565C0),        // blue
    "Groceries" to Color(0xFF2E7D32),      // green
    "Restaurant" to Color(0xFFE65100),     // orange
    "Transport" to Color(0xFF6A1B9A),      // purple
    "Utilities" to Color(0xFFF9A825),      // amber
    "Medical" to Color(0xFFC62828),        // red
    "Entertainment" to Color(0xFF00838F),  // teal
    "Clothing" to Color(0xFFAD1457),       // pink
    "Education" to Color(0xFF283593),      // indigo
    "Fun" to Color(0xFFFF6F00),            // deep orange
    "Loan" to Color(0xFF4E342E),           // brown
    "Savings & Investments" to Color(0xFF00695C), // dark teal
    "Other" to Color(0xFF546E7A),          // blue grey
)
val CategoryColorFallback = Color(0xFF78909C)
