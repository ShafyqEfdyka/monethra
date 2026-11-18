package com.example.monethra.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val isOnboardingCompleted: Flow<Boolean>
    val isLoggedIn: Flow<Boolean>
    val activeUserId: Flow<Long>
    val sessionToken: Flow<String?>
    val userNamePreference: Flow<String?>
    val userEmailPreference: Flow<String?>
    val currencySymbol: Flow<String>
    val selectedCurrencyCode: Flow<String> get() = flowOf("IDR")
    val currencyRateFromIdr: Flow<Double> get() = flowOf(1.0)
    val cachedRatesJson: Flow<String> get() = flowOf("")
    val themePreference: Flow<String>
    val notificationsEnabled: Flow<Boolean>
    val paydayStartDay: Flow<Int>
    val lastProcessedPaydayCycle: Flow<String>
    val lastActiveTimestamp: Flow<Long>
    val lastBackgroundTimestamp: Flow<Long> get() = flowOf(0L)

    fun isSessionLoggedIn(): Boolean = false
    fun getLastActiveTimestamp(): Long = getLastBackgroundTimestamp()
    fun saveLastActiveTimestampSync(timestamp: Long) { saveLastBackgroundTimestampSync(timestamp) }
    fun getLastBackgroundTimestamp(): Long = 0L
    fun saveLastBackgroundTimestampSync(timestamp: Long) {}
    fun isInitialSessionValid(timeoutMillis: Long = 5 * 60 * 1000L): Boolean = isSessionLoggedIn()

    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setSession(isLoggedIn: Boolean, token: String = "", name: String = "", email: String = "", userId: Long = 0L)
    suspend fun clearSession()
    suspend fun saveLastBackgroundTimestamp(timestamp: Long) {}
    suspend fun updateLastActiveTimestamp(timestamp: Long = System.currentTimeMillis())
    suspend fun checkAndInvalidateSessionIfExpired(timeoutMillis: Long = 5 * 60 * 1000L, currentTime: Long = System.currentTimeMillis()): Boolean
    suspend fun setCurrencySymbol(symbol: String)
    suspend fun setSelectedCurrency(code: String, symbol: String, rateFromIdr: Double) {}
    suspend fun setCachedRatesJson(json: String) {}
    suspend fun setThemePreference(theme: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setPaydayStartDay(day: Int)
    suspend fun setLastProcessedPaydayCycle(cycleKey: String)
    suspend fun clearPreferences()
}

class UserPreferencesRepositoryImpl(context: Context) : UserPreferencesRepository {

    private val appContext = context.applicationContext
    private val sessionPrefs = appContext.getSharedPreferences("monethra_session", Context.MODE_PRIVATE)

    companion object {
        const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_ACTIVE_USER_ID = longPreferencesKey("active_user_id")
        private val KEY_SESSION_TOKEN = stringPreferencesKey("session_token")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val KEY_SELECTED_CURRENCY_CODE = stringPreferencesKey("selected_currency_code")
        private val KEY_CURRENCY_RATE_FROM_IDR = doublePreferencesKey("currency_rate_from_idr")
        private val KEY_CACHED_RATES_JSON = stringPreferencesKey("cached_rates_json")
        private val KEY_THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_PAYDAY_START_DAY = intPreferencesKey("payday_start_day")
        private val KEY_LAST_PROCESSED_PAYDAY_CYCLE = stringPreferencesKey("last_processed_payday_cycle")
        private val KEY_LAST_ACTIVE_TIMESTAMP = longPreferencesKey("last_active_timestamp")
        private val KEY_LAST_BACKGROUND_TIMESTAMP = longPreferencesKey("last_background_timestamp")
    }

