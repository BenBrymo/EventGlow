package com.example.eventglow.ticket_management


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventglow.ui.theme.AccentOrange
import com.example.eventglow.ui.theme.BackgroundBlack
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch


@Composable
fun ScanQrScreen(
    viewModel: TicketManagementViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("Ready to scan") }
    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .enableAutoZoom()
            .build()
    }
    val scanner = remember { GmsBarcodeScanning.getClient(context, scannerOptions) }

    Scaffold(
        containerColor = BackgroundBlack,
        topBar = {
            ScanTopBar(onBackClick = onBackClick)
        },
        bottomBar = {
            ScanBottomPanel(
                statusText = statusText,
                onStartScan = {
                    statusText = "Opening scanner..."
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            val rawValue = barcode.rawValue.orEmpty()
                            if (rawValue.isBlank()) {
                                statusText = "No QR value found."
                                return@addOnSuccessListener
                            }
                            scope.launch {
                                when (val result = viewModel.markTicketAsScanned(rawValue)) {
                                    is TicketScanResult.Success -> {
                                        val eventLabel =
                                            if (result.eventName.isBlank()) "" else " (${result.eventName})"
                                        statusText = "Valid ticket${eventLabel}. Marked as scanned."
                                    }

                                    is TicketScanResult.NotFound -> {
                                        statusText = "Ticket not found for reference ${result.reference}."
                                    }

                                    is TicketScanResult.AlreadyScanned -> {
                                        val by = result.scannedBy.ifBlank { "another admin" }
                                        statusText = "Already scanned by $by at ${result.scannedAt}."
                                    }

                                    is TicketScanResult.Error -> {
                                        statusText = result.message
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            statusText = if (exception is MlKitException &&
                                exception.errorCode == CommonStatusCodes.CANCELED
                            ) {
                                "Scan canceled."
                            } else {
                                exception.message ?: "Scan failed."
                            }
                        }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBlack)
                .padding(innerPadding)
        ) {
            CameraPreview(modifier = Modifier.fillMaxSize())
            ScannerOverlay()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Scan QR Code",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = BackgroundBlack.copy(alpha = 0.95f)
        ),
        modifier = Modifier.statusBarsPadding()
    )
}


@Composable
fun ScannerOverlay() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val size = if (maxHeight < 640.dp) 210.dp else 260.dp
        Box(
            modifier = Modifier
                .size(size)
                .border(
                    width = 3.dp,
                    color = AccentOrange,
                    shape = RoundedCornerShape(24.dp)
                )
        )
    }
}

@Composable
private fun ScanBottomPanel(
    statusText: String,
    onStartScan: () -> Unit
) {
    Surface(
        color = BackgroundBlack.copy(alpha = 0.92f),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan an Event Ticket QR code",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onStartScan) {
                    Text("Start Scan")
                }
            }
        }
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

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ScanQrScreenPreview() {
    ScanQrScreen(onBackClick = {})
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ScanTopBarPreview() {
    ScanTopBar(onBackClick = {})
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ScannerOverlayPreview() {
    ScannerOverlay()
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun CameraPreviewPreview() {
    CameraPreview(modifier = Modifier.size(240.dp))
}
