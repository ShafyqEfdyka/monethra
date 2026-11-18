package com.example.monethra.data.repository

import com.example.monethra.data.dao.UserDao
import com.example.monethra.data.datastore.UserPreferencesRepository
import com.example.monethra.data.entity.UserEntity
import com.example.monethra.utils.SecurityUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepository(
    private val userDao: UserDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        const val DEMO_EMAIL = "monthra@andro.app"
        const val DEMO_PASSWORD = "1234"
        const val DEMO_NAME = "Monethra Demo"
    }

    @get:JvmName("getCurrentUserProperty")
    val currentUser: Flow<UserEntity?> get() = userPreferencesRepository.activeUserId.flatMapLatest { id ->
        if (id > 0) userDao.getUserById(id) else userDao.getUser()
    }

    @get:JvmName("isLoggedInProperty")
    val isLoggedIn: Flow<Boolean> get() = userPreferencesRepository.isLoggedIn

    fun getCurrentUser(): Flow<UserEntity?> = userPreferencesRepository.activeUserId.flatMapLatest { id ->
        if (id > 0) userDao.getUserById(id) else userDao.getUser()
    }

    fun getCurrentUserFlow(): Flow<UserEntity?> = getCurrentUser()

    fun isLoggedIn(): Flow<Boolean> = userPreferencesRepository.isLoggedIn

    suspend fun getCurrentUserSync(): UserEntity? = withContext(ioDispatcher) {
        try {
            val activeId = userPreferencesRepository.activeUserId.firstOrNull() ?: 0L
            if (activeId > 0) userDao.getUserByIdSync(activeId) else userDao.getUserSync()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(ioDispatcher) {
        try {
            val cleanEmail = email.trim()
            val cleanPassword = password.trim()

            if (cleanEmail.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Email cannot be empty"))
            }
            if (cleanPassword.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Password cannot be empty"))
            }

            // Demo Account Check
            if (cleanEmail.equals(DEMO_EMAIL, ignoreCase = true)) {
                if (cleanPassword != DEMO_PASSWORD) {
                    return@withContext Result.failure(Exception("Invalid email or password."))
                }
                var demoUser = userDao.getUserByEmail(DEMO_EMAIL)
                if (demoUser == null) {
                    val hashedPassword = SecurityUtils.hashPassword(DEMO_PASSWORD)
                    val newDemoUser = UserEntity(
                        name = DEMO_NAME,
                        email = DEMO_EMAIL,
                        password = hashedPassword
                    )
                    val id = userDao.insertUser(newDemoUser)
                    demoUser = newDemoUser.copy(id = id)
                }

                val sessionToken = UUID.randomUUID().toString()
                userPreferencesRepository.setSession(
                    isLoggedIn = true,
                    token = sessionToken,
                    name = demoUser.name,
                    email = demoUser.email ?: DEMO_EMAIL,
                    userId = demoUser.id
                )
                userPreferencesRepository.updateLastActiveTimestamp()
                return@withContext Result.success(demoUser)
            }

            // Standard Account Login
            var user = userDao.getUserByEmail(cleanEmail)
            if (user == null) {
                val existingSingleUser = userDao.getUserSync()
                if (existingSingleUser != null && existingSingleUser.email.equals(cleanEmail, ignoreCase = true)) {
                    user = existingSingleUser
                }
            }

            if (user == null) {
                val existingSingleUser = userDao.getUserSync()
                if (existingSingleUser != null && (existingSingleUser.email.isNullOrBlank() || existingSingleUser.email == "user@monethra.app")) {
                    val hashedPassword = SecurityUtils.hashPassword(cleanPassword)
                    val updatedUser = existingSingleUser.copy(email = cleanEmail, password = hashedPassword)
                    userDao.updateUser(updatedUser)
                    user = updatedUser
                } else {
                    return@withContext Result.failure(Exception("Account with email '$cleanEmail' not found. Please sign up."))
                }
            } else {
                if (!user.password.isNullOrEmpty() && !SecurityUtils.verifyPassword(cleanPassword, user.password)) {
                    return@withContext Result.failure(Exception("Invalid email or password."))
                }
                if (user.password.isNullOrEmpty() || user.password == cleanPassword) {
                    val hashedPassword = SecurityUtils.hashPassword(cleanPassword)
                    val updatedUser = user.copy(password = hashedPassword)
                    userDao.updateUser(updatedUser)
                    user = updatedUser
                }
            }

            val sessionToken = UUID.randomUUID().toString()
            userPreferencesRepository.setSession(
                isLoggedIn = true,
                token = sessionToken,
                name = user.name,
                email = user.email ?: cleanEmail,
                userId = user.id
            )
            userPreferencesRepository.updateLastActiveTimestamp()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(name: String, email: String, password: String): Result<UserEntity> = withContext(ioDispatcher) {
        try {
            val cleanName = name.trim()
            val cleanEmail = email.trim()
            val cleanPassword = password.trim()

            if (cleanName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Full Name cannot be empty"))
            }
            if (cleanEmail.isBlank() || !isValidEmail(cleanEmail)) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
            }
            if (cleanPassword.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
            }

            val hashedPassword = SecurityUtils.hashPassword(cleanPassword)
            val existingUser = userDao.getUserByEmail(cleanEmail)
            val userToSave = if (existingUser != null) {
                existingUser.copy(name = cleanName, email = cleanEmail, password = hashedPassword)
            } else {
                UserEntity(name = cleanName, email = cleanEmail, password = hashedPassword)
            }

            val id = if (userToSave.id > 0) {
                userDao.updateUser(userToSave)
                userToSave.id
            } else {
                userDao.insertUser(userToSave)
            }

            val savedUser = userToSave.copy(id = id)

            val sessionToken = UUID.randomUUID().toString()
            userPreferencesRepository.setSession(
                isLoggedIn = true,
                token = sessionToken,
                name = cleanName,
                email = cleanEmail,
                userId = savedUser.id
            )
            userPreferencesRepository.updateLastActiveTimestamp()

            Result.success(savedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val lastActiveTimestamp: Flow<Long> get() = userPreferencesRepository.lastActiveTimestamp

    val lastBackgroundTimestamp: Flow<Long> get() = userPreferencesRepository.lastBackgroundTimestamp

    fun isSessionLoggedIn(): Boolean = userPreferencesRepository.isSessionLoggedIn()

    fun getLastActiveTimestamp(): Long = userPreferencesRepository.getLastActiveTimestamp()

    fun getLastBackgroundTimestamp(): Long = userPreferencesRepository.getLastBackgroundTimestamp()

    fun saveLastActiveTimestamp(timestamp: Long) {
        userPreferencesRepository.saveLastActiveTimestampSync(timestamp)
        CoroutineScope(ioDispatcher).launch {
            userPreferencesRepository.updateLastActiveTimestamp(timestamp)
        }
    }

    suspend fun saveLastActiveTimestampSuspend(timestamp: Long) = withContext(ioDispatcher) {
        userPreferencesRepository.updateLastActiveTimestamp(timestamp)
    }

    fun saveLastBackgroundTimestamp(timestamp: Long) {
        saveLastActiveTimestamp(timestamp)
    }

    suspend fun saveLastBackgroundTimestampSuspend(timestamp: Long) = withContext(ioDispatcher) {
        userPreferencesRepository.updateLastActiveTimestamp(timestamp)
    }

    fun isInitialSessionValid(): Boolean = userPreferencesRepository.isInitialSessionValid(SESSION_TIMEOUT_MS)

    suspend fun updateLastActiveTimestamp(timestamp: Long = System.currentTimeMillis()) = withContext(ioDispatcher) {
        userPreferencesRepository.updateLastActiveTimestamp(timestamp)
    }

    suspend fun validateSession(timeoutMillis: Long = SESSION_TIMEOUT_MS): Boolean = withContext(ioDispatcher) {
        userPreferencesRepository.checkAndInvalidateSessionIfExpired(timeoutMillis)
    }

    suspend fun logout() = withContext(ioDispatcher) {
        try {
            userPreferencesRepository.clearSession()
        } catch (_: Exception) {}
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"))
    }
}
