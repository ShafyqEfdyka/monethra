package com.example.monethra.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monethra.R
import com.example.monethra.ui.theme.DeepIndigo
import com.example.monethra.ui.theme.ExpenseRed
import com.example.monethra.ui.theme.MonethraPastelPalette
import com.example.monethra.ui.theme.MonethraTheme
import com.example.monethra.ui.theme.PastelPeriwinkle
import com.example.monethra.viewmodel.AuthViewModel
import com.example.monethra.viewmodel.SignUpUiState

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signUpState by viewModel.signUpState.collectAsState()

    SignUpContent(
        state = signUpState,
        onFullNameChanged = viewModel::onSignUpFullNameChanged,
        onEmailChanged = viewModel::onSignUpEmailChanged,
        onPasswordChanged = viewModel::onSignUpPasswordChanged,
        onConfirmPasswordChanged = viewModel::onSignUpConfirmPasswordChanged,
        onTogglePasswordVisibility = viewModel::toggleSignUpPasswordVisibility,
        onToggleConfirmPasswordVisibility = viewModel::toggleSignUpConfirmPasswordVisibility,
        onSignUpClick = { viewModel.signUp(onSuccess = onSignUpSuccess) },
        onNavigateToLogin = onNavigateToLogin,
        modifier = modifier
    )
}

@Composable
fun SignUpContent(
    state: SignUpUiState,
    onFullNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSignUpClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayError = state.errorMessage ?: state.generalError
    val backgroundBrush = remember { MonethraPastelPalette.getRandomThreeColorGradient() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundBrush)
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "MONETHRA Logo",
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepIndigo,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Start managing your finances today",
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Elevated Semi-Translucent Card Container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = DeepIndigo.copy(alpha = 0.15f),
                            spotColor = DeepIndigo.copy(alpha = 0.2f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.94f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Error message
                        if (!displayError.isNullOrBlank()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = displayError,
                                    color = ExpenseRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Full Name
                        OutlinedTextField(
                            value = state.fullName,
                            onValueChange = onFullNameChanged,
                            label = { Text("Full Name", color = DeepIndigo) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "Person Icon",
                                    tint = DeepIndigo
                                )
                            },
                            isError = state.fullNameError != null,
                            supportingText = state.fullNameError?.let { { Text(it, color = ExpenseRed) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelPeriwinkle,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedTextColor = DeepIndigo,
                                unfocusedTextColor = DeepIndigo
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = onEmailChanged,
                            label = { Text("Email Address", color = DeepIndigo) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Email,
                                    contentDescription = "Email Icon",
                                    tint = DeepIndigo
                                )
                            },
                            isError = state.emailError != null,
                            supportingText = state.emailError?.let { { Text(it, color = ExpenseRed) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelPeriwinkle,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedTextColor = DeepIndigo,
                                unfocusedTextColor = DeepIndigo
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = onPasswordChanged,
                            label = { Text("Password", color = DeepIndigo) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Password Icon",
                                    tint = DeepIndigo
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = onTogglePasswordVisibility) {
                                    Icon(
                                        imageVector = if (state.isPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                        contentDescription = if (state.isPasswordVisible) "Hide password" else "Show password",
                                        tint = DeepIndigo
                                    )
                                }
                            },
                            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = state.passwordError != null,
                            supportingText = state.passwordError?.let { { Text(it, color = ExpenseRed) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelPeriwinkle,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedTextColor = DeepIndigo,
                                unfocusedTextColor = DeepIndigo
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm Password
                        OutlinedTextField(
                            value = state.confirmPassword,
                            onValueChange = onConfirmPasswordChanged,
                            label = { Text("Confirm Password", color = DeepIndigo) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Confirm Password Icon",
                                    tint = DeepIndigo
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = onToggleConfirmPasswordVisibility) {
                                    Icon(
                                        imageVector = if (state.isConfirmPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                        contentDescription = if (state.isConfirmPasswordVisible) "Hide password" else "Show password",
                                        tint = DeepIndigo
                                    )
                                }
                            },
                            visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = state.confirmPasswordError != null,
                            supportingText = state.confirmPasswordError?.let { { Text(it, color = ExpenseRed) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelPeriwinkle,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedTextColor = DeepIndigo,
                                unfocusedTextColor = DeepIndigo
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // High-Contrast CTA Button
                        Button(
                            onClick = onSignUpClick,
                            enabled = !state.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepIndigo,
                                contentColor = Color.White
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = "Sign Up",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Link to Login
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Already have an account?",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                            TextButton(onClick = onNavigateToLogin) {
                                Text(
                                    text = "Log In",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepIndigo
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    MonethraTheme {
        SignUpContent(
            state = SignUpUiState(),
            onFullNameChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onSignUpClick = {},
            onNavigateToLogin = {}
        )
    }
}
