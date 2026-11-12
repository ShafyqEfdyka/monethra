@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.monethra.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsSystemDaydream
import androidx.compose.material.icons.rounded.Storage
import com.example.monethra.ui.components.ExportPdfRecapDialog
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monethra.MonethraApplication
import com.example.monethra.ui.theme.DeepIndigo
import com.example.monethra.ui.theme.ExpenseRed
import com.example.monethra.ui.theme.PastelPeriwinkle
import com.example.monethra.ui.theme.PrimaryText
import com.example.monethra.ui.theme.SecondaryText
import com.example.monethra.ui.theme.SoftLavender
import com.example.monethra.ui.theme.WarmIvory
import com.example.monethra.viewmodel.ProfileViewModel
import com.example.monethra.viewmodel.ProfileViewModelFactory

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = run {
        val context = LocalContext.current
        val app = context.applicationContext as? MonethraApplication
        if (app != null) {
            viewModel(factory = ProfileViewModelFactory(app.financeRepository, app.authRepository))
        } else {
            viewModel()
        }
    },
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MonethraApplication
    val authRepository = app?.authRepository

    val user by viewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val userEmail = user?.email.orEmpty().ifBlank { uiState.userEmail.orEmpty() }
    val userName = user?.name.orEmpty().ifBlank { uiState.userName }.ifBlank { "User" }
    val initials = userName.firstOrNull()?.uppercase() ?: "U"
    val avatarInitial = initials.toString()
    val isDemoAccount = userEmail.isNotBlank() && userEmail.equals("monthra@andro.app", ignoreCase = true)
    val paydayStartDay = uiState.paydayStartDay.coerceIn(1, 28)
    val selectedCurrency = uiState.currencySymbol.ifBlank { "Rp" }
    val selectedTheme = uiState.themePreference.ifBlank { "SYSTEM" }
    val snackbarHostState = remember { SnackbarHostState() }

    var isAboutDialogOpen by remember { mutableStateOf(false) }
    var isExportPdfDialogOpen by remember { mutableStateOf(false) }
    var isLogoutDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Screen Title
                    Text(
                        text = "Profile & Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Text(
                        text = "Manage user details, preferences, and data",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // User Header Card
                    UserProfileCard(
                        userName = userName,
                        userEmail = if (userEmail.isNotBlank()) userEmail else "No email set",
                        avatarInitial = avatarInitial,
                        onEditClick = { viewModel.openEditProfileDialog() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preferences Section
                    PreferencesSection(
                        currencySymbol = selectedCurrency,
                        themePreference = selectedTheme,
                        notificationsEnabled = uiState.notificationsEnabled,
                        paydayStartDay = paydayStartDay,
                        onCurrencyClick = { viewModel.openCurrencyDialog() },
                        onPaydayStartDayClick = { viewModel.openPaydayStartDayDialog() },
                        onThemeSelect = { theme -> viewModel.updateTheme(theme) },
                        onNotificationsToggle = { enabled -> viewModel.updateNotificationSettings(enabled) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Data Management Section
                    DataManagementSection(
                        isDemoAccount = isDemoAccount,
                        onExportPdfRecapClick = { isExportPdfDialogOpen = true },
                        onLoadSampleDataClick = { viewModel.openSampleDataDialog() },
                        onResetDataClick = { viewModel.openResetDataDialog() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Info Section
                    AppInfoSection(
                        onAboutClick = { isAboutDialogOpen = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Log Out Section Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLogoutDialogOpen = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Log Out",
                                tint = ExpenseRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Log Out",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Dialogs
            if (uiState.isEditProfileDialogOpen) {
                EditProfileDialog(
                    initialName = userName,
                    initialEmail = userEmail,
                    onDismiss = { viewModel.closeEditProfileDialog() },
                    onConfirm = { name, email ->
                        viewModel.updateProfile(name, email)
                    }
                )
            }

            if (uiState.isCurrencyDialogOpen) {
                CurrencySelectorDialog(
                    currentSymbol = selectedCurrency,
                    onDismiss = { viewModel.closeCurrencyDialog() },
                    onSelectCurrency = { symbol ->
                        viewModel.updateCurrency(symbol)
                    }
                )
            }

            if (uiState.isPaydayStartDayDialogOpen) {
                PaydaySelectionSheet(
                    initialDay = paydayStartDay,
                    onDismissRequest = { viewModel.closePaydayStartDayDialog() },
                    onConfirm = { day ->
                        viewModel.updatePaydayStartDay(day)
                        viewModel.closePaydayStartDayDialog()
                    }
                )
            }

            if (uiState.isSampleDataDialogOpen) {
                AlertDialog(
                    onDismissRequest = { viewModel.closeSampleDataDialog() },
                    title = { Text("Load Sample Data?", fontWeight = FontWeight.Bold) },
                    text = { Text("This will overwrite existing financial records with realistic sample transactions and budget entries.") },
                    confirmButton = {
                        Button(onClick = { viewModel.loadSampleData() }) {
                            Text("Load Sample Data")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closeSampleDataDialog() }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (uiState.isResetDataDialogOpen) {
                AlertDialog(
                    onDismissRequest = { viewModel.closeResetDataDialog() },
                    title = { Text("Reset All Financial Data?", fontWeight = FontWeight.Bold, color = ExpenseRed) },
                    text = { Text("Are you sure you want to permanently delete all transactions, budgets, and category allocations? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.resetFinancialData() },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                        ) {
                            Text("Reset Data", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closeResetDataDialog() }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (isAboutDialogOpen) {
                AboutMonethraDialog(onDismiss = { isAboutDialogOpen = false })
            }

            if (isExportPdfDialogOpen) {
                ExportPdfRecapDialog(
                    currentMonth = com.example.monethra.utils.DateUtils.getCurrentMonth(),
                    currentYear = com.example.monethra.utils.DateUtils.getCurrentYear(),
                    paydayStartDay = uiState.paydayStartDay,
                    onConfirmExport = { month, year ->
                        viewModel.exportPdfRecap(context, month, year)
                    },
                    onDismiss = { isExportPdfDialogOpen = false }
                )
            }

            if (isLogoutDialogOpen) {
                AlertDialog(
                    onDismissRequest = { isLogoutDialogOpen = false },
                    title = { Text("Log Out Confirmation", fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to log out of MONETHRA?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                isLogoutDialogOpen = false
                                viewModel.logout(authRepository) {
                                    onLogout()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                        ) {
                            Text("Log Out", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isLogoutDialogOpen = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun UserProfileCard(
    userName: String,
    userEmail: String?,
    avatarInitial: String = userName.firstOrNull()?.uppercase()?.toString() ?: "U",
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SoftLavender),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitial,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = userEmail ?: "No email set",
                    fontSize = 13.sp,
                    color = SecondaryText
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PreferencesSection(
    currencySymbol: String,
    themePreference: String,
    notificationsEnabled: Boolean,
    paydayStartDay: Int,
    onCurrencyClick: () -> Unit,
    onPaydayStartDayClick: () -> Unit,
    onThemeSelect: (String) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Preferences",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Currency Row
            SettingsRowItem(
                icon = Icons.Rounded.AttachMoney,
                title = "Currency",
                subtitle = "Selected symbol: $currencySymbol",
                trailing = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarmIvory)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currencySymbol,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }
                },
                onClick = onCurrencyClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Payday Start Date Row
            SettingsRowItem(
                icon = Icons.Rounded.Event,
                title = "Payday Start Date",
                subtitle = "Cycle starts on day $paydayStartDay of each month",
                trailing = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarmIvory)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Day $paydayStartDay",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }
                },
                onClick = onPaydayStartDayClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Theme Preference Row
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LightMode,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Theme Preference",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        Text(
                            text = "Choose application visual theme",
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionChip(
                        label = "Light",
                        icon = Icons.Rounded.LightMode,
                        selected = themePreference == "LIGHT",
                        onClick = { onThemeSelect("LIGHT") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionChip(
                        label = "Dark",
                        icon = Icons.Rounded.DarkMode,
                        selected = themePreference == "DARK",
                        onClick = { onThemeSelect("DARK") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionChip(
                        label = "System",
                        icon = Icons.Rounded.SettingsSystemDaydream,
                        selected = themePreference == "SYSTEM",
                        onClick = { onThemeSelect("SYSTEM") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Notifications Toggle Row
            SettingsRowItem(
                icon = Icons.Rounded.Notifications,
                title = "Notifications",
                subtitle = "Receive budget alerts and reminders",
                trailing = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationsToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            uncheckedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFD8E5F8),
                            uncheckedTrackColor = Color(0xFFE2DCE5),
                            uncheckedBorderColor = Color.Transparent,
                            checkedBorderColor = Color.Transparent
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun DataManagementSection(
    isDemoAccount: Boolean,
    onExportPdfRecapClick: () -> Unit,
    onLoadSampleDataClick: () -> Unit,
    onResetDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Data Management",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Export Monthly Recap as PDF Button
            Button(
                onClick = onExportPdfRecapClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PastelPeriwinkle,
                    contentColor = DeepIndigo
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = DeepIndigo
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export Monthly Recap to PDF",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = DeepIndigo
                )
            }

            if (isDemoAccount) {
                Spacer(modifier = Modifier.height(10.dp))

                // Load Sample Data Button
                OutlinedButton(
                    onClick = onLoadSampleDataClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load Sample Data", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reset Data Button
                OutlinedButton(
                    onClick = onResetDataClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = ExpenseRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Financial Data", fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                }
            }
        }
    }
}


@Composable
fun AppInfoSection(
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Application Info",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MONETHRA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Text(
                        text = "Personal Finance Tracker",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarmIvory)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v1.0.0",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryText
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SettingsRowItem(
                icon = Icons.AutoMirrored.Rounded.Help,
                title = "About MONETHRA & Privacy",
                subtitle = "App information, features & privacy details",
                onClick = onAboutClick
            )
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = SecondaryText
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else WarmIvory
    val contentColor = if (selected) PrimaryText else SecondaryText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = name.trim().ifBlank { "Monethra User" }
                    val finalEmail = email.trim().ifBlank { null }
                    onConfirm(finalName, finalEmail)
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CurrencySelectorDialog(
    currentSymbol: String,
    onDismiss: () -> Unit,
    onSelectCurrency: (String) -> Unit
) {
    val currencies = listOf(
        "Rp" to "Indonesian Rupiah (IDR)",
        "$" to "US Dollar (USD)",
        "€" to "Euro (EUR)",
        "£" to "British Pound (GBP)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency Preference", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                currencies.forEach { (symbol, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCurrency(symbol) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSymbol == symbol,
                            onClick = { onSelectCurrency(symbol) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "$symbol ($description)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AboutMonethraDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("About MONETHRA", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "MONETHRA is a modern personal finance tracking app built using Jetpack Compose, Room Database, and Material Design 3.",
                    fontSize = 13.sp,
                    color = PrimaryText
                )
                Text(
                    text = "Key Features:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = "• Income & Expense tracking with custom categories\n• Monthly budget planning with dynamic threshold alerts\n• Visual reports with Bar, Line, and Donut charts\n• Automated financial recap insights\n• 100% offline-first local data storage",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }
    )
}
