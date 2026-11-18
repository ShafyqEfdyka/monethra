package com.example.monethra.viewmodel

import com.example.monethra.data.dao.UserDao
import com.example.monethra.data.datastore.UserPreferencesRepository
import com.example.monethra.data.entity.UserEntity
import com.example.monethra.data.repository.AuthRepository
import com.example.monethra.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestUserDao : UserDao {
        val users = mutableListOf<UserEntity>()
        val userFlow = MutableStateFlow<UserEntity?>(null)

        override fun getUser(): Flow<UserEntity?> = userFlow
        override suspend fun getUserSync(): UserEntity? = userFlow.value ?: users.firstOrNull()
        override fun getUserById(userId: Long): Flow<UserEntity?> = flowOf(users.find { it.id == userId })
        override suspend fun getUserByIdSync(userId: Long): UserEntity? = users.find { it.id == userId }
        override suspend fun getUserByEmail(email: String): UserEntity? = users.find { it.email == email }
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
        val isLoggedInFlow = MutableStateFlow(false)
        val activeUserIdFlow = MutableStateFlow(0L)
        val tokenFlow = MutableStateFlow<String?>("")
        val userNameFlow = MutableStateFlow<String?>("")
        val userEmailFlow = MutableStateFlow<String?>("")
        val lastActiveFlow = MutableStateFlow(0L)
        val lastBackgroundFlow = MutableStateFlow(0L)

        override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
        override val isLoggedIn: Flow<Boolean> = isLoggedInFlow
        override val activeUserId: Flow<Long> = activeUserIdFlow
        override val sessionToken: Flow<String?> = tokenFlow
        override val userNamePreference: Flow<String?> = userNameFlow
        override val userEmailPreference: Flow<String?> = userEmailFlow
        override val currencySymbol: Flow<String> = flowOf("Rp")
        override val themePreference: Flow<String> = flowOf("SYSTEM")
        override val notificationsEnabled: Flow<Boolean> = flowOf(true)
        override val paydayStartDay: Flow<Int> = flowOf(1)
        override val lastProcessedPaydayCycle: Flow<String> = flowOf("")
        override val lastActiveTimestamp: Flow<Long> = lastActiveFlow
        override val lastBackgroundTimestamp: Flow<Long> = lastBackgroundFlow

        override fun isSessionLoggedIn(): Boolean = isLoggedInFlow.value
        override fun getLastActiveTimestamp(): Long = if (lastActiveFlow.value > 0L) lastActiveFlow.value else lastBackgroundFlow.value
        override fun saveLastActiveTimestampSync(timestamp: Long) {
            lastActiveFlow.value = timestamp
            lastBackgroundFlow.value = timestamp
        }
        override fun getLastBackgroundTimestamp(): Long = lastBackgroundFlow.value
        override fun saveLastBackgroundTimestampSync(timestamp: Long) {
            lastBackgroundFlow.value = timestamp
            lastActiveFlow.value = timestamp
        }
        override fun isInitialSessionValid(timeoutMillis: Long): Boolean {
            if (!isLoggedInFlow.value) return false
            if (lastBackgroundFlow.value > 0L) {
                if ((System.currentTimeMillis() - lastBackgroundFlow.value) > timeoutMillis) {
                    isLoggedInFlow.value = false
                    lastBackgroundFlow.value = 0L
                    return false
                }
            }
            return true
        }

        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setSession(isLoggedIn: Boolean, token: String, name: String, email: String, userId: Long) {
            isLoggedInFlow.value = isLoggedIn
            tokenFlow.value = token
            if (name.isNotBlank()) userNameFlow.value = name
            if (email.isNotBlank()) userEmailFlow.value = email
            activeUserIdFlow.value = userId
            lastBackgroundFlow.value = 0L
            if (isLoggedIn) {
                lastActiveFlow.value = System.currentTimeMillis()
            }
        }
        override suspend fun clearSession() {
            isLoggedInFlow.value = false
            tokenFlow.value = null
            activeUserIdFlow.value = 0L
            lastActiveFlow.value = 0L
            lastBackgroundFlow.value = 0L
        }
        override suspend fun saveLastBackgroundTimestamp(timestamp: Long) {
            lastBackgroundFlow.value = timestamp
        }
        override suspend fun updateLastActiveTimestamp(timestamp: Long) {
            lastActiveFlow.value = timestamp
        }
        override suspend fun checkAndInvalidateSessionIfExpired(timeoutMillis: Long, currentTime: Long): Boolean {
            if (!isLoggedInFlow.value) return false
            val checkTime = if (lastBackgroundFlow.value > 0L) lastBackgroundFlow.value else lastActiveFlow.value
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
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        SessionManager.isProcessAlive = true
        Dispatchers.setMain(testDispatcher)
        userDao = TestUserDao()
        prefsRepo = TestUserPreferencesRepository()
        authRepository = AuthRepository(userDao, prefsRepo, testDispatcher)
        viewModel = AuthViewModel(authRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signUp_invalidEmail_showsEmailError() = runTest {
        viewModel.onSignUpFullNameChanged("John Doe")
        viewModel.onSignUpEmailChanged("invalidemail")
        viewModel.onSignUpPasswordChanged("123456")
        viewModel.onSignUpConfirmPasswordChanged("123456")

        viewModel.signUp()

        val state = viewModel.signUpState.value
        assertNotNull(state.emailError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun signUp_shortPassword_showsPasswordError() = runTest {
        viewModel.onSignUpFullNameChanged("John Doe")
        viewModel.onSignUpEmailChanged("john@example.com")
        viewModel.onSignUpPasswordChanged("12345")
        viewModel.onSignUpConfirmPasswordChanged("12345")

        viewModel.signUp()

        val state = viewModel.signUpState.value
        assertNotNull(state.passwordError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun signUp_mismatchedPasswords_showsConfirmPasswordError() = runTest {
        viewModel.onSignUpFullNameChanged("John Doe")
        viewModel.onSignUpEmailChanged("john@example.com")
        viewModel.onSignUpPasswordChanged("123456")
        viewModel.onSignUpConfirmPasswordChanged("654321")

        viewModel.signUp()

        val state = viewModel.signUpState.value
        assertNotNull(state.confirmPasswordError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun signUp_validInputs_success() = runTest {
        viewModel.onSignUpFullNameChanged("John Doe")
        viewModel.onSignUpEmailChanged("john@example.com")
        viewModel.onSignUpPasswordChanged("123456")
        viewModel.onSignUpConfirmPasswordChanged("123456")

        var callbackCalled = false
        viewModel.signUp(onSuccess = { callbackCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.signUpState.value
        assertTrue(state.isSuccess)
        assertTrue(callbackCalled)
        assertTrue(prefsRepo.isLoggedInFlow.value)
        assertEquals("John Doe", prefsRepo.userNameFlow.value)
    }

    @Test
    fun login_blankFields_showsError() = runTest {
        viewModel.onLoginEmailChanged("")
        viewModel.onLoginPasswordChanged("")

        viewModel.login()

        val state = viewModel.loginState.value
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun login_validCredentials_success() = runTest {
        userDao.insertUser(UserEntity(1L, "John Doe", "john@example.com", "123456"))

        viewModel.onLoginEmailChanged("john@example.com")
        viewModel.onLoginPasswordChanged("123456")

        var callbackCalled = false
        viewModel.login(onSuccess = { callbackCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.loginState.value
        assertTrue(state.isSuccess)
        assertTrue(callbackCalled)
        assertTrue(prefsRepo.isLoggedInFlow.value)
    }

    @Test
    fun login_demoAccount_success() = runTest {
        viewModel.onLoginEmailChanged("monthra@andro.app")
        viewModel.onLoginPasswordChanged("1234")

        var callbackCalled = false
        viewModel.login(onSuccess = { callbackCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.loginState.value
        assertTrue(state.isSuccess)
        assertTrue(callbackCalled)
        assertTrue(prefsRepo.isLoggedInFlow.value)
        assertEquals("Monethra Demo", prefsRepo.userNameFlow.value)
    }

    @Test
    fun logout_clearsSession() = runTest {
        prefsRepo.isLoggedInFlow.value = true

        var callbackCalled = false
        viewModel.logout(onComplete = { callbackCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(prefsRepo.isLoggedInFlow.value)
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
        assertTrue(callbackCalled)
    }

    @Test
    fun logoutDueToTimeout_clearsSession() = runTest {
        prefsRepo.isLoggedInFlow.value = true

        var callbackCalled = false
        viewModel.logoutDueToTimeout(onComplete = { callbackCalled = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(prefsRepo.isLoggedInFlow.value)
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
        assertTrue(callbackCalled)
    }

    @Test
    fun authViewModel_initializesWithPersistedSession() = runTest {
        SessionManager.isProcessAlive = true
        prefsRepo.isLoggedInFlow.value = true
        val vm = AuthViewModel(authRepository, testDispatcher)
        assertTrue(vm.isLoggedIn.value)
        assertTrue(vm.authState.value is AuthState.Authenticated)
    }

    @Test
    fun authViewModel_coldStart_clearsPersistedSession() = runTest {
        SessionManager.isProcessAlive = false
        prefsRepo.isLoggedInFlow.value = true
        val vm = AuthViewModel(authRepository, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(prefsRepo.isLoggedInFlow.value)
        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertTrue(SessionManager.isProcessAlive)
    }

    @Test
    fun onUserInteraction_whenLoggedIn_restartsInactivityTimer() = runTest {
        prefsRepo.isLoggedInFlow.value = true
        val vm = AuthViewModel(authRepository, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        // 4 minutes pass
        testDispatcher.scheduler.advanceTimeBy(4 * 60 * 1000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.isLoggedIn.value)

        // User interacts (screen touch / scroll)
        vm.onUserInteraction()

        // Another 4 minutes pass (8 minutes total, but timer was reset at min 4)
        testDispatcher.scheduler.advanceTimeBy(4 * 60 * 1000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(vm.isLoggedIn.value)

        // 2 more minutes pass (6 minutes since last interaction -> exceeds 5 minutes)
        testDispatcher.scheduler.advanceTimeBy(2 * 60 * 1000L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(prefsRepo.isLoggedInFlow.value)
    }
}
