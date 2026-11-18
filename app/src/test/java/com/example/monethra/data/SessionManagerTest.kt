package com.example.monethra.data

import android.content.SharedPreferences
import com.example.monethra.data.session.SessionManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    private class FakeEditor(private val values: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            temp[key] = value
            return this
        }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            temp[key] = values
            return this
        }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            temp[key] = value
            return this
        }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            temp[key] = value
            return this
        }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            temp[key] = value
            return this
        }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            temp[key] = value
            return this
        }
        override fun remove(key: String): SharedPreferences.Editor {
            temp.remove(key)
            values.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            temp.clear()
            values.clear()
            return this
        }
        override fun commit(): Boolean {
            values.putAll(temp)
            return true
        }
        override fun apply() {
            values.putAll(temp)
        }
    }

    private class FakeSharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = map
        override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (map[key] as? Set<String>) ?: defValues
        override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        sessionManager = SessionManager(injectedSharedPrefs = fakePrefs)
    }

    @Test
    fun setSession_savesLoggedInAndTimestamp() = runTest {
        sessionManager.setSession(isLoggedIn = true, activeUserId = 42L, userEmail = "test@example.com")

        assertTrue(sessionManager.isSessionLoggedIn())
        assertTrue(sessionManager.getLastActiveTimestamp() > 0L)
    }

    @Test
    fun clearSession_clearsAllSessionData() = runTest {
        sessionManager.setSession(isLoggedIn = true, activeUserId = 42L, userEmail = "test@example.com")
        sessionManager.clearSession()

        assertFalse(sessionManager.isSessionLoggedIn())
        assertEquals(0L, sessionManager.getLastActiveTimestamp())
    }

    @Test
    fun isSessionExpired_withinFiveMinutes_returnsFalse() = runTest {
        val now = 1_000_000L
        sessionManager.setSession(isLoggedIn = true, activeUserId = 1L, userEmail = "user@monethra.app")
        sessionManager.saveLastActiveTimestampSync(now)

        // 4 minutes later
        val expired = sessionManager.isSessionExpired(currentTime = now + (4 * 60 * 1000L))
        assertFalse(expired)
    }

    @Test
    fun isSessionExpired_afterFiveMinutes_returnsTrue() = runTest {
        val now = 1_000_000L
        sessionManager.setSession(isLoggedIn = true, activeUserId = 1L, userEmail = "user@monethra.app")
        sessionManager.saveLastActiveTimestampSync(now)

        // 5 minutes and 1 second later
        val expired = sessionManager.isSessionExpired(currentTime = now + SessionManager.SESSION_TIMEOUT_MS + 1000L)
        assertTrue(expired)
    }

    @Test
    fun clearSessionSync_clearsSessionSynchronously() = runTest {
        sessionManager.setSession(isLoggedIn = true, activeUserId = 99L, userEmail = "sync@example.com")
        assertTrue(sessionManager.isSessionLoggedIn())

        sessionManager.clearSessionSync()
        assertFalse(sessionManager.isSessionLoggedIn())
        assertEquals(0L, sessionManager.getLastActiveTimestamp())
    }

    @Test
    fun isProcessAlive_flag_tracksState() {
        SessionManager.isProcessAlive = false
        assertFalse(SessionManager.isProcessAlive)

        SessionManager.isProcessAlive = true
        assertTrue(SessionManager.isProcessAlive)
    }
}
