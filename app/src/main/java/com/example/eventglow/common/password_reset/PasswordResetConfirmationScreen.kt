package com.example.eventglow.common.password_reset

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordResetConfirmationScreen(
    navController: NavController,
    viewModel: PasswordRecoveryViewModel = viewModel(),
    sharedPrefsViewModel: SharedPreferencesViewModel = viewModel()
) {

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    var isButtonEnabled by remember { mutableStateOf(true) }
    var timeRemaining by remember { mutableStateOf(60) }
    var timeRunning = remember { mutableStateOf(false) }

    //shared preferences data
    val userData by sharedPrefsViewModel.userInfo.collectAsState()

    //retrives email from userData
    val email = userData["USER_EMAIL"]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Password Reset", color = MaterialTheme.colorScheme.primary)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color.Green,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Password Reset Email Sent",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please check your email for instructions on how to reset your password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                //Resend Email Button
                Button(
                    onClick = {
                        email.let { email ->
                            scope.launch {
                                viewModel.sendPasswordResetEmail(
                                    email!!,
                                    onSuccess = {
                                        scope.launch {
                                            Toast.makeText(context, "Email has been sent again", Toast.LENGTH_SHORT)
                                                .show()
                                            //disable button
                                            isButtonEnabled = false
                                            //reset time remaining
                                            timeRemaining = 60
                                            //start clock
                                            timeRunning.value = true
                                            // jumpstart counting
                                            while (timeRemaining > 0) {
                                                delay(1000)
                                                //decrease time
                                                timeRemaining--
                                            }
                                            //enable button
                                            isButtonEnabled = true
                                            //stop clock
                                            timeRunning.value = false
                                        }
                                    },
                                    onError = {
                                        Toast.makeText(
                                            context,
                                            "An error occurred please wait for some time",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }

                    },
                    enabled = isButtonEnabled
                ) {//viewModel.sendPasswordResetEmail()
                    Text(text = "Resend Email")
                }
                Spacer(modifier = Modifier.height(10.dp))

                //only show timer when button is disabled
                if (!isButtonEnabled) {
                    Text(
                        text = timeRemaining.toString(),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate(Routes.LOGIN_SCREEN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Back to Login")
                }
            }
        }
    }
}
