package com.example.monethra.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.monethra.MonethraApplication
import com.example.monethra.data.entity.UserEntity
import com.example.monethra.data.model.CurrencyModel
import com.example.monethra.data.model.GLOBAL_CURRENCY_CATALOG
import com.example.monethra.data.model.WorldCurrency
import com.example.monethra.data.repository.AuthRepository
import com.example.monethra.data.repository.CurrencyRepository
import com.example.monethra.data.repository.FinanceRepository
import com.example.monethra.utils.CurrencyConfig
import com.example.monethra.utils.CurrencyFormatter
import com.example.monethra.utils.DateUtils
import com.example.monethra.utils.EmailRecapHelper
import com.example.monethra.utils.PdfReportGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val userName: String = "Monethra User",
    val userEmail: String? = null,
    val themePreference: String = "SYSTEM",
    val currencySymbol: String = "Rp",
    val currencyCode: String = "IDR",
    val currencyRate: Double = 1.0,
    val availableCurrencies: List<CurrencyModel> = emptyList(),
    val cachedRates: Map<String, Double> = emptyMap(),
    val notificationsEnabled: Boolean = true,
    val paydayStartDay: Int = 1,
    val isLoading: Boolean = true,
    val userMessage: String? = null,
    val isEditProfileDialogOpen: Boolean = false,
    val isCurrencyDialogOpen: Boolean = false,
    val isAddCustomCurrencyDialogOpen: Boolean = false,
    val isFetchingRate: Boolean = false,
    val fetchedRate: Double? = null,
    val addingCurrencyCode: String? = null,
    val isPaydayStartDayDialogOpen: Boolean = false,
    val isResetDataDialogOpen: Boolean = false,
    val isSampleDataDialogOpen: Boolean = false
)

private data class PreferencesState(
    val theme: String,
    val currency: String,
    val currencyCode: String,
    val currencyRate: Double,
    val notifications: Boolean,
    val paydayStartDay: Int
)

private data class ProfileViewDialogState(
    val message: String?,
    val isEditOpen: Boolean,
    val isCurrencyOpen: Boolean,
    val isAddCustomCurrencyOpen: Boolean,
    val isFetchingRate: Boolean,
    val fetchedRate: Double?,
    val addingCurrencyCode: String?,
    val isPaydayOpen: Boolean,
    val isResetOpen: Boolean,
    val isSampleOpen: Boolean
)

