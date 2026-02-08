package com.example.eventglow.common.password_reset

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordRecoveryScreen(navController: NavController) {

    // Scaffold to provide a basic material layout structure
    Scaffold(
        // top bar
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Password Recovery", color = MaterialTheme.colorScheme.primary)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        // Surface to hold main contents with padding applied
        Surface(Modifier.padding(paddingValues)) {
            PasswordRecoveryContent(navController = navController)
        }
    }
}

@Composable
fun PasswordRecoveryContent(
    navController: NavController,
    viewModel: PasswordRecoveryViewModel = viewModel()
) {

    val context = LocalContext.current

    var validationResult by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var emailEmpty by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun validateFields() {
        //resets validationResult on entry
        validationResult = null

        //set is error properties
        if (email.isEmpty()) emailEmpty = "Empty"

        //checks if a field is not filed
        if (email.isEmpty()) {
            validationResult = "EmptyFields"
        }
        //check if email has an error message
        if (emailError != null) {
            validationResult = "Invalid Email"
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Forgot Password",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Reset Your Password",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Enter your email address and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        //email outlined field
        OutlinedTextField(
            value = email,
            onValueChange = { newValue ->
                email = newValue
                emailError = if (Patterns.EMAIL_ADDRESS.matcher(newValue)
                        .matches()
                ) null else "Invalid email address" //checks and sets emailError is emailo address does not match standard
                //sets isError to false
                if (email.isNotEmpty()) emailEmpty = null
            },
            label = { Text("Email Address") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = "Email Icon")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailError != null || emailEmpty != null, //sets to false
            modifier = Modifier.fillMaxWidth()
        )
        //checks if email address has errors
        if (emailError != null) {
            Text(
                text = emailError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        //send revovery passwod email
        Button(
            onClick = {
                isLoading = true // sets state of loading to true
                validateFields()
                //check if there are no validation messages
                if (validationResult == null) {
                    coroutineScope.launch {
                        isLoading = false // sets state of loading to false
                        viewModel.sendPasswordResetEmail(
                            email,
                            onSuccess = { navController.navigate(Routes.PASSWORD_RESET_CONFIRMATION_SCREEN) },
                            onError = {
                                Toast.makeText(
                                    context,
                                    "An error occurred while sending password reset email please make sure you have internet access",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                } else {
                    if (validationResult == "EmptyFields") {
                        Toast.makeText(context, "Please enter email address ", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (validationResult == "Invalid Email") {
                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                }

            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = !isLoading //sets enabled property to !isLoading
        ) {
            //checks value of isloading
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Send Recovery Email")
            }
        }
    }
}
