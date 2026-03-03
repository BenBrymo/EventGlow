package com.example.eventglow.user

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventglow.common.formatDisplayDate
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Transaction
import com.example.eventglow.ticket_management.TicketManagementViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.graphics.Color as AndroidColor

private val DetailDanger = Color(0xFFFF5A5F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    transactionReference: String,
    navController: NavController,
    ticketManagementViewModel: TicketManagementViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val boughtTickets by userViewModel.boughtTickets.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val isSubmittingRefundRequest by userViewModel.isSubmittingRefundRequest.collectAsState()
    val refundRequestMessage by userViewModel.refundRequestMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var isTransactionLoading by remember { mutableStateOf(false) }
    var transactionError by remember { mutableStateOf<String?>(null) }

    val ticket = boughtTickets.find { it.transactionReference == transactionReference }
    val qrBitmap = remember(ticket?.qrCodeData) { generateQrBitmap(ticket?.qrCodeData.orEmpty(), 640) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = qrBitmap
        if (bitmap == null) {
            scope.launch { snackbarHostState.showSnackbar("QR code is not available.") }
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val bytes = bitmap.toPngBytes()
                stream.write(bytes)
                stream.flush()
            } ?: error("Could not open output stream.")
        }.onSuccess {
            scope.launch { snackbarHostState.showSnackbar("QR code downloaded successfully.") }
        }.onFailure { error ->
            scope.launch { snackbarHostState.showSnackbar("Download failed: ${error.message.orEmpty()}") }
        }
    }

    LaunchedEffect(transactionReference) {
        userViewModel.fetchBoughtTickets()
    }

    LaunchedEffect(ticket?.transactionReference) {
        if (ticket?.transactionReference.isNullOrBlank()) return@LaunchedEffect
        isTransactionLoading = true
        transactionError = null
        transaction = null
        runCatching {
            ticketManagementViewModel.getTransactionByReference(ticket!!.transactionReference.orEmpty())
        }.onSuccess {
            transaction = it
        }.onFailure {
            Log.e("TicketDetailScreen", "Failed to fetch transaction details", it)
            transactionError = it.message ?: "Transaction details unavailable."
        }
        isTransactionLoading = false
    }

    LaunchedEffect(refundRequestMessage) {
        val message = refundRequestMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        userViewModel.clearRefundRequestMessage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Ticket Details", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                transactionReference.isBlank() -> TicketErrorState(
                    message = "Invalid ticket reference.",
                    onBack = { navController.popBackStack() },
                    onRetry = {}
                )

                isLoading && ticket == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                ticket == null -> TicketErrorState(
                    message = "Ticket not found.",
                    onBack = { navController.popBackStack() },
                    onRetry = { userViewModel.fetchBoughtTickets() }
                )

                else -> TicketDetailContent(
                    ticket = ticket,
                    transaction = transaction,
                    isTransactionLoading = isTransactionLoading,
                    transactionError = transactionError,
                    qrBitmap = qrBitmap,
                    isSubmittingRefundRequest = isSubmittingRefundRequest,
                    onSubmitRefund = { reason ->
                        userViewModel.submitRefundRequest(ticket, reason)
                    },
                    onDownloadQr = {
                        createDocumentLauncher.launch("EventGlow_${ticket.transactionReference.orEmpty()}.png")
                    }
                )
            }
        }
    }
}

