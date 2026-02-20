package com.example.eventglow.settings

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eventglow.MainActivityViewModel
import com.example.eventglow.navigation.Routes
import com.example.eventglow.navigation.navigateAndClearTo
import com.example.eventglow.notifications.FirestoreNotificationSenderViewModel
import com.example.eventglow.notifications.NotificationSettingsViewModel
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderStrong
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.EventGlowTheme
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(
    navController: NavController,
    mainActivityViewModel: MainActivityViewModel = viewModel(),
    notificationSettingsViewModel: NotificationSettingsViewModel = viewModel(),
    senderViewModel: FirestoreNotificationSenderViewModel = viewModel()
) {
    val notificationsEnabled by notificationSettingsViewModel.notificationsEnabled.collectAsState()
    val isUpdatingNotificationPreference by notificationSettingsViewModel.isUpdating.collectAsState()
    val notificationError by notificationSettingsViewModel.errorMessage.collectAsState()
    val isSendingPush by senderViewModel.isSending.collectAsState()
    val pushError by senderViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pushTitle by remember { mutableStateOf("") }
    var pushBody by remember { mutableStateOf("") }
    var testEventId by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("all") }

    LaunchedEffect(notificationError) {
        val message = notificationError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        notificationSettingsViewModel.clearError()
    }

    LaunchedEffect(pushError) {
        val message = pushError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        senderViewModel.clearError()
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AdminSettingsTopBar(onBack = { navController.popBackStack() })
        }
    ) { padding ->
        AdminSettingsContent(
            notificationsEnabled = notificationsEnabled,
            isUpdatingNotificationPreference = isUpdatingNotificationPreference,
            onProfileSettings = { navController.navigate(Routes.ADMIN_UPDATE_PROFILE_SCREEN) },
            onChangePassword = { navController.navigate(Routes.ADMIN_CHANGE_PASSWORD_SCREEN) },
            onToggleNotifications = { notificationSettingsViewModel.updateNotificationPreference(it) },
            onSupport = { navController.navigate(Routes.ADMIN_SUPPORT_SCREEN) },
            onHelpCenter = { navController.navigate(Routes.ADMIN_HELP_CENTER_SCREEN) },
            pushTitle = pushTitle,
            onPushTitleChange = { pushTitle = it },
            pushBody = pushBody,
            onPushBodyChange = { pushBody = it },
            testEventId = testEventId,
            onTestEventIdChange = { testEventId = it },
            targetRole = targetRole,
            onTargetRoleChange = { targetRole = it },
            isSendingPush = isSendingPush,
            onSendTestPush = {
                val normalizedRole = targetRole.trim().lowercase()
                val targetRoute = if (normalizedRole == "admin") {
                    "detailed_event_screen_admin"
                } else {
                    "detailed_event_screen"
                }
                senderViewModel.sendNotificationToRole(
                    title = pushTitle,
                    body = pushBody,
                    targetRole = targetRole,
                    route = targetRoute,
                    eventId = testEventId
                )
            },
            onLogout = {
                mainActivityViewModel.signOut(
                    onSuccess = {
                        navController.navigateAndClearTo(Routes.LOGIN_SCREEN)
                    },
                    onError = { error ->
                        Log.d("AdminSettingsScreen", "Logout failed: ${error.message}")
                    }
                )
            },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun AdminSettingsContent(
    notificationsEnabled: Boolean,
    isUpdatingNotificationPreference: Boolean,
    onProfileSettings: () -> Unit,
    onChangePassword: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSupport: () -> Unit,
    onHelpCenter: () -> Unit,
    pushTitle: String,
    onPushTitleChange: (String) -> Unit,
    pushBody: String,
    onPushBodyChange: (String) -> Unit,
    testEventId: String,
    onTestEventIdChange: (String) -> Unit,
    targetRole: String,
    onTargetRoleChange: (String) -> Unit,
    isSendingPush: Boolean,
    onSendTestPush: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        AdminSettingsSectionTitle("Account Management")

        AdminSettingsItem(
            icon = Icons.Filled.Person,
            title = "Profile Settings",
            subtitle = "Update your profile information",
            onClick = onProfileSettings
        )

        AdminSettingsItem(
            icon = Icons.Filled.Lock,
            title = "Change Password",
            subtitle = "Update your account password",
            onClick = onChangePassword
        )

        Spacer(Modifier.height(20.dp))

        AdminSettingsSectionTitle("Notification Settings")

        AdminNotificationItem(
            icon = Icons.Filled.Notifications,
            title = "Notifications",
            subtitle = "Receive notifications about events",
            checked = notificationsEnabled,
            enabled = !isUpdatingNotificationPreference,
            onCheckedChange = onToggleNotifications
        )

        Spacer(Modifier.height(20.dp))

        AdminSettingsSectionTitle("Support & Feedback")

        AdminSettingsItem(
            icon = Icons.Filled.SupportAgent,
            title = "Contact Support",
            subtitle = "Reach out for assistance",
            onClick = onSupport
        )

        AdminSettingsItem(
            icon = Icons.Filled.Feedback,
            title = "Help Center",
            subtitle = "Find answers to your questions",
            onClick = onHelpCenter
        )

        Spacer(Modifier.height(20.dp))

        AdminSettingsSectionTitle("Push Notifications")

        OutlinedTextField(
            value = pushTitle,
            onValueChange = onPushTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notification title") },
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = pushBody,
            onValueChange = onPushBodyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notification body") },
            minLines = 2
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = targetRole,
            onValueChange = onTargetRoleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Target role (all/user/admin)") },
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = testEventId,
            onValueChange = onTestEventIdChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Event ID for deep link (optional)") },
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onSendTestPush,
            enabled = !isSendingPush,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSendingPush) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Send Test Push")
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = TextPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminSettingsTopBar(
    onBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Background
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = TextPrimary
                )
            }
        },
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
    )
}

