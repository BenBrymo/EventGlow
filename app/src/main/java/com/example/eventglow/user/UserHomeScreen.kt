package com.example.eventglow.user

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.eventglow.common.formatDisplayDate
import com.example.eventglow.dataClass.Event
import com.example.eventglow.navigation.Routes
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun userHomeScreen(
    navController: NavController,
    userViewModel: UserViewModel = viewModel()
) {
    val allEvents by userViewModel.events.collectAsState()
    val featuredEvents by userViewModel.featuredEvents.collectAsState()
    val categories by userViewModel.eventCategories.collectAsState()

    LaunchedEffect(Unit) {
        userViewModel.fetchFeaturedEvents()
        userViewModel.fetchEventCategories()
    }

    val sections = buildSections(allEvents)
    val visibleFeaturedEvents = featuredEvents.ifEmpty {
        sections.liveEvents.ifEmpty { allEvents.take(8) }
    }

    UserHomeContent(
        featuredEvents = visibleFeaturedEvents,
        categories = categories.map { it.name },
        liveEvents = sections.liveEvents,
        todayEvents = sections.todayEvents,
        upcomingEvents = sections.upcomingEvents,
        onSearchAction = { query ->
            userViewModel.onSearchQueryChange(query.trim())
            navController.navigate(Routes.USER_SEARCH_SCREEN)
        },
        onCategoryClick = { category ->
            navController.navigate("user_events_section/category:${Uri.encode(category)}")
        },
        onEventClick = { event -> navController.navigate("detailed_event_screen/${event.id}") },
        onSeeAll = { sectionType -> navController.navigate("user_events_section/$sectionType") }
    )
}

@Composable
private fun UserHomeContent(
    featuredEvents: List<Event>,
    categories: List<String>,
    liveEvents: List<Event>,
    todayEvents: List<Event>,
    upcomingEvents: List<Event>,
    onSearchAction: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onEventClick: (Event) -> Unit,
    onSeeAll: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SearchBar(onSearchAction = onSearchAction)
        }

        item {
            FeaturedAutoSlideRail(
                events = featuredEvents,
                onEventClick = onEventClick
            )
        }

        item {
            SectionHeader(title = "Browse by category", onSeeAll = null)
        }

        item {
            if (categories.isEmpty()) {
                Text(
                    text = "No categories yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { category ->
                        CategoryRailCard(
                            name = category,
                            onClick = { onCategoryClick(category) }
                        )
                    }
                }
            }
        }

        item {
            EventsRail(
                title = "Live Events",
                events = liveEvents.ifEmpty { featuredEvents },
                onEventClick = onEventClick,
                onSeeAll = { onSeeAll("live") }
            )
        }

        item {
            EventsRail(
                title = "Today Events",
                events = todayEvents,
                onEventClick = onEventClick,
                onSeeAll = { onSeeAll("today") }
            )
        }

        item {
            EventsRail(
                title = "Upcoming Events",
                events = upcomingEvents,
                onEventClick = onEventClick,
                onSeeAll = { onSeeAll("upcoming") }
            )
        }
    }
}

