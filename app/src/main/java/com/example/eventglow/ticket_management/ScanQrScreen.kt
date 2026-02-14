package com.example.eventglow.ticket_management


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.eventglow.ui.theme.AccentOrange
import com.example.eventglow.ui.theme.BackgroundBlack


@Composable
fun ScanQrScreen(
    onBackClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {

        // Camera Preview Placeholder
        CameraPreview(
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar
        ScanTopBar(
            onBackClick = onBackClick
        )

        // Scanner Frame
        ScannerOverlay()

        // Bottom Instruction
        Text(
            text = "Scan an Event Ticket QR code",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        )
    }
}


@Composable
fun ScanTopBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundBlack.copy(alpha = 0.85f))
            .padding(top = 16.dp, bottom = 16.dp, start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Scan QR Code",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
    }
}


@Composable
fun ScannerOverlay() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(260.dp)
                .border(
                    width = 3.dp,
                    color = AccentOrange,
                    shape = RoundedCornerShape(24.dp)
                )
        )
    }
}


@Composable
fun CameraPreview(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black)
    )
}
