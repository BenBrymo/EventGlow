package com.example.eventglow.user


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderDefault
import com.example.eventglow.ui.theme.BorderSubtle
import com.example.eventglow.ui.theme.BottomNavGray
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.SurfaceLevel3
import com.example.eventglow.ui.theme.TextHint
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Change Password",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        },
        bottomBar = {
            BottomNavigationBarPlaceholder()
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {

            PasswordField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = "Current Password",
                visible = showCurrent,
                onVisibilityToggle = { showCurrent = !showCurrent }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New Password",
                visible = showNew,
                onVisibilityToggle = { showNew = !showNew }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm New Password",
                visible = showConfirm,
                onVisibilityToggle = { showConfirm = !showConfirm }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceLevel3,
                    contentColor = TextSecondary
                )
            ) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(label, color = TextHint)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
        visualTransformation =
        if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (visible)
                        Icons.Default.Visibility
                    else
                        Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        },
        shape = RoundedCornerShape(6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceLevel3,
            unfocusedContainerColor = SurfaceLevel3,
            focusedBorderColor = BorderDefault,
            unfocusedBorderColor = BorderSubtle,
            cursorColor = BrandPrimary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
fun BottomNavigationBarPlaceholder() {
    NavigationBar(
        containerColor = BottomNavGray
    ) {
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Bookmark, null) },
            label = { Text("Bookmarks") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.ConfirmationNumber, null) },
            label = { Text("Tickets") }
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Profile") }
        )
    }
}
