package com.example.eventglow.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.eventglow.common.formatDisplayDate
import com.example.eventglow.dataClass.Event
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
    navController: NavController,
    userViewModel: UserViewModel = viewModel()
) {

    val bookmarkedEvents by userViewModel.bookmarkEvents.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredBookmarks = remember(bookmarkedEvents, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            bookmarkedEvents
        } else {
            bookmarkedEvents.filter { event ->
                event.eventName.contains(query, ignoreCase = true) ||
                        event.eventCategory.contains(query, ignoreCase = true) ||
                        event.eventVenue.contains(query, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        userViewModel.fetchBookmarkEvents()
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BookmarksTopBar(onBack = { navController.popBackStack() })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(Modifier.height(12.dp))
            BookmarksSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
            Divider(
                color = Divider,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isLoading && bookmarkedEvents.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    filteredBookmarks.isEmpty() -> {
                        Text(
                            text = if (searchQuery.isBlank()) "No bookmarks yet." else "No bookmarked events match your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredBookmarks,
                                key = { it.id }
                            ) { event ->
                                BookmarkItem(
                                    event = event,
                                    onClick = {
                                        navController.navigate("detailed_event_screen/${event.id}")
                                    },
                                    onDelete = {
                                        userViewModel.deleteBookmarkEventFromFirestore(event)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksTopBar(
    onBack: () -> Unit
) {
    TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
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
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextHint
            )
        },
        placeholder = {
            Text(
                text = "Search bookmarks",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(24.dp)),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceLevel3,
            unfocusedContainerColor = SurfaceLevel3,
            focusedBorderColor = Divider,
            unfocusedBorderColor = Divider,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(24.dp)
    )
}


@Composable
private fun BookmarkItem(
    event: Event,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    Column {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardGray)
                .clickable { onClick() }
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
                        model = event.imageUri?.ifBlank { "https://picsum.photos/800/600" },
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
                            text = event.eventName.ifBlank { "Untitled Event" },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = event.startDate.ifBlank { "Date not set" }.let { formatDisplayDate(it) },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {

                        IconButton(
                            onClick = onDelete
                        ) {
                            Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = TextPrimary
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = event.eventVenue.ifBlank { "Venue not set" },
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

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun BookmarkScreenPreview() {
    BookmarksScreen(navController = rememberNavController())
}
