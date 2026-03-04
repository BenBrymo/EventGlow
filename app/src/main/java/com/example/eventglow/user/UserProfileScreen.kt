package com.example.eventglow.user

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.eventglow.MainActivityViewModel
import com.example.eventglow.R
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.navigation.Routes
import com.example.eventglow.navigation.navigateAndClearTo
import com.example.eventglow.navigation.navigateSingleTop
import kotlinx.coroutines.launch


private fun String?.toImageUriOrNull(): Uri? {
    if (this.isNullOrBlank() || this.equals("null", ignoreCase = true)) return null
    return runCatching { Uri.parse(this) }.getOrNull()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    mainNavController: NavController, // Main NavController
    bottomNavController: NavController, // Bottom NavController
    sharedPreferencesViewModel: SharedPreferencesViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    mainActivityViewModel: MainActivityViewModel = viewModel()
) {
    val userData by sharedPreferencesViewModel.userInfo.collectAsState()
    val username = userData["USERNAME"]
    val profileImageUri = userData["PROFILE_PICTURE_URL"].toImageUriOrNull()
    val headerImageUri = userData["HEADER_PICTURE_URL"].toImageUriOrNull()
    val scope = rememberCoroutineScope()

    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val storageUrl = userViewModel.uploadImageToSupabaseStorage(
                imageUri = uri,
                folder = "eventGlow/profile"
            ) ?: return@launch
            userViewModel.updateProfilePictureUrlInFirestore(storageUrl)
            sharedPreferencesViewModel.refreshUserInfo()
        }
    }

    val headerImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val storageUrl = userViewModel.uploadImageToSupabaseStorage(
                imageUri = uri,
                folder = "eventGlow/header"
            ) ?: return@launch
            userViewModel.updateHeaderPictureUrlInFirestore(storageUrl)
            sharedPreferencesViewModel.refreshUserInfo()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        ProfileScreenContent(
            modifier = Modifier.padding(innerPadding),
            username = username,
            headerImageData = headerImageUri,
            profileImageData = profileImageUri,
            onHeaderImageClick = { headerImagePickerLauncher.launch("image/*") },
            onProfileImageClick = { profileImagePickerLauncher.launch("image/*") },
            onFavoriteEvents = { bottomNavController.navigateSingleTop(RoutesUser.FAVOURITE_EVENTS_SCREEN) },
            onTransactions = { bottomNavController.navigateSingleTop(RoutesUser.TRANSACTIONS) },
            onHelpCenter = { bottomNavController.navigateSingleTop(RoutesUser.HELP_CENTER) },
            onSettings = { bottomNavController.navigateSingleTop(RoutesUser.SETTINGS) },
            onLogOut = {
                mainActivityViewModel.signOut(
                    onSuccess = { mainNavController.navigateAndClearTo(Routes.LOGIN_SCREEN) },
                    onError = { Log.e("ProfileScreen", "Could not log out: ${it.message}", it) }
                )
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreenContent(
    modifier: Modifier = Modifier,
    username: String?,
    headerImageData: Any?,
    profileImageData: Any?,
    onHeaderImageClick: () -> Unit,
    onProfileImageClick: () -> Unit,
    onFavoriteEvents: () -> Unit,
    onTransactions: () -> Unit,
    onHelpCenter: () -> Unit,
    onSettings: () -> Unit,
    onLogOut: () -> Unit
) {
    val fallbackPainter = painterResource(id = R.drawable.applogo)
    val safeHeaderModel = sanitizeImageModel(headerImageData)
    val safeProfileModel = sanitizeImageModel(profileImageData)

    Column(
        modifier = Modifier
            .then(modifier)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            AsyncImage(
                model = safeHeaderModel ?: R.drawable.applogo,
                contentDescription = "Header Image",
                fallback = fallbackPainter,
                error = fallbackPainter,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onHeaderImageClick)
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 20.dp, y = 40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            ) {
                AsyncImage(
                    model = safeProfileModel ?: R.drawable.user_logo,
                    contentDescription = "User Avatar",
                    fallback = fallbackPainter,
                    error = fallbackPainter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onProfileImageClick)
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
                text = username.orEmpty().ifBlank { "User" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Badge(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text("User")
            }

            Spacer(modifier = Modifier.height(28.dp))

            ProfileMenuRow(
                icon = Icons.Filled.Favorite,
                title = "Favorite Events",
                onClick = onFavoriteEvents
            )
            ProfileMenuRow(
                icon = Icons.Outlined.AttachMoney,
                title = "Transactions",
                onClick = onTransactions
            )
            ProfileMenuRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                title = "Help Center",
                onClick = onHelpCenter
            )
            ProfileMenuRow(
                icon = Icons.Filled.Settings,
                title = "Settings",
                onClick = onSettings
            )
            ProfileMenuRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Log Out",
                onClick = onLogOut
            )

            Spacer(modifier = Modifier.height(24.dp))
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
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreenContent(
            username = "BenBrymo",
            headerImageData = R.drawable.applogo,
            profileImageData = R.drawable.user_logo,
            onHeaderImageClick = {},
            onProfileImageClick = {},
            onFavoriteEvents = {},
            onTransactions = {},
            onHelpCenter = {},
            onSettings = {},
            onLogOut = {},
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ProfileScreenContainerPreview() {
    ProfileScreen(
        mainNavController = rememberNavController(),
        bottomNavController = rememberNavController(),
    )
}
