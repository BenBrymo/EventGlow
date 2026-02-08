package com.example.eventglow.common.login

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.navigation.Routes


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    // Scaffold to provide a basic material layout structure
    Scaffold(
        // top bar
        topBar = {
            TopAppBar(
                // title of screen
                title = { Text(text = "Login", color = MaterialTheme.colorScheme.primary) }
            )
        }
    ) { paddingValues ->
        // Surface to hold main contents with padding applied
        Surface(Modifier.padding(paddingValues)) {
            Login(navController = navController)
        }
    }
}


@Composable
fun Login(
    navController: NavController, // NavController for navigation
    viewModel: LoginViewModel = viewModel(),  // ViewModel for login
    sharedPreferencesViewModel: SharedPreferencesViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsState()   // Observe login state
    var validationResult by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var emailEmpty by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var passwordEmpty by remember { mutableStateOf<String?>(null) }
    // mutableState variable for toggling password visibility
    var passwordVisible by remember { mutableStateOf(false) }

    Log.d("LoginScreen", "Login State at startup: $loginState")

    //shared preferences data
    val userData by sharedPreferencesViewModel.userInfo.collectAsState()
    Log.d("LoginScreen", "Current Shared Preference data: $userData")

    // local context
    val context = LocalContext.current

    fun validateFields() {
        //resets validationResult on entry
        validationResult = null

        //set is error properties
        if (email.isEmpty()) emailEmpty = "Empty"
        if (password.isEmpty()) passwordEmpty = "Empty"

        //checks if a field is not filed
        if (email.isEmpty() || password.isEmpty()) {
            validationResult = "EmptyFields"
        }
        //check if email has an error message
        if (emailError != null) {
            validationResult = "Invalid Email"
        }
    }
    // Main container
    Surface(
        modifier = Modifier

            //fill full screen
            .fillMaxSize()

            //background colour set to colorScheme.background
            .background(MaterialTheme.colorScheme.background)
    ) {
        //login content
        Box(
            modifier = Modifier.fillMaxSize(),

            //content aligned in the center of the box
            contentAlignment = Alignment.Center
        ) {

            // Card for the login form
            Card(
                shape = RoundedCornerShape(16.dp), //card made to have rounded corners
                elevation = CardDefaults.cardElevation(8.dp), //elevation effect added to card
                modifier = Modifier.padding(16.dp) //all round padding property applied to card
            ) {

                //Card Content
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth() // column mad to fillMaxWidth of card
                        .padding(32.dp)
                ) {

                    // App logo or title
                    Text(
                        text = "EventTS",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // Email input field with an icon
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it  // Update email state

                            //sets emailError to null or an error message
                            emailError =
                                if (Patterns.EMAIL_ADDRESS.matcher(it).matches()) null else "Invalid email address"

                            //sets isError to false
                            if (email.isNotEmpty()) emailEmpty = null
                        },
                        label = { Text("Email") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true, // Single line input
                        isError = emailError != null || emailEmpty != null, //sets to false
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    //Check is there is an existing emailError and display error message
                    if (emailError != null) {
                        Text(
                            text = emailError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Password input field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it.trim() //trim input

                            //sets isError to false
                            if (password.isNotEmpty()) passwordEmpty = null

                        },
                        label = { Text("Password") },
                        isError = passwordEmpty != null, //sets to false
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {

                            // changes passwordVisible State
                            IconButton(onClick = {
                                passwordVisible = !passwordVisible //set passwordVisible State
                            }) {
                                Icon(
                                    //sets icon depending on State of passwordVisible
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,

                                    //Same for content Description of icon
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), //fill maximum width of the screen
                        singleLine = true,

                        // hides password input depending on State of passwordVisible
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(16.dp)) //adds a vertical space

                    // Forgot password link
                    TextButton(
                        onClick = { navController.navigate(Routes.PASSWORD_RECOVERY_SCREEN) } // navigates to Password Recovery Screen
                    ) {
                        Text(text = "Forgot Password?", color = MaterialTheme.colorScheme.primary)
                    }

                    //create account button
                    TextButton(
                        onClick = { navController.navigate(Routes.CREATE_ACCOUNT_SCREEN) } // Navigate to sign-up screen
                    ) {
                        Text("Don't have an account? Sign up") // Display sign-up text
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Login button
                    Button(
                        onClick = {

                            //call to validate fields
                            validateFields()

                            //check if there are no validation messages
                            if (validationResult == null) {

                                //call to create account
                                viewModel.login(email, password)

                                Log.d("LoginScreen", "Login State at after LoginViewModel performs login: $loginState")
                                //if there are validation errors
                            } else {
                                if (validationResult == "EmptyFields") {
                                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (validationResult == "Invalid Email") {
                                    Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT)
                                        .show()
                                    return@Button
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Login")
                    }

                    // checks login state
                    when (val state = loginState) {

                        // when login state is Error
                        is LoginState.Error -> {
                            Log.d("LoginScreen", "Login State returned error now showing error message")
                            Text(
                                text = if (state.message == "Authentication error: The supplied auth credential is incorrect, malformed or has expired.") "Wrong username or password" else state.message, // Display error message
                                color = MaterialTheme.colorScheme.error // Set text color to red
                            )
                        }

                        //when login state is loading
                        LoginState.Loading -> {
                            Log.d("LoginScreen", "Login State returned loading now showing loading indicator")
                            CircularProgressIndicator() // Display loading indicator
                        }

                        //when login state is success
                        is LoginState.Success -> {
                            Log.d(
                                "LoginScreen",
                                "Login State returned success redirecting to the appropriate user screen"
                            )
                            val role = state.role
                            Log.d("LoginScreen", "Role $role has been returned by LoginState")
                            // Launch effect to navigate
                            LaunchedEffect(role) {

                                //checks role of user
                                when (role) {

                                    //when role is user
                                    "user" -> {
                                        navController.navigate(Routes.USER_MAIN_SCREEN)
                                    }

                                    //when role is admin
                                    "admin" -> {
                                        navController.navigate(Routes.ADMIN_MAIN_SCREEN)
                                    }
                                }
                            }
                        }

                        else -> Unit // Do nothing if state is idle
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginPreview() {
    LoginScreen(navController = rememberNavController())
}