package com.example.eventglow.events_management

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.ui.theme.AppBlack
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.CardGray
import com.example.eventglow.ui.theme.Success
import com.example.eventglow.ui.theme.SurfaceLevel2
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsAdminScreen(
    eventId: String,
    title: String = "bjj",
    dateLabel: String = "Wednesday",
    dateValue: String = "18 Feb",
    time: String = "5:00 PM",
    duration: String = "2 hr 30 min",
    venue: String = "dtuj",
    description: String = "fhkkllll",
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onCopy: () -> Unit = {},
    onDeleteConfirmed: () -> Unit = {},
    onManageTickets: () -> Unit = {},
    navController: NavController
) {

    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Event Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            onClick = {
                                menuExpanded = false
                                onCopy()
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
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onManageTickets,
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
                        color = TextPrimary
                    )
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(12.dp))

            // image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceLevel2)
            )

            Spacer(Modifier.height(16.dp))

            FreeEventChip()

            Spacer(Modifier.height(12.dp))

            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    dateValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(20.dp))

            InfoBlock("Time", time)
            InfoBlock("Duration", duration)
            InfoBlock("Venue", venue)
            InfoBlock("Description", description)

            Spacer(Modifier.height(20.dp))

            Text(
                "Tickets",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            TicketChip()

            Spacer(Modifier.height(90.dp))
        }
    }

    if (showDeleteDialog) {
        DeleteEventDialog(
            eventName = title,
            onCancel = { showDeleteDialog = false },
            onDelete = {
                showDeleteDialog = false
                onDeleteConfirmed()
            }
        )
    }
}


@Composable
private fun FreeEventChip() {
    Box(
        modifier = Modifier
            .background(
                color = Success,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            "FREE EVENT",
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
    }
}


@Composable
private fun InfoBlock(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}


@Composable
private fun TicketChip() {
    Row(
        modifier = Modifier
            .background(
                color = CardGray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = Success,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                "FREE",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary
            )
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedEventScreenAdmin(
    eventId: String,
    viewModel: EventsManagementViewModel = viewModel(),
    navController: NavController,
) {

    val event = viewModel.getEventById(eventId)
    Log.d("DetailedEventScreen", "Event fetched: $event")


    // State to hold the chosen ticket type
    var selectedTicketType by remember { mutableStateOf<TicketType?>(null) }

    if (event == null) {
        Log.d("DetailedEventScreen", "Event is null, displaying loading indicator.")
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator() // Display loading indicator
            }
        }
        return
    }






    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Event Details", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            DetailedEventContent(
                event = event,
                chosenTicketType = { ticketType -> selectedTicketType = ticketType }
            )
        }
    }
}

@Composable
fun DetailedEventContent(
    event: Event,
    chosenTicketType: (ticketType: TicketType) -> Unit
) {


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Banner Section
            Image(
                painter = rememberAsyncImagePainter(event.imageUri),
                contentDescription = "Event Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Log.d("AdminDetailedEventScreen", "Event banner image loaded: ${event.imageUri}")
        }
        item {
            // Event Name
            Text(
                text = event.eventName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Log.d("AdminDetailedEventScreen", "Event name displayed: ${event.eventName}")
        }
        item {
            // Event Date and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (event.isMultiDayEvent) "${event.startDate} - ${event.endDate}" else event.startDate,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Time: ${event.eventTime}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.5.sp
                    )
                )
            }
            Log.d(
                "AdminDetailedEventScreen",
                "Event date and time displayed: ${if (event.isMultiDayEvent) "${event.startDate} - ${event.endDate}" else event.startDate}, Time: ${event.eventTime}"
            )
        }
        item {
            // Event Venue and Description
            Column {
                // Venue
                Text(
                    text = "Venue",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = event.eventVenue,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Description
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = event.eventDescription,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.5.sp
                    )
                )
            }
            Log.d(
                "AdminDetailedEventScreen",
                "Event venue and description displayed: ${event.eventVenue}, ${event.eventDescription}"
            )
        }
        item {
            // Ticket Types
            Text(
                text = "Ticket Types",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(event.ticketTypes) { ticketType ->
                    TicketTypeCard(
                        ticketType = ticketType,
                        onClick = {
                            Log.d("AdminDetailedEventScreen", "Ticket type selected: ${ticketType.name}")
                            chosenTicketType(ticketType)

                        }
                    )
                }
            }
        }
        item {
            // Manage Ticket
            Button(
                onClick = {

                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Manage Ticket")
            }
        }
    }
}

@Composable
fun TicketTypeCard(
    ticketType: TicketType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = ticketType.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Price: GHS ${ticketType.price}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}