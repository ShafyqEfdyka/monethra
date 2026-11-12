package com.example.monethra.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.monethra.MonethraApplication
import com.example.monethra.ui.auth.LoginScreen
import com.example.monethra.ui.auth.SignUpScreen
import com.example.monethra.ui.budget.BudgetScreen
import com.example.monethra.ui.components.MonethraBottomBar
import com.example.monethra.ui.components.MonethraNavRail
import com.example.monethra.ui.components.TopLevelDestination
import com.example.monethra.ui.dashboard.DashboardScreen
import com.example.monethra.ui.onboarding.OnboardingScreen
import com.example.monethra.ui.onboarding.SplashScreen
import com.example.monethra.ui.profile.ProfileScreen
import com.example.monethra.ui.reports.ReportsScreen
import com.example.monethra.ui.transactions.TransactionsScreen
import com.example.monethra.viewmodel.AuthViewModel
import com.example.monethra.viewmodel.AuthViewModelFactory
import com.example.monethra.viewmodel.ProfileViewModel
import com.example.monethra.viewmodel.ProfileViewModelFactory

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val BUDGET = "budget"
    const val REPORTS = "reports"
    const val PROFILE = "profile"
}

@Composable
fun MonethraApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val app = context.applicationContext as? MonethraApplication
    val authRepository = app?.authRepository
    val userPreferencesRepository = app?.userPreferencesRepository
    val authViewModel: AuthViewModel? = authRepository?.let {
        viewModel(factory = AuthViewModelFactory(it))
    }
    val isLoggedIn by (authViewModel?.isLoggedIn?.collectAsState() ?: remember { mutableStateOf(false) })
    val isOnboardingCompleted by (userPreferencesRepository?.isOnboardingCompleted?.collectAsState(initial = true) ?: remember { mutableStateOf(true) })

    val startDestination = when {
        isLoggedIn -> Routes.DASHBOARD
        !isOnboardingCompleted -> Routes.ONBOARDING
        else -> Routes.LOGIN
    }

    LaunchedEffect(isLoggedIn, currentRoute) {
        if (!isLoggedIn &&
            currentRoute != null &&
            currentRoute != Routes.SPLASH &&
            currentRoute != Routes.ONBOARDING &&
            currentRoute != Routes.LOGIN &&
            currentRoute != Routes.SIGNUP
        ) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authRepository?.let { repo ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val expired = repo.validateSession()
                        if (!expired && repo.isLoggedIn.first()) {
                            repo.updateLastActiveTimestamp()
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isFullScreenRoute = currentRoute == null ||
            currentRoute == Routes.SPLASH ||
            currentRoute == Routes.ONBOARDING ||
            currentRoute == Routes.LOGIN ||
            currentRoute == Routes.SIGNUP

    val navigateToDestination: (TopLevelDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val onLogout: () -> Unit = {
        authViewModel?.logout {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 600.dp

            if (isTablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (!isFullScreenRoute) {
                        MonethraNavRail(
                            currentRoute = currentRoute,
                            onNavigateToDestination = navigateToDestination
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        AppNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            onNavigateToDestination = navigateToDestination,
                            onLogout = onLogout
                        )
                    }
                }
            } else {
                Scaffold(
                    bottomBar = {
                        if (!isFullScreenRoute) {
                            MonethraBottomBar(
                                currentRoute = currentRoute,
                                onNavigateToDestination = navigateToDestination
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (!isFullScreenRoute) innerPadding else PaddingValues(0.dp))
                    ) {
                        AppNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            onNavigateToDestination = navigateToDestination,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Routes.LOGIN,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    onLogout: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            val context = LocalContext.current
            val app = context.applicationContext as? MonethraApplication
            val authRepository = app?.authRepository
            if (authRepository != null) {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(authRepository)
                )
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        try {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate(Routes.SIGNUP)
                    }
                )
            }
        }
        composable(Routes.SIGNUP) {
            val context = LocalContext.current
            val app = context.applicationContext as? MonethraApplication
            val authRepository = app?.authRepository
            if (authRepository != null) {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(authRepository)
                )
                SignUpScreen(
                    viewModel = authViewModel,
                    onSignUpSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    }
                )
            }
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onAddIncomeClick = {
                    navController.navigate("${Routes.TRANSACTIONS}?filter=INCOME&autoOpen=true") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAddExpenseClick = {
                    navController.navigate("${Routes.TRANSACTIONS}?filter=EXPENSE&autoOpen=true") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSetBudgetClick = { onNavigateToDestination(TopLevelDestination.BUDGET) },
                onSeeAllTransactionsClick = { onNavigateToDestination(TopLevelDestination.TRANSACTIONS) },
                onProfileClick = { onNavigateToDestination(TopLevelDestination.PROFILE) }
            )
        }
        composable(
            route = "${Routes.TRANSACTIONS}?filter={filter}&autoOpen={autoOpen}",
            arguments = listOf(
                navArgument("filter") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("autoOpen") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter")
            val autoOpen = backStackEntry.arguments?.getBoolean("autoOpen") ?: false
            TransactionsScreen(
                initialTypeFilter = filter,
                initialAutoOpenAdd = autoOpen
            )
        }
        composable(Routes.BUDGET) {
            BudgetScreen()
        }
        composable(Routes.REPORTS) {
            ReportsScreen()
        }
        composable(Routes.PROFILE) {
            val context = LocalContext.current
            val app = context.applicationContext as? MonethraApplication
            val profileViewModel: ProfileViewModel? = app?.let {
                viewModel(
                    factory = ProfileViewModelFactory(
                        repository = it.financeRepository,
                        authRepository = it.authRepository
                    )
                )
            }
            if (profileViewModel != null) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLogout = onLogout
                )
            } else {
                ProfileScreen(
                    onLogout = onLogout
                )
            }
        }
    }
}
