package com.example.eventglow.user


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.eventglow.R
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderStrong
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.CardBlack
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    name: String = "BenBrymo",
    email: String = "benbrymo5@gmail.com",
    onBack: () -> Unit = {},
    onDeletePhoto: () -> Unit = {},
    onSave: (String, String) -> Unit = { _, _ -> }
) {

    var fullName by rememberSaveable { mutableStateOf(name) }
    var userEmail by rememberSaveable { mutableStateOf(email) }

    Scaffold(
        containerColor = Background,
        topBar = {
            UpdateProfileTopBar(onBack = onBack)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(28.dp))

            ProfileAvatar(
                onDelete = onDeletePhoto
            )

            Spacer(Modifier.height(32.dp))

            UpdateProfileTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = "Full name"
            )

            Spacer(Modifier.height(16.dp))

            UpdateProfileTextField(
                value = userEmail,
                onValueChange = { userEmail = it },
                placeholder = "Email address"
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onSave(fullName, userEmail) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary
                )
            ) {
                Text(
                    text = "Save",
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateProfileTopBar(
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
                text = "Update Profile",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
    )
}

@Composable
private fun ProfileAvatar(
    onDelete: () -> Unit
) {

    Box(
        contentAlignment = Alignment.Center
    ) {

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
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape),
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
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = TextPrimary
            )
        }
    }
}


@Composable
private fun UpdateProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        placeholder = {
            Text(
                text = placeholder,
                color = TextSecondary
            )
        },
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
fun UpdateProfileScreenPreview() {
    UpdateProfileScreen()
}