class ProfileViewModel @JvmOverloads constructor(
    application: Application,
    financeRepository: FinanceRepository? = (application as? MonethraApplication)?.financeRepository,
    authRepository: AuthRepository? = (application as? MonethraApplication)?.authRepository,
    currencyRepository: CurrencyRepository? = (application as? MonethraApplication)?.currencyRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val repository: FinanceRepository? = financeRepository ?: (application as? MonethraApplication)?.financeRepository
    private val authRepository: AuthRepository? = authRepository ?: (application as? MonethraApplication)?.authRepository
    private val currencyRepository: CurrencyRepository? = currencyRepository ?: (application as? MonethraApplication)?.currencyRepository

    constructor(
        repository: FinanceRepository,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        application = Application(),
        financeRepository = repository,
        authRepository = null,
        currencyRepository = null,
        ioDispatcher = ioDispatcher
    )

    constructor(
        repository: FinanceRepository,
        authRepository: AuthRepository?,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        application = Application(),
        financeRepository = repository,
        authRepository = authRepository,
        currencyRepository = null,
        ioDispatcher = ioDispatcher
    )

    val currentUser: StateFlow<UserEntity?> = (authRepository?.getCurrentUserFlow() ?: repository?.userProfile ?: flowOf(null))
        .catch { emit(null) }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _userMessage = MutableStateFlow<String?>(null)
    private val _isEditProfileDialogOpen = MutableStateFlow(false)
    private val _isCurrencyDialogOpen = MutableStateFlow(false)
    private val _isAddCustomCurrencyDialogOpen = MutableStateFlow(false)
    private val _isFetchingRate = MutableStateFlow(false)
    private val _fetchedRate = MutableStateFlow<Double?>(null)
    private val _addingCurrencyCode = MutableStateFlow<String?>(null)
    private val _isPaydayStartDayDialogOpen = MutableStateFlow(false)
    private val _isResetDataDialogOpen = MutableStateFlow(false)
    private val _isSampleDataDialogOpen = MutableStateFlow(false)

    private val userFlow = (authRepository?.getCurrentUserFlow() ?: repository?.userProfile ?: flowOf(null))
        .catch { emit(null) }
        .flowOn(ioDispatcher)

    val allCurrenciesFlow: Flow<List<CurrencyModel>> = currencyRepository?.allCurrencies
        ?: flowOf(CurrencyRepository.DEFAULT_CURRENCIES)

    val cachedRatesFlow: Flow<Map<String, Double>> = currencyRepository?.cachedRates
        ?: flowOf(emptyMap())

    val activeCurrencyFlow: Flow<CurrencyModel> = currencyRepository?.activeCurrency
        ?: repository?.currencySymbol?.map { symbol ->
            val matching = CurrencyRepository.DEFAULT_CURRENCIES.find { it.symbol == symbol }
            matching ?: CurrencyModel(code = symbol, symbol = symbol, name = "Custom", rateFromIdr = 1.0, isDefault = true)
        } ?: flowOf(CurrencyRepository.DEFAULT_CURRENCIES.first())

    private val preferencesFlow = combine(
        repository?.themePreference?.catch { emit("SYSTEM") } ?: flowOf("SYSTEM"),
        activeCurrencyFlow,
        repository?.notificationsEnabled?.catch { emit(true) } ?: flowOf(true),
        repository?.paydayStartDay?.catch { emit(1) } ?: flowOf(1)
    ) { theme, currency, notifications, payday ->
        PreferencesState(
            theme = theme,
            currency = currency.symbol,
            currencyCode = currency.code,
            currencyRate = currency.rateFromIdr,
            notifications = notifications,
            paydayStartDay = payday
        )
    }
        .flowOn(ioDispatcher)
        .catch { emit(PreferencesState("SYSTEM", "Rp", "IDR", 1.0, true, 1)) }

    private val dialogBooleans: Flow<List<Boolean>> = combine(
        listOf(
            _isEditProfileDialogOpen,
            _isCurrencyDialogOpen,
            _isAddCustomCurrencyDialogOpen,
            _isFetchingRate,
            _isPaydayStartDayDialogOpen,
            _isResetDataDialogOpen,
            _isSampleDataDialogOpen
        )
    ) { array ->
        array.toList()
    }

    private val dialogStateFlow = combine(
        _userMessage,
        _fetchedRate,
        _addingCurrencyCode,
        dialogBooleans
    ) { message, fetchedRate, addingCode, booleans ->
        ProfileViewDialogState(
            message = message,
            isEditOpen = booleans[0],
            isCurrencyOpen = booleans[1],
            isAddCustomCurrencyOpen = booleans[2],
            isFetchingRate = booleans[3],
            fetchedRate = fetchedRate,
            addingCurrencyCode = addingCode,
            isPaydayOpen = booleans[4],
            isResetOpen = booleans[5],
            isSampleOpen = booleans[6]
        )
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        userFlow,
        preferencesFlow,
        allCurrenciesFlow,
        cachedRatesFlow,
        dialogStateFlow
    ) { user, prefs, currencies, rates, dialogs ->
        ProfileUiState(
            userName = user?.name?.ifBlank { "Monethra User" } ?: "Monethra User",
            userEmail = user?.email,
            themePreference = prefs.theme.ifBlank { "SYSTEM" },
            currencySymbol = prefs.currency.ifBlank { "Rp" },
            currencyCode = prefs.currencyCode.ifBlank { "IDR" },
            currencyRate = prefs.currencyRate,
            availableCurrencies = currencies,
            cachedRates = rates,
            notificationsEnabled = prefs.notifications,
            paydayStartDay = prefs.paydayStartDay,
            isLoading = false,
            userMessage = dialogs.message,
            isEditProfileDialogOpen = dialogs.isEditOpen,
            isCurrencyDialogOpen = dialogs.isCurrencyOpen,
            isAddCustomCurrencyDialogOpen = dialogs.isAddCustomCurrencyOpen,
            isFetchingRate = dialogs.isFetchingRate,
            fetchedRate = dialogs.fetchedRate,
            addingCurrencyCode = dialogs.addingCurrencyCode,
            isPaydayStartDayDialogOpen = dialogs.isPaydayOpen,
            isResetDataDialogOpen = dialogs.isResetOpen,
            isSampleDataDialogOpen = dialogs.isSampleOpen
        )
    }
    .flowOn(ioDispatcher)
    .catch { emit(ProfileUiState(isLoading = false)) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState(isLoading = true)
    )

    fun updateProfile(name: String, email: String?) {
        if (name.isBlank()) {
            _userMessage.value = "Display name cannot be empty"
            return
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                repository?.saveUserProfile(name.trim(), email?.trim()?.takeIf { it.isNotBlank() })
                closeEditProfileDialog()
                _userMessage.value = "Profile updated successfully"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun selectCurrency(currency: CurrencyModel) {
        viewModelScope.launch(ioDispatcher) {
            try {
                currencyRepository?.selectCurrency(currency)
                repository?.setCurrencySymbol(currency.symbol)
                CurrencyFormatter.currentConfig = CurrencyConfig(
                    currencyCode = currency.code,
                    symbol = currency.symbol,
                    rateFromIdr = currency.rateFromIdr
                )
                closeCurrencyDialog()
                _userMessage.value = "Currency updated to ${currency.code} (${currency.symbol})"
                // Background refresh of live rates
                try {
                    currencyRepository?.fetchLiveRates()
                } catch (_: Exception) {}
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to update currency"
            }
        }
    }

    fun updateCurrency(symbol: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val matching = currencyRepository?.allCurrencies?.firstOrNull()?.find { it.symbol == symbol }
                if (matching != null) {
                    currencyRepository?.selectCurrency(matching)
                    CurrencyFormatter.currentConfig = CurrencyConfig(
                        currencyCode = matching.code,
                        symbol = matching.symbol,
                        rateFromIdr = matching.rateFromIdr
                    )
                }
                repository?.setCurrencySymbol(symbol)
                closeCurrencyDialog()
                _userMessage.value = "Currency updated to $symbol"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to update currency"
            }
        }
    }

    fun fetchRateForCurrencyCode(code: String) {
        if (code.length != 3) {
            _fetchedRate.value = null
            return
        }
        viewModelScope.launch(ioDispatcher) {
            _isFetchingRate.value = true
            try {
                val rate = currencyRepository?.fetchRateForCode(code)
                _fetchedRate.value = rate
            } catch (_: Exception) {
                _fetchedRate.value = null
            } finally {
                _isFetchingRate.value = false
            }
        }
    }

    fun getSuggestedMetadata(code: String): Pair<String, String>? {
        return currencyRepository?.getSuggestedMetadata(code)
    }

    fun addCustomCurrency(code: String, symbol: String, name: String, rateFromIdr: Double) {
        viewModelScope.launch(ioDispatcher) {
            try {
                currencyRepository?.addCustomCurrency(code, symbol, name, rateFromIdr)
                closeAddCustomCurrencyDialog()
                _userMessage.value = "Added custom currency ${code.uppercase()}"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to add currency"
            }
        }
    }

    fun addCurrencyFromWorldCatalog(worldCurrency: WorldCurrency) {
        viewModelScope.launch(ioDispatcher) {
            _addingCurrencyCode.value = worldCurrency.code
            try {
                currencyRepository?.addCurrencyFromCatalog(worldCurrency)
                closeAddCustomCurrencyDialog()
                _userMessage.value = "Added ${worldCurrency.name} (${worldCurrency.symbol})"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to add currency"
            } finally {
                _addingCurrencyCode.value = null
            }
        }
    }

    fun deleteCustomCurrency(code: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                currencyRepository?.deleteCustomCurrency(code)
                _userMessage.value = "Deleted currency ${code.uppercase()}"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to delete currency"
            }
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository?.setThemePreference(theme)
                _userMessage.value = "Theme updated to ${theme.lowercase()}"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to update theme"
            }
        }
    }

    fun updateNotificationSettings(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository?.setNotificationsEnabled(enabled)
                _userMessage.value = if (enabled) "Notifications enabled" else "Notifications disabled"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to update notifications"
            }
        }
    }

    fun updatePaydayStartDay(day: Int) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository?.setPaydayStartDay(day)
                closePaydayStartDayDialog()
                _userMessage.value = "Payday start date updated to day $day"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to update payday start day"
            }
        }
    }

    fun sendEmailRecap(context: Context) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val state = uiState.value
                val user = currentUser.value
                val effectiveName = user?.name.orEmpty().ifBlank { state.userName }.ifBlank { "Monethra User" }
                val effectiveEmail = user?.email.orEmpty().ifBlank { state.userEmail ?: "" }

                val currentMonth = DateUtils.getCurrentMonth()
                val currentYear = DateUtils.getCurrentYear()
                val (start, end) = DateUtils.getPaydayCycleStartAndEnd(currentMonth, currentYear, state.paydayStartDay)
                val cycleLabel = DateUtils.getPaydayCycleLabel(currentMonth, currentYear, state.paydayStartDay)

                val monthTx = repository?.getTransactionsByDateRange(start, end)?.firstOrNull() ?: emptyList()
                val totalIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
                val totalExpenses = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val netSavings = totalIncome - totalExpenses

                val budget = repository?.getBudgetForMonth(currentMonth, currentYear)?.firstOrNull()
                val totalBudget = budget?.totalBudget ?: 0L
                val utilization = if (totalBudget > 0) ((totalExpenses.toFloat() / totalBudget.toFloat()) * 100f).toInt() else 0

                val topCategory = monthTx.filter { it.type == "EXPENSE" }
                    .groupBy { it.category }
                    .maxByOrNull { entry -> entry.value.sumOf { it.amount } }?.key ?: "N/A"

                val insight = "Active payday cycle recap for $cycleLabel."

                val subject = EmailRecapHelper.generateEmailSubject(cycleLabel)
                val body = EmailRecapHelper.generateEmailBody(
                    userName = effectiveName,
                    cycleLabel = cycleLabel,
                    totalIncome = totalIncome,
                    totalExpense = totalExpenses,
                    netSavings = netSavings,
                    budgetUtilization = utilization,
                    topCategory = topCategory,
                    insight = insight
                )

                withContext(Dispatchers.Main) {
                    EmailRecapHelper.sendEmailRecap(
                        context = context,
                        userEmail = effectiveEmail,
                        subject = subject,
                        body = body
                    )
                }
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to send email recap"
            }
        }
    }

    fun exportPdfRecap(context: Context, month: Int = DateUtils.getCurrentMonth(), year: Int = DateUtils.getCurrentYear()) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val state = uiState.value
                val user = currentUser.value
                val effectiveName = user?.name.orEmpty().ifBlank { state.userName }.ifBlank { "Monethra User" }
                val effectiveEmail = user?.email.orEmpty().ifBlank { state.userEmail ?: "" }

                val (start, end) = DateUtils.getPaydayCycleStartAndEnd(month, year, state.paydayStartDay)
                val cycleLabel = DateUtils.getPaydayCycleLabel(month, year, state.paydayStartDay)

                val monthTx = repository?.getTransactionsByDateRange(start, end)?.firstOrNull() ?: emptyList()
                val totalIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
                val totalExpenses = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val netSavings = totalIncome - totalExpenses

                val pdfFile = PdfReportGenerator.generateMonthlyRecapPdf(
                    context = context,
                    userName = effectiveName,
                    userEmail = effectiveEmail.takeIf { it.isNotBlank() },
                    cycleLabel = cycleLabel,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    netSavings = netSavings,
                    transactions = monthTx,
                    currencySymbol = state.currencySymbol
                )

                val subject = EmailRecapHelper.generateEmailSubject(cycleLabel)
                val body = EmailRecapHelper.generateEmailBody(
                    userName = effectiveName,
                    cycleLabel = cycleLabel,
                    totalIncome = totalIncome,
                    totalExpense = totalExpenses,
                    netSavings = netSavings,
                    insight = "Monthly financial recap for $cycleLabel"
                )

                withContext(Dispatchers.Main) {
                    EmailRecapHelper.sendEmailRecapWithPdf(
                        context = context,
                        userEmail = effectiveEmail,
                        subject = subject,
                        body = body,
                        pdfFile = pdfFile
                    )
                }
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to export PDF recap"
            }
        }
    }

    fun logout(authRepository: AuthRepository? = null, onComplete: () -> Unit = {}) {
        val targetAuthRepo = authRepository ?: this.authRepository
        viewModelScope.launch(ioDispatcher) {
            try {
                targetAuthRepo?.logout()
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun resetFinancialData() {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository?.resetFinancialData()
                closeResetDataDialog()
                _userMessage.value = "All financial data has been reset"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to reset financial data"
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository?.loadSampleData()
                closeSampleDataDialog()
                _userMessage.value = "Sample data loaded successfully"
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Failed to load sample data"
            }
        }
    }

    fun openEditProfileDialog() {
        _isEditProfileDialogOpen.value = true
    }

    fun closeEditProfileDialog() {
        _isEditProfileDialogOpen.value = false
    }

    fun openCurrencyDialog() {
        _isCurrencyDialogOpen.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                currencyRepository?.fetchLiveRates()
            } catch (_: Exception) {}
        }
    }

    fun closeCurrencyDialog() {
        _isCurrencyDialogOpen.value = false
    }

    fun openAddCustomCurrencyDialog() {
        _isAddCustomCurrencyDialogOpen.value = true
        _fetchedRate.value = null
        viewModelScope.launch(ioDispatcher) {
            try {
                currencyRepository?.fetchLiveRates()
            } catch (_: Exception) {}
        }
    }

    fun closeAddCustomCurrencyDialog() {
        _isAddCustomCurrencyDialogOpen.value = false
        _fetchedRate.value = null
    }

    fun openPaydayStartDayDialog() {
        _isPaydayStartDayDialogOpen.value = true
    }

    fun closePaydayStartDayDialog() {
        _isPaydayStartDayDialogOpen.value = false
    }

    fun openResetDataDialog() {
        _isResetDataDialogOpen.value = true
    }

    fun closeResetDataDialog() {
        _isResetDataDialogOpen.value = false
    }

    fun openSampleDataDialog() {
        _isSampleDataDialogOpen.value = true
    }

    fun closeSampleDataDialog() {
        _isSampleDataDialogOpen.value = false
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

class ProfileViewModelFactory(
    private val repository: FinanceRepository,
    private val authRepository: AuthRepository? = null,
    private val currencyRepository: CurrencyRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(
                application = Application(),
                financeRepository = repository,
                authRepository = authRepository,
                currencyRepository = currencyRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
