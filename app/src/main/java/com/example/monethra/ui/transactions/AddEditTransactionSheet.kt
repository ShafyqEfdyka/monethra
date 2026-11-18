package com.example.monethra.ui.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalActivity
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monethra.data.entity.TransactionEntity
import com.example.monethra.ui.theme.ButterCream
import com.example.monethra.ui.theme.DustyPink
import com.example.monethra.ui.theme.ExpenseRed
import com.example.monethra.ui.theme.IncomeGreen
import com.example.monethra.ui.theme.MonethraTheme
import com.example.monethra.ui.theme.PowderBlue
import com.example.monethra.ui.theme.PrimaryText
import com.example.monethra.ui.theme.SageGreen
import com.example.monethra.ui.theme.SecondaryText
import com.example.monethra.ui.theme.SoftLavender
import com.example.monethra.ui.theme.WarmIvory
import com.example.monethra.utils.CurrencyFormatter
import com.example.monethra.utils.DateUtils
import com.example.monethra.utils.LocalCurrencyConfig
import com.example.monethra.utils.ThousandSeparatorVisualTransformation
import java.util.Calendar

data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

object CategoryData {
    val incomeCategories = listOf(
        CategoryItem("Salary", Icons.Rounded.MonetizationOn, SageGreen),
        CategoryItem("Freelance", Icons.Rounded.Work, PowderBlue),
        CategoryItem("Business", Icons.Rounded.Store, ButterCream),
        CategoryItem("Investment", Icons.Rounded.TrendingUp, SoftLavender),
        CategoryItem("Allowance", Icons.Rounded.AccountBalance, SageGreen),
        CategoryItem("Other Income", Icons.Rounded.AttachMoney, WarmIvory)
    )

    val expenseCategories = listOf(
        CategoryItem("Food & Drinks", Icons.Rounded.Fastfood, Color(0xFF81C784)),
        CategoryItem("Transportation", Icons.Rounded.DirectionsCar, Color(0xFF64B5F6)),
        CategoryItem("Shopping", Icons.Rounded.ShoppingBag, Color(0xFFE57373)),
        CategoryItem("Entertainment", Icons.Rounded.LocalActivity, Color(0xFFBA68C8)),
        CategoryItem("Education", Icons.Rounded.School, Color(0xFFAED581)),
        CategoryItem("Health", Icons.Rounded.MedicalServices, Color(0xFF4DD0E1)),
        CategoryItem("Bills & Utilities", Icons.Rounded.Receipt, Color(0xFFFFB74D)),
        CategoryItem("Housing", Icons.Rounded.Home, Color(0xFFFF8A65)),
        CategoryItem("Subscriptions", Icons.Rounded.Subscriptions, Color(0xFFB39DDB)),
        CategoryItem("Other Expenses", Icons.Rounded.MoreHoriz, Color(0xFFCFD8DC))
    )

    fun getIconForCategory(categoryName: String): ImageVector {
        val all = incomeCategories + expenseCategories
        return all.find { it.name.equals(categoryName, ignoreCase = true) }?.icon ?: Icons.Rounded.Category
    }

