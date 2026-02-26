package com.example.eventglow.ticket_management

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.BackgroundBlack
import com.example.eventglow.ui.theme.BorderStrong
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.CardBlack
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketManagementScreen(
    navController: NavController,
    viewModel: TicketManagementViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEvent by remember { mutableStateOf<ManagedTicketEvent?>(null) }
    var editEvent by remember { mutableStateOf<ManagedTicketEvent?>(null) }
    var attendeesEvent by remember { mutableStateOf<ManagedTicketEvent?>(null) }
    var showFeatureDrawer by remember { mutableStateOf(false) }
    var showSalesSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val allEvents by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val attendees by viewModel.attendees.collectAsState()
    val recentScanActivity by viewModel.recentScanActivity.collectAsState()

    val filteredEvents = allEvents.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.date.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    LaunchedEffect(attendeesEvent?.id) {
        val eventId = attendeesEvent?.id ?: return@LaunchedEffect
        viewModel.loadAttendeesForEvent(eventId)
    }

    Scaffold(
        containerColor = BackgroundBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundBlack),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                title = {
                    Text(
                        text = "Ticket Management",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundBlack)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            item {
                StatsRow(events = allEvents)
            }

            item {
                VerifyCtaCard(
                    onVerify = { navController.navigate(Routes.SCAN_QR_SCREEN) }
                )
            }

            item {
                Text(
                    text = "Manage Event Tickets",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by event name or date", color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine = true
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandPrimary)
                    }
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredEvents) { event ->
                            SpotifyEventTicketCard(
                                event = event,
                                onEditTickets = { selectedEvent = event },
                                onVerifyTickets = { navController.navigate(Routes.SCAN_QR_SCREEN) }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recent Verification Activity",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (recentScanActivity.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBlack),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No scans yet. Verified tickets will appear here.",
                            color = TextSecondary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(recentScanActivity) { activity ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBlack),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = BrandPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Verified: ${activity.eventName}",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Ref ${activity.ticketReference} - ${activity.scannedAt}",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (activity.scannedByAdminName.isNotBlank()) {
                                    Text(
                                        text = "By ${activity.scannedByAdminName}",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (selectedEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedEvent = null },
            containerColor = CardBlack,
            dragHandle = null
        ) {
            EventTicketManagementDrawer(
                event = selectedEvent!!,
                onDismiss = { selectedEvent = null },
                onEditTickets = {
                    editEvent = selectedEvent
                    selectedEvent = null
                },
                onManageAttendees = {
                    attendeesEvent = selectedEvent
                    selectedEvent = null
                },
                onSalesReport = {
                    showSalesSheet = true
                    selectedEvent = null
                },
                onScan = {
                    selectedEvent = null
                    navController.navigate(Routes.SCAN_QR_SCREEN)
                }
            )
        }
    }

    if (editEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { editEvent = null },
            containerColor = CardBlack,
            dragHandle = null
        ) {
            EditTicketTypesSheet(
                event = editEvent!!,
                onDismiss = { editEvent = null },
                onSave = { newTypes ->
                    viewModel.updateEventTicketTypes(editEvent!!.id, newTypes) { success ->
                        if (success) {
                            scope.launch { snackbarHostState.showSnackbar("Ticket types updated.") }
                            editEvent = null
                        }
                    }
                }
            )
        }
    }

    if (attendeesEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { attendeesEvent = null },
            containerColor = CardBlack,
            dragHandle = null
        ) {
            AttendeesSheet(
                eventTitle = attendeesEvent!!.title,
                attendees = attendees,
                onDismiss = { attendeesEvent = null }
            )
        }
    }

}

@Composable
private fun StatsRow(events: List<ManagedTicketEvent>) {
    val totalSold = events.sumOf { it.soldCount }
    val totalAvailable = events.sumOf { it.availableCount }
    val activeEvents = events.size
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MiniStatCard("Sold", totalSold.toString(), Modifier.weight(1f))
        MiniStatCard("Available", totalAvailable.toString(), Modifier.weight(1f))
        MiniStatCard("Events", activeEvents.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBlack)
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 10.dp)) {
            Text(text = value, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun VerifyCtaCard(onVerify: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1DB954).copy(alpha = 0.28f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Verify Event Tickets Fast",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Scan and validate tickets instantly at entry points.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onVerify,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Verify Event Ticket")
                }
            }
        }
    }
}


