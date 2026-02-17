package com.example.eventglow.user

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.CardGray
import com.example.eventglow.ui.theme.SurfaceLevel2
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTicketsScreen(
    navController: NavController
) {

    Scaffold(
        containerColor = Background,
        topBar = {
            MyTicketsTopBar(onBack = {})
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            MyTicketItem(
                organizer = "fhikkm",
                title = "bjj",
                date = "18, Feb",
                price = "GHS 0.0"
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTicketsTopBar(
    onBack: () -> Unit
) {

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Background
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
                text = "My Tickets",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
    )
}

@Composable
private fun MyTicketItem(
    organizer: String,
    title: String,
    date: String,
    price: String
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardGray)
            .padding(16.dp)
    ) {

        Column {

            Text(
                text = "Organizer: $organizer",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceLevel2)
                ) {

                    AsyncImage(
                        model = "https://picsum.photos/200/200",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Text(
                    text = price,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTicketScreen2(
    navController: NavController,
    userViewModel: UserViewModel = viewModel()
) {
    val isLoading by userViewModel.isLoading.collectAsState()
    val tickets by userViewModel.boughtTickets.collectAsState()
    val isRefreshing by userViewModel.isRefreshing.collectAsState()

    // Pull to Refresh state
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    LaunchedEffect(Unit) {
        userViewModel.fetchBoughtTickets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Tickets",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .background(Brush.verticalGradient(listOf(Color(0xFFDEE4FD), Color(0xFFB9D0F9))))
                .fillMaxSize()
        ) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    userViewModel.fetchBoughtTickets()
                }
            ) {
                if (isLoading) {
                    ShimmerEffect()
                } else {
                    if (tickets.isEmpty()) {
                        EmptyStateView()
                    } else {
                        TicketList(tickets, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Event,
            contentDescription = "No Tickets",
            tint = Color(0xFF4A90E2),
            modifier = Modifier.size(120.dp) // Larger icon
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Tickets Purchased",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = Color(0xFF1E3A8A),
            fontSize = 22.sp
        )
        Text(
            text = "You haven't bought any tickets yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5A5A5A),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun TicketList(tickets: List<BoughtTicket>, navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        items(tickets) { ticket ->
            TicketItem(ticket = ticket, modifier = Modifier.padding(4.dp), navController = navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketItem(
    ticket: BoughtTicket,
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = viewModel(),
    navController: NavController

) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "",
    )

    val formattedStartDate = viewModel.convertToFormattedDate(ticket.startDate)
    val formattedEndDate = viewModel.convertToFormattedDate(ticket.endDate)

    val dayOfWeekStart = formattedStartDate.first.first
    val monthStart = formattedStartDate.first.second
    val dayOfMonthStart = formattedStartDate.second

    val dayOfWeekEnd = formattedEndDate.first.first
    val monthEnd = formattedEndDate.first.second
    val dayOfMonthEnd = formattedEndDate.second

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(interactionSource = interactionSource, indication = null) {
                Log.d("Navigation", "Navigating to ticketDetail with reference: ${ticket.transactionReference}")
                navController.navigate("detailed_ticket_screen/${ticket.transactionReference}")
            }
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Organizer's Name
            Text(
                text = "Organizer: ${ticket.eventOrganizer}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Row containing the event image and details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Event Image
                Image(
                    painter = rememberAsyncImagePainter(ticket.imageUrl),
                    contentDescription = "Event Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Ticket Details
                Column {
                    Text(
                        text = ticket.eventName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    if (ticket.startDate.isNotEmpty() && ticket.endDate.isNotEmpty()) {
                        Text(
                            text = "$dayOfMonthStart - $dayOfMonthEnd, $monthStart",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "$dayOfMonthStart, $monthStart",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price at the bottom right of the card
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GHS ${ticket.ticketPrice}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}


@Composable
fun ShimmerEffect() {
    val shimmerAlpha = remember { Animatable(0.3f) }

    LaunchedEffect(key1 = Unit) {
        shimmerAlpha.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(3) {
            ShimmerItem(shimmerAlpha.value)
        }
    }
}

@Composable
fun ShimmerItem(alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 8.dp)
            .alpha(alpha)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.LightGray.copy(alpha = 0.3f),
                        Color.LightGray.copy(alpha = 0.6f),
                        Color.LightGray.copy(alpha = 0.3f),
                        Color.LightGray.copy(alpha = 0.6f),
                        Color.LightGray.copy(alpha = 0.3f),
                        Color.LightGray.copy(alpha = 0.6f),
                        Color.LightGray.copy(alpha = 0.3f),
                        Color.LightGray.copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun PreviewTicketItem() {
    // Mock data for preview
    val mockTicket = BoughtTicket(
        eventName = "Music Concert 2024",
        eventOrganizer = "Top Events",
        startDate = "2024-09-10",
        endDate = "2024-09-11",
        ticketPrice = "50.00",
        imageUrl = "https://example.com/event_image.png"
    )
    TicketItem(ticket = mockTicket, navController = rememberNavController())
}
