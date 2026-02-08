package com.example.eventglow.events_management

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventglow.dataClass.Event


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
        topBar = {
            TopAppBar(
                title = { Text(text = "Explore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Display a message if there are no filtered events
            if (filteredEventsAdvanced.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(text = "No events found", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                // Display filtered events in a LazyColumn
                LazyColumn(
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(filteredEventsAdvanced) { event ->
                        FilteredResultsEventRow(event = event, onClick = { selectedEvent ->
                            // Handle event row click if needed
                            // For example, navigate to event details screen
                            //navController.navigate("eventDetails/${selectedEvent.id}")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun FilteredResultsEventRow(event: Event, onClick: (Event) -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onClick(event) }
            .padding(8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Event Image
        Card(
            modifier = Modifier
                .size(105.dp)
                .clip(RoundedCornerShape(8.dp)),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            AsyncImage(
                model = event.imageUri,
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
                text = event.eventName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.eventCategory,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = event.eventStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Start: ${event.startDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "End: ${event.endDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
