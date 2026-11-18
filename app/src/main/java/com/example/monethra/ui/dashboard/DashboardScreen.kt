package com.example.monethra.ui.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.LocalActivity
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monethra.MonethraApplication
import com.example.monethra.data.entity.TransactionEntity
import com.example.monethra.ui.components.DonutChart
import com.example.monethra.ui.theme.ButterCream
import com.example.monethra.ui.theme.DustyPink
import com.example.monethra.ui.theme.ExpenseRed
import com.example.monethra.ui.theme.IncomeGreen
import com.example.monethra.ui.theme.PowderBlue
import com.example.monethra.ui.theme.PrimaryText
import com.example.monethra.ui.theme.SageGreen
import com.example.monethra.ui.theme.SecondaryText
import com.example.monethra.ui.theme.SoftLavender
import com.example.monethra.ui.theme.WarmIvory
import com.example.monethra.ui.transactions.AddEditTransactionSheet
import com.example.monethra.utils.CurrencyFormatter
import com.example.monethra.utils.DateUtils
import com.example.monethra.viewmodel.DashboardUiState
import com.example.monethra.viewmodel.DashboardViewModel
import com.example.monethra.viewmodel.DashboardViewModelFactory
import com.example.monethra.viewmodel.TransactionViewModel
import com.example.monethra.viewmodel.TransactionViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddIncomeClick: (() -> Unit)? = null,
    onAddExpenseClick: (() -> Unit)? = null,
    onSetBudgetClick: () -> Unit = {},
    onSeeAllTransactionsClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MonethraApplication
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(app.financeRepository)
    )
    val transactionViewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModelFactory(app.financeRepository)
    )

    val uiState by dashboardViewModel.uiState.collectAsState()

    var isAddSheetOpen by remember { mutableStateOf(false) }
    var selectedTransactionType by remember { mutableStateOf("EXPENSE") }

    val handleAddIncome: () -> Unit = {
        if (onAddIncomeClick != null) {
            onAddIncomeClick()
        } else {
            selectedTransactionType = "INCOME"
            isAddSheetOpen = true
        }
    }

    val handleAddExpense: () -> Unit = {
        if (onAddExpenseClick != null) {
            onAddExpenseClick()
        } else {
            selectedTransactionType = "EXPENSE"
            isAddSheetOpen = true
        }
    }

    DashboardContent(
        uiState = uiState,
        onAddIncomeClick = handleAddIncome,
        onAddExpenseClick = handleAddExpense,
        onSetBudgetClick = onSetBudgetClick,
        onSeeAllTransactionsClick = onSeeAllTransactionsClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onDismissPaydayBanner = { dashboardViewModel.dismissPaydayBanner() },
        modifier = modifier
    )

    if (isAddSheetOpen) {
        AddEditTransactionSheet(
            initialTransactionType = selectedTransactionType,
            defaultType = selectedTransactionType,
            onDismiss = { isAddSheetOpen = false },
            onSave = { title, amount, type, category, date, notes ->
                transactionViewModel.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = date,
                    notes = notes
                )
                isAddSheetOpen = false
            },
            currencySymbol = uiState.currencySymbol.ifBlank { "Rp" }
        )
    }
}

@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onSetBudgetClick: () -> Unit,
    onSeeAllTransactionsClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDismissPaydayBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isTablet = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header
            HeaderSection(
                userName = uiState.userName,
                onNotificationClick = onNotificationClick,
                onProfileClick = onProfileClick
            )

            if (uiState.showPaydayBanner) {
                PaydayBannerCard(
                    message = uiState.paydayBannerMessage ?: "New Payday Cycle Started!",
                    cycleLabel = uiState.paydayCycleLabel,
                    onDismiss = onDismissPaydayBanner,
                    modifier = Modifier.padding(horizontal = if (isTablet) 24.dp else 20.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isTablet) {
                // Tablet Layout: 2 Columns
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FinancialSummaryGrid(uiState = uiState)
                        QuickActionsSection(
                            onAddIncomeClick = onAddIncomeClick,
                            onAddExpenseClick = onAddExpenseClick,
                            onSetBudgetClick = onSetBudgetClick
                        )
                        BudgetProgressCard(uiState = uiState, onSetBudgetClick = onSetBudgetClick)
                    }

                    // Right Column
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ExpenseOverviewCard(uiState = uiState)
                        RecentTransactionsCard(
                            recentTransactions = uiState.recentTransactions,
                            currencySymbol = uiState.currencySymbol,
                            onSeeAllClick = onSeeAllTransactionsClick
                        )
                    }
                }
            } else {
                // Phone Layout: Single Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FinancialSummaryGrid(uiState = uiState)
                    QuickActionsSection(
                        onAddIncomeClick = onAddIncomeClick,
                        onAddExpenseClick = onAddExpenseClick,
                        onSetBudgetClick = onSetBudgetClick
                    )
                    ExpenseOverviewCard(uiState = uiState)
                    BudgetProgressCard(uiState = uiState, onSetBudgetClick = onSetBudgetClick)
                    RecentTransactionsCard(
                        recentTransactions = uiState.recentTransactions,
                        currencySymbol = uiState.currencySymbol,
                        onSeeAllClick = onSeeAllTransactionsClick
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    userName: String,
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val greeting = rememberGreeting()
    val formattedDate = rememberFormattedDate()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = "$greeting, $userName! 👋",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formattedDate,
            fontSize = 13.sp,
            color = SecondaryText
        )
    }
}

