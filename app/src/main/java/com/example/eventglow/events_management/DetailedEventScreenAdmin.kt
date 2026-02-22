package com.example.eventglow.events_management

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.AppBlack
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderDefault
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.CardGray
import com.example.eventglow.ui.theme.Success
import com.example.eventglow.ui.theme.SurfaceLevel2
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsAdminScreen(
    eventId: String,
    navController: NavController,
    viewModel: EventsManagementViewModel = viewModel(),
) {
    val fetchState by viewModel.fetchEventsState.collectAsState()
    val event = viewModel.getEventById(eventId)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        if (eventId.isBlank() || event != null) return@LaunchedEffect
        viewModel.fetchEvents()
    }

    if (eventId.isBlank()) {
        EventDetailsStateScreen(
            title = "Invalid Event",
            message = "This event link is missing a valid event id.",
            onPrimaryAction = { navController.popBackStack() },
            primaryActionLabel = "Go Back"
        )
        return
    }

    if (event == null) {
        when (val state = fetchState) {
            is FetchEventsState.Loading -> EventDetailsStateScreen(
                title = "Loading Event",
                message = "Please wait while we load event details.",
                showLoading = true
            )

            is FetchEventsState.Failure -> EventDetailsStateScreen(
                title = "Could Not Load Event",
                message = state.errorMessage,
                onPrimaryAction = { viewModel.fetchEvents() },
                primaryActionLabel = "Retry",
                onSecondaryAction = { navController.popBackStack() },
                secondaryActionLabel = "Go Back"
            )

            is FetchEventsState.Success -> EventDetailsStateScreen(
                title = "Event Not Found",
                message = "This event may have been deleted or is no longer available.",
                onPrimaryAction = { navController.popBackStack() },
                primaryActionLabel = "Go Back"
            )
        }
        return
    }

    val isFreeEvent = event.ticketTypes.isNotEmpty() && event.ticketTypes.all { it.isFree || it.price <= 0.0 }
    val dateValue = if (event.isMultiDayEvent) "${event.startDate} - ${event.endDate}" else event.startDate

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Event Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(CardGray)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate("edit_event_screen/${event.id}")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate("copy_event_screen/${event.id}")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Routes.TICKET_MANAGEMENT_SCREEN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary
                    )
                ) {
                    Text(
                        "Manage Tickets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
            }

            item {
                Image(
                    painter = rememberAsyncImagePainter(event.imageUri),
                    contentDescription = "Event banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(event.eventStatus.uppercase())
                    if (isFreeEvent) {
                        StatusChip("FREE EVENT", color = Success)
                    }
                }
            }

            item {
                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailValueBlock("Date", dateValue)
                    DetailValueBlock("Time", event.eventTime)
                }
            }

            item {
                DetailValueBlock("Duration", event.durationLabel.ifBlank { "-" })
            }
            item { DetailValueBlock("Venue", event.eventVenue) }
            item { DetailValueBlock("Category", event.eventCategory) }
            item { DetailValueBlock("Organizer", event.eventOrganizer) }
            item { DetailValueBlock("Description", event.eventDescription) }

            item {
                Text(
                    "Tickets",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            item {
                if (event.ticketTypes.isEmpty()) {
                    Text(
                        text = "No ticket types available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(event.ticketTypes) { ticket ->
                            EventTicketSummaryCard(ticketType = ticket)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(90.dp))
            }
        }
    }

    if (showDeleteDialog) {
        DeleteEventDialog(
            eventName = event.eventName,
            onCancel = { showDeleteDialog = false },
            onDelete = {
                showDeleteDialog = false
                viewModel.deleteEvent(
                    event = event,
                    onFailure = { errorMessage ->
                        scope.launch { snackbarHostState.showSnackbar(errorMessage) }
                    }
                )
                navController.previousBackStackEntry?.savedStateHandle?.set("events_updated", true)
                navController.popBackStack()
            }
        )
    }
}


@Composable
private fun StatusChip(text: String, color: androidx.compose.ui.graphics.Color = BrandPrimary) {
    Box(
        modifier = Modifier
            .background(
                color = color,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}


@Composable
private fun DetailValueBlock(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}


@Composable
private fun EventTicketSummaryCard(ticketType: TicketType) {
    val isFreeTicket = ticketType.isFree || ticketType.price <= 0.0
    val priceLabel = if (isFreeTicket) "FREE" else "GHS ${"%.2f".format(ticketType.price)}"

    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLevel2)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(ticketType.name.ifBlank { "Ticket" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(
            "Available: ${ticketType.availableTickets}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        StatusChip(
            text = priceLabel,
            color = if (isFreeTicket) Success else BrandPrimary
        )
    }
}

@Composable
private fun EventDetailsStateScreen(
    title: String,
    message: String,
    showLoading: Boolean = false,
    onPrimaryAction: (() -> Unit)? = null,
    primaryActionLabel: String = "",
    onSecondaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String = ""
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            if (onPrimaryAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = onPrimaryAction) { Text(primaryActionLabel) }
            }
            if (onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSecondaryAction,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDefault)
                ) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}


@Composable
private fun DeleteEventDialog(
    eventName: String,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = CardGray,
        title = {
            Text(
                "Delete Event",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        },
        text = {
            Text(
                "Are you sure you want to delete the event \"$eventName\"? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(
                    "Delete",
                    color = BrandPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(
                    "Cancel",
                    color = TextSecondary
                )
            }
        }
    )
}



@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EventDetailsAdminScreenPreview() {
    EventDetailsAdminScreen(
        eventId = "ev_1",
        navController = rememberNavController()
    )
}