    override val isOnboardingCompleted: Flow<Boolean> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }

    override val isLoggedIn: Flow<Boolean> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_IS_LOGGED_IN] ?: false
        }

    override val activeUserId: Flow<Long> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_ACTIVE_USER_ID] ?: 0L
        }

    override val sessionToken: Flow<String?> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_SESSION_TOKEN]
        }

    override val userNamePreference: Flow<String?> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_USER_NAME]
        }

    override val userEmailPreference: Flow<String?> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_USER_EMAIL]
        }

    override val currencySymbol: Flow<String> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_CURRENCY_SYMBOL] ?: "Rp"
        }

    override val selectedCurrencyCode: Flow<String> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_SELECTED_CURRENCY_CODE] ?: "IDR"
        }

    override val currencyRateFromIdr: Flow<Double> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_CURRENCY_RATE_FROM_IDR] ?: 1.0
        }

    override val cachedRatesJson: Flow<String> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_CACHED_RATES_JSON] ?: ""
        }

    override val themePreference: Flow<String> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_THEME_PREFERENCE] ?: "SYSTEM"
        }

    override val notificationsEnabled: Flow<Boolean> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
        }

    override val paydayStartDay: Flow<Int> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_PAYDAY_START_DAY] ?: 1
        }

    override val lastProcessedPaydayCycle: Flow<String> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_LAST_PROCESSED_PAYDAY_CYCLE] ?: ""
        }

    override val lastActiveTimestamp: Flow<Long> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_LAST_ACTIVE_TIMESTAMP] ?: 0L
        }

    override val lastBackgroundTimestamp: Flow<Long> = appContext.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_LAST_BACKGROUND_TIMESTAMP] ?: sessionPrefs.getLong("last_background_timestamp", 0L)
        }

    override fun isSessionLoggedIn(): Boolean {
        return sessionPrefs.getBoolean("is_logged_in", false)
    }

    override fun getLastActiveTimestamp(): Long {
        val ts = sessionPrefs.getLong("last_active_timestamp", 0L)
        if (ts > 0L) return ts
        return sessionPrefs.getLong("last_background_timestamp", 0L)
    }

    override fun saveLastActiveTimestampSync(timestamp: Long) {
        sessionPrefs.edit()
            .putLong("last_active_timestamp", timestamp)
            .putLong("last_background_timestamp", timestamp)
            .apply()
    }

    override fun getLastBackgroundTimestamp(): Long {
        return getLastActiveTimestamp()
    }

    override fun saveLastBackgroundTimestampSync(timestamp: Long) {
        saveLastActiveTimestampSync(timestamp)
    }

    override fun isInitialSessionValid(timeoutMillis: Long): Boolean {
        if (!com.example.monethra.data.session.SessionManager.isProcessAlive) {
            return false
        }
        val loggedIn = sessionPrefs.getBoolean("is_logged_in", false)
        if (!loggedIn) return false
        val lastActive = getLastActiveTimestamp()
        if (lastActive > 0L) {
            val elapsed = System.currentTimeMillis() - lastActive
            if (elapsed >= timeoutMillis) {
                sessionPrefs.edit()
                    .putBoolean("is_logged_in", false)
                    .putLong("last_background_timestamp", 0L)
                    .putLong("last_active_timestamp", 0L)
                    .apply()
                return false
            }
        }
        return true
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setSession(isLoggedIn: Boolean, token: String, name: String, email: String, userId: Long) {
        val now = if (isLoggedIn) System.currentTimeMillis() else 0L
        sessionPrefs.edit()
            .putBoolean("is_logged_in", isLoggedIn)
            .putLong("active_user_id", userId)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("session_token", token)
            .putLong("last_background_timestamp", now)
            .putLong("last_active_timestamp", now)
            .apply()

        appContext.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = isLoggedIn
            preferences[KEY_SESSION_TOKEN] = token
            if (name.isNotBlank()) preferences[KEY_USER_NAME] = name
            if (email.isNotBlank()) preferences[KEY_USER_EMAIL] = email
            preferences[KEY_ACTIVE_USER_ID] = userId
            preferences[KEY_LAST_BACKGROUND_TIMESTAMP] = now
            if (isLoggedIn) {
                preferences[KEY_LAST_ACTIVE_TIMESTAMP] = now
            } else {
                preferences[KEY_LAST_ACTIVE_TIMESTAMP] = 0L
            }
        }
    }

    override suspend fun clearSession() {
        sessionPrefs.edit()
            .putBoolean("is_logged_in", false)
            .putLong("active_user_id", 0L)
            .putString("user_email", "")
            .putString("user_name", "")
            .putString("session_token", "")
            .putLong("last_background_timestamp", 0L)
            .putLong("last_active_timestamp", 0L)
            .apply()

        appContext.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            preferences[KEY_LAST_BACKGROUND_TIMESTAMP] = 0L
            preferences.remove(KEY_SESSION_TOKEN)
            preferences.remove(KEY_ACTIVE_USER_ID)
            preferences.remove(KEY_LAST_ACTIVE_TIMESTAMP)
        }
    }

    override suspend fun saveLastBackgroundTimestamp(timestamp: Long) {
        saveLastActiveTimestampSync(timestamp)
        appContext.dataStore.edit { preferences ->
            preferences[KEY_LAST_BACKGROUND_TIMESTAMP] = timestamp
            preferences[KEY_LAST_ACTIVE_TIMESTAMP] = timestamp
        }
    }

    override suspend fun updateLastActiveTimestamp(timestamp: Long) {
        saveLastActiveTimestampSync(timestamp)
        appContext.dataStore.edit { preferences ->
            preferences[KEY_LAST_ACTIVE_TIMESTAMP] = timestamp
            preferences[KEY_LAST_BACKGROUND_TIMESTAMP] = timestamp
        }
    }

    override suspend fun checkAndInvalidateSessionIfExpired(timeoutMillis: Long, currentTime: Long): Boolean {
        val preferences = appContext.dataStore.data
            .catch { emit(emptyPreferences()) }
            .first()
        val loggedIn = preferences[KEY_IS_LOGGED_IN] ?: sessionPrefs.getBoolean("is_logged_in", false)
        if (!loggedIn) {
            return false
        }
        val lastActive = preferences[KEY_LAST_ACTIVE_TIMESTAMP]
            ?: (preferences[KEY_LAST_BACKGROUND_TIMESTAMP] ?: getLastActiveTimestamp())
        if ((lastActive > 0L) && ((currentTime - lastActive) >= timeoutMillis)) {
            clearSession()
            return true
        }
        return false
    }

    override suspend fun setCurrencySymbol(symbol: String) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_SYMBOL] = symbol
        }
    }

    override suspend fun setSelectedCurrency(code: String, symbol: String, rateFromIdr: Double) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_CURRENCY_CODE] = code
            preferences[KEY_CURRENCY_SYMBOL] = symbol
            preferences[KEY_CURRENCY_RATE_FROM_IDR] = rateFromIdr
        }
    }

    override suspend fun setCachedRatesJson(json: String) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_CACHED_RATES_JSON] = json
        }
    }

    override suspend fun setThemePreference(theme: String) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_THEME_PREFERENCE] = theme
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun setPaydayStartDay(day: Int) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_PAYDAY_START_DAY] = day.coerceIn(1, 28)
        }
    }

    override suspend fun setLastProcessedPaydayCycle(cycleKey: String) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_LAST_PROCESSED_PAYDAY_CYCLE] = cycleKey
        }
    }

    override suspend fun clearPreferences() {
        appContext.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
