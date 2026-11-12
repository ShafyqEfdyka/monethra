package com.example.monethra.ui.budget

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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monethra.MonethraApplication
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
import com.example.monethra.ui.transactions.CategoryData
import com.example.monethra.utils.CurrencyFormatter
import com.example.monethra.viewmodel.AlertLevel
import com.example.monethra.viewmodel.BudgetAlert
import com.example.monethra.viewmodel.BudgetUiState
import com.example.monethra.viewmodel.BudgetViewModel
import com.example.monethra.viewmodel.BudgetViewModelFactory
import com.example.monethra.viewmodel.CategoryAllocationUiModel

@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MonethraApplication
    val budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModelFactory(app.financeRepository)
    )

    val uiState by budgetViewModel.uiState.collectAsState()

    BudgetContent(
        uiState = uiState,
        onPreviousMonth = budgetViewModel::previousMonth,
        onNextMonth = budgetViewModel::nextMonth,
        onOpenSetTotalBudgetDialog = budgetViewModel::openSetTotalBudgetDialog,
        onCloseSetTotalBudgetDialog = budgetViewModel::closeSetTotalBudgetDialog,
        onSaveTotalBudget = budgetViewModel::setTotalMonthlyBudget,
        onOpenAddCategoryDialog = { budgetViewModel.openAddCategoryAllocationDialog(null) },
        onOpenEditCategoryDialog = budgetViewModel::openAddCategoryAllocationDialog,
        onCloseCategoryDialog = budgetViewModel::closeAddCategoryAllocationDialog,
        onSaveCategoryAllocation = budgetViewModel::saveCategoryAllocation,
        onDeleteCategoryAllocation = budgetViewModel::deleteCategoryAllocation,
        modifier = modifier
    )
}

