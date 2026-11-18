package com.example.monethra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.monethra.data.repository.AuthRepository
import com.example.monethra.data.session.SessionManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val errorMessage: String? = generalError,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val errorMessage: String? = generalError,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

sealed interface AuthState {
    data object Loading : AuthState
    data class Authenticated(val userId: Long = 0L, val email: String = "") : AuthState
    data object Unauthenticated : AuthState
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    companion object {
        const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        private const val THROTTLE_PERSIST_MS = 15 * 1000L // 15 seconds
    }

    private var inactivityJob: Job? = null
    private var lastPersistedTimestamp: Long = 0L

    // On cold app launch (process was killed), invalidate persisted session immediately
    private val isInitialValid: Boolean = if (!SessionManager.isProcessAlive) {
        false
    } else {
        authRepository.isInitialSessionValid()
    }

    private val _authState = MutableStateFlow<AuthState>(
        if (isInitialValid) AuthState.Authenticated() else AuthState.Unauthenticated
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpUiState())
    val signUpState: StateFlow<SignUpUiState> = _signUpState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn()
        .catch { emit(false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = isInitialValid
        )

    init {
        if (!SessionManager.isProcessAlive) {
            viewModelScope.launch(ioDispatcher) {
                try {
                    authRepository.logout()
                } catch (_: Exception) {}
            }
            SessionManager.isProcessAlive = true
        }

        viewModelScope.launch(ioDispatcher) {
            authRepository.isLoggedIn().collect { loggedIn ->
                if (loggedIn) {
                    val user = authRepository.getCurrentUserSync()
                    _authState.value = AuthState.Authenticated(
                        userId = user?.id ?: 0L,
                        email = user?.email ?: ""
                    )
                } else {
                    cancelInactivityTimer()
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun onUserInteraction() {
        val currentlyLoggedIn = authRepository.isSessionLoggedIn() || isLoggedIn.value
        if (!currentlyLoggedIn) {
            cancelInactivityTimer()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastPersistedTimestamp >= THROTTLE_PERSIST_MS) {
            lastPersistedTimestamp = now
            authRepository.saveLastActiveTimestamp(now)
        }

        restartInactivityTimer()
    }

    fun restartInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch(ioDispatcher) {
            delay(SESSION_TIMEOUT_MS)
            logoutDueToTimeout()
        }
    }

    fun cancelInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = null
    }

    // Login functions
    fun onLoginEmailChanged(email: String) {
        _loginState.update {
            it.copy(
                email = email,
                emailError = null,
                generalError = null,
                errorMessage = null
            )
        }
    }

    fun onLoginPasswordChanged(password: String) {
        _loginState.update {
            it.copy(
                password = password,
                passwordError = null,
                generalError = null,
                errorMessage = null
            )
        }
    }

    fun toggleLoginPasswordVisibility() {
        _loginState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login(onSuccess: () -> Unit = {}) {
        try {
            val current = _loginState.value
            val email = current.email.trim()
            val password = current.password.trim()

            var hasError = false
            var emailErr: String? = null
            var passwordErr: String? = null

            if (email.isBlank()) {
                emailErr = "Email is required"
                hasError = true
            } else if (!isValidEmail(email)) {
                emailErr = "Invalid email format"
                hasError = true
            }

            if (password.isBlank()) {
                passwordErr = "Password is required"
                hasError = true
            }

            if (hasError) {
                _loginState.update {
                    it.copy(
                        emailError = emailErr,
                        passwordError = passwordErr
                    )
                }
                return
            }

            _loginState.update {
                it.copy(
                    isLoading = true,
                    generalError = null,
                    errorMessage = null
                )
            }

            viewModelScope.launch(ioDispatcher) {
                try {
                    val result = authRepository.login(email, password)
                    result.onSuccess {
                        SessionManager.isProcessAlive = true
                        _loginState.update { state ->
                            state.copy(
                                isLoading = false,
                                isSuccess = true,
                                generalError = null,
                                errorMessage = null
                            )
                        }
                        withContext(Dispatchers.Main) {
                            try {
                                onSuccess()
                            } catch (_: Exception) {
                                // Prevent navigation crash
                            }
                        }
                    }.onFailure { exception ->
                        val errorMsg = exception.localizedMessage ?: "Invalid email or password. Please try again."
                        _loginState.update { state ->
                            state.copy(
                                isLoading = false,
                                generalError = errorMsg,
                                errorMessage = errorMsg
                            )
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = e.localizedMessage ?: "Invalid email or password. Please try again."
                    _loginState.update { state ->
                        state.copy(
                            isLoading = false,
                            generalError = errorMsg,
                            errorMessage = errorMsg
                        )
                    }
                }
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Invalid email or password. Please try again."
            _loginState.update { state ->
                state.copy(
                    isLoading = false,
                    generalError = errorMsg,
                    errorMessage = errorMsg
                )
            }
        }
    }

    // Sign Up functions
    fun onSignUpFullNameChanged(name: String) {
        _signUpState.update {
            it.copy(
                fullName = name,
                fullNameError = null,
                generalError = null,
                errorMessage = null
            )
        }
    }

    fun onSignUpEmailChanged(email: String) {
        _signUpState.update {
            it.copy(
                email = email,
                emailError = null,
                generalError = null,
                errorMessage = null
            )
        }
    }

    fun onSignUpPasswordChanged(password: String) {
        _signUpState.update {
            it.copy(
                password = password,
                passwordError = null,
                generalError = null,
                errorMessage = null
            )
        }
    }

    fun onSignUpConfirmPasswordChanged(confirm: String) {
        _signUpState.update {
            it.copy(
                confirmPassword = confirm,
                confirmPasswordError = null,
                generalError = null,
                errorMessage = null
            )
        }
    }

    fun toggleSignUpPasswordVisibility() {
        _signUpState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleSignUpConfirmPasswordVisibility() {
        _signUpState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun signUp(onSuccess: () -> Unit = {}) {
        try {
            val current = _signUpState.value
            val name = current.fullName.trim()
            val email = current.email.trim()
            val password = current.password.trim()
            val confirm = current.confirmPassword.trim()

            var hasError = false
            var nameErr: String? = null
            var emailErr: String? = null
            var passwordErr: String? = null
            var confirmErr: String? = null

            if (name.isBlank()) {
                nameErr = "Full Name is required"
                hasError = true
            }

            if (email.isBlank()) {
                emailErr = "Email is required"
                hasError = true
            } else if (!isValidEmail(email)) {
                emailErr = "Enter a valid email address"
                hasError = true
            }

            if (password.isBlank()) {
                passwordErr = "Password is required"
                hasError = true
            } else if (password.length < 6) {
                passwordErr = "Password must be at least 6 characters"
                hasError = true
            }

            if (confirm.isBlank()) {
                confirmErr = "Confirm Password is required"
                hasError = true
            } else if (confirm != password) {
                confirmErr = "Passwords do not match"
                hasError = true
            }

            if (hasError) {
                _signUpState.update {
                    it.copy(
                        fullNameError = nameErr,
                        emailError = emailErr,
                        passwordError = passwordErr,
                        confirmPasswordError = confirmErr
                    )
                }
                return
            }

            _signUpState.update {
                it.copy(
                    isLoading = true,
                    generalError = null,
                    errorMessage = null
                )
            }

            viewModelScope.launch(ioDispatcher) {
                try {
                    val result = authRepository.signUp(name, email, password)
                    result.onSuccess {
                        SessionManager.isProcessAlive = true
                        _signUpState.update { state ->
                            state.copy(
                                isLoading = false,
                                isSuccess = true,
                                generalError = null,
                                errorMessage = null
                            )
                        }
                        withContext(Dispatchers.Main) {
                            try {
                                onSuccess()
                            } catch (_: Exception) {
                                // Prevent navigation crash
                            }
                        }
                    }.onFailure { exception ->
                        val errorMsg = exception.localizedMessage ?: "Sign up failed. Please try again."
                        _signUpState.update { state ->
                            state.copy(
                                isLoading = false,
                                generalError = errorMsg,
                                errorMessage = errorMsg
                            )
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = e.localizedMessage ?: "Sign up failed. Please try again."
                    _signUpState.update { state ->
                        state.copy(
                            isLoading = false,
                            generalError = errorMsg,
                            errorMessage = errorMsg
                        )
                    }
                }
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Sign up failed. Please try again."
            _signUpState.update { state ->
                state.copy(
                    isLoading = false,
                    generalError = errorMsg,
                    errorMessage = errorMsg
                )
            }
        }
    }

    fun logoutDueToTimeout(onComplete: () -> Unit = {}) {
        cancelInactivityTimer()
        viewModelScope.launch(ioDispatcher) {
            try {
                authRepository.logout()
            } catch (_: Exception) {}
            _authState.value = AuthState.Unauthenticated
            withContext(Dispatchers.Main) {
                resetStates()
                onComplete()
            }
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        cancelInactivityTimer()
        viewModelScope.launch(ioDispatcher) {
            try {
                authRepository.logout()
            } catch (_: Exception) {}
            _authState.value = AuthState.Unauthenticated
            withContext(Dispatchers.Main) {
                resetStates()
                onComplete()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelInactivityTimer()
    }

    fun resetStates() {
        _loginState.value = LoginUiState()
        _signUpState.value = SignUpUiState()
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"))
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
