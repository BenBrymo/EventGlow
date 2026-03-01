package com.example.eventglow.events_management

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventglow.R
import com.example.eventglow.common.formatDisplayDate
import com.example.eventglow.dataClass.Event
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BorderSubtle
import com.example.eventglow.ui.theme.SurfaceLevel3
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredResultScreenAdmin(
    navController: NavController,
    viewModel: EventsManagementViewModel = viewModel()
    //serializedEvents: String // Receive serialized data as a parameter){}
) {

    //val json = Json { ignoreUnknownKeys = true } // Configure JSON to ignore unknown keys
    // Deserialize the events from the JSON string
    // val filteredEvents: List<Event> = Json.decodeFromString(serializedEvents)

    val filteredEventsAdvanced = viewModel.filteredEventsAdvanced.collectAsState().value

    // Log the filtered events
    Log.d("FilteredResultScreen", "Number of events from filtered result screen state: ${filteredEventsAdvanced.size}")

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(text = "Filtered Events", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.FILTER_SEARCH_SCREEN_ADMIN) }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Edit filters")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "${filteredEventsAdvanced.size} result${if (filteredEventsAdvanced.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            // Display a message if there are no filtered events
            if (filteredEventsAdvanced.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceLevel3,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No events found",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting your status, category, or date range filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // Display filtered events in a LazyColumn
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEventsAdvanced, key = { it.id }) { event ->
                        FilteredResultsEventRow(event = event, onClick = { selectedEvent ->
                            if (selectedEvent.id.isNotBlank()) {
                                navController.navigate("detailed_event_screen_admin/${selectedEvent.id}")
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun FilteredResultsEventRow(event: Event, onClick: (Event) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick(event) },
        color = SurfaceLevel3,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
        ) {
        // Event Image
        Card(
            modifier = Modifier
                .size(width = 120.dp, height = 92.dp)
                .clip(RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            AsyncImage(
                model = event.imageUri?.ifBlank { R.drawable.applogo },
                contentDescription = "Event Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Event Details Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = event.eventName.ifBlank { "Untitled Event" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.eventCategory.ifBlank { "Uncategorized" },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = BorderSubtle,
                    shape = RoundedCornerShape(50)
                )
            ) {
                Text(
                    text = event.eventStatus.ifBlank { "Status Unknown" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Start: ${formatDisplayDate(event.startDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "End: ${formatDisplayDate(event.endDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
    }
}
