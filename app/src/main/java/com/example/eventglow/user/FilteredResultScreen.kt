package com.example.eventglow.user

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredResultScreen(
    navController: NavController,
    userPreferences: UserPreferences = viewModel()
) {

    val filterResult = userPreferences.getFilteredEvents()
    // Log the filtered events
    Log.d("FilteredResultScreen", "Number of events from sharedPreferences: ${filterResult?.size}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Explore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            userPreferences.resetFilteredEventsToEmpty()
                            navController.popBackStack()
                        }
                    ) {
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
            if (filterResult != null) {
                if (filterResult.isEmpty()) {
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
                        items(filterResult) { event ->
                            FilteredResultsEventRow(
                                event = event,
                                onClick = { navController.navigate("detailed_event_screen/${event.id}") })
                        }
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

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun FilteredResultsEventRowPreview() {
    FilteredResultsEventRow(
        event = Event(
            id = "f1",
            eventName = "Community Meetup",
            startDate = "16/2/2026",
            endDate = "16/2/2026",
            eventCategory = "Tech",
            eventStatus = "Upcoming",
            imageUri = ""
        ),
        onClick = {}
    )
}
