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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.UserPreferences
import com.example.eventglow.events_management.EventsManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouriteScreen(
    viewModel: UserViewModel = viewModel(),
    userPreferences: UserPreferences = viewModel(),
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val favouriteEvents by viewModel.favoriteEvents.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchFavoriteEvents()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Favourite Events") },
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
            if (favouriteEvents.isEmpty()) {
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
                    items(favouriteEvents) { event ->
                        FavouriteEventItem(
                            event = event,
                            onRemoveClick = {
                                viewModel.deleteFavoriteEventFromFirestore(event)
                                userPreferences.removeFavoriteEvent(event)
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
fun FavouriteEventItem(
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
                            contentDescription = "Remove favourite",
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
fun FavouriteScreenPreview() {
    FavouriteScreen(navController = rememberNavController(), modifier = Modifier)
}
