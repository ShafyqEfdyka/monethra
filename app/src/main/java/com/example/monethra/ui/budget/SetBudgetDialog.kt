package com.example.monethra.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monethra.ui.transactions.CategoryData
import com.example.monethra.ui.theme.ExpenseRed
import com.example.monethra.ui.theme.PowderBlue
import com.example.monethra.ui.theme.PrimaryText
import com.example.monethra.ui.theme.SecondaryText
import com.example.monethra.ui.theme.WarmIvory
import com.example.monethra.utils.CurrencyFormatter
import com.example.monethra.utils.LocalCurrencyConfig
import com.example.monethra.utils.ThousandSeparatorVisualTransformation
import com.example.monethra.viewmodel.CategoryAllocationUiModel

@Composable
fun SetTotalBudgetDialog(
    currentTotalBudget: Long,
    currencySymbol: String = "Rp",
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val currencyConfig = LocalCurrencyConfig.current
    val rateFromIdr = currencyConfig.rateFromIdr
    val activeSymbol = currencyConfig.symbol.ifBlank { currencySymbol }

    val initialDisplayBudget = if (currentTotalBudget > 0) {
        val converted = if (rateFromIdr > 0.0 && rateFromIdr != 1.0) {
            kotlin.math.round(CurrencyFormatter.convertFromIdr(currentTotalBudget, rateFromIdr)).toLong()
        } else {
            currentTotalBudget
        }
        converted.toString()
    } else ""
    var amountText by remember(currentTotalBudget, rateFromIdr) { mutableStateOf(initialDisplayBudget) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Monthly Budget",
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter your overall spending limit for this month.",
                    fontSize = 13.sp,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.let {
                            if (it.length > 1 && it.startsWith("0")) it.trimStart('0').ifEmpty { "0" } else it
                        }
                        amountText = digits
                        localError = null
                    },
                    label = { Text("Total Monthly Budget") },
                    prefix = { Text("$activeSymbol ") },
                    visualTransformation = ThousandSeparatorVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                val activeError = localError ?: errorMessage
                if (activeError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeError,
                        fontSize = 12.sp,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.toLongOrNull() ?: CurrencyFormatter.parseAmount(amountText)
                    if (parsed == null || parsed < 0) {
                        localError = "Please enter a valid amount"
                        return@Button
                    }
                    val amountInIdr = if (rateFromIdr > 0.0 && rateFromIdr != 1.0) {
                        CurrencyFormatter.convertToIdr(parsed.toDouble(), rateFromIdr)
                    } else {
                        parsed
                    }
                    onSave(amountInIdr)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PowderBlue,
                    contentColor = PrimaryText
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryText)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditCategoryBudgetDialog(
    allocationToEdit: CategoryAllocationUiModel? = null,
    currencySymbol: String = "Rp",
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (categoryName: String, amount: Long) -> Unit,
    onDelete: ((categoryName: String) -> Unit)? = null
) {
    val isEditing = allocationToEdit != null
    val currencyConfig = LocalCurrencyConfig.current
    val rateFromIdr = currencyConfig.rateFromIdr
    val activeSymbol = currencyConfig.symbol.ifBlank { currencySymbol }

    var categoryName by remember { mutableStateOf(allocationToEdit?.categoryName ?: "Food & Drinks") }
    val initialDisplayAllocated = allocationToEdit?.allocatedAmount?.let { amt ->
        if (rateFromIdr > 0.0 && rateFromIdr != 1.0) {
            kotlin.math.round(CurrencyFormatter.convertFromIdr(amt, rateFromIdr)).toLong()
        } else {
            amt
        }
    }?.toString() ?: ""
    var amountText by remember(allocationToEdit, rateFromIdr) { mutableStateOf(initialDisplayAllocated) }
    var localError by remember { mutableStateOf<String?>(null) }

    val presetCategories = CategoryData.expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Category Budget" else "Add Category Budget",
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        },
        text = {
            Column {
                Text(
                    text = "Select a category and set its monthly allocation limit.",
                    fontSize = 13.sp,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Preset categories selection
                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCategories.forEach { cat ->
                        val isSelected = categoryName.equals(cat.name, ignoreCase = true)
                        val borderModifier = if (isSelected) {
                            Modifier.border(2.dp, PrimaryText, RoundedCornerShape(10.dp))
                        } else {
                            Modifier
                        }

                        Row(
                            modifier = Modifier
                                .then(borderModifier)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) cat.color else WarmIvory)
                                .clickable {
                                    categoryName = cat.name
                                    localError = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.name,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cat.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom category name if needed or selected
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {
                        categoryName = it
                        localError = null
                    },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.let {
                            if (it.length > 1 && it.startsWith("0")) it.trimStart('0').ifEmpty { "0" } else it
                        }
                        amountText = digits
                        localError = null
                    },
                    label = { Text("Allocated Budget Amount") },
                    prefix = { Text("$activeSymbol ") },
                    visualTransformation = ThousandSeparatorVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                val activeError = localError ?: errorMessage
                if (activeError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeError,
                        fontSize = 12.sp,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing && onDelete != null && allocationToEdit != null) {
                    OutlinedButton(
                        onClick = { onDelete(allocationToEdit.categoryName) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val parsed = amountText.toLongOrNull() ?: CurrencyFormatter.parseAmount(amountText)
                        if (categoryName.isBlank()) {
                            localError = "Category name is required"
                            return@Button
                        }
                        if (parsed == null || parsed <= 0) {
                            localError = "Amount must be greater than 0"
                            return@Button
                        }
                        val amountInIdr = if (rateFromIdr > 0.0 && rateFromIdr != 1.0) {
                            CurrencyFormatter.convertToIdr(parsed.toDouble(), rateFromIdr)
                        } else {
                            parsed
                        }
                        onSave(categoryName, amountInIdr)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PowderBlue,
                        contentColor = PrimaryText
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryText)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
