package com.example.eventglow.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.UserPreferences
import com.example.eventglow.events_management.EventsManagementViewModel
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.CardGray
import com.example.eventglow.ui.theme.Divider
import com.example.eventglow.ui.theme.SurfaceLevel2
import com.example.eventglow.ui.theme.SurfaceLevel3
import com.example.eventglow.ui.theme.TextHint
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    navController: NavController
) {

    Scaffold(
        containerColor = Background,
        topBar = {
            BookmarksTopBar(onBack = {})
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Spacer(Modifier.height(12.dp))

            BookmarksSearchBar(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            Divider(
                color = Divider,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                BookmarkItem(
                    title = "bjj",
                    date = "Wednesday Feb, 18",
                    venue = "dtuj"
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksTopBar(
    onBack: () -> Unit
) {

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Background,
            titleContentColor = TextPrimary
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = TextPrimary
                )
            }
        },
        title = {
            Text(
                text = "Bookmarks",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
    )
}

@Composable
private fun BookmarksSearchBar(
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceLevel3)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextHint
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Search bookmarks",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint
            )
        }
    }
}


@Composable
private fun BookmarkItem(
    title: String,
    date: String,
    venue: String
) {

    Column {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardGray)
        ) {

            Column {

                // image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(SurfaceLevel2)
                ) {

                    AsyncImage(
                        model = "https://picsum.photos/800/600",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // content row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = TextPrimary
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = venue,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Divider(color = Divider)
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen2(
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = viewModel(),
    userPreferences: UserPreferences = viewModel(),
    navController: NavController,
) {
    val bookmarkedEvents by viewModel.bookmarkEvents.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchBookmarkEvents()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->

        if (isLoading) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            }
        } else {
            if (bookmarkedEvents.isEmpty()) {
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
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {

                    items(bookmarkedEvents) { event ->
                        BookmarkEventItem(
                            event = event,
                            onRemoveClick = {
                                viewModel.deleteBookmarkEventFromFirestore(event)
                                userPreferences.removeBookmark(event)
                            },
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkEventItem(
    event: Event,
    onRemoveClick: () -> Unit,
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
            .clickable { navController.navigate("detailed_event_screen/${event.id}") }, // Makes the whole card clickable
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

                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .background(Color.Red, shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove bookmark",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun BookmarkScreenPreview() {
    BookmarkScreen2(navController = rememberNavController(), modifier = Modifier)
}
