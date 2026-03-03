package com.example.eventglow.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventglow.R
import com.example.eventglow.common.LoadState
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderStrong
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.CardBlack
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    onBack: () -> Unit = {},
    onDeletePhoto: () -> Unit = {},
    onSave: (String, String) -> Unit = { _, _ -> },
    viewModel: UpdateProfileViewModel = viewModel()
) {
    val fullName by viewModel.fullName.collectAsState()
    val userEmail by viewModel.email.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val loadState by viewModel.loadState.collectAsState()
    val isSaving = loadState == LoadState.LOADING

    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            onSave(fullName, userEmail)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = { AdminUpdateProfileTopBar(onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            AdminProfileAvatar(onDelete = onDeletePhoto)
            Spacer(Modifier.height(32.dp))
            AdminUpdateProfileTextField(
                value = fullName,
                onValueChange = viewModel::onFullNameChange,
                placeholder = "Full name"
            )
            Spacer(Modifier.height(16.dp))
            AdminUpdateProfileTextField(
                value = userEmail,
                onValueChange = viewModel::onEmailChange,
                placeholder = "Email address"
            )
            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!successMessage.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = successMessage.orEmpty(),
                    color = BrandPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = viewModel::saveProfile,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = TextPrimary
                    )
                } else {
                    Text("Save", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminUpdateProfileTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Background),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
            }
        },
        title = {
            Text("Update Profile", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }
    )
}

@Composable
private fun AdminProfileAvatar(onDelete: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.user_logo),
                contentDescription = null,
                modifier = Modifier.size(92.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(32.dp)
        ) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = TextPrimary)
        }
    }
}

@Composable
private fun AdminUpdateProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        placeholder = { Text(text = placeholder, color = TextSecondary) },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = CardBlack,
            focusedContainerColor = CardBlack,
            unfocusedBorderColor = BorderStrong,
            focusedBorderColor = BorderStrong,
            cursorColor = TextPrimary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun AdminUpdateProfileScreenPreview() {
    UpdateProfileScreen()
}
