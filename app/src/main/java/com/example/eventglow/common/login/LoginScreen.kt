package com.example.eventglow.common.login

import android.content.res.Configuration
import android.util.Patterns
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.common.LoadState
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.EventGlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun loginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val loadState by viewModel.loadState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoading = loadState == LoadState.LOADING

    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    if (loginState is LoginState.Success) {
        val role = (loginState as LoginState.Success).role
        LaunchedEffect(role) {
            when (role) {
                "user" -> navController.navigate(Routes.USER_MAIN_SCREEN)
                "admin" -> navController.navigate(Routes.ADMIN_MAIN_SCREEN)
            }
        }
    }

    LoginScreenContent(
        isLoading = isLoading,
        snackbarHostState = snackbarHostState,
        onLogin = { email, password -> viewModel.login(email, password) },
        onForgotPassword = { navController.navigate(Routes.PASSWORD_RECOVERY_SCREEN) },
        onSignUp = { navController.navigate(Routes.CREATE_ACCOUNT_SCREEN) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenContent(
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onLogin: (String, String) -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        emailError = when {
            email.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email address"
            else -> null
        }

        passwordError = when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }

        return emailError == null && passwordError == null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
        ) {
            BubbleBackground(bubbleColor = MaterialTheme.colorScheme.onBackground)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(Modifier.height(160.dp))

                Text(
                    text = "Welcome",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(30.dp))

                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = cleanEmailInput(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            isError = emailError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                autoCorrect = false
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        emailError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = passwordError != null,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        passwordError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Forgot Password?",
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable(onClick = onForgotPassword),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (validate()) {
                                    onLogin(email.trim(), password)
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = "Login",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sign Up",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(onClick = onSignUp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleBackground(
    bubbleColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = bubbleColor.copy(alpha = 0.12f),
            radius = size.minDimension * 0.45f,
            center = Offset(
                x = size.width * 0.2f,
                y = size.height * 0.1f
            )
        )

        drawCircle(
            color = bubbleColor.copy(alpha = 0.10f),
            radius = size.minDimension * 0.35f,
            center = Offset(
                x = size.width * 0.9f,
                y = size.height * 0.9f
            )
        )

        drawCircle(
            color = bubbleColor.copy(alpha = 0.08f),
            radius = size.minDimension * 0.18f,
            center = Offset(
                x = size.width * 0.85f,
                y = size.height * 0.3f
            )
        )
    }
}

fun cleanEmailInput(input: String): String {
    return input
        .trim()
        .replace(" ", "")
}

@Preview(name = "Login Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 34)
@Composable
fun LoginLightPreview() {
    EventGlowTheme(darkTheme = false) {
        LoginScreenContent(
            isLoading = false,
            snackbarHostState = remember { SnackbarHostState() },
            onLogin = { _, _ -> },
            onForgotPassword = {},
            onSignUp = {}
        )
    }
}

@Preview(name = "Login Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 34)
@Composable
fun LoginDarkPreview() {
    EventGlowTheme(darkTheme = true) {
        LoginScreenContent(
            isLoading = true,
            snackbarHostState = remember { SnackbarHostState() },
            onLogin = { _, _ -> },
            onForgotPassword = {},
            onSignUp = {}
        )
    }
}
