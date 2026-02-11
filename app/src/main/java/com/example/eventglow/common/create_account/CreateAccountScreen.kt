package com.example.eventglow.common.create_account


import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.R
import com.example.eventglow.navigation.Routes
import kotlinx.coroutines.launch


val BackgroundBlack = Color(0xFF0F0F0F)
val FieldBorderGray = Color(0xFF5A5A5A)
val HintGray = Color(0xFF9E9E9E)
val AccentOrange = Color(0xFFE65100)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createAccountScreen(
    navController: NavController,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Account",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = BackgroundBlack
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            // Username
            AuthOutlinedField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                leadingIcon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email
            AuthOutlinedField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                leadingIcon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Password
            AuthOutlinedField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onVisibilityChange = {
                    passwordVisible = !passwordVisible
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Password
            AuthOutlinedField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = confirmPasswordVisible,
                onVisibilityChange = {
                    confirmPasswordVisible = !confirmPasswordVisible
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Create Account Button
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
                    text = "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
    onVisibilityChange: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        label = {
            Text(text = label, color = HintGray)
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = HintGray
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onVisibilityChange?.invoke() }) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = HintGray
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation()
        else VisualTransformation.None,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FieldBorderGray,
            unfocusedBorderColor = FieldBorderGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = AccentOrange
        ),
        shape = RoundedCornerShape(12.dp)
    )
}