@Composable
private fun SpotifyEventTicketCard(
    event: ManagedTicketEvent,
    onEditTickets: () -> Unit,
    onVerifyTickets: () -> Unit
) {
    Card(
        modifier = Modifier.width(250.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlack),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(10.dp))
            Text(
                event.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(event.date, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sold ${event.soldCount} - Available ${event.availableCount}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onEditTickets,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier.weight(1f)
                        .fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun EventTicketManagementDrawer(
    event: ManagedTicketEvent,
    onDismiss: () -> Unit,
    onEditTickets: () -> Unit,
    onManageAttendees: () -> Unit,
    onSalesReport: () -> Unit,
    onScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = event.title, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Edit tickets and manage event operations.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        DrawerAction("Edit Ticket Types", Icons.Filled.Edit) { onEditTickets() }
        DrawerAction("Adjust Pricing", Icons.Filled.Sell) { onEditTickets() }
        DrawerAction("Manage Attendees", Icons.Filled.Groups) { onManageAttendees() }
        DrawerAction("Ticket Sales Report", Icons.Filled.Analytics) { onSalesReport() }
        DrawerAction("Verify Ticket at Gate", Icons.Filled.QrCodeScanner) { onScan() }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTicketTypesSheet(
    event: ManagedTicketEvent,
    onDismiss: () -> Unit,
    onSave: (List<TicketType>) -> Unit
) {
    val editable = remember(event.id) {
        mutableStateListOf<EditableTicketType>().apply {
            addAll(event.ticketTypes.map {
                EditableTicketType(
                    name = it.name,
                    price = it.price.toString(),
                    available = it.availableTickets.toString(),
                    isFree = it.isFree
                )
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Edit Ticket Types", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        Text(event.title, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

        editable.forEachIndexed { index, item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { editable[index] = item.copy(name = it) },
                        label = { Text("Ticket Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = item.price,
                        onValueChange = { editable[index] = item.copy(price = it) },
                        label = { Text("Price") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = item.available,
                        onValueChange = { editable[index] = item.copy(available = it) },
                        label = { Text("Available Tickets") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Free", color = TextSecondary, modifier = Modifier.weight(1f))
                        Switch(
                            checked = item.isFree,
                            onCheckedChange = { editable[index] = item.copy(isFree = it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = BrandPrimary)
                        )
                    }
                    Button(
                        onClick = { if (editable.size > 1) editable.removeAt(index) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove")
                    }
                }
            }
        }

        Button(
            onClick = { editable.add(EditableTicketType()) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
        ) { Text("Add Ticket Type") }

        Button(
            onClick = {
                val parsed = editable.mapNotNull {
                    val price = it.price.toDoubleOrNull()
                    val available = it.available.toIntOrNull()
                    if (it.name.trim()
                            .isBlank() || price == null || available == null || price < 0.0 || available < 0
                    ) {
                        null
                    } else {
                        TicketType(
                            name = it.name.trim(),
                            price = price,
                            availableTickets = available,
                            isFree = it.isFree
                        )
                    }
                }
                if (parsed.size == editable.size) onSave(parsed)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Ticket Types") }

        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AttendeesSheet(
    eventTitle: String,
    attendees: List<ManagedTicketAttendee>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Attendees", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        Text(eventTitle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

        if (attendees.isEmpty()) {
            Text("No attendees yet.", color = TextSecondary)
        } else {
            attendees.forEach { attendee ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(attendee.userName.ifBlank { attendee.userEmail }, color = TextPrimary)
                        Text(
                            "Ticket: ${attendee.ticketName} | Ref: ${attendee.ticketReference}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            if (attendee.isScanned) "Scanned at ${attendee.scannedAt}" else "Not scanned",
                            color = if (attendee.isScanned) Color(0xFF1DB954) else TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        Spacer(Modifier.height(12.dp))
    }
}



@Composable
private fun DrawerAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundBlack)
            .border(width = 1.dp, color = BorderStrong, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = BrandPrimary)
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
    }
}

private data class EditableTicketType(
    val name: String = "",
    val price: String = "0.0",
    val available: String = "0",
    val isFree: Boolean = false
)

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun TicketManagementScreenPreview() {
    TicketManagementScreen(navController = rememberNavController())
}
