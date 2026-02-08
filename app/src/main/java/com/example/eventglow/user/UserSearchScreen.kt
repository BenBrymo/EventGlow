package com.example.eventglow.user


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun UserSearchScreen(
    navController: NavController,
    viewModel: UserViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredEvents by viewModel.filteredEvents.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TextField(
            value = searchQuery,
            placeholder = { Text(text = "Search Event name,venue...") },
            onValueChange = { input ->
                viewModel.onSearchQueryChange(input)
            },
            leadingIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Back")
                }
            },
            trailingIcon = {
                IconButton(onClick = { navController.navigate(Routes.FILTER_SEARCH_SCREEN) }) {
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter List")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        // container to keep filterd results
        LazyColumn {
            items(filteredEvents) { event ->
                EventRow(event = event, onClick = { navController.navigate("detailed_event_screen/${event.id}") })
            }
        }
    }
}

@Composable
fun EventRow(event: Event, onClick: (Event) -> Unit, viewModel: EventsManagementViewModel = viewModel()) {
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

            val formattedStartDate = viewModel.convertToFormattedDate(event.startDate)
            val formattedEndDate = viewModel.convertToFormattedDate(event.endDate)

            val dayOfWeekStart = formattedStartDate.first.first
            val monthStart = formattedStartDate.first.second
            val dayOfMonthStart = formattedStartDate.second

            val dayOfWeekEnd = formattedEndDate.first.first
            val monthEnd = formattedEndDate.first.second
            val dayOfMonthEnd = formattedEndDate.second

            val multiDayEvent = event.isMultiDayEvent

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
            if (multiDayEvent) {
                //Event Start Date
                Text(
                    text = "Start: $dayOfWeekStart, $dayOfMonthStart $monthStart",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                //Event End Date
                Text(
                    text = "End: $dayOfWeekEnd, $dayOfMonthEnd $monthEnd",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                Text(
                    text = "Start: $dayOfWeekStart, $dayOfMonthStart $monthStart",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}


@Preview
@Composable
fun ScreenPreview() {
    UserSearchScreen(navController = rememberNavController())
}