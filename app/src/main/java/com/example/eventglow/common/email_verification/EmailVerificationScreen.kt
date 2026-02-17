package com.example.eventglow.common.email_verification

import android.util.Log
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    navController: NavController,
    viewModel: EmailVerificationViewModel = viewModel(),
) {

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    var isButtonEnabled by remember { mutableStateOf(true) }
    var timeRemaining by remember { mutableStateOf(60) }
    var timeRunning = remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        viewModel.sendVerificationEmail(
            onSuccess = {
                scope.launch {
                    //disable button
                    isButtonEnabled = false
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
            onError = { error ->
                // Show error message to the user
                Log.e("EmailVerification", "Failed to send verification email: ${error.message}")
            }
        )
    }

    EmailVerificationContent(
        isButtonEnabled = isButtonEnabled,
        timeRemaining = timeRemaining,
        onBack = { navController.popBackStack() },
        onResend = {
            scope.launch {
                viewModel.sendVerificationEmail(
                    onSuccess = {
                        scope.launch {
                            Toast.makeText(context, "Email has been sent again", Toast.LENGTH_SHORT).show()
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
                            "An error occurred please wait for sometime",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        },
        onDone = { navController.navigate(Routes.LOGIN_SCREEN) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationContent(
    isButtonEnabled: Boolean,
    timeRemaining: Int,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onDone: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Email Verification", color = MaterialTheme.colorScheme.primary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                    text = "Verification Email Sent",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onResend,
                    enabled = isButtonEnabled
                ) {
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
                Text(
                    text = "Please check your email for instructions on how to verify your email address.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EmailVerificationScreenPreview() {
    EmailVerificationContent(
        isButtonEnabled = false,
        timeRemaining = 42,
        onBack = {},
        onResend = {},
        onDone = {}
    )
}
