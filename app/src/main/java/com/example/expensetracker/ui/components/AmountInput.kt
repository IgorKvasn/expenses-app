package com.example.expensetracker.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount (€)",
    errorMessage: String? = null,
    focusRequester: FocusRequester? = null,
) {
    val focusModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Allow only valid decimal input: digits and at most one decimal point with max 2 decimal places
            val filtered = newValue.filter { it.isDigit() || it == '.' }
            val parts = filtered.split(".")
            val isValid = parts.size <= 2 && (parts.size < 2 || parts[1].length <= 2)
            if (isValid) {
                onValueChange(filtered)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it) } },
        modifier = focusModifier,
    )
}

fun amountStringToCents(value: String): Long? {
    val amount = value.toDoubleOrNull() ?: return null
    return (amount * 100).toLong()
}

fun centsToAmountString(cents: Long): String {
    val euros = cents / 100
    val remainingCents = cents % 100
    return if (remainingCents == 0L) "$euros" else "$euros.${"%02d".format(remainingCents)}"
}
