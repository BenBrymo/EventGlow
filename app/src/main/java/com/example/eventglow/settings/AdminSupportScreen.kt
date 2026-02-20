package com.example.eventglow.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.eventglow.ui.theme.AccentTeal
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.Success
import com.example.eventglow.ui.theme.SurfaceLevel3
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSupportScreen(
    onBackClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onEmailClick: () -> Unit = {},
    onHelpCenterClick: () -> Unit = {}
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Support", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            AdminSupportContactCard(
                icon = Icons.Default.Call,
                iconColor = Success,
                title = "Call Us",
                subtitle = "+233 123 456 789",
                onClick = onCallClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            AdminSupportContactCard(
                icon = Icons.Default.Email,
                iconColor = AccentTeal,
                title = "Email Us",
                subtitle = "support@eventapp.com",
                onClick = onEmailClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            AdminSupportHoursCard()
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onHelpCenterClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = TextPrimary)
            ) {
                Text("Help Center", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun AdminSupportContactCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLevel3, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(iconColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun AdminSupportHoursCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLevel3, shape = RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Support Hours", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Monday to Friday: 9:00 AM - 6:00 PM", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Saturday & Sunday: 10:00 AM - 4:00 PM",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun AdminSupportScreenPreview() {
    AdminSupportScreen()
}