@Composable
private fun AdminSettingsSectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun AdminSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandPrimary
        )

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AdminNotificationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandPrimary
        )

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = BrandPrimary,
                uncheckedThumbColor = TextPrimary,
                uncheckedTrackColor = BorderStrong
            )
        )
    }
}

@Preview(name = "Admin Settings Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 34)
@Composable
fun AdminSettingsScreenLightPreview() {
    EventGlowTheme(darkTheme = false) {
        AdminSettingsContent(
            notificationsEnabled = true,
            isUpdatingNotificationPreference = false,
            onProfileSettings = {},
            onChangePassword = {},
            onToggleNotifications = {},
            onSupport = {},
            onHelpCenter = {},
            pushTitle = "New Event",
            onPushTitleChange = {},
            pushBody = "Tap to open event details",
            onPushBodyChange = {},
            testEventId = "sample-event-id",
            onTestEventIdChange = {},
            targetRole = "all",
            onTargetRoleChange = {},
            isSendingPush = false,
            onSendTestPush = {},
            onLogout = {}
        )
    }
}

@Preview(name = "Admin Settings Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 34)
@Composable
fun AdminSettingsScreenDarkPreview() {
    EventGlowTheme(darkTheme = true) {
        AdminSettingsContent(
            notificationsEnabled = true,
            isUpdatingNotificationPreference = false,
            onProfileSettings = {},
            onChangePassword = {},
            onToggleNotifications = {},
            onSupport = {},
            onHelpCenter = {},
            pushTitle = "New Event",
            onPushTitleChange = {},
            pushBody = "Tap to open event details",
            onPushBodyChange = {},
            testEventId = "sample-event-id",
            onTestEventIdChange = {},
            targetRole = "all",
            onTargetRoleChange = {},
            isSendingPush = false,
            onSendTestPush = {},
            onLogout = {}
        )
    }
}
