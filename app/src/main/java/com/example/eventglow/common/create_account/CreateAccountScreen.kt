package com.example.eventglow.common.create_account

import android.content.res.Configuration
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.common.LoadState
import com.example.eventglow.navigation.Routes
import com.example.eventglow.navigation.navigateAndClearTo
import com.example.eventglow.ui.theme.EventGlowTheme
import kotlinx.coroutines.launch

private enum class UsernameAvailability {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createAccountScreen(
    navController: NavController,
    viewModel: CreateAccountViewModel = viewModel()
) {
    val createAccountState by viewModel.createAccountState.collectAsState()
    val loadState by viewModel.loadState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createAccountState) {
        if (createAccountState is CreateAccountState.Success) {
            navController.navigateAndClearTo(Routes.EMAIL_VERIFICATION_SCREEN)
        }
    }

    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    CreateAccountScreenContent(
        isLoading = loadState == LoadState.LOADING,
        snackbarHostState = snackbarHostState,
        onBack = { navController.popBackStack() },
        onCheckUsername = { username, onResult ->
            viewModel.checkIfUsernameIsTaken(username, onResult)
        },
        onCreateAccount = { username, email, password ->
            viewModel.createAccount(username, email, password)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAccountScreenContent(
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCheckUsername: suspend (String, (Boolean) -> Unit) -> Unit,
    onCreateAccount: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var usernameMessage by remember { mutableStateOf<String?>(null) }
    var usernameAvailability by remember { mutableStateOf(UsernameAvailability.IDLE) }
    var usernameHadFocus by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Account",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            AuthOutlinedField(
                value = username,
                onValueChange = {
                    username = it
                    usernameAvailability = UsernameAvailability.IDLE
                    usernameMessage = null
                },
                label = "Username",
                leadingIcon = Icons.Default.Person,
                onFocusChanged = { isFocused ->
                    if (usernameHadFocus && !isFocused) {
                        val trimmedUsername = username.trim()
                        if (trimmedUsername.isBlank()) {
                            usernameAvailability = UsernameAvailability.IDLE
                            usernameMessage = null
                        } else {
                            usernameAvailability = UsernameAvailability.CHECKING
                            scope.launch {
                                onCheckUsername(trimmedUsername) { isTaken ->
                                    usernameAvailability =
                                        if (isTaken) UsernameAvailability.TAKEN else UsernameAvailability.AVAILABLE
                                    usernameMessage = if (isTaken) "Username is already taken" else null
                                }
                            }
                        }
                    }
                    usernameHadFocus = isFocused
                },
                trailingIconContent = {
                    when (usernameAvailability) {
                        UsernameAvailability.CHECKING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        UsernameAvailability.AVAILABLE -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Username available",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        UsernameAvailability.TAKEN -> {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Username taken",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        UsernameAvailability.IDLE -> Unit
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            AuthOutlinedField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                leadingIcon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(20.dp))

            AuthOutlinedField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onVisibilityChange = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(20.dp))

            AuthOutlinedField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = confirmPasswordVisible,
                onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val trimmedUsername = username.trim()
                    val trimmedEmail = email.trim()
                    val trimmedPassword = password.trim()
                    val trimmedConfirmPassword = confirmPassword.trim()

                    when {
                        trimmedUsername.isBlank() || trimmedEmail.isBlank() || trimmedPassword.isBlank() || trimmedConfirmPassword.isBlank() -> {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        trimmedPassword.length < 8 || !trimmedPassword.any { it.isDigit() } -> {
                            Toast.makeText(
                                context,
                                "Password must be at least 8 characters and contain a number",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        trimmedPassword != trimmedConfirmPassword -> {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        usernameAvailability == UsernameAvailability.TAKEN -> {
                            Toast.makeText(
                                context,
                                "Username is already taken. Please enter a different username",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                    }

                    scope.launch {
                        onCheckUsername(trimmedUsername) { isTaken ->
                            usernameAvailability =
                                if (isTaken) UsernameAvailability.TAKEN else UsernameAvailability.AVAILABLE
                            usernameMessage = if (isTaken) "Username is already taken" else null

                            if (isTaken) {
                                Toast.makeText(
                                    context,
                                    "Username is already taken. Please enter a different username",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onCreateAccount(trimmedUsername, trimmedEmail, trimmedPassword)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(28.dp),
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
                        text = "Create Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!usernameMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = usernameMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

        }
    }
}

@Composable
fun AuthOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onVisibilityChange: (() -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    trailingIconContent: (@Composable (() -> Unit))? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .onFocusChanged { onFocusChanged?.invoke(it.isFocused) },
        label = { Text(text = label) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onVisibilityChange?.invoke() }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            }
        } else trailingIconContent,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Preview(name = "Create Account Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 34)
@Composable
fun CreateAccountScreenLightPreview() {
    EventGlowTheme(darkTheme = false) {
        CreateAccountScreenContent(
            isLoading = false,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onCheckUsername = { _, onResult -> onResult(false) },
            onCreateAccount = { _, _, _ -> }
        )
    }
}

@Preview(name = "Create Account Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 34)
@Composable
fun CreateAccountScreenDarkPreview() {
    EventGlowTheme(darkTheme = true) {
        CreateAccountScreenContent(
            isLoading = true,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onCheckUsername = { _, onResult -> onResult(true) },
            onCreateAccount = { _, _, _ -> }
        )
    }
}
