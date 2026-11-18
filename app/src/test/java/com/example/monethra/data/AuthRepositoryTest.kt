package com.example.monethra.data

import com.example.monethra.data.dao.UserDao
import com.example.monethra.data.datastore.UserPreferencesRepository
import com.example.monethra.data.entity.UserEntity
import com.example.monethra.data.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private class TestUserDao : UserDao {
        val users = mutableListOf<UserEntity>()
        val userFlow = MutableStateFlow<UserEntity?>(null)

        override fun getUser(): Flow<UserEntity?> = userFlow
        override suspend fun getUserSync(): UserEntity? = userFlow.value ?: users.firstOrNull()
        override fun getUserById(userId: Long): Flow<UserEntity?> = flowOf(users.find { it.id == userId })
        override suspend fun getUserByIdSync(userId: Long): UserEntity? = users.find { it.id == userId }
        override suspend fun getUserByEmail(email: String): UserEntity? = users.find { it.email.equals(email, ignoreCase = true) }
        override suspend fun getUserCount(): Int = users.size
        override suspend fun insertUser(user: UserEntity): Long {
            val newUser = user.copy(id = (users.size + 1).toLong())
            users.add(newUser)
            userFlow.value = newUser
            return newUser.id
        }
        override suspend fun updateUser(user: UserEntity) {
            val idx = users.indexOfFirst { it.id == user.id || it.email == user.email }
            if (idx >= 0) {
                users[idx] = user
            } else {
                users.add(user)
            }
            userFlow.value = user
        }
        override suspend fun deleteUser(user: UserEntity) {
            users.remove(user)
            if (userFlow.value?.id == user.id) userFlow.value = null
        }
        override suspend fun deleteAllUsers() {
            users.clear()
            userFlow.value = null
        }
    }

    private class TestUserPreferencesRepository : UserPreferencesRepository {
        var loggedIn = false
        var token: String? = null
        var name: String? = null
        var email: String? = null
        var activeId: Long = 0L
        var lastActive: Long = 0L
        var lastBackground: Long = 0L

        override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
        override val isLoggedIn: Flow<Boolean> get() = flowOf(loggedIn)
        override val activeUserId: Flow<Long> get() = flowOf(activeId)
        override val sessionToken: Flow<String?> get() = flowOf(token)
        override val userNamePreference: Flow<String?> get() = flowOf(name)
        override val userEmailPreference: Flow<String?> get() = flowOf(email)
        override val currencySymbol: Flow<String> = flowOf("Rp")
        override val themePreference: Flow<String> = flowOf("SYSTEM")
        override val notificationsEnabled: Flow<Boolean> = flowOf(true)
        override val paydayStartDay: Flow<Int> = flowOf(1)
        override val lastProcessedPaydayCycle: Flow<String> = flowOf("")
        override val lastActiveTimestamp: Flow<Long> get() = flowOf(lastActive)
        override val lastBackgroundTimestamp: Flow<Long> get() = flowOf(lastBackground)

        override fun isSessionLoggedIn(): Boolean = loggedIn
        override fun getLastBackgroundTimestamp(): Long = lastBackground
        override fun saveLastBackgroundTimestampSync(timestamp: Long) {
            this.lastBackground = timestamp
        }
        override fun isInitialSessionValid(timeoutMillis: Long): Boolean {
            if (!loggedIn) return false
            if (lastBackground > 0L) {
                if ((System.currentTimeMillis() - lastBackground) > timeoutMillis) {
                    clearSessionSync()
                    return false
                }
            }
            return true
        }

        private fun clearSessionSync() {
            this.loggedIn = false
            this.token = null
            this.activeId = 0L
            this.lastActive = 0L
            this.lastBackground = 0L
        }

        override fun getLastActiveTimestamp(): Long = if (lastActive > 0L) lastActive else lastBackground
        override fun saveLastActiveTimestampSync(timestamp: Long) {
            this.lastActive = timestamp
            this.lastBackground = timestamp
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setSession(isLoggedIn: Boolean, token: String, name: String, email: String, userId: Long) {
            this.loggedIn = isLoggedIn
            this.token = token
            if (name.isNotBlank()) this.name = name
            if (email.isNotBlank()) this.email = email
            this.activeId = userId
            this.lastBackground = 0L
            if (isLoggedIn) {
                this.lastActive = System.currentTimeMillis()
            }
        }
        override suspend fun clearSession() {
            clearSessionSync()
        }
        override suspend fun saveLastBackgroundTimestamp(timestamp: Long) {
            this.lastBackground = timestamp
            this.lastActive = timestamp
        }
        override suspend fun updateLastActiveTimestamp(timestamp: Long) {
            this.lastActive = timestamp
        }
        override suspend fun checkAndInvalidateSessionIfExpired(timeoutMillis: Long, currentTime: Long): Boolean {
            if (!loggedIn) return false
            val checkTime = if (lastBackground > 0L) lastBackground else lastActive
            if ((currentTime - checkTime) > timeoutMillis) {
                clearSession()
                return true
            }
            return false
        }
        override suspend fun setCurrencySymbol(symbol: String) {}
        override suspend fun setThemePreference(theme: String) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setPaydayStartDay(day: Int) {}
        override suspend fun setLastProcessedPaydayCycle(cycleKey: String) {}
        override suspend fun clearPreferences() {}
    }

    private lateinit var userDao: TestUserDao
    private lateinit var prefsRepo: TestUserPreferencesRepository
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        userDao = TestUserDao()
        prefsRepo = TestUserPreferencesRepository()
        authRepository = AuthRepository(userDao, prefsRepo)
    }

    @Test
    fun signUp_savesUserAndSetsSession() = runTest {
        val result = authRepository.signUp("Alice Smith", "alice@monethra.app", "pass1234")

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertEquals("Alice Smith", user?.name)
        assertEquals("alice@monethra.app", user?.email)
        assertTrue(prefsRepo.loggedIn)
        assertEquals("Alice Smith", prefsRepo.name)
    }

    @Test
    fun login_existingUser_succeeds() = runTest {
        authRepository.signUp("Bob Ross", "bob@monethra.app", "secret123")

        val loginResult = authRepository.login("bob@monethra.app", "secret123")

        assertTrue(loginResult.isSuccess)
        assertEquals("Bob Ross", loginResult.getOrNull()?.name)
        assertTrue(prefsRepo.loggedIn)
    }

    @Test
    fun login_demoAccount_succeedsDirectly() = runTest {
        val loginResult = authRepository.login("monthra@andro.app", "1234")

        assertTrue(loginResult.isSuccess)
        val user = loginResult.getOrNull()
        assertEquals("Monethra Demo", user?.name)
        assertEquals("monthra@andro.app", user?.email)
        assertTrue(prefsRepo.loggedIn)
    }

    @Test
    fun login_demoAccount_wrongPassword_fails() = runTest {
        val loginResult = authRepository.login("monthra@andro.app", "wrong")

        assertTrue(loginResult.isFailure)
    }

    @Test
    fun login_wrongPassword_fails() = runTest {
        authRepository.signUp("Bob Ross", "bob@monethra.app", "secret123")

        val loginResult = authRepository.login("bob@monethra.app", "wrongpass")

        assertTrue(loginResult.isFailure)
    }

    @Test
    fun logout_resetsSessionFlag() = runTest {
        authRepository.signUp("Bob Ross", "bob@monethra.app", "secret123")
        assertTrue(prefsRepo.loggedIn)

        authRepository.logout()

        assertFalse(prefsRepo.loggedIn)
    }

    @Test
    fun validateSession_withinOneMinute_returnsFalse() = runTest {
        authRepository.signUp("Alice Smith", "alice@monethra.app", "pass1234")
        assertTrue(prefsRepo.loggedIn)

        val expired = prefsRepo.checkAndInvalidateSessionIfExpired(
            timeoutMillis = 60_000L,
            currentTime = prefsRepo.lastActive + 30_000L
        )

        assertFalse(expired)
        assertTrue(prefsRepo.loggedIn)
    }

    @Test
    fun validateSession_afterOneMinute_returnsTrueAndClearsSession() = runTest {
        authRepository.signUp("Alice Smith", "alice@monethra.app", "pass1234")
        assertTrue(prefsRepo.loggedIn)

        val expired = prefsRepo.checkAndInvalidateSessionIfExpired(
            timeoutMillis = 60_000L,
            currentTime = prefsRepo.lastActive + 61_000L
        )

        assertTrue(expired)
        assertFalse(prefsRepo.loggedIn)
    }

    @Test
    fun login_recordsLastActiveTimestamp() = runTest {
        val nowBefore = System.currentTimeMillis()
        val loginResult = authRepository.login("monthra@andro.app", "1234")

        assertTrue(loginResult.isSuccess)
        assertTrue(prefsRepo.lastActive >= nowBefore)
    }

    @Test
    fun session_withinFiveMinutes_remainsValid() = runTest {
        authRepository.signUp("Alice Smith", "alice@monethra.app", "pass1234")
        assertTrue(authRepository.isSessionLoggedIn())

        val bgTime = 100_000L
        authRepository.saveLastActiveTimestamp(bgTime)
        assertEquals(bgTime, authRepository.getLastActiveTimestamp())

        // 3 minutes in background (within 5 minutes)
        val expired = prefsRepo.checkAndInvalidateSessionIfExpired(
            timeoutMillis = AuthRepository.SESSION_TIMEOUT_MS,
            currentTime = bgTime + (3 * 60 * 1000L)
        )

        assertFalse(expired)
        assertTrue(authRepository.isSessionLoggedIn())
    }

    @Test
    fun session_afterFiveMinutes_expires() = runTest {
        authRepository.signUp("Alice Smith", "alice@monethra.app", "pass1234")
        assertTrue(authRepository.isSessionLoggedIn())

        val bgTime = 100_000L
        authRepository.saveLastActiveTimestamp(bgTime)

        // 6 minutes in background (after 5 minutes)
        val expired = prefsRepo.checkAndInvalidateSessionIfExpired(
            timeoutMillis = AuthRepository.SESSION_TIMEOUT_MS,
            currentTime = bgTime + (6 * 60 * 1000L)
        )

        assertTrue(expired)
        assertFalse(authRepository.isSessionLoggedIn())
        assertEquals(0L, authRepository.getLastActiveTimestamp())
    }
}
