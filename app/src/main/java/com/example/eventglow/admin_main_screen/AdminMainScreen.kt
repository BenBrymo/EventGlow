package com.example.eventglow.admin_main_screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    navController: NavController,
    sharedPreferencesViewModel: SharedPreferencesViewModel = viewModel()
) {

    //shared preferences data
    val userData by sharedPreferencesViewModel.userInfo.collectAsState()
    Log.d("AdminMainScreen", "Data in Shared Preferences now: $userData")

    //retrives username from userData
    val username = userData["USERNAME"]
    Log.d("AdminMainScreen", "Retrived Username: $username")

    // Remember coroutine scope for performing asynchronous actions
    val scope = rememberCoroutineScope()

    // State to control drawer open/close
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Scaffold to provide a basic material layout structure
    Scaffold(

        // Top bar with navigation menu and profile icon
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Welcome ${username ?: "Admin"}", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "Navigation menu")
                    }
                },
                actions = {
                    // navigates to Admin Profile screen
                    IconButton(onClick = { navController.navigate(Routes.ADMIN_PROFILE_SCREEN) }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        // Modal drawer for the navigation menu
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                //content of drawer
                Column(
                    modifier = Modifier
                        .width(240.dp) // Set the width of the drawer
                        .fillMaxHeight() // Set the height to max available
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = 50.dp)
                ) {

                    Spacer(Modifier.height(16.dp))

                    // Event Navigation item
                    NavigationItem(
                        "Event Management",
                        onClick = {
                            scope.launch {
                                drawerState.close()

                                //navigates to Event Management screen and removes this screen from stack
                                navController.navigate(Routes.EVENTS_MANAGEMENT_SCREEN) {
                                    popUpTo(Routes.ADMIN_MAIN_SCREEN) { inclusive = false }
                                }
                            }
                        },
                        imageVector = Icons.Default.Event
                    )

                    // User Navigation item
                    NavigationItem(
                        "User Management",
                        onClick = {
                            scope.launch {
                                drawerState.close()

                                // navigates to User Management screen and removes this screen from stack
                                navController.navigate(Routes.USER_MANAGEMENT_SCREEN) {
                                    popUpTo(Routes.ADMIN_MAIN_SCREEN) { inclusive = false }
                                }
                            }
                        },
                        imageVector = Icons.Default.AccountBox
                    )

                    // Ticket Navigation item
                    NavigationItem(
                        "Ticket Management",
                        onClick = {
                            scope.launch {
                                drawerState.close()

                                // navigates to Ticket Management screen and removes this screen from stack
                                navController.navigate(Routes.TICKET_MANAGEMENT_SCREEN) {
                                    popUpTo(Routes.ADMIN_MAIN_SCREEN) { inclusive = false }
                                }
                            }
                        },
                        imageVector = Icons.AutoMirrored.Filled.AirplaneTicket
                    )

                    // Report & Analytics Navigation item
                    NavigationItem(
                        "Reporting & Analytics",
                        onClick = {
                            scope.launch {
                                drawerState.close()

                                // navigates to REPORTING_AND_ANALYTICS screen and removes this screen from stack
                                navController.navigate(Routes.REPORTING_AND_ANALYTICS) {
                                    popUpTo(Routes.ADMIN_MAIN_SCREEN) { inclusive = false }
                                }
                            }
                        },
                        imageVector = Icons.Default.Analytics
                    )

                    // Settings Navigation item
                    NavigationItem(
                        "Settings",
                        onClick = {
                            scope.launch {
                                drawerState.close()

                                // navigates to REPORTING_AND_ANALYTICS screen and removes this screen from stack
                                navController.navigate(Routes.SETTINGS) {
                                    popUpTo(Routes.ADMIN_MAIN_SCREEN) { inclusive = false }
                                }
                            }
                        },
                        imageVector = Icons.Default.Settings
                    )
                }
            },

            scrimColor = Color.White // sets color of modal navigation drawer
        ) {

            // Main content based on currentRoute
            Box(Modifier.padding(paddingValues)) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = "Under Development",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Coming Soon",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Stay tuned for updates on new features!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun NavigationItem(
    text: String,
    onClick: () -> Unit,
    imageVector: ImageVector
) {

    //container for NavigationItem
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {

        //content of navigation item
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            Modifier.padding(end = 15.dp)
        )
        Text(
            text,
            Modifier.padding(top = 2.dp)
        )
    }
}