@Composable
private fun FeaturedAutoSlideRail(
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Featured Events", onSeeAll = null)
        if (events.isEmpty()) {
            Text(
                text = "No featured events yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            val listState = rememberLazyListState()
            AutoSlideFeaturedEvents(listState = listState, eventCount = events.size)
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 72.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events) { event ->
                    FeaturedEventCard(
                        event = event,
                        onClick = { onEventClick(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoSlideFeaturedEvents(
    listState: LazyListState,
    eventCount: Int
) {
    LaunchedEffect(eventCount) {
        if (eventCount <= 1) return@LaunchedEffect
        while (true) {
            delay(3600)
            val next = (listState.firstVisibleItemIndex + 1) % eventCount
            listState.animateScrollToItem(next)
        }
    }
}

@Composable
private fun FeaturedEventCard(
    event: Event,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(190.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = event.imageUri,
                contentDescription = event.eventName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.36f))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "FEATURED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = event.eventName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = event.startDate.ifBlank { "Date not set" }.let { formatDisplayDate(it) },
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SearchBar(onSearchAction: (String) -> Unit) {
    var searchValue by remember { mutableStateOf("") }
    var didNavigate by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = searchValue,
        onValueChange = { searchValue = it },
        placeholder = { Text("Search events") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        shape = RoundedCornerShape(50),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                if (!didNavigate) {
                    didNavigate = true
                    onSearchAction(searchValue)
                }
            }
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !didNavigate) {
                    didNavigate = true
                    onSearchAction(searchValue)
                }
                if (!focusState.isFocused) {
                    didNavigate = false
                }
            },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
private fun CategoryRailCard(
    name: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(150.dp)
            .height(80.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun EventsRail(
    title: String,
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = title, onSeeAll = onSeeAll)
        if (events.isEmpty()) {
            Text(
                text = "No events yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 72.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events) { event ->
                    NetflixEventCard(event = event, onClick = { onEventClick(event) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (onSeeAll != null) {
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSeeAll)
            )
        }
    }
}

@Composable
private fun NetflixEventCard(
    event: Event,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(230.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = event.imageUri,
                contentDescription = event.eventName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = event.eventName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    text = event.startDate.ifBlank { "Date not set" }.let { formatDisplayDate(it) },
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun UserEventsSectionScreen(
    navController: NavController,
    sectionType: String,
    userViewModel: UserViewModel = viewModel()
) {
    val allEvents by userViewModel.events.collectAsState()
    val sections = buildSections(allEvents)

    val categoryPrefix = "category:"
    val selectedCategory = if (sectionType.startsWith(categoryPrefix)) {
        Uri.decode(sectionType.removePrefix(categoryPrefix))
    } else {
        ""
    }

    val sectionTitle = when {
        selectedCategory.isNotBlank() -> "$selectedCategory Events"
        sectionType == "live" -> "Live Events"
        sectionType == "today" -> "Today Events"
        sectionType == "upcoming" -> "Upcoming Events"
        else -> "Events"
    }
    val sectionEvents = when {
        selectedCategory.isNotBlank() -> allEvents.filter {
            it.eventCategory.equals(selectedCategory, ignoreCase = true)
        }

        sectionType == "live" -> sections.liveEvents
        sectionType == "today" -> sections.todayEvents
        sectionType == "upcoming" -> sections.upcomingEvents
        else -> allEvents
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(title = sectionTitle, onSeeAll = null)
        }

        if (sectionEvents.isEmpty()) {
            item {
                Text(
                    text = "No events available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(sectionEvents) { event ->
                NetflixEventCard(
                    event = event,
                    onClick = { navController.navigate("detailed_event_screen/${event.id}") }
                )
            }
        }
    }
}

private data class UserHomeSections(
    val liveEvents: List<Event>,
    val todayEvents: List<Event>,
    val upcomingEvents: List<Event>
)

private fun buildSections(events: List<Event>): UserHomeSections {
    val now = Calendar.getInstance().time
    val todayDate = normalizeDate(now)

    val withDates = events.mapNotNull { event ->
        val startDate = parseDate(event.startDate) ?: return@mapNotNull null
        val endDate = parseDate(event.endDate).takeIf { event.isMultiDayEvent } ?: startDate
        val startAt = combineDateAndTime(
            date = startDate,
            time = event.eventTime,
            fallbackHour = 0,
            fallbackMinute = 0
        )
        val endAt = if (event.durationMinutes > 0) {
            Date(startAt.time + (event.durationMinutes * 60_000L))
        } else {
            combineDateAndTime(
                date = endDate,
                time = event.eventTime,
                fallbackHour = 23,
                fallbackMinute = 59
            )
        }
        LiveWindowEvent(
            event = event,
            startDate = normalizeDate(startDate),
            endDate = normalizeDate(endDate),
            startAt = startAt,
            endAt = endAt
        )
    }

    val live = withDates
        .filter { candidate ->
            !now.before(candidate.startAt) && !now.after(candidate.endAt)
        }
        .map { it.event }

    val today = withDates
        .filter { candidate -> candidate.startDate == todayDate }
        .map { it.event }

    val upcoming = withDates
        .filter { candidate -> candidate.startAt.after(now) }
        .map { it.event }

    return UserHomeSections(
        liveEvents = live,
        todayEvents = today,
        upcomingEvents = upcoming
    )
}

private fun parseDate(value: String): Date? {
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(value)
    } catch (_: Exception) {
        null
    }
}

private fun normalizeDate(date: Date): Date {
    return Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}

private data class LiveWindowEvent(
    val event: Event,
    val startDate: Date,
    val endDate: Date,
    val startAt: Date,
    val endAt: Date
)

private fun combineDateAndTime(
    date: Date,
    time: String,
    fallbackHour: Int,
    fallbackMinute: Int
): Date {
    val calendar = Calendar.getInstance().apply {
        this.time = date
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val parsedTime = parseTime(time)
    val hour = parsedTime?.first ?: fallbackHour
    val minute = parsedTime?.second ?: fallbackMinute
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    return calendar.time
}

private fun parseTime(value: String): Pair<Int, Int>? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null

    val patterns = listOf(
        "h:mm a",
        "hh:mm a",
        "H:mm",
        "HH:mm"
    )
    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            val parsed = sdf.parse(trimmed) ?: continue
            val calendar = Calendar.getInstance().apply { time = parsed }
            return calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
        } catch (_: Exception) {
            // Continue trying other patterns.
        }
    }
    return null
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun HomeSectionHeaderPreview() {
    SectionHeader(title = "Live Events", onSeeAll = {})
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun NetflixCardPreview() {
    NetflixEventCard(
        event = Event(
            id = "ev_1",
            eventName = "Open Air Concert",
            startDate = "16/02/2026",
            endDate = "16/02/2026",
            imageUri = ""
        ),
        onClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun UserHomeContentPreview() {
    UserHomeContent(
        featuredEvents = listOf(
            Event(id = "1", eventName = "Live Jam", startDate = "16/02/2026", imageUri = "")
        ),
        categories = listOf("Music", "Sports", "Tech"),
        liveEvents = listOf(
            Event(id = "2", eventName = "City Live", startDate = "16/02/2026", imageUri = "")
        ),
        todayEvents = listOf(
            Event(id = "3", eventName = "Today Meetup", startDate = "16/02/2026", imageUri = "")
        ),
        upcomingEvents = listOf(
            Event(id = "4", eventName = "Future Expo", startDate = "20/02/2026", imageUri = "")
        ),
        onSearchAction = {},
        onCategoryClick = {},
        onEventClick = {},
        onSeeAll = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun UserHomeEmptyPreview() {
    UserHomeContent(
        featuredEvents = emptyList(),
        categories = emptyList(),
        liveEvents = emptyList(),
        todayEvents = emptyList(),
        upcomingEvents = emptyList(),
        onSearchAction = {},
        onCategoryClick = {},
        onEventClick = {},
        onSeeAll = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
private fun UserEventsSectionScreenPreview() {
    UserEventsSectionScreen(
        navController = rememberNavController(),
        sectionType = "live"
    )
}
