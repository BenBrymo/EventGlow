package com.example.eventglow.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(navController: NavController) {
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

        SettingsItem(
            icon = Icons.Default.Person,
            title = "Profile Settings",
            description = "Update your profile information"
        ) {
            // Navigate to Profile Settings screen
        }

        SettingsItem(
            icon = Icons.Default.Password,
            title = "Change Password",
            description = "Update your account password"
        ) {
            // Navigate to Change Password screen
        }

        SettingsItem(
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

        SettingsItem(
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

        SettingsItem(
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

        SettingsItem(
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

        SettingsItem(
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

        SettingsItem(
            icon = Icons.Default.SupportAgent,
            title = "Contact Support",
            description = "Reach out for assistance"
        ) {
            // Navigate to Contact Support screen
        }

        SettingsItem(
            icon = Icons.Default.Feedback,
            title = "Send Feedback",
            description = "Help us improve with your feedback"
        ) {
            // Navigate to Feedback form screen
        }
    }
}

@Composable
fun SettingsItem(
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