@Composable
private fun TicketDetailContent(
    ticket: BoughtTicket,
    transaction: Transaction?,
    isTransactionLoading: Boolean,
    transactionError: String?,
    qrBitmap: Bitmap?,
    isSubmittingRefundRequest: Boolean,
    onSubmitRefund: (String) -> Unit,
    onDownloadQr: () -> Unit
) {
    var refundReason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TicketHeaderCard(ticket = ticket)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ticket Details",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                DetailRow("Reference", ticket.transactionReference.orEmpty())
                DetailRow("Organizer", ticket.eventOrganizer.ifBlank { "N/A" })
                DetailRow("Ticket Type", ticket.ticketName.ifBlank { "General" })
                DetailRow("Amount", if (ticket.isFreeTicket) "FREE" else "GHS ${ticket.ticketPrice.ifBlank { "0.00" }}")
                DetailRow("Status", ticket.paymentStatus.ifBlank { if (ticket.isFreeTicket) "success" else "N/A" })
                DetailRow("Provider", ticket.paymentProvider.ifBlank { if (ticket.isFreeTicket) "free" else "N/A" })
                DetailRow("Channel", ticket.paymentChannel.ifBlank { "N/A" })
                DetailRow("Scanned", if (ticket.isScanned) "Yes" else "No")
                if (ticket.isScanned) {
                    DetailRow("Scanned At", ticket.scannedAt.ifBlank { "N/A" })
                    DetailRow(
                        "Scanned By",
                        ticket.scannedByAdminName.ifBlank { ticket.scannedByAdminId.ifBlank { "N/A" } })
                }
            }
        }

        if (!ticket.isFreeTicket && ticket.paymentStatus.equals("success", ignoreCase = true)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Need a refund?",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter refund reason (optional)") },
                        minLines = 2,
                        maxLines = 4
                    )
                    Button(
                        onClick = { onSubmitRefund(refundReason) },
                        enabled = !isSubmittingRefundRequest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSubmittingRefundRequest) "Submitting..." else "Request Refund")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Payment Timeline",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                if (isTransactionLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    DetailRow(
                        "Created At",
                        formatDisplayDateTime(
                            transaction?.createdAt?.ifBlank { ticket.paymentCreatedAt }
                                ?: ticket.paymentCreatedAt.ifBlank { "N/A" }
                        )
                    )
                    DetailRow(
                        "Paid At",
                        formatDisplayDateTime(
                            transaction?.paidAt?.ifBlank { ticket.paymentPaidAt }
                                ?: ticket.paymentPaidAt.ifBlank { "N/A" }
                        )
                    )
                    if (!transactionError.isNullOrBlank()) {
                        Text(
                            transactionError,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ticket QR Code",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Ticket QR Code",
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                    Button(onClick = onDownloadQr) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download QR")
                    }
                } else {
                    Text(
                        "QR code data is unavailable.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketHeaderCard(ticket: BoughtTicket) {
    val statusColor =
        if (ticket.paymentStatus.equals("success", true) || ticket.isFreeTicket) {
            MaterialTheme.colorScheme.primary
        } else {
            DetailDanger
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                AsyncImage(
                    model = ticket.imageUrl,
                    contentDescription = ticket.eventName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.36f))
                )
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (ticket.isFreeTicket) "FREE" else ticket.paymentStatus.uppercase()
                            .ifBlank { "PENDING" },
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = ticket.eventName.ifBlank { "Event Ticket" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatDisplayDate(ticket.startDate.ifBlank { "N/A" })}  •  ${formatDisplayDate(ticket.endDate.ifBlank { "•" })}",
                        color = Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketErrorState(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onBack) { Text("Back") }
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            value.ifBlank { "N/A" },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    if (content.isBlank()) return null
    return runCatching {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    }.getOrNull()
}

private fun Bitmap.toPngBytes(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

private fun formatDisplayDateTime(raw: String): String {
    val value = raw.trim()
    if (value.isBlank() || value.equals("N/A", ignoreCase = true)) return raw

    val output = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.ENGLISH)
    val parsers = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
    )

    for (parser in parsers) {
        try {
            val parsed = parser.parse(value)
            if (parsed != null) return output.format(parsed)
        } catch (_: Exception) {
            // Try next pattern.
        }
    }
    return raw
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun TicketHeaderCardPreview() {
    TicketHeaderCard(
        ticket = BoughtTicket(
            transactionReference = "REF123",
            paymentStatus = "success",
            eventName = "Afro Sunset Party",
            eventOrganizer = "EventGlow",
            startDate = "16/02/2026",
            endDate = "16/02/2026",
            ticketName = "VIP",
            ticketPrice = "75.00",
            qrCodeData = "eventglow_ticket|REF123|event_1|user_1"
        )
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun TicketErrorStatePreview() {
    TicketErrorState(message = "Ticket not found.", onBack = {}, onRetry = {})
}
