package com.example.eventglow.user


import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.dataClass.Event
import com.example.eventglow.events_management.EventsManagementViewModel
import com.example.eventglow.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun userHomeScreen(
    navController: NavController,
    viewModel: EventsManagementViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {

    val scope = rememberCoroutineScope()
    val events by viewModel.events.collectAsState()
    val featuredEvents by userViewModel.featuredEvents.collectAsState()


    LaunchedEffect(Unit) {
        scope.launch {
            userViewModel.fetchFeaturedEvents()
        }
        Log.d("UserHomeScreen", "Launched effect is executed, Featured events fetched: ${featuredEvents.size}")
    }
    val categories = listOf("Music", "Sports", "Tech", "Arts", "Health")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {

            Spacer(modifier = Modifier.height(16.dp))

            searchBar(navController)

            Spacer(modifier = Modifier.height(24.dp))

            // Featured Event
            Text(
                text = "Featured Event",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeaturedEventPager(
                events = featuredEvents,
                onEventClick = { event ->
                    navController.navigate("detailed_event_screen/${event.id}")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {

            filterRow()

            Spacer(modifier = Modifier.height(32.dp))
        }

        categories.forEach { category ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                val categoryEvents = events.filter { it.eventCategory == category }

                if (categoryEvents.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        categoryEvents.forEach { event ->
                            EventCard(
                                event,
                                onEventClicked = { navController.navigate("detailed_event_screen/${event.id}") }
                            )
                        }
                    }
                } else {
                    Text(text = "No events yet", color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}


@Composable
private fun filterRow() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        eventFilterButton("Upcoming")
        eventFilterButton("Today")
    }
}


@Composable
private fun eventFilterButton(text: String) {

    Button(
        onClick = {},
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.width(140.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeaturedEventPager(
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    val scope = rememberCoroutineScope()

    var timer by remember { mutableStateOf(3) }
    var startTimer by remember { mutableStateOf(false) }
    val count = events.size
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { count })

    LaunchedEffect(Unit) {
        scope.launch {
            startTimer = true
            if (timer > 0) {
                delay(1000)
                timer--
            }
            pagerState.animateScrollToPage(
                pagerState.currentPage + 1
            )
            timer = 3
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        HorizontalPager(
            state = pagerState,
        ) { index ->
            val event = events[index]
            FeaturedEventCard(
                eventImageUrl = event.imageUri!!,
                eventTitle = event.eventName,
                onClick = { onEventClick(event) }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            PagerIndicator(
                pagerState = pagerState,
                count = count,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerIndicator(
    pagerState: PagerState,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            val isSelected = pagerState.currentPage == index
            IndicatorDot(isSelected)
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun IndicatorDot(isSelected: Boolean) {
    val color = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, shape = RoundedCornerShape(50))
    )
}


@Composable
fun FeaturedEventCard(
    eventImageUrl: String,
    eventTitle: String,
    onClick: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(targetValue = scale)

    LaunchedEffect(Unit) {
        scale = 1.1f
        delay(300) // Delay to let animation run
        scale = 1f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = rememberAsyncImagePainter(eventImageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            Text(
                text = eventTitle,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}


@Composable
fun EventCard(event: Event, onEventClicked: (Event) -> Unit) {
    Card(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(120.dp, 160.dp)
            .clickable { onEventClicked(event) },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)

    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = rememberAsyncImagePainter(event.imageUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            Text(
                text = event.eventName,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}


@Composable
private fun searchBar(navController: NavController) {

    var isSearchBarFocussed by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = "",
        onValueChange = {

        },
        placeholder = {
            Text(
                "Search events",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        shape = RoundedCornerShape(50),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !isSearchBarFocussed) {
                    navController.navigate(Routes.USER_SEARCH_SCREEN)
                }

                isSearchBarFocussed = focusState.isFocused
            },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun FeaturedEventCardPreview() {
    FeaturedEventCard(
        eventImageUrl = "",
        eventTitle = "Featured Tech Expo",
        onClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EventCardPreview() {
    EventCard(
        event = Event(
            id = "ev_1",
            eventName = "Music Night",
            startDate = "16/2/2026",
            endDate = "16/2/2026",
            eventCategory = "Music",
            imageUri = ""
        ),
        onEventClicked = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun IndicatorDotPreview() {
    IndicatorDot(isSelected = true)
}