@Composable
fun FinancialSummaryGrid(
    uiState: DashboardUiState
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Total Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = PowderBlue
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total Balance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryText
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = PrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = CurrencyFormatter.formatIdr(uiState.totalBalance, uiState.currencySymbol),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }
        }

        // 3 Sub Cards: Income, Expense, Remaining Budget
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Income
            SummaryItemCard(
                title = "Monthly Income",
                amount = CurrencyFormatter.formatIdr(uiState.monthlyIncome, uiState.currencySymbol),
                icon = Icons.Rounded.ArrowDownward,
                iconColor = IncomeGreen,
                backgroundColor = SageGreen,
                modifier = Modifier.weight(1f)
            )

            // Expenses
            SummaryItemCard(
                title = "Monthly Expenses",
                amount = CurrencyFormatter.formatIdr(uiState.monthlyExpenses, uiState.currencySymbol),
                icon = Icons.Rounded.ArrowUpward,
                iconColor = ExpenseRed,
                backgroundColor = DustyPink,
                modifier = Modifier.weight(1f)
            )

            // Budget
            SummaryItemCard(
                title = "Remaining Budget",
                amount = CurrencyFormatter.formatIdr(uiState.remainingBudget, uiState.currencySymbol),
                icon = Icons.Rounded.Tune,
                iconColor = PrimaryText,
                backgroundColor = ButterCream,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SummaryItemCard(
    title: String,
    amount: String,
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SecondaryText,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Clip
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onSetBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Actions",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    label = "Income",
                    icon = Icons.Rounded.Add,
                    color = SageGreen,
                    onClick = onAddIncomeClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Expense",
                    icon = Icons.Rounded.Remove,
                    color = DustyPink,
                    onClick = onAddExpenseClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "Set Budget",
                    icon = Icons.Rounded.Tune,
                    color = SoftLavender,
                    onClick = onSetBudgetClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PrimaryText,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ExpenseOverviewCard(
    uiState: DashboardUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Expense Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(16.dp))
            DonutChart(
                slices = uiState.categoryExpenses,
                totalAmount = uiState.monthlyExpenses,
                currencySymbol = uiState.currencySymbol
            )
        }
    }
}

@Composable
fun BudgetProgressCard(
    uiState: DashboardUiState,
    onSetBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Budget Progress",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                TextButton(onClick = onSetBudgetClick) {
                    Text("Manage", fontSize = 12.sp, color = SecondaryText)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val spentText = CurrencyFormatter.formatIdr(uiState.monthlyExpenses, uiState.currencySymbol)
            val budgetText = CurrencyFormatter.formatIdr(uiState.monthlyBudget, uiState.currencySymbol)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$spentText spent",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                Text(
                    text = "of $budgetText",
                    fontSize = 13.sp,
                    color = SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val progressColor = if (uiState.budgetUsagePercentage > 0.9f) ExpenseRed else IncomeGreen

            LinearProgressIndicator(
                progress = { uiState.budgetUsagePercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = WarmIvory,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            val percentText = String.format(Locale.forLanguageTag("id-ID"), "%.1f", uiState.budgetUsagePercentage * 100)
            Text(
                text = "$percentText% of total budget used",
                fontSize = 12.sp,
                color = SecondaryText
            )
        }
    }
}

@Composable
fun RecentTransactionsCard(
    recentTransactions: List<TransactionEntity>,
    currencySymbol: String,
    onSeeAllClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                TextButton(onClick = onSeeAllClick) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("See All", fontSize = 13.sp, color = SecondaryText)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = SecondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recentTransactions.isEmpty()) {
                Text(
                    text = "No recent transactions found.",
                    fontSize = 13.sp,
                    color = SecondaryText,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentTransactions.forEach { tx ->
                        RecentTransactionItem(
                            transaction = tx,
                            currencySymbol = currencySymbol
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionItem(
    transaction: TransactionEntity,
    currencySymbol: String
) {
    val isIncome = transaction.type == "INCOME"
    val icon = getCategoryIcon(transaction.category)
    val bgColor = if (isIncome) SageGreen else WarmIvory
    val amountPrefix = if (isIncome) "+" else "-"
    val amountColor = if (isIncome) IncomeGreen else PrimaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarmIvory)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.category,
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = transaction.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.category} • ${DateUtils.formatDate(transaction.date, "dd MMM")}",
                    fontSize = 12.sp,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = "$amountPrefix${CurrencyFormatter.formatIdr(transaction.amount, currencySymbol)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food & beverages", "food", "f&b" -> Icons.Rounded.Fastfood
        "transportation", "transport" -> Icons.Rounded.DirectionsCar
        "shopping" -> Icons.Rounded.ShoppingBag
        "bills & utilities", "bills", "utilities" -> Icons.Rounded.Receipt
        "entertainment" -> Icons.Rounded.LocalActivity
        "salary", "side income", "income" -> Icons.Rounded.MonetizationOn
        else -> Icons.Rounded.Category
    }
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

@Composable
private fun rememberFormattedDate(): String {
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
    return sdf.format(Date())
}

@Composable
fun PaydayBannerCard(
    message: String,
    cycleLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    if (cycleLabel.isNotBlank()) {
                        Text(
                            text = "Current cycle: $cycleLabel",
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                    }
                }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss Banner",
                    tint = SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
