package com.example.eventglow.events_management

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.dataClass.Event


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftedEventsScreen(navController: NavController, viewModel: EventsManagementViewModel = viewModel()) {

    var showDeleteDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }

    //Fetch drafted events on initial composition
    LaunchedEffect(Unit) {
        viewModel.fetchEvents()
    }

    val draftedEvents by viewModel.draftedEvents.collectAsState()

    // Scaffold to provide a basic material layout structure
    Scaffold(
        // top bar
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Drafted Events", color = MaterialTheme.colorScheme.primary)
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() } // navigates to Events Management Screen
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Surface to hold main contents with padding applied
        Surface(Modifier.padding(paddingValues)) {
            DraftedEventsMgt(
                navController = navController,
                onDeleteEventClick = { event ->
                    eventToDelete = event
                    showDeleteDialog = true
                },
                draftedEvents = draftedEvents,
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            eventToDelete?.let { event ->
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(text = "Delete Event") },
                    text = { Text("Are you sure you want to delete the event \"${event.eventName}\"? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteEvent(
                                    event, onFailure = {})
                                showDeleteDialog = false
                                eventToDelete = null
                            }
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                eventToDelete = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DraftedEventsMgt(
    modifier: Modifier = Modifier,
    navController: NavController,
    draftedEvents: List<Event>,
    onDeleteEventClick: (Event) -> Unit
) {

    Column(modifier.fillMaxSize()) {
        // Search Bar
        SearchBarDrafted(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            onQueryChange = {},
            query = ""
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (draftedEvents.isEmpty()) {
            Text(
                text = "No events available",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(draftedEvents) { draftedEvent ->
                    EventItemDrafted(
                        event = draftedEvent,
                        onEventClick = { /* Handle event click */ },
                        onDeleteClick = { onDeleteEventClick(draftedEvent) },
                        onEditClick = {
                            navController.navigate("edit_event_screen/${draftedEvent.id}")
                        },
                        onCopyClick = {
                            //viewModel.copyEvent(draftedEvent)
                        }
                    )
                }
            }
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarDrafted(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search events"
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            placeholder = {
                Text(placeholder, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        )
    }
}


@Composable
fun EventItemDrafted(
    event: Event,
    onEventClick: (Event) -> Unit,
    onDeleteClick: (Event) -> Unit,
    onEditClick: () -> Unit,
    onCopyClick: (Event) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEventClick(event) },
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
            ) {
                Image(
                    painter = rememberAsyncImagePainter(event.imageUri),
                    contentDescription = event.eventName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = event.eventName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = "${event.startDate} at ${event.eventTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.eventVenue,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = event.eventStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { onDeleteClick(event) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Event"
                    )
                }
                IconButton(onClick = { onEditClick() }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Event"
                    )
                }
                IconButton(onClick = { onCopyClick(event) }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Event"
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun DraftedEventsMgtPreview() {
    DraftedEventsMgt(
        navController = rememberNavController(),
        draftedEvents = listOf(
            Event(
                id = "e1",
                eventName = "Street Food Fest",
                startDate = "18/2/2026",
                endDate = "18/2/2026",
                eventTime = "6:00 PM",
                eventVenue = "Tema",
                eventStatus = "Draft",
                imageUri = ""
            )
        ),
        onDeleteEventClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EventItemDraftedPreview() {
    EventItemDrafted(
        event = Event(
            id = "e2",
            eventName = "Indie Music Night",
            startDate = "19/2/2026",
            endDate = "19/2/2026",
            eventTime = "8:30 PM",
            eventVenue = "Kumasi",
            eventStatus = "Draft",
            imageUri = ""
        ),
        onEventClick = {},
        onDeleteClick = {},
        onEditClick = {},
        onCopyClick = {}
    )
}

