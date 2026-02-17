package com.example.eventglow.common.password_reset

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.AccentOrange
import com.example.eventglow.ui.theme.BorderGray
import com.example.eventglow.ui.theme.HintGray
import com.example.eventglow.ui.theme.ScreenBlack
import com.example.eventglow.ui.theme.TopBarGray
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun passwordRecoveryScreen(
    navController: NavController,
) {
    var email by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Password Recovery",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TopBarGray
                )
            )
        },
        containerColor = ScreenBlack
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(56.dp))

            // Email Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = AccentOrange,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Reset Your Password",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Enter your email address and we'll send you\n" +
                        "a link to reset your password.",
                color = HintGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Rounded Outlined Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                label = {
                    Text("Email Address", color = HintGray)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = HintGray
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorderGray,
                    unfocusedBorderColor = BorderGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AccentOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Send Email Button
            Button(
                onClick = { /* logic later */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange
                )
            ) {
                Text(
                    text = "Send Email",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}


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
            //PasswordRecoveryContent(navController = navController)
        }
    }
}

@Composable
fun PasswordRecoveryContent(
    email: String,
    emailError: String?,
    emailEmpty: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onSendRecoveryEmail: () -> Unit
) {

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
                onEmailChange(newValue)
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
                onSendRecoveryEmail()
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordRecoveryScreen(
    navController: NavController,
    viewModel: PasswordRecoveryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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
            PasswordRecoveryContent(
                email = email,
                emailError = emailError,
                emailEmpty = emailEmpty,
                isLoading = isLoading,
                onEmailChange = { newValue ->
                    email = newValue
                    emailError = if (Patterns.EMAIL_ADDRESS.matcher(newValue)
                            .matches()
                    ) null else "Invalid email address"
                    //sets isError to false
                    if (email.isNotEmpty()) emailEmpty = null
                },
                onSendRecoveryEmail = {
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
                            return@PasswordRecoveryContent
                        }
                        if (validationResult == "Invalid Email") {
                            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                            return@PasswordRecoveryContent
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun PasswordRecoveryContentPreview() {
    PasswordRecoveryContent(
        email = "admin@example.com",
        emailError = null,
        emailEmpty = null,
        isLoading = false,
        onEmailChange = {},
        onSendRecoveryEmail = {}
    )
}
