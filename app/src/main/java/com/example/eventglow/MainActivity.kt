package com.example.eventglow

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.navigation.NavGraph
import com.example.eventglow.navigation.Routes
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // initializes firebase
        FirebaseApp.initializeApp(this)
        setContent {
            MyApp()
        }
    }
}


@Composable
fun MyApp() {
    val navController = rememberNavController() //for navigation
    NavGraph(navController = navController) // calls navgraph for navigation
    val viewModel: MainActivityViewModel = viewModel() // sets the persistentLoginState
}

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: MainActivityViewModel = viewModel(),
    sharedPreferencesViewModel: SharedPreferencesViewModel = viewModel()
) {
    val scope = rememberCoroutineScope() // for asynchronous actions

    val context = LocalContext.current

    //shared preferences data
    val userData by sharedPreferencesViewModel.userInfo.collectAsState()

    //retrives role from userData
    val role = userData["ROLE"]

    Log.d("SplashScreen", "Retrived role: $role")

    //retrives state of persistentLoginState
    val loginState by viewModel.persistentLoginState.collectAsState()

    //performs action on launch
    LaunchedEffect(Unit) {
        scope.launch {
            delay(3000) // 3 seconds delay
            when (loginState) {
                // When user is already signed in direct to appropriate screen
                is PersistentLoginState.SignedIn -> {
                    //checks role of user
                    when (role) {

                        //when role is user
                        "user" -> {
                            navController.navigate(Routes.USER_MAIN_SCREEN) {
                                popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                            }
                        }

                        //when role is admin
                        "admin" -> {
                            navController.navigate(Routes.ADMIN_MAIN_SCREEN) {
                                popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                            }
                        }
                    }
                }
                // When user is logged out show login screen
                PersistentLoginState.SignedOut -> {
                    navController.navigate(Routes.LOGIN_SCREEN) {
                        popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                    }
                }

                PersistentLoginState.EmailNotVerified -> {
                    Toast.makeText(context, "Please verify your email address before continuing", Toast.LENGTH_SHORT)
                        .show()
                    navController.navigate(Routes.EMAIL_VERIFICATION_SCREEN) {
                        popUpTo(Routes.SPLASH_SCREEN) { inclusive = true }
                    }
                }
            }
        }
    }

    // splash screen content container
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        //splash screen content
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Splash Screen Image
            Image(
                painter = painterResource(R.drawable.applogo),
                modifier = Modifier.clip(RoundedCornerShape(50.dp)), //Makes image take round shape
                contentScale = ContentScale.FillBounds, //Image fillbounds or stretches
                contentDescription = "Image"
            )
        }
        //Copyright Statement
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(id = R.string.copyright), //copyright label
                modifier.paddingFromBaseline(bottom = 25.dp), // padding from baseline of the text applied at the bottom
                color = Color.Black,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold //FontWeight set to Bold
                )
            )
        }
    }
}

