package com.example.eventglow.admin_main_screen

import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventglow.MainActivityViewModel
import com.example.eventglow.R
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.EventGlowTheme


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

    var hasFetchedMissingFields by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(userData["USERNAME"]) }
    var profilePictureUrl by remember {
        mutableStateOf(userData["PROFILE_PICTURE_URL"]?.takeIf { it.isNotBlank() }?.toUri())
    }
    var headerPictureUrl by remember {
        mutableStateOf(userData["HEADER_PICTURE_URL"]?.takeIf { it.isNotBlank() }?.toUri())
    }

    val isUsernameMissing = userData["USERNAME"].isNullOrBlank()
    val isProfilePictureMissing = userData["PROFILE_PICTURE_URL"].isNullOrBlank()
    val isHeaderPictureMissing = userData["HEADER_PICTURE_URL"].isNullOrBlank()

    LaunchedEffect(isUsernameMissing, isProfilePictureMissing, isHeaderPictureMissing) {
        if (hasFetchedMissingFields) return@LaunchedEffect
        if (!isUsernameMissing && !isProfilePictureMissing && !isHeaderPictureMissing) return@LaunchedEffect

        hasFetchedMissingFields = true
        val fallbackData = viewModel.fetchMissingProfileFieldsFromFirestore(
            fetchUsername = isUsernameMissing,
            fetchProfilePictureUrl = isProfilePictureMissing,
            fetchHeaderPictureUrl = isHeaderPictureMissing
        ) ?: return@LaunchedEffect

        if (isUsernameMissing && !fallbackData.username.isNullOrBlank()) {
            username = fallbackData.username
            viewModel.updateUsernameInSharedPreferences(fallbackData.username)
        }

        if (isProfilePictureMissing && !fallbackData.profilePictureUrl.isNullOrBlank()) {
            profilePictureUrl = fallbackData.profilePictureUrl.toUri()
            viewModel.updateProfileImageUrlInSharedPreferences(fallbackData.profilePictureUrl)
        }

        if (isHeaderPictureMissing && !fallbackData.headerPictureUrl.isNullOrBlank()) {
            headerPictureUrl = fallbackData.headerPictureUrl.toUri()
            viewModel.updateHeaderInSharedPreferences(fallbackData.headerPictureUrl)
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
            //Update headerPictureUrl in Firestore
            viewModel.updateHeaderPictureUrlInFirestore(imageUrl.toString())

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
        onSettings = { navController.navigate(Routes.SETTINGS) },
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
    onSettings: () -> Unit,
) {
    val fallbackPainter = painterResource(id = R.drawable.applogo)
    val safeHeaderModel = sanitizeImageModel(headerImageData)
    val safeProfileModel = sanitizeImageModel(profileImageData)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clickable(onClick = onHeaderImageClick)
        ) {
            AsyncImage(
                model = safeHeaderModel ?: R.drawable.applogo,
                contentDescription = "Header Image",
                fallback = fallbackPainter,
                error = fallbackPainter,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 20.dp, y = 40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .clickable(onClick = onProfileImageClick)
            ) {
                AsyncImage(
                    model = safeProfileModel ?: R.drawable.applogo,
                    contentDescription = "Admin Avatar",
                    fallback = fallbackPainter,
                    error = fallbackPainter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = username ?: "Username",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Badge { Text("Admin") }

            Spacer(modifier = Modifier.height(28.dp))

            MenuItem(
                icon = Icons.Default.Event,
                title = "Drafted Events",
                onClick = onDraftedEvents
            )
            MenuItem(
                icon = Icons.Filled.Settings,
                title = "Settings",
                onClick = onSettings
            )
        }
    }
}

private fun sanitizeImageModel(model: Any?): Any? {
    return when (model) {
        null -> null
        is String -> model.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        is Uri -> {
            val value = model.toString()
            if (value.isBlank() || value.equals("null", ignoreCase = true)) null else model
        }

        else -> model
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
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(name = "Admin Profile Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 34)
@Composable
fun AdminProfileScreenLightPreview() {
    EventGlowTheme(darkTheme = false) {
        AdminProfileScreenContent(
            username = "Admin User",
            headerImageData = R.drawable.applogo,
            profileImageData = R.drawable.user_logo,
            onHeaderImageClick = {},
            onProfileImageClick = {},
            onDraftedEvents = {},
            onSettings = {},
        )
    }
}

@Preview(name = "Admin Profile Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 34)
@Composable
fun AdminProfileScreenDarkPreview() {
    EventGlowTheme(darkTheme = true) {
        AdminProfileScreenContent(
            username = "Admin User",
            headerImageData = R.drawable.applogo,
            profileImageData = R.drawable.user_logo,
            onHeaderImageClick = {},
            onProfileImageClick = {},
            onDraftedEvents = {},
            onSettings = {},
        )
    }
}
