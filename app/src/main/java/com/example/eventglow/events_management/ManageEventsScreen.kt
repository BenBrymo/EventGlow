package com.example.eventglow.events_management

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.R
import com.example.eventglow.dataClass.Event
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderSubtle
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.SurfaceLevel3
import com.example.eventglow.ui.theme.TextHint
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary
import com.example.eventglow.ui.theme.Warning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun ManageEventsScreen(
    events: List<EventItem> = sampleEvents,
    onBackClick: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onAddEvent: () -> Unit = {},
    onEventClick: (EventItem) -> Unit = {},
    navController: NavController
) {

    var search by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEvent,
                containerColor = BrandPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            ManageEventsHeader(onBackClick)

            Spacer(Modifier.height(16.dp))

            SearchBar(
                value = search,
                onValueChange = {
                    search = it
                    onSearch(it)
                }
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(events) { event ->
                    ManageEventCard(event) {
                        onEventClick(event)
                    }
                }
            }
        }
    }
}


@Composable
fun ManageEventsHeader(onBackClick: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
        }

        Text(
            "Manage Events",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
    }
}


@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text("Search events", color = TextHint)
        },
        leadingIcon = {
            Icon(Icons.Default.Search, null, tint = TextSecondary)
        },
        singleLine = true,
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceLevel3,
            unfocusedContainerColor = SurfaceLevel3,
            focusedBorderColor = BorderSubtle,
            unfocusedBorderColor = BorderSubtle,
            cursorColor = BrandPrimary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}


