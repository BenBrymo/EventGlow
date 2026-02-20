package com.example.eventglow.events_management

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
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


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ManageEventsScreen(
    navController: NavController,
    viewModel: EventsManagementViewModel = viewModel(),
    onBackClick: () -> Unit = { navController.popBackStack() },
    onAddEvent: () -> Unit = { navController.navigate(Routes.CREATE_EVENT_SCREEN) },
    onEventClick: (Event) -> Unit = { event ->
        if (event.id.isNotBlank()) {
            navController.navigate("detailed_event_screen_admin/${event.id}")
        }
    }
) {
    val events by viewModel.events.collectAsState()
    val fetchState by viewModel.fetchEventsState.collectAsState()
    val isRefreshing = fetchState is FetchEventsState.Loading
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val refreshFlagFlow = remember(currentBackStackEntry) {
        currentBackStackEntry?.savedStateHandle?.getStateFlow("events_updated", false)
    }
    val shouldRefreshFromResult by refreshFlagFlow?.collectAsState() ?: remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.fetchEvents() }
    )
    var search by remember { mutableStateOf("") }
    val filteredEvents = remember(events, search) {
        if (search.isBlank()) {
            events
        } else {
            events.filter { event ->
                event.eventName.contains(search, ignoreCase = true) ||
                        event.eventVenue.contains(search, ignoreCase = true) ||
                        event.eventCategory.contains(search, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(shouldRefreshFromResult) {
        if (shouldRefreshFromResult) {
            viewModel.fetchEvents()
            currentBackStackEntry?.savedStateHandle?.set("events_updated", false)
        }
    }

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
                }
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentColor = BrandPrimary
                )

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (val state = fetchState) {
                        is FetchEventsState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is FetchEventsState.Failure -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = state.errorMessage,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.fetchEvents() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }

                        is FetchEventsState.Success -> {
                            if (filteredEvents.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (search.isBlank()) "No events yet." else "No events match your search.",
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(filteredEvents, key = { it.id }) { event ->
                                        ManageEventCard(event) {
                                            onEventClick(event)
                                        }
                                    }
                                }
                            }
                        }
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
    event: Event,
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
                painter = if (!event.imageUri.isNullOrBlank()) {
                    rememberAsyncImagePainter(event.imageUri)
                } else {
                    painterResource(R.drawable.applogo)
                },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            StatusBadge(event.eventStatus)
        }

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                event.eventName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    event.startDate,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    event.eventVenue,
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


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ManageEventsHeaderPreview() {
    ManageEventsHeader(onBackClick = {})
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ManageEventCardPreview() {
    ManageEventCard(
        event = Event(
            id = "preview_event_1",
            eventName = "Jazz Night",
            startDate = "20/2/2026",
            eventVenue = "Accra",
            eventStatus = "Ongoing",
            imageUri = ""
        ),
        onClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun StatusBadgePreview() {
    StatusBadge(status = "Upcoming")
}

