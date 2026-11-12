package com.example.monethra.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val isOnboardingCompleted: Flow<Boolean>
    val isLoggedIn: Flow<Boolean>
    val activeUserId: Flow<Long>
    val sessionToken: Flow<String?>
    val userNamePreference: Flow<String?>
    val userEmailPreference: Flow<String?>
    val currencySymbol: Flow<String>
    val themePreference: Flow<String>
    val notificationsEnabled: Flow<Boolean>
    val paydayStartDay: Flow<Int>
    val lastProcessedPaydayCycle: Flow<String>
    val lastActiveTimestamp: Flow<Long>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setSession(isLoggedIn: Boolean, token: String = "", name: String = "", email: String = "", userId: Long = 0L)
    suspend fun clearSession()
    suspend fun updateLastActiveTimestamp(timestamp: Long = System.currentTimeMillis())
    suspend fun checkAndInvalidateSessionIfExpired(timeoutMillis: Long = 60_000L, currentTime: Long = System.currentTimeMillis()): Boolean
    suspend fun setCurrencySymbol(symbol: String)
    suspend fun setThemePreference(theme: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setPaydayStartDay(day: Int)
    suspend fun setLastProcessedPaydayCycle(cycleKey: String)
    suspend fun clearPreferences()
}

class UserPreferencesRepositoryImpl(context: Context) : UserPreferencesRepository {

    private val appContext = context.applicationContext

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_ACTIVE_USER_ID = longPreferencesKey("active_user_id")
        private val KEY_SESSION_TOKEN = stringPreferencesKey("session_token")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val KEY_THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_PAYDAY_START_DAY = intPreferencesKey("payday_start_day")
        private val KEY_LAST_PROCESSED_PAYDAY_CYCLE = stringPreferencesKey("last_processed_payday_cycle")
        private val KEY_LAST_ACTIVE_TIMESTAMP = longPreferencesKey("last_active_timestamp")
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

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setSession(isLoggedIn: Boolean, token: String, name: String, email: String, userId: Long) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = isLoggedIn
            preferences[KEY_SESSION_TOKEN] = token
            if (name.isNotBlank()) preferences[KEY_USER_NAME] = name
            if (email.isNotBlank()) preferences[KEY_USER_EMAIL] = email
            preferences[KEY_ACTIVE_USER_ID] = userId
            if (isLoggedIn) {
                preferences[KEY_LAST_ACTIVE_TIMESTAMP] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun clearSession() {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            preferences.remove(KEY_SESSION_TOKEN)
            preferences.remove(KEY_ACTIVE_USER_ID)
            preferences.remove(KEY_LAST_ACTIVE_TIMESTAMP)
        }
    }

    override suspend fun updateLastActiveTimestamp(timestamp: Long) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_LAST_ACTIVE_TIMESTAMP] = timestamp
        }
    }

    override suspend fun checkAndInvalidateSessionIfExpired(timeoutMillis: Long, currentTime: Long): Boolean {
        val preferences = appContext.dataStore.data
            .catch { emit(emptyPreferences()) }
            .first()
        val isLoggedIn = preferences[KEY_IS_LOGGED_IN] ?: false
        if (!isLoggedIn) {
            return false
        }
        val lastActive = preferences[KEY_LAST_ACTIVE_TIMESTAMP] ?: 0L
        if ((currentTime - lastActive) > timeoutMillis) {
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
