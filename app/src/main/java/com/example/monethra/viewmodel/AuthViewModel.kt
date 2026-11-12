package com.example.monethra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.monethra.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpUiState())
    val signUpState: StateFlow<SignUpUiState> = _signUpState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn()
        .catch { emit(false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

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

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch(ioDispatcher) {
            try {
                authRepository.logout()
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                resetStates()
                onComplete()
            }
        }
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
