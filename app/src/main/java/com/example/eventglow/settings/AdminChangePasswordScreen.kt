package com.example.eventglow.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventglow.common.LoadState
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderDefault
import com.example.eventglow.ui.theme.BorderSubtle
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.SurfaceLevel3
import com.example.eventglow.ui.theme.TextHint
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChangePasswordScreen(
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    viewModel: ChangePasswordViewModel = viewModel()
) {
    val currentPassword by viewModel.currentPassword.collectAsState()
    val newPassword by viewModel.newPassword.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val showCurrent by viewModel.showCurrent.collectAsState()
    val showNew by viewModel.showNew.collectAsState()
    val showConfirm by viewModel.showConfirm.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val loadState by viewModel.loadState.collectAsState()
    val isSaving = loadState == LoadState.LOADING

    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            onSaveClick()
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Change Password", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
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
            AdminPasswordField(
                value = currentPassword,
                onValueChange = viewModel::onCurrentPasswordChanged,
                label = "Current Password",
                visible = showCurrent,
                onVisibilityToggle = viewModel::toggleShowCurrent
            )
            Spacer(modifier = Modifier.height(16.dp))
            AdminPasswordField(
                value = newPassword,
                onValueChange = viewModel::onNewPasswordChanged,
                label = "New Password",
                visible = showNew,
                onVisibilityToggle = viewModel::toggleShowNew
            )
            Spacer(modifier = Modifier.height(16.dp))
            AdminPasswordField(
                value = confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChanged,
                label = "Confirm New Password",
                visible = showConfirm,
                onVisibilityToggle = viewModel::toggleShowConfirm
            )
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!successMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = successMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = viewModel::changePassword,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLevel3, contentColor = TextSecondary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = TextPrimary
                    )
                } else {
                    Text("Save", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun AdminPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = TextHint) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
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


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun AdminChangePasswordScreenPreview() {
    AdminChangePasswordScreen()
}
