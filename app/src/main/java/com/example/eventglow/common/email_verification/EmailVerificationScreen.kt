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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.common.LoadState
import com.example.eventglow.navigation.Routes
import com.example.eventglow.navigation.navigateAndClearTo
import com.example.eventglow.ui.theme.Success
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    navController: NavController,
    viewModel: EmailVerificationViewModel = viewModel(),
) {
    val context = LocalContext.current
    val loadState by viewModel.loadState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val emailSentCount by viewModel.emailSentCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isButtonEnabled by remember { mutableStateOf(true) }
    var timeRemaining by remember { mutableStateOf(60) }
    var timeRunning by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        viewModel.sendVerificationEmail()
    }

    LaunchedEffect(emailSentCount) {
        if (emailSentCount <= 0) return@LaunchedEffect
        isButtonEnabled = false
        timeRemaining = 60
        timeRunning = true
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
        isButtonEnabled = true
        timeRunning = false
    }

    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        Log.e("EmailVerification", message)
        viewModel.clearError()
    }

    EmailVerificationContent(
        isButtonEnabled = isButtonEnabled,
        timeRemaining = timeRemaining,
        loadState = loadState,
        snackbarHostState = snackbarHostState,
        onBack = { navController.popBackStack() },
        onResend = {
            viewModel.sendVerificationEmail()
            if (!timeRunning) {
                Toast.makeText(context, "Email has been sent again", Toast.LENGTH_SHORT).show()
            }
        },
        onDone = { navController.navigateAndClearTo(Routes.LOGIN_SCREEN) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationContent(
    isButtonEnabled: Boolean,
    timeRemaining: Int,
    loadState: LoadState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onDone: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    tint = Success,
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
                    enabled = isButtonEnabled && loadState != LoadState.LOADING
                ) {
                    if (loadState == LoadState.LOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = "Resend Email")
                }
                Spacer(modifier = Modifier.height(10.dp))
                //only show timer when button is disabled
                if (!isButtonEnabled) {
                    Text(
                        text = timeRemaining.toString(),
                        color = MaterialTheme.colorScheme.onBackground,
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
        loadState = LoadState.SUCCESS,
        snackbarHostState = SnackbarHostState(),
        onBack = {},
        onResend = {},
        onDone = {}
    )
}
