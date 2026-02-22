package com.example.eventglow.events_management

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderSubtle
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.SurfaceLevel3
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary


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
        containerColor = Background,
        // top bar
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Drafted Events", color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() } // navigates to Events Management Screen
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        // Surface to hold main contents with padding applied
        Surface(
            modifier = Modifier.padding(paddingValues),
            color = Background
        ) {
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

    var searchQuery by remember { mutableStateOf("") }
    val filteredEvents = remember(draftedEvents, searchQuery) {
        if (searchQuery.isBlank()) draftedEvents
        else draftedEvents.filter {
            it.eventName.contains(searchQuery, ignoreCase = true) ||
                    it.eventVenue.contains(searchQuery, ignoreCase = true) ||
                    it.eventCategory.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Search Bar
        SearchBarDrafted(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            onQueryChange = { searchQuery = it },
            query = searchQuery
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredEvents.isEmpty()) {
            Text(
                text = if (searchQuery.isBlank()) "No drafted events available." else "No drafted events match your search.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 28.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredEvents) { draftedEvent ->
                    EventItemDrafted(
                        event = draftedEvent,
                        onEventClick = { /* Handle event click */ },
                        onDeleteClick = { onDeleteEventClick(draftedEvent) },
                        onEditClick = {
                            navController.navigate("edit_event_screen/${draftedEvent.id}")
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
        color = SurfaceLevel3,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp))
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary
                )
            },
            colors = TextFieldDefaults.textFieldColors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                containerColor = SurfaceLevel3,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = BrandPrimary
            ),
            placeholder = {
                Text(placeholder, color = TextSecondary.copy(alpha = 0.7f))
            },
            shape = RoundedCornerShape(18.dp),
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
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEventClick(event) },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceLevel3
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(190.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
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
                        .padding(14.dp)
                ) {
                    Text(
                        text = event.eventName,
                        style = MaterialTheme.typography.titleMedium,
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
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = event.eventVenue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = event.eventCategory.ifBlank { "Uncategorized" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = event.eventStatus.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandPrimary
                    )
                }
            }

            Divider(color = BorderSubtle)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onEditClick() }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Event",
                        tint = BrandPrimary
                    )
                }
                IconButton(onClick = { onDeleteClick(event) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Event",
                        tint = MaterialTheme.colorScheme.error
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
        onEditClick = {}
    )
}

