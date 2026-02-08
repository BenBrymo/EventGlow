package com.example.eventglow.events_management

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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