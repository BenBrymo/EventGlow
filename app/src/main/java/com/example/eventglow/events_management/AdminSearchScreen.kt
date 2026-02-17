package com.example.eventglow.events_management

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventglow.dataClass.Event
import com.example.eventglow.navigation.Routes


@Composable
fun AdminSearchScreen(
    viewModel: EventsManagementViewModel = viewModel(),
    navController: NavController
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredEvents by viewModel.filteredEvents.collectAsState()

    AdminSearchContent(
        searchQuery = searchQuery,
        filteredEvents = filteredEvents,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onBackClick = { navController.popBackStack() },
        onFilterClick = { navController.navigate(Routes.FILTER_SEARCH_SCREEN_ADMIN) },
        onEventClick = { event -> navController.navigate("detailed_event_screen_admin/${event.id}") },
        formatDate = viewModel::convertToFormattedDate
    )
}

@Composable
fun AdminSearchContent(
    searchQuery: String,
    filteredEvents: List<Event>,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onFilterClick: () -> Unit,
    onEventClick: (Event) -> Unit,
    formatDate: (String) -> Pair<Pair<String, String>, String>
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TextField(
            value = searchQuery,
            placeholder = { Text(text = "Search Event name,venue...") },
            onValueChange = onSearchQueryChange,
            leadingIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Back")
                }
            },
            trailingIcon = {
                IconButton(onClick = onFilterClick) {
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter List")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(filteredEvents) { event ->
                EventRow(
                    event = event,
                    onClick = onEventClick,
                    formatDate = formatDate
                )
            }
        }
    }
}

@Composable
fun EventRow(
    event: Event,
    onClick: (Event) -> Unit,
    formatDate: (String) -> Pair<Pair<String, String>, String>
) {
    //container for event row
    Row(
        modifier = Modifier
            .clickable { onClick(event) }
            .padding(8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Event Image Card
        Card(
            modifier = Modifier
                .size(105.dp)
                .clip(RoundedCornerShape(8.dp)),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            //Card Content
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

            // Event details directly below the image
            val formattedStartDate = formatDate(event.startDate)
            val formattedEndDate = formatDate(event.endDate)

            val dayOfWeekStart = formattedStartDate.first.first
            val monthStart = formattedStartDate.first.second
            val dayOfMonthStart = formattedStartDate.second

            val dayOfWeekEnd = formattedEndDate.first.first
            val monthEnd = formattedEndDate.first.second
            val dayOfMonthEnd = formattedEndDate.second

            val multiDayEvent = event.isMultiDayEvent

            //Event Name
            Text(
                text = event.eventName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            //Event Category
            Text(
                text = event.eventCategory,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            //Event Status
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

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
    // Add other text styles as needed
)

@Composable
fun MyAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography,
        content = content
    )
}


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun ScreenPreview() {
    AdminSearchContent(
        searchQuery = "",
        filteredEvents = listOf(
            Event(
                id = "1",
                eventName = "Tech Conference",
                startDate = "20/2/2026",
                endDate = "20/2/2026",
                eventVenue = "Accra",
                eventStatus = "Upcoming",
                eventCategory = "Tech",
                imageUri = ""
            )
        ),
        onSearchQueryChange = {},
        onBackClick = {},
        onFilterClick = {},
        onEventClick = {},
        formatDate = { ("Wed" to "Feb") to "20" }
    )
}
