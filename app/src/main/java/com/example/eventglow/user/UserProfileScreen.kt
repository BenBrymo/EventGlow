package com.example.eventglow.user

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.MainActivityViewModel
import com.example.eventglow.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    mainNavController: NavController, // Main NavController
    bottomNavController: NavController, // Bottom NavController
    mainActivityViewModel: MainActivityViewModel = viewModel()
) {

    // Remember coroutine scope for performing asynchronous actions
    val scope = rememberCoroutineScope()

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                // Avatar
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUri),
                        contentDescription = "Admin Avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { /* Open image picker */ },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Admin Avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { /* Open image picker */ }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Name and Admin Badge Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
//                    Text(
//                        text = "userName",
//                        style = MaterialTheme.typography.titleLarge,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(end = 8.dp)
//                    )
                    Badge { Text("User") }
                }
            }
        }

        // Scrollable Menu Items
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                MenuItem(
                    icon = Icons.Default.Favorite,
                    title = "Favorite Events",
                    onClick = { bottomNavController.navigate(RoutesUser.FAVOURITE_EVENTS_SCREEN) }
                )
            }
            item {
                MenuItem(
                    icon = Icons.Default.History,
                    title = "History",
                    onClick = { /* Navigate to History */ }
                )
            }
            item {
                MenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    onClick = { bottomNavController.navigate(RoutesUser.SETTINGS) }
                )
            }
            item {
                MenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Log Out",
                    onClick = {
                        // Performs log out operation
                        mainActivityViewModel.signOut(
                            onSuccess = {

                                // Navigate to login screen and clear the main navigation stack
                                mainNavController.navigate(Routes.LOGIN_SCREEN) {
                                    popUpTo(Routes.LOGIN_SCREEN) {
                                        inclusive = true
                                    }
                                }
                            },
                            onError = { Log.d("Log Out UserProfile Screen ", "Could not log out : ${it.message}") }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}
