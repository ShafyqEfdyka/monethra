package com.example.monethra.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.monethra.data.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SessionManager(
    context: Context? = null,
    injectedSharedPrefs: SharedPreferences? = null,
    injectedDataStore: DataStore<Preferences>? = null
) {

    companion object {
        var isProcessAlive: Boolean = false
        const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        private const val PREFS_NAME = "monethra_session"
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_ACTIVE_USER_ID = longPreferencesKey("active_user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_LAST_ACTIVE_TIMESTAMP = longPreferencesKey("last_active_timestamp")
    }

    private val sharedPrefs: SharedPreferences = injectedSharedPrefs
        ?: (context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?: error("Context or SharedPreferences required"))

    private val dataStore: DataStore<Preferences>? = injectedDataStore
        ?: context?.applicationContext?.dataStore

    val isLoggedIn: Flow<Boolean> = (dataStore?.data ?: flowOf(emptyPreferences()))
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_IS_LOGGED_IN] ?: sharedPrefs.getBoolean("is_logged_in", false)
        }

    val activeUserId: Flow<Long> = (dataStore?.data ?: flowOf(emptyPreferences()))
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_ACTIVE_USER_ID] ?: sharedPrefs.getLong("active_user_id", 0L)
        }

    val userEmail: Flow<String> = (dataStore?.data ?: flowOf(emptyPreferences()))
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_USER_EMAIL] ?: (sharedPrefs.getString("user_email", "") ?: "")
        }

    val lastActiveTimestamp: Flow<Long> = (dataStore?.data ?: flowOf(emptyPreferences()))
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[KEY_LAST_ACTIVE_TIMESTAMP] ?: sharedPrefs.getLong("last_active_timestamp", 0L)
        }

    fun isSessionLoggedIn(): Boolean {
        return sharedPrefs.getBoolean("is_logged_in", false)
    }

    fun getLastActiveTimestamp(): Long {
        val ts = sharedPrefs.getLong("last_active_timestamp", 0L)
        if (ts > 0L) return ts
        return sharedPrefs.getLong("last_background_timestamp", 0L)
    }

    fun saveLastActiveTimestampSync(timestamp: Long) {
        sharedPrefs.edit()
            .putLong("last_active_timestamp", timestamp)
            .putLong("last_background_timestamp", timestamp)
            .apply()
    }

    suspend fun saveLastActiveTimestamp(timestamp: Long) {
        saveLastActiveTimestampSync(timestamp)
        dataStore?.edit { preferences ->
            preferences[KEY_LAST_ACTIVE_TIMESTAMP] = timestamp
        }
    }

    suspend fun setSession(
        isLoggedIn: Boolean,
        activeUserId: Long = 0L,
        userEmail: String = "",
        sessionToken: String = ""
    ) {
        isProcessAlive = true
        val timestamp = if (isLoggedIn) System.currentTimeMillis() else 0L
        sharedPrefs.edit()
            .putBoolean("is_logged_in", isLoggedIn)
            .putLong("active_user_id", activeUserId)
            .putString("user_email", userEmail)
            .putString("session_token", sessionToken)
            .putLong("last_active_timestamp", timestamp)
            .putLong("last_background_timestamp", timestamp)
            .apply()

        dataStore?.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = isLoggedIn
            preferences[KEY_ACTIVE_USER_ID] = activeUserId
            preferences[KEY_USER_EMAIL] = userEmail
            preferences[KEY_LAST_ACTIVE_TIMESTAMP] = timestamp
        }
    }

    fun clearSessionSync() {
        sharedPrefs.edit()
            .putBoolean("is_logged_in", false)
            .putLong("active_user_id", 0L)
            .putString("user_email", "")
            .putString("user_name", "")
            .putString("session_token", "")
            .putLong("last_active_timestamp", 0L)
            .putLong("last_background_timestamp", 0L)
            .apply()
    }

    suspend fun clearSession() {
        clearSessionSync()
        dataStore?.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            preferences[KEY_ACTIVE_USER_ID] = 0L
            preferences[KEY_USER_EMAIL] = ""
            preferences[KEY_LAST_ACTIVE_TIMESTAMP] = 0L
        }
    }

    fun isSessionExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        if (!isSessionLoggedIn()) return false
        val lastActive = getLastActiveTimestamp()
        if (lastActive <= 0L) return false
        return (currentTime - lastActive) >= SESSION_TIMEOUT_MS
    }
}
