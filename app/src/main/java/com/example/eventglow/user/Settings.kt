package com.example.eventglow.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderStrong
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {

    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        containerColor = Background,
        topBar = {
            SettingsTopBar(onBack = { })
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {

            Spacer(Modifier.height(16.dp))

            SettingsSectionTitle("Account Management")

            SettingsItem(
                icon = Icons.Filled.Person,
                title = "Profile Settings",
                subtitle = "Update your profile information",
                onClick = { }
            )

            SettingsItem(
                icon = Icons.Filled.Lock,
                title = "Change Password",
                subtitle = "Update your account password",
                onClick = { }
            )

            Spacer(Modifier.height(20.dp))

            SettingsSectionTitle("Notification Settings")

            NotificationItem(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = "Receive notifications about events",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            Spacer(Modifier.height(20.dp))

            SettingsSectionTitle("Support & Feedback")

            SettingsItem(
                icon = Icons.Filled.SupportAgent,
                title = "Contact Support",
                subtitle = "Reach out for assistance",
                onClick = { }
            )

            SettingsItem(
                icon = Icons.Filled.Feedback,
                title = "Send Feedback",
                subtitle = "Help us improve with your feedback",
                onClick = { }
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { },
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
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
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
private fun SettingsSectionTitle(
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
private fun SettingsItem(
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
private fun NotificationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = BrandPrimary,
                uncheckedThumbColor = TextPrimary,
                uncheckedTrackColor = BorderStrong
            )
        )
    }
}


@Composable
fun SettingsScreen2(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Account Management Section
        AccountManagement()

        Spacer(modifier = Modifier.height(16.dp))

        // App Preferences Section
        AppPreferences()

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Settings Section
        NotificationSettings()

        Spacer(modifier = Modifier.height(16.dp))

        // Support & Feedback Section
        SupportAndFeedback(navController)

        Spacer(modifier = Modifier.height(16.dp))

        // Log Out Button
        Button(
            onClick = {
                // Handle logout
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.AutoMirrored.Default.ExitToApp, contentDescription = "Log Out")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out")
        }
    }
}

@Composable
fun AccountManagement() {
    Column {
        Text(
            text = "Account Management",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem2(
            icon = Icons.Default.Person,
            title = "Profile Settings",
            description = "Update your profile information"
        ) {
            // Navigate to Profile Settings screen
        }

        SettingsItem2(
            icon = Icons.Default.Password,
            title = "Change Password",
            description = "Update your account password"
        ) {
            // Navigate to Change Password screen
        }

        SettingsItem2(
            icon = Icons.Default.Lock,
            title = "Manage Security",
            description = "Set up two-factor authentication (2FA)"
        ) {
            // Navigate to Security Settings screen
        }
    }
}

@Composable
fun AppPreferences() {
    var darkModeEnabled by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }

    Column {
        Text(
            text = "App Preferences",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem2(
            icon = Icons.Default.Brightness6,
            title = "Dark Mode",
            description = "Enable or disable dark theme"
        ) {
            darkModeEnabled = !darkModeEnabled
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Dark Mode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = darkModeEnabled,
                onCheckedChange = { darkModeEnabled = it }
            )
        }

        SettingsItem2(
            icon = Icons.Default.Language,
            title = "App Language",
            description = "Select your preferred language"
        ) {
            // Handle language selection dialog or screen
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Language",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = selectedLanguage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun NotificationSettings() {
    var emailNotificationsEnabled by remember { mutableStateOf(true) }
    var appNotificationsEnabled by remember { mutableStateOf(true) }

    Column {
        Text(
            text = "Notification Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem2(
            icon = Icons.Default.Email,
            title = "Email Notifications",
            description = "Receive notifications via email"
        ) {
            emailNotificationsEnabled = !emailNotificationsEnabled
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Email Notifications",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = emailNotificationsEnabled,
                onCheckedChange = { emailNotificationsEnabled = it }
            )
        }

        SettingsItem2(
            icon = Icons.Default.Notifications,
            title = "In-App Notifications",
            description = "Receive notifications within the app"
        ) {
            appNotificationsEnabled = !appNotificationsEnabled
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "In-App Notifications",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = appNotificationsEnabled,
                onCheckedChange = { appNotificationsEnabled = it }
            )
        }
    }
}

@Composable
fun SupportAndFeedback(navController: NavController) {
    Column {
        Text(
            text = "Support & Feedback",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsItem2(
            icon = Icons.Default.SupportAgent,
            title = "Contact Support",
            description = "Reach out for assistance"
        ) {
            // Navigate to Contact Support screen
        }

        SettingsItem2(
            icon = Icons.Default.Feedback,
            title = "Send Feedback",
            description = "Help us improve with your feedback"
        ) {
            // Navigate to Feedback form screen
        }
    }
}

@Composable
fun SettingsItem2(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(navController = rememberNavController())
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun SettingsScreen2Preview() {
    SettingsScreen2(navController = rememberNavController())
}
