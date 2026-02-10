package com.example.eventglow.common.login


import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.navigation.Routes



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun loginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val loginState by viewModel.loginState.collectAsState()

    fun validate(): Boolean {
        emailError = when {
            email.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Invalid email address"

            else -> null
        }

        passwordError = when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }

        return emailError == null && passwordError == null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF9A8B),
                        Color(0xFFFF6A5C),
                        Color(0xFFFFB347)
                    )
                )
            )
    ) {
        BubbleBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

            Spacer(Modifier.height(160.dp))

            Text(
                text = "Welcome",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(30.dp))

            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF121212)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = cleanEmailInput(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        isError = emailError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            autoCorrect = false
                        )
                    )


                    emailError?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else PasswordVisualTransformation(),
                        isError = passwordError != null,
                        singleLine = true
                    )

                    passwordError?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Forgot Password?",
                        modifier = Modifier
                            .align(Alignment.End),
                        color = Color(0xFFFF6A5C),
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    // Login Button
                    Button(
                        onClick = {
                            val cleanedEmail = email.trim()
                            if (validate()) {
                                isLoading = true
                                viewModel.login(cleanedEmail, password)
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6A5C)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
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
                            color = Color.Gray
                        )
                        Text(
                            text = "Sign Up",
                            color = Color(0xFFFF6A5C),
                            fontWeight = FontWeight.Bold,

                            )
                    }

                    when (val state = loginState) {

                        // when login state is Error
                        is LoginState.Error -> {
                            Text(
                                text = if (state.message == "Authentication error: The supplied auth credential is incorrect, malformed or has expired.") "Wrong username or password" else state.message, // Display error message
                                color = MaterialTheme.colorScheme.error // Set text color to red
                            )
                        }

                        //when login state is loading
                        LoginState.Loading -> {
                            CircularProgressIndicator()
                        }

                        is LoginState.Success -> {
                            val role = state.role
                            LaunchedEffect(role) {
                                when (role) {
                                    "user" -> {
                                        navController.navigate(Routes.USER_MAIN_SCREEN)
                                    }

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


@Composable
fun BubbleBackground() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top-left big bubble
        drawCircle(
            color = Color.White.copy(alpha = 0.12f),
            radius = size.minDimension * 0.45f,
            center = Offset(
                x = size.width * 0.2f,
                y = size.height * 0.1f
            )
        )

        // Bottom-right bubble
        drawCircle(
            color = Color.White.copy(alpha = 0.10f),
            radius = size.minDimension * 0.35f,
            center = Offset(
                x = size.width * 0.9f,
                y = size.height * 0.9f
            )
        )

        // Mid-right small bubble
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
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
        .trim()          // remove leading & trailing spaces
        .replace(" ", "") // remove internal spaces (autofill issue)
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

                }
            }
        }
    }
}


@Preview
@Composable
fun LoginPreview() {
    loginScreen(navController = rememberNavController())
}