    fun getColorForCategory(categoryName: String): Color {
        val all = incomeCategories + expenseCategories
        return all.find { it.name.equals(categoryName, ignoreCase = true) }?.color ?: WarmIvory
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    transaction: TransactionEntity? = null,
    initialTransactionType: String = "EXPENSE",
    defaultType: String = initialTransactionType,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Long, type: String, category: String, date: Long, notes: String?) -> Unit,
    currencySymbol: String = "Rp",
    errorMessage: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val effectiveInitialType = transaction?.type
        ?: if (initialTransactionType in listOf("INCOME", "EXPENSE")) initialTransactionType
        else if (defaultType in listOf("INCOME", "EXPENSE")) defaultType
        else "EXPENSE"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AddEditTransactionForm(
            transaction = transaction,
            initialTransactionType = effectiveInitialType,
            defaultType = effectiveInitialType,
            onDismiss = onDismiss,
            onSave = onSave,
            currencySymbol = currencySymbol,
            errorMessage = errorMessage
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionForm(
    transaction: TransactionEntity? = null,
    initialTransactionType: String = "EXPENSE",
    defaultType: String = initialTransactionType,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Long, type: String, category: String, date: Long, notes: String?) -> Unit,
    currencySymbol: String = "Rp",
    errorMessage: String? = null
) {
    val isEditing = transaction != null
    val effectiveType = transaction?.type
        ?: if (initialTransactionType in listOf("INCOME", "EXPENSE")) initialTransactionType
        else if (defaultType in listOf("INCOME", "EXPENSE")) defaultType
        else "EXPENSE"

    val currencyConfig = LocalCurrencyConfig.current
    val rateFromIdr = currencyConfig.rateFromIdr
    val activeSymbol = currencyConfig.symbol.ifBlank { currencySymbol }

    var selectedType by remember(transaction, initialTransactionType) {
        mutableStateOf(effectiveType)
    }
    var title by remember(transaction) { mutableStateOf(transaction?.title ?: "") }
    var amountText by remember(transaction, rateFromIdr) {
        mutableStateOf(
            transaction?.amount?.let { amt ->
                val displayAmt = if (rateFromIdr > 0.0 && rateFromIdr != 1.0) {
                    kotlin.math.round(CurrencyFormatter.convertFromIdr(amt, rateFromIdr)).toLong()
                } else {
                    amt
                }
                displayAmt.toString().filter { c -> c.isDigit() }
            } ?: ""
        )
    }
    var selectedCategory by remember(selectedType, transaction) {
        mutableStateOf(
            if (transaction != null && transaction.type == selectedType) {
                transaction.category
            } else if (selectedType == "INCOME") {
                CategoryData.incomeCategories.first().name
            } else {
                CategoryData.expenseCategories.first().name
            }
        )
    }
    var selectedDate by remember(transaction) { mutableLongStateOf(transaction?.date ?: System.currentTimeMillis()) }
    var notes by remember(transaction) { mutableStateOf(transaction?.notes ?: "") }
    var localError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            selectedDate = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val currentCategories = if (selectedType == "INCOME") {
        CategoryData.incomeCategories
    } else {
        CategoryData.expenseCategories
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Sheet Title
        Text(
            text = if (isEditing) "Edit Transaction" else "Add Transaction",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Type Segmented Switch: Income vs Expense
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(WarmIvory)
                .padding(4.dp)
        ) {
            val isIncome = selectedType == "INCOME"
            val isExpense = selectedType == "EXPENSE"

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isIncome) SageGreen else Color.Transparent)
                    .clickable {
                        selectedType = "INCOME"
                        if (CategoryData.incomeCategories.none { it.name == selectedCategory }) {
                            selectedCategory = CategoryData.incomeCategories.first().name
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Income",
                    fontSize = 14.sp,
                    fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                    color = if (isIncome) IncomeGreen else SecondaryText
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isExpense) DustyPink else Color.Transparent)
                    .clickable {
                        selectedType = "EXPENSE"
                        if (CategoryData.expenseCategories.none { it.name == selectedCategory }) {
                            selectedCategory = CategoryData.expenseCategories.first().name
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Expense",
                    fontSize = 14.sp,
                    fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                    color = if (isExpense) ExpenseRed else SecondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Field
        OutlinedTextField(
            value = amountText,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }.let {
                    if (it.length > 1 && it.startsWith("0")) it.trimStart('0').ifEmpty { "0" } else it
                }
                amountText = digitsOnly
                localError = null
            },
            label = { Text("Amount") },
            prefix = { Text("$activeSymbol ") },
            visualTransformation = ThousandSeparatorVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Title Field
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                localError = null
            },
            label = { Text("Title / Description") },
            placeholder = { Text("e.g. Supermarket Grocery") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category Selector Grid
        Text(
            text = "Category",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currentCategories.forEach { cat ->
                val isSelected = selectedCategory == cat.name
                val borderModifier = if (isSelected) {
                    Modifier.border(2.dp, PrimaryText, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }

                Row(
                    modifier = Modifier
                        .then(borderModifier)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) cat.color else WarmIvory)
                        .clickable {
                            selectedCategory = cat.name
                            localError = null
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = cat.name,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryText
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date Picker Field
        Text(
            text = "Date",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(WarmIvory)
                .clickable { datePickerDialog.show() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = DateUtils.formatDate(selectedDate, "EEEE, dd MMMM yyyy"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryText
            )
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = "Select Date",
                tint = PrimaryText,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)") },
            placeholder = { Text("Add any extra details...") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        // Error message display
        val activeError = localError ?: errorMessage
        if (activeError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = activeError,
                fontSize = 13.sp,
                color = ExpenseRed,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel", color = PrimaryText)
            }

            Button(
                onClick = {
                    val rawDigits = amountText.filter { it.isDigit() }
                    val parsedAmount = rawDigits.toLongOrNull() ?: 0L
                    if (title.isBlank()) {
                        localError = "Title is required"
                        return@Button
                    }
                    if (parsedAmount <= 0L) {
                        localError = "Amount must be greater than 0"
                        return@Button
                    }
                    if (selectedCategory.isBlank()) {
                        localError = "Category is required"
                        return@Button
                    }

                    val amountInIdr = if (rateFromIdr > 0.0 && rateFromIdr != 1.0) {
                        CurrencyFormatter.convertToIdr(parsedAmount.toDouble(), rateFromIdr)
                    } else {
                        parsedAmount
                    }

                    onSave(
                        title.trim(),
                        amountInIdr,
                        selectedType,
                        selectedCategory,
                        selectedDate,
                        notes.trim().ifBlank { null }
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PowderBlue,
                    contentColor = PrimaryText
                )
            ) {
                Text(
                    text = if (isEditing) "Update" else "Save",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview
@Composable
fun AddEditTransactionFormPreview() {
    MonethraTheme {
        Surface {
            AddEditTransactionForm(
                onDismiss = {},
                onSave = { _, _, _, _, _, _ -> }
            )
        }
    }
}