@Composable
fun BudgetContent(
    uiState: BudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenSetTotalBudgetDialog: () -> Unit,
    onCloseSetTotalBudgetDialog: () -> Unit,
    onSaveTotalBudget: (Long) -> Unit,
    onOpenAddCategoryDialog: () -> Unit,
    onOpenEditCategoryDialog: (CategoryAllocationUiModel) -> Unit,
    onCloseCategoryDialog: () -> Unit,
    onSaveCategoryAllocation: (String, Long) -> Unit,
    onDeleteCategoryAllocation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth >= 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Title & Month Selector Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget & Limits",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )

                    // Month Selector: < MMMM yyyy >
                    MonthSelectorHeader(
                        monthYearText = uiState.monthYearText,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Overall Budget Summary Card
                BudgetSummaryCard(
                    uiState = uiState,
                    onEditBudgetClick = onOpenSetTotalBudgetDialog
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Alert Warning Banners Section
                if (uiState.budgetAlerts.isNotEmpty()) {
                    BudgetAlertsSection(alerts = uiState.budgetAlerts)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Category Allocations Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Allocations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )

                    Button(
                        onClick = onOpenAddCategoryDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PowderBlue,
                            contentColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Limit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Allocations List
                if (uiState.categoryAllocations.isEmpty()) {
                    EmptyCategoryAllocationsState(onAddClick = onOpenAddCategoryDialog)
                } else {
                    if (isTablet) {
                        // 2 Columns Grid for Tablet
                        val chunked = uiState.categoryAllocations.chunked(2)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunked.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { catAlloc ->
                                        CategoryAllocationItem(
                                            allocation = catAlloc,
                                            currencySymbol = uiState.currencySymbol,
                                            onEditClick = { onOpenEditCategoryDialog(catAlloc) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // Single Column for Mobile
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.categoryAllocations.forEach { catAlloc ->
                                CategoryAllocationItem(
                                    allocation = catAlloc,
                                    currencySymbol = uiState.currencySymbol,
                                    onEditClick = { onOpenEditCategoryDialog(catAlloc) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Dialogs
        if (uiState.isSetTotalBudgetDialogOpen) {
            SetTotalBudgetDialog(
                currentTotalBudget = uiState.totalMonthlyBudget,
                currencySymbol = uiState.currencySymbol,
                errorMessage = uiState.errorMessage,
                onDismiss = onCloseSetTotalBudgetDialog,
                onSave = onSaveTotalBudget
            )
        }

        if (uiState.isAddEditCategoryDialogOpen) {
            AddEditCategoryBudgetDialog(
                allocationToEdit = uiState.selectedCategoryToEdit,
                currencySymbol = uiState.currencySymbol,
                errorMessage = uiState.errorMessage,
                onDismiss = onCloseCategoryDialog,
                onSave = onSaveCategoryAllocation,
                onDelete = onDeleteCategoryAllocation
            )
        }
    }
}

@Composable
fun MonthSelectorHeader(
    monthYearText: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Previous Month",
                tint = PrimaryText
            )
        }

        Text(
            text = monthYearText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = PrimaryText
            )
        }
    }
}

@Composable
fun BudgetSummaryCard(
    uiState: BudgetUiState,
    onEditBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PowderBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Total Monthly Budget + Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Monthly Budget",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.formatIdr(uiState.totalMonthlyBudget, uiState.currencySymbol),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f))
                        .clickable { onEditBudgetClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Budget",
                        tint = PrimaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub Stats Grid (4 Cards in 2 Rows)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Allocated
                SubSummaryBox(
                    title = "Allocated",
                    value = CurrencyFormatter.formatIdr(uiState.totalAllocated, uiState.currencySymbol),
                    bgColor = SageGreen,
                    modifier = Modifier.weight(1f)
                )

                // Unallocated
                val unallocatedColor = if (uiState.unallocatedBudget < 0) DustyPink else ButterCream
                SubSummaryBox(
                    title = "Unallocated",
                    value = CurrencyFormatter.formatIdr(uiState.unallocatedBudget, uiState.currencySymbol),
                    bgColor = unallocatedColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Actual Expenses
                SubSummaryBox(
                    title = "Actual Spent",
                    value = CurrencyFormatter.formatIdr(uiState.totalActualExpenses, uiState.currencySymbol),
                    bgColor = DustyPink,
                    modifier = Modifier.weight(1f)
                )

                // Total Remaining
                val remainingColor = if (uiState.totalRemainingBudget < 0) ExpenseRed.copy(alpha = 0.2f) else SoftLavender
                SubSummaryBox(
                    title = "Remaining",
                    value = CurrencyFormatter.formatIdr(uiState.totalRemainingBudget, uiState.currencySymbol),
                    bgColor = remainingColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SubSummaryBox(
    title: String,
    value: String,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BudgetAlertsSection(
    alerts: List<BudgetAlert>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alerts.forEach { alert ->
            val (bgColor, iconColor, titlePrefix) = when (alert.alertLevel) {
                AlertLevel.EXCEEDED_100 -> Triple(DustyPink, ExpenseRed, "🚨 Budget Exceeded!")
                AlertLevel.NEAR_90 -> Triple(ButterCream, Color(0xFFE65100), "⚠️ Near Budget Limit")
                AlertLevel.APPROACHING_75 -> Triple(ButterCream, Color(0xFFF57F17), "⚡ Approaching Limit")
                AlertLevel.NONE -> Triple(WarmIvory, PrimaryText, "")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = titlePrefix,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alert.message,
                            fontSize = 12.sp,
                            color = PrimaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryAllocationItem(
    allocation: CategoryAllocationUiModel,
    currencySymbol: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryIcon = CategoryData.getIconForCategory(allocation.categoryName)
    val categoryColor = CategoryData.getColorForCategory(allocation.categoryName)

    val progressColor = when {
        allocation.percentage >= 100f -> ExpenseRed
        allocation.percentage >= 75f -> Color(0xFFFFA726) // Amber
        else -> IncomeGreen
    }

    val clampedProgress = (allocation.percentage / 100f).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(categoryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = allocation.categoryName,
                            tint = PrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = allocation.categoryName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Limit: ${CurrencyFormatter.formatIdr(allocation.allocatedAmount, currencySymbol)}",
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%.0f%%", allocation.percentage),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Limit",
                            tint = SecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pastel Progress Indicator
            LinearProgressIndicator(
                progress = { clampedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = WarmIvory,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Spent vs Remaining details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${CurrencyFormatter.formatIdr(allocation.spentAmount, currencySymbol)}",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
                Text(
                    text = "Remaining: ${CurrencyFormatter.formatIdr(allocation.remainingAmount, currencySymbol)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (allocation.remainingAmount < 0) ExpenseRed else PrimaryText
                )
            }
        }
    }
}

@Composable
fun EmptyCategoryAllocationsState(
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(WarmIvory),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PieChart,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No category budgets set yet",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Set spending limits per category to get alerts when you approach your budget.",
                fontSize = 12.sp,
                color = SecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Category Limit", fontWeight = FontWeight.Bold, color = PrimaryText)
            }
        }
    }
}

@Preview
@Composable
fun BudgetContentPreview() {
    MonethraTheme {
        BudgetContent(
            uiState = BudgetUiState(
                monthYearText = "October 2023",
                totalMonthlyBudget = 5000000L,
                totalAllocated = 3500000L,
                unallocatedBudget = 1500000L,
                totalActualExpenses = 2800000L,
                totalRemainingBudget = 2200000L,
                categoryAllocations = listOf(
                    CategoryAllocationUiModel("Food & Drinks", 1500000L, 1600000L, -100000L, 106.6f, AlertLevel.EXCEEDED_100),
                    CategoryAllocationUiModel("Transportation", 800000L, 720000L, 80000L, 90f, AlertLevel.NEAR_90),
                    CategoryAllocationUiModel("Shopping", 1000000L, 400000L, 600000L, 40f, AlertLevel.NONE)
                ),
                budgetAlerts = listOf(
                    BudgetAlert("Food & Drinks", 1500000L, 1600000L, 106.6f, AlertLevel.EXCEEDED_100, "Exceeded budget limit: Food & Drinks is at 107%")
                )
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onOpenSetTotalBudgetDialog = {},
            onCloseSetTotalBudgetDialog = {},
            onSaveTotalBudget = {},
            onOpenAddCategoryDialog = {},
            onOpenEditCategoryDialog = {},
            onCloseCategoryDialog = {},
            onSaveCategoryAllocation = { _, _ -> },
            onDeleteCategoryAllocation = {}
        )
    }
}
