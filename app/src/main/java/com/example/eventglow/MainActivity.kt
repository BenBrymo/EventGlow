package com.example.eventglow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.dataClass.UserPreferences
import com.example.eventglow.navigation.NavGraph
import com.example.eventglow.navigation.Routes
import com.example.eventglow.navigation.navigateAndClearTo
import com.example.eventglow.notifications.NotificationDeepLinkStore
import com.example.eventglow.notifications.ROUTE_ADMIN_MAIN_SCREEN
import com.example.eventglow.notifications.ROUTE_DETAILED_EVENT_SCREEN
import com.example.eventglow.notifications.ROUTE_DETAILED_EVENT_SCREEN_ADMIN
import com.example.eventglow.notifications.ROUTE_USER_MAIN_SCREEN
import com.example.eventglow.ui.theme.EventGlowTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val userPreferences by lazy { UserPreferences(application) }


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_EventGlow)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        installFirebaseAppCheck()
        NotificationDeepLinkStore.setFromIntent(intent)
        requestNotificationPermissionIfNeeded()
        updateFcmTokenForCurrentUser()
        setContent {
            EventGlowTheme() {
                MyApp()
            }
        }
    }


    private fun installFirebaseAppCheck() {
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotificationDeepLinkStore.setFromIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
    }

    private fun updateFcmTokenForCurrentUser() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val cachedToken = userPreferences.getUserInfo()["FCM_TOKEN"]

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val normalizedCached = cachedToken?.trim().orEmpty()
            val normalizedFresh = token.trim()
            val hasChanged = normalizedFresh.isNotBlank() && normalizedFresh != normalizedCached

            if (!hasChanged) return@addOnSuccessListener

            userPreferences.updateFcmToken(normalizedFresh)

            if (userId.isNullOrBlank()) return@addOnSuccessListener

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(mapOf("fcmToken" to normalizedFresh), SetOptions.merge())
        }
    }
}


@Composable
fun MyApp() {
    val navController = rememberNavController()
    val sharedPreferencesViewModel: SharedPreferencesViewModel = viewModel()
    val userData by sharedPreferencesViewModel.userInfo.collectAsState()
    val role = userData["ROLE"]?.trim()?.lowercase().orEmpty()
    val pendingDeepLink by NotificationDeepLinkStore.pendingDeepLink.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(pendingDeepLink, role, currentRoute) {
        val deepLink = pendingDeepLink ?: return@LaunchedEffect
        if (currentRoute == Routes.SPLASH_SCREEN) return@LaunchedEffect
        if (role != "admin" && role != "user") return@LaunchedEffect

        if (role == "admin") {
            if ((deepLink.route == ROUTE_DETAILED_EVENT_SCREEN_ADMIN ||
                        deepLink.route == ROUTE_DETAILED_EVENT_SCREEN) &&
                !deepLink.eventId.isNullOrBlank()
            ) {
                navController.navigateAndClearTo(Routes.ADMIN_MAIN_SCREEN)
                navController.navigate("${ROUTE_DETAILED_EVENT_SCREEN_ADMIN}/${deepLink.eventId}")
            } else {
                navController.navigateAndClearTo(Routes.ADMIN_MAIN_SCREEN)
            }
        } else {
            if (deepLink.route == ROUTE_DETAILED_EVENT_SCREEN && !deepLink.eventId.isNullOrBlank()) {
                navController.navigateAndClearTo("${Routes.USER_MAIN_SCREEN}?eventId=${deepLink.eventId}")
            } else {
                navController.navigateAndClearTo(Routes.USER_MAIN_SCREEN)
            }
        }
        NotificationDeepLinkStore.consume()
    }

    NavGraph(navController = navController)
}


@Composable
fun SplashScreen(
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
            val pendingDeepLink = NotificationDeepLinkStore.consume()
            when (loginState) {
                // When user is already signed in direct to appropriate screen
                is PersistentLoginState.SignedIn -> {
                    //checks role of user
                    when (role) {

                        //when role is user
                        "user" -> {
                            if (pendingDeepLink?.route == ROUTE_DETAILED_EVENT_SCREEN && !pendingDeepLink.eventId.isNullOrBlank()) {
                                navController.navigateAndClearTo("${Routes.USER_MAIN_SCREEN}?eventId=${pendingDeepLink.eventId}")
                            } else if (pendingDeepLink?.route == ROUTE_USER_MAIN_SCREEN || pendingDeepLink?.route == ROUTE_ADMIN_MAIN_SCREEN) {
                                navController.navigateAndClearTo(Routes.USER_MAIN_SCREEN)
                            } else {
                                navController.navigateAndClearTo(Routes.USER_MAIN_SCREEN)
                            }
                        }

                        //when role is admin
                        "admin" -> {
                            if ((pendingDeepLink?.route == ROUTE_DETAILED_EVENT_SCREEN_ADMIN ||
                                        pendingDeepLink?.route == ROUTE_DETAILED_EVENT_SCREEN) &&
                                !pendingDeepLink.eventId.isNullOrBlank()
                            ) {
                                navController.navigateAndClearTo(Routes.ADMIN_MAIN_SCREEN)
                                navController.navigate("${ROUTE_DETAILED_EVENT_SCREEN_ADMIN}/${pendingDeepLink.eventId}")
                            } else if (pendingDeepLink?.route == ROUTE_ADMIN_MAIN_SCREEN || pendingDeepLink?.route == ROUTE_USER_MAIN_SCREEN) {
                                navController.navigateAndClearTo(Routes.ADMIN_MAIN_SCREEN)
                            } else {
                                navController.navigateAndClearTo(Routes.ADMIN_MAIN_SCREEN)
                            }
                        }
                    }
                }
                // When user is logged out show login screen
                PersistentLoginState.SignedOut -> {
                    navController.navigateAndClearTo(Routes.LOGIN_SCREEN)
                }

                PersistentLoginState.EmailNotVerified -> {
                    Toast.makeText(context, "Please verify your email address before continuing", Toast.LENGTH_SHORT)
                        .show()
                    navController.navigateAndClearTo(Routes.EMAIL_VERIFICATION_SCREEN)
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.applogo),
                        contentDescription = "App logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))


            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EventGlow",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.paddingFromBaseline(25.dp),
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(id = R.string.copyright),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.paddingFromBaseline(25.dp),
                )
            }
        }
    }
}
