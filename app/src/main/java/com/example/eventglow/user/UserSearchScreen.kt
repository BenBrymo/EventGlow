package com.example.eventglow.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.eventglow.dataClass.Event
import com.example.eventglow.events_management.EventsManagementViewModel
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.Divider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    navController: NavController,
    viewModel: UserViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredEvents by viewModel.filteredEvents.collectAsState()
    val allEvents by viewModel.events.collectAsState()
    val categories by viewModel.eventCategories.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchEventCategories()
    }

    val topPicks = allEvents.take(8)
    val showSearchResults = searchQuery.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeroSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onOpenFilters = { navController.navigate(Routes.FILTER_SEARCH_SCREEN) }
                )
            }

            if (!showSearchResults) {

                item {
                    SectionTitle("Top picks")
                }

                item {
                    if (topPicks.isEmpty()) {
                        Text(
                            text = "No events available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(topPicks) { event ->
                                TopPickCard(
                                    event = event,
                                    onClick = { navController.navigate("detailed_event_screen/${event.id}") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle(if (showSearchResults) "Search results" else "All events")
            }

            if (showSearchResults && filteredEvents.isEmpty()) {
                item {
                    Text(
                        text = "No events match \"$searchQuery\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val rows = if (showSearchResults) filteredEvents else allEvents
                items(rows) { event ->
                    EventResultRow(
                        event = event,
                        onClick = { navController.navigate("detailed_event_screen/${event.id}") }
                    )
                    Spacer(Modifier.height(4.dp))
                    Divider(color = Divider)
                }
            }
        }
    }
}

@Composable
private fun HeroSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Text(
                text = "Find your next event",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                placeholder = { Text("Search by event name or venue") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpenFilters,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advanced filters")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun TopPickCard(
    event: Event,
    onClick: () -> Unit
) {
    val isFree = event.ticketTypes.any { it.price <= 0.0 || it.isFree }

    Card(
        modifier = Modifier
            .width(210.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = event.imageUri,
                    contentDescription = event.eventName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )

                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isFree) Color(0xFF1DB954).copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isFree) "Free" else "Paid",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFree) Color(0xFF1DB954) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = event.eventName.ifBlank { "Untitled event" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.eventVenue.ifBlank { "Venue not set" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}



@Composable
private fun EventResultRow(
    event: Event,
    onClick: () -> Unit,
    viewModel: EventsManagementViewModel = viewModel()
) {
    val formattedStartDate = viewModel.convertToFormattedDate(event.startDate)
    val dayOfWeekStart = formattedStartDate.first.first
    val monthStart = formattedStartDate.first.second
    val dayOfMonthStart = formattedStartDate.second
    val freeLabel = event.ticketTypes.any { it.price <= 0.0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = event.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.eventVenue.ifBlank { "Venue not set" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dayOfWeekStart, $dayOfMonthStart $monthStart",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (freeLabel) Color(0xFF1DB954).copy(alpha = 0.20f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (freeLabel) "Free" else "Paid",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (freeLabel) Color(0xFF1DB954) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun ScreenPreview() {
    UserSearchScreen(navController = rememberNavController())
}