@Composable
fun CreateAccount(navController: NavController, viewModel: CreateAccountViewModel = viewModel()) {

    // Get the context for displaying Toast messages
    val context = LocalContext.current

    val scope = rememberCoroutineScope() // for asynchronous actions

    val createAccountState by viewModel.createAccountState.collectAsState()
    var validationResult by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("") }
    var usernameEmpty by remember { mutableStateOf<String?>(null) }
    var usernameExisting by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var emailEmpty by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var passwordEmpty by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordEmpty by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) } // State for toggling password field visibility
    var confirmPasswordVisible by remember { mutableStateOf(false) } // State for toggling  confirm password field visibility
    var isLoading by remember { mutableStateOf(false) } //toggle loading indicator

    //track  username focus state
    var isUsernameFocussed by remember { mutableStateOf(false) }


    fun validateFields() {
        //set validationResult null on entry
        validationResult = null

        //set is error properties
        if (username.isEmpty()) usernameEmpty = "Empty"
        if (email.isEmpty()) emailEmpty = "Empty"
        if (password.isEmpty()) passwordEmpty = "Empty"
        if (confirmPassword.isEmpty()) confirmPasswordEmpty = "Empty"

        Log.d("fields:", "$username, $email, $password, $confirmPassword")
        //checks if a field is not filed
        if (email.isEmpty() || password.isEmpty() || username.isEmpty() || confirmPassword.isEmpty()) {
            validationResult = "EmptyFields"
        }
        //checks if password and confirm password do not match
        if (password != confirmPassword) {
            validationResult = "Mismatch Passwords"
        }
        //check if email has an error message
        if (emailError != null) {
            validationResult = "Invalid Email"
        }
        // password length has no erros
        if (passwordError != null) {
            validationResult = "Invalid Password"
        }
        //check if username
        if (usernameExisting != null) {
            validationResult = "Username Taken"
        }
    }
    LaunchedEffect(createAccountState) {
        //check create Account State
        when (val state = createAccountState) {
            //when account creation process fails
            is CreateAccountState.Failure -> {
                //stop showing loading indicator
                isLoading = false
                //displays failure message
                Toast.makeText(context, state.errorMessage, Toast.LENGTH_SHORT).show()
            }
            //when process is loading
            CreateAccountState.Loading -> {
                //start loading indicator
                isLoading = true
            }
            //when account creation process is successful
            is CreateAccountState.Success -> {
                //stop showing loading indicator
                isLoading = false
                //navigate to email verification screen
                navController.navigate(Routes.EMAIL_VERIFICATION_SCREEN)
            }

            else -> Unit
        }
    }

    // UI elements
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image
            Image(painter = painterResource(R.drawable.user_logo), contentDescription = null)
            // Username input field
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.trim()
                    //sets isError property to false
                    if (username.isNotEmpty()) usernameEmpty = null
                },
                label = { Text("Username") },
                singleLine = true,
                isError = usernameEmpty != null || usernameExisting != null, //sets to false
                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Username Icon") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        //only check name when user has typed an input
                        if (usernameEmpty == null) {
                            //check if field is not focussed
                            if (!focusState.isFocused && isUsernameFocussed) {
                                //when focus is lost
                                scope.launch {
                                    //call to check if username is taken
                                    viewModel.checkIfUsernameIsTaken(
                                        username,
                                        onResult = { isUsernameTaken ->
                                            Log.e(
                                                "Create Account Screen",
                                                "Retrived boolean is username Taken: $isUsernameTaken"
                                            )
                                            //if username is taken set error message or null
                                            usernameExisting = if (isUsernameTaken) {
                                                "Username is already taken"
                                            } else {
                                                null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        isUsernameFocussed = focusState.isFocused //set to true
                    }
                    .padding(bottom = 16.dp)
            )
            //if username is already taken
            if (usernameExisting != null) {
                Text(
                    text = usernameExisting!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Email input field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    //sets email error property
                    emailError = if (Patterns.EMAIL_ADDRESS.matcher(it)
                            .matches()
                    ) null else "Invalid email address" //checks and sets emailError is email address does not match standard
                    //sets isError to false
                    if (email.isNotEmpty()) emailEmpty = null
                },
                label = { Text("Email") },
                singleLine = true,
                isError = emailError != null || emailEmpty != null, //sets to false
                leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = "Email Icon") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            //displays email error message
            if (emailError != null) {
                Text(
                    text = emailError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Password input field
            OutlinedTextField(
                value = password,
                onValueChange = { input ->
                    //trim input
                    password = input.trim()
                    //check is password is less than 8 char
                    passwordError = if (password.length < 8 || !password.any { it.isDigit() }) {
                        "Password must not be less than 8 characters and must contain at least one number"
                    } else null
                    //sets isError to false
                    if (password.isNotEmpty()) passwordEmpty = null
                },
                label = { Text("Password") },
                singleLine = true,
                isError = passwordError != null || passwordEmpty != null, //sets to false
                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Password Icon") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    // changes passwordVisible State
                    IconButton(onClick = {
                        passwordVisible = !passwordVisible
                    }) {
                        Icon(
                            // changes password visibility icon and text
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            //displays password error message
            if (passwordError != null) {
                Text(
                    text = passwordError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Confirm Password input field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    //trims input
                    confirmPassword = it.trim()
                    //sets isError to false
                    if (confirmPassword.isNotEmpty()) confirmPasswordEmpty = null
                },
                label = { Text("Confirm Password") },
                singleLine = true,
                isError = confirmPasswordEmpty != null, //sets to false
                leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Confirm Password Icon") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    // changes passwordVisible State
                    IconButton(onClick = {
                        confirmPasswordVisible = !confirmPasswordVisible
                    }) {
                        Icon(
                            // changes password visibility icon and text
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            // Create Account button
            Button(
                onClick = {
                    //call to validate fields
                    validateFields()
                    Log.d("vaildationResult", "$validationResult")
                    //check if there are no validation messages
                    if (validationResult == null) {
                        //call to create account
                        viewModel.createAccount(username, email, password)
                    } else {
                        when (validationResult) {
                            "EmptyFields" -> {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            "Mismatch Passwords" -> {
                                Toast.makeText(
                                    context,
                                    "Passwords do not match, please check passwords",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            "Invalid Email" -> {
                                Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            "Invalid Password" -> {
                                Toast.makeText(
                                    context,
                                    "Password must not be less than 8 characters and must contain at least one number",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            "Username Taken" -> {
                                Toast.makeText(
                                    context,
                                    "Username is already taken please enter a different username",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Create Account", style = MaterialTheme.typography.bodyLarge)
            }
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun CreateAccountScreenPreview() {
    createAccountScreen(navController = rememberNavController())
}