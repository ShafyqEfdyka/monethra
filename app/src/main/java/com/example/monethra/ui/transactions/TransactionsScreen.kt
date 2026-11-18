package com.example.monethra.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monethra.MonethraApplication
import com.example.monethra.data.entity.TransactionEntity
import com.example.monethra.ui.theme.DustyPink
import com.example.monethra.ui.theme.ExpenseRed
import com.example.monethra.ui.theme.IncomeGreen
import com.example.monethra.ui.theme.MonethraTheme
import com.example.monethra.ui.theme.PowderBlue
import com.example.monethra.ui.theme.PrimaryText
import com.example.monethra.ui.theme.SageGreen
import com.example.monethra.ui.theme.SecondaryText
import com.example.monethra.ui.theme.WarmIvory
import com.example.monethra.utils.CurrencyFormatter
import com.example.monethra.utils.DateUtils
import com.example.monethra.viewmodel.TransactionUiState
import com.example.monethra.viewmodel.TransactionViewModel
import com.example.monethra.viewmodel.TransactionViewModelFactory

@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    initialTypeFilter: String? = null,
    initialAutoOpenAdd: Boolean = false
) {
    val context = LocalContext.current
    val app = context.applicationContext as MonethraApplication
    val transactionViewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModelFactory(app.financeRepository)
    )

    val uiState by transactionViewModel.uiState.collectAsState()

    var hasHandledAutoOpen by rememberSaveable(initialAutoOpenAdd, initialTypeFilter) {
        mutableStateOf(false)
    }

    LaunchedEffect(initialTypeFilter, initialAutoOpenAdd) {
        if (!hasHandledAutoOpen) {
            if (!initialTypeFilter.isNullOrBlank() && initialTypeFilter in listOf("INCOME", "EXPENSE")) {
                transactionViewModel.onFilterChange(initialTypeFilter)
            }
            if (initialAutoOpenAdd) {
                val preselected = if (!initialTypeFilter.isNullOrBlank() && initialTypeFilter in listOf("INCOME", "EXPENSE")) {
                    initialTypeFilter
                } else {
                    "EXPENSE"
                }
                transactionViewModel.openAddTransactionSheet(preselectedType = preselected)
            }
            hasHandledAutoOpen = true
        }
    }

    TransactionsContent(
        uiState = uiState,
        onSearchQueryChange = transactionViewModel::onSearchQueryChange,
        onFilterChange = transactionViewModel::onFilterChange,
        onAddClick = {
            val preselected = if (uiState.typeFilter in listOf("INCOME", "EXPENSE")) uiState.typeFilter else "EXPENSE"
            transactionViewModel.openAddTransactionSheet(preselected)
        },
        onEditClick = transactionViewModel::openEditTransactionSheet,
        onDeleteClick = transactionViewModel::openDeleteConfirmation,
        onDismissAddEditSheet = transactionViewModel::closeAddEditSheet,
        onSaveTransaction = { title, amount, type, category, date, notes ->
            val selectedTx = uiState.selectedTransaction
            if (selectedTx != null) {
                transactionViewModel.updateTransaction(
                    selectedTx.copy(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        date = date,
                        notes = notes
                    )
                )
            } else {
                transactionViewModel.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date,
                    notes = notes
                )
            }
        },
        onDismissDeleteDialog = transactionViewModel::closeDeleteConfirmation,
        onConfirmDelete = {
            uiState.transactionToDelete?.let { tx ->
                transactionViewModel.deleteTransaction(tx)
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsContent(
    uiState: TransactionUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (TransactionEntity) -> Unit,
    onDeleteClick: (TransactionEntity) -> Unit,
    onDismissAddEditSheet: () -> Unit,
    onSaveTransaction: (title: String, amount: Long, type: String, category: String, date: Long, notes: String?) -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = PowderBlue,
                contentColor = PrimaryText,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Header
                Text(
                    text = "Transactions",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                )

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search transactions...", color = SecondaryText) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = SecondaryText
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                    tint = SecondaryText
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = PowderBlue,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("ALL" to "All", "INCOME" to "Income", "EXPENSE" to "Expenses")
                    filters.forEach { (key, label) ->
                        val selected = uiState.typeFilter.equals(key, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(key) },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PowderBlue,
                                selectedLabelColor = PrimaryText,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = SecondaryText
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transactions List or Empty State
                if (uiState.filteredTransactions.isEmpty()) {
                    EmptyTransactionsState(
                        searchQuery = uiState.searchQuery,
                        typeFilter = uiState.typeFilter,
                        onAddClick = onAddClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = uiState.filteredTransactions,
                            key = { it.id }
                        ) { tx ->
                            TransactionListItem(
                                transaction = tx,
                                currencySymbol = uiState.currencySymbol,
                                onEditClick = { onEditClick(tx) },
                                onDeleteClick = { onDeleteClick(tx) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }

        // Add / Edit Transaction Sheet
        if (uiState.isAddEditSheetOpen) {
            val preselectedType = if (uiState.typeFilter in listOf("INCOME", "EXPENSE")) uiState.typeFilter else "EXPENSE"
            AddEditTransactionSheet(
                transaction = uiState.selectedTransaction,
                initialTransactionType = preselectedType,
                defaultType = preselectedType,
                onDismiss = onDismissAddEditSheet,
                onSave = onSaveTransaction,
                currencySymbol = uiState.currencySymbol,
                errorMessage = uiState.errorMessage
            )
        }

        // Delete Confirmation Dialog
        if (uiState.isDeleteConfirmationOpen && uiState.transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = onDismissDeleteDialog,
                title = {
                    Text(
                        text = "Delete Transaction?",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete \"${uiState.transactionToDelete.title}\"? This action cannot be undone.",
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onConfirmDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDeleteDialog) {
                        Text("Cancel", color = PrimaryText)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val icon = CategoryData.getIconForCategory(transaction.category)
    val bgColor = CategoryData.getColorForCategory(transaction.category)
    val amountPrefix = if (isIncome) "+" else "-"
    val amountColor = if (isIncome) IncomeGreen else PrimaryText

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = transaction.category,
                        tint = PrimaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.category} • ${DateUtils.formatDate(transaction.date, "dd MMM yyyy")}",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$amountPrefix${CurrencyFormatter.formatIdr(transaction.amount, currencySymbol)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = SecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete",
                        tint = ExpenseRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTransactionsState(
    searchQuery: String,
    typeFilter: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(WarmIvory),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val message = when {
                searchQuery.isNotEmpty() -> "No transactions match \"$searchQuery\""
                typeFilter != "ALL" -> "No ${typeFilter.lowercase()} transactions found"
                else -> "No transactions recorded yet"
            }

            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tap below to record a new transaction",
                fontSize = 13.sp,
                color = SecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PowderBlue,
                    contentColor = PrimaryText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Transaction", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview
@Composable
fun TransactionsContentPreview() {
    MonethraTheme {
        TransactionsContent(
            uiState = TransactionUiState(
                filteredTransactions = listOf(
                    TransactionEntity(1L, "Grocery Shopping", 350000L, "EXPENSE", "Food & Drinks", System.currentTimeMillis()),
                    TransactionEntity(2L, "Monthly Salary", 7500000L, "INCOME", "Salary", System.currentTimeMillis())
                )
            ),
            onSearchQueryChange = {},
            onFilterChange = {},
            onAddClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onDismissAddEditSheet = {},
            onSaveTransaction = { _, _, _, _, _, _ -> },
            onDismissDeleteDialog = {},
            onConfirmDelete = {}
        )
    }
}
