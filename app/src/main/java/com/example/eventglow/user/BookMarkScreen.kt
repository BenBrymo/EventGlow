package com.example.eventglow.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
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

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun BookmarkScreenPreview() {
    BookmarksScreen(navController = rememberNavController())
}