@Composable
fun ManageEventCard(
    event: EventItem,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceLevel3)
            .clickable(onClick = onClick)
    ) {

        Box {

            Image(
                painterResource(event.image),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            StatusBadge(event.status)
        }

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                event.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    event.date,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    event.location,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


@Composable
fun StatusBadge(status: String) {

    Box(
        modifier = Modifier
            .padding(12.dp)
            .background(Warning, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopEnd

    ) {
        Text(
            status.uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}


data class EventItem(
    val image: Int,
    val title: String,
    val date: String,
    val location: String,
    val status: String
)

val sampleEvents = listOf(
    EventItem(
        R.drawable.applogo,
        "bjj",
        "Wednesday Feb, 18",
        "dtuj",
        "Upcoming"
    )
)



@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsMgmtScreen(navController: NavController, viewModel: EventsManagementViewModel = viewModel()) {

    val scope = rememberCoroutineScope()

    //to show snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    //to toggle delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    //stores event to delete
    var eventToDelete by remember { mutableStateOf<Event?>(null) }

    val events by viewModel.events.collectAsState()

    val isEventDataFetched by viewModel.fetchEventsState.collectAsState()

    LaunchedEffect(events) {

        viewModel.fetchEvents()

        //when fetching events state is failure
        when (isEventDataFetched) {
            is FetchEventsState.Failure -> {
                scope.launch {
                    //show an error message
                    snackbarHostState.showSnackbar(
                        message = "Cannot retrieve events at this at moment please again later",
                        duration = SnackbarDuration.Short
                    )
                }
            }

            else -> {}
        }
    }


    // Scaffold to provide a basic material layout structure
    Scaffold(
        // top bar
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Manage Events", color = MaterialTheme.colorScheme.primary)
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
            EventsMgt(
                navController = navController,
                onDeleteEventClick = { event ->
                    eventToDelete = event
                    showDeleteDialog = true
                },
                events = events
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
                                    event,
                                    onFailure = { Toast.makeText(context, "", Toast.LENGTH_SHORT) }
                                )
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
fun EventsMgt(
    modifier: Modifier = Modifier,
    navController: NavController,
    onDeleteEventClick: (Event) -> Unit,
    events: List<Event>,
    viewModel: EventsManagementViewModel = viewModel()
) {

    val isEventDataFetched by viewModel.fetchEventsState.collectAsState()

    //check FetchEventsState
    when (isEventDataFetched) {

        //When is state is  loading
        is FetchEventsState.Loading -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
        }

        //when state is Successful
        is FetchEventsState.Success -> {
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                scope.launch {
                    delay(1000)
                }
            }
            Column(
                modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {

                // Search Bar
                SearchBar(navController = navController)

                Spacer(modifier = Modifier.height(16.dp))

                if (events.isEmpty()) {
                    Surface(modifier.fillMaxSize()) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No events available",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                } else {

                    // Add Event
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CreateEventButton(
                            navController = navController,
                            modifier = Modifier
                                .size(65.dp)
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(events) { event ->
                            EventItem(
                                event = event,
                                onEventClick = { navController.navigate("detailed_event_screen_admin/${event.id}") },
                                onDeleteClick = { onDeleteEventClick(event) },
                                onEditClick = { navController.navigate("edit_event_screen/${event.id}") },
                                onCopyClick = { navController.navigate("copy_event_screen/${event.id}") },
                                navController = navController
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        else -> Unit //Show Nothing
    }
}


@Composable
fun CreateEventButton(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    FloatingActionButton(
        onClick = {
            navController.navigate(Routes.CREATE_EVENT_SCREEN) {
                popUpTo(Routes.EVENTS_MANAGEMENT_SCREEN)
            }
        },
        modifier = Modifier,
        contentColor = Color.White,
        containerColor = MaterialTheme.colorScheme.scrim
    ) {
        Icon(Icons.Default.Add, contentDescription = "Create Event")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    placeholder: String = "Search events",
    navController: NavController
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(4.dp))
    ) {
        var isSearchBarFocussed by remember { mutableStateOf(false) }
        Column() {
            TextField(
                value = "",
                onValueChange = {},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                },
                placeholder = {
                    Text(placeholder, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                },
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !isSearchBarFocussed) {
                            Log.d("EventsManagementScreen", "Search Bar is focussed now navigating to search screen")
                            navController.navigate(Routes.ADMIN_SEARCH_SCREEN)
                        }
                        isSearchBarFocussed = focusState.isFocused
                    }
            )
        }
    }
}

@Composable
fun EventItem(
    event: Event,
    onEventClick: (Event) -> Unit,
    onDeleteClick: (Event) -> Unit,
    onEditClick: () -> Unit,
    onCopyClick: (Event) -> Unit,
    viewModel: EventsManagementViewModel = viewModel(),
    navController: NavController
) {

    // Event details directly below the image
    val formattedStartDate = viewModel.convertToFormattedDate(event.startDate)
    val formattedEndDate = viewModel.convertToFormattedDate(event.endDate)

    val dayOfWeekStart = formattedStartDate.first.first
    val monthStart = formattedStartDate.first.second
    val dayOfMonthStart = formattedStartDate.second

    val dayOfWeekEnd = formattedEndDate.first.first
    val monthEnd = formattedEndDate.first.second
    val dayOfMonthEnd = formattedEndDate.second

    val multiDayEvent = event.isMultiDayEvent

    //for all content
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEventClick(event) }, // Makes the whole card clickable
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )

    ) {
        //for main content
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            //Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Image at the top of the card
                Image(
                    painter = rememberAsyncImagePainter(event.imageUri!!.toUri()),
                    contentDescription = event.eventName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row() {
                    //Day and day column
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .size(55.dp)
                            .background(MaterialTheme.colorScheme.inverseSurface)
                    ) {
                        Text(
                            text = monthStart,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (multiDayEvent) "$dayOfMonthStart - $dayOfMonthEnd" else dayOfMonthStart,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(end = if (multiDayEvent) 3.dp else 0.dp),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Column() {
                        Text(
                            text = if (multiDayEvent) "$dayOfWeekStart - $dayOfWeekEnd " else dayOfWeekStart,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = event.eventVenue,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = event.eventStatus,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Column() {
                        Button(
                            onClick = { navController.navigate(Routes.TICKET_MANAGEMENT_SCREEN) },
                            modifier = Modifier.size(width = 100.dp, height = 35.dp)
                                .padding(start = 8.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Tickets")
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun CreateEventButtonPreview() {
    CreateEventButton(navController = rememberNavController())
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ManageEventsHeaderPreview() {
    ManageEventsHeader(onBackClick = {})
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ManageEventCardPreview() {
    ManageEventCard(
        event = EventItem(
            image = com.example.eventglow.R.drawable.applogo,
            title = "Jazz Night",
            date = "Tue, 20 Feb",
            location = "Accra",
            status = "Ongoing"
        ),
        onClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun StatusBadgePreview() {
    StatusBadge(status = "Upcoming")
}

