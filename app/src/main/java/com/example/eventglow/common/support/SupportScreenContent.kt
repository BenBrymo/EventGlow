package com.example.eventglow.common.support

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
fun SupportScreenContent(
    title: String = "Support",
    onBackClick: () -> Unit,
    onCallClick: () -> Unit,
    onEmailClick: () -> Unit,
    onHelpCenterClick: () -> Unit,
    navigationIcon: @Composable () -> Unit
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    Box(modifier = Modifier.clickable(onClick = onBackClick)) {
                        navigationIcon()
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            SupportContactCard(
                icon = Icons.Default.Call,
                iconColor = Success,
                title = "Call Us",
                subtitle = "+233 123 456 789",
                onClick = onCallClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            SupportContactCard(
                icon = Icons.Default.Email,
                iconColor = AccentTeal,
                title = "Email Us",
                subtitle = "support@eventapp.com",
                onClick = onEmailClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            SupportHoursCard()
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onHelpCenterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = "Help Center",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SupportContactCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                SurfaceLevel3,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SupportHoursCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                SurfaceLevel3,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Support Hours",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Monday to Friday: 9:00 AM - 6:00 PM",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Saturday & Sunday: 10:00 AM - 4:00 PM",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
