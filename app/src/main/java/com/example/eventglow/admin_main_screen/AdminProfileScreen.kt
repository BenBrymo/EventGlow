package com.example.eventglow.admin_main_screen

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.MainActivityViewModel
import com.example.eventglow.R
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(
    navController: NavController,
    sharedPreferencesViewModel: SharedPreferencesViewModel = viewModel(),
    mainActivityViewModel: MainActivityViewModel = viewModel(),
    viewModel: AdminViewModel = viewModel()
) {
    // Shared preferences data
    val userData by sharedPreferencesViewModel.userInfo.collectAsState()

    val scope = rememberCoroutineScope()

    var profilePictureUrl by remember { mutableStateOf<Uri?>(null) }

    //header image uri variable
    var headerPictureUrl by remember { mutableStateOf(userData["HEADER_PICTURE_URL"]?.toUri()) }
    Log.d(
        "Admin Profile Screen:",
        "Header Picture retrived from shared preference data: ${userData["HEADER_PICTURE_URL"]}"
    )


    // Retrieves username from userData
    val username = userData["USERNAME"]
    Log.d("Admin Profile Screen:", "Username retrived from shared preference data: ${userData["USERNAME"]}")

    LaunchedEffect(Unit) {
        // Retrieves profile picture from userData

        scope.launch {
            profilePictureUrl = viewModel.fetchProfilePictureUrlFromFirestore()?.toUri()
            Log.d("AdminProfileScreen", "Retrived profilePictureUrl FROM Firestore: $profilePictureUrl")
        }
    }

    // profile image launcher
    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri.let { imageUrl ->
            //sets profilePictureUrl
            profilePictureUrl = imageUrl

            //Update profilePictureUrl in User's SharedPreferences
            viewModel.updateProfileImageUrlInSharedPreferences(imageUrl.toString())

            //update profilePictureUrl in firestore
            viewModel.updateProfilePictureUrlInFirestore(imageUrl.toString())

            Log.d("SharedPreference new profile url:", "$userData[PROFILE_PICTURE_URL]")

        }
    }


    // header image launcher
    val headerImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri.let { imageUrl ->
            //sets headerPictureUrl
            headerPictureUrl = imageUrl

            //Update headerPictureUrl in User's SharedPreferences
            viewModel.updateHeaderInSharedPreferences(imageUrl.toString())

            Log.d("SharedPreference new header url:", "$userData[HEADER_PICTURE_URL]")
        }
    }

    AdminProfileScreenContent(
        username = username,
        headerImageData = headerPictureUrl,
        profileImageData = profilePictureUrl,
        onHeaderImageClick = { headerImagePickerLauncher.launch("image/*") },
        onProfileImageClick = { profileImagePickerLauncher.launch("image/*") },
        onDraftedEvents = { navController.navigate(Routes.DRAFTED_EVENTS_SCREEN) },
        onHistory = { /* Navigate to History */ },
        onStatistics = { /* Navigate to Statistics */ },
        onLogout = {
            mainActivityViewModel.signOut(
                onSuccess = {
                    navController.navigate(Routes.LOGIN_SCREEN) {
                        popUpTo(Routes.ADMIN_PROFILE_SCREEN) {
                            inclusive = true // Clear the back stack to avoid returning to the Profile Screen
                        }
                    }
                },
                onError = { Log.d("Log Out AdminProfile Screen ", "Could not log out") }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreenContent(
    username: String?,
    headerImageData: Any?,
    profileImageData: Any?,
    onHeaderImageClick: () -> Unit,
    onProfileImageClick: () -> Unit,
    onDraftedEvents: () -> Unit,
    onHistory: () -> Unit,
    onStatistics: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Card to keep content and for ui design
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                //Header image
                Image(
                    painter = rememberAsyncImagePainter(headerImageData),
                    contentDescription = "Header Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clickable(onClick = onHeaderImageClick),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Profile Box
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier,
            ) {
                //Profile Image
                Column {
                    // Displays profile image if it exists
                    if (profileImageData != null) {
                        Log.d("ProfilePicture", "Loading image from $profileImageData")
                        Card(
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(profileImageData),
                                contentDescription = "Admin Avatar",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onProfileImageClick),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Admin Avatar",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(onClick = onProfileImageClick)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(22.dp))
                // Name and Admin Badge Row
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = username ?: "Username",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge { Text("Admin") }
                }
            }
        }
        //Menu items
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Scrollable Menu Items
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Event menu item
                MenuItem(
                    icon = Icons.Default.Event,
                    title = "Drafted Events",
                    onClick = onDraftedEvents
                )

                // History menu item
                MenuItem(
                    icon = Icons.Default.History,
                    title = "History",
                    onClick = onHistory
                )
                // Statistics menu item
                MenuItem(
                    icon = Icons.Default.Analytics,
                    title = "Statistics",
                    onClick = onStatistics
                )
                // Log out menu item
                MenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Log Out",
                    onClick = onLogout
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

@Preview(showBackground = true)
@Composable
fun AdminProfileScreenPreview() {
    AdminProfileScreenContent(
        username = "Admin User",
        headerImageData = R.drawable.applogo,
        profileImageData = R.drawable.user_logo,
        onHeaderImageClick = {},
        onProfileImageClick = {},
        onDraftedEvents = {},
        onHistory = {},
        onStatistics = {},
        onLogout = {}
    )
}
