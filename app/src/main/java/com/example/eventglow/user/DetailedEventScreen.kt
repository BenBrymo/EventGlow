package com.example.eventglow.user

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.dataClass.UserPreferences
import com.example.eventglow.events_management.EventsManagementViewModel
import com.example.eventglow.navigation.Routes
import com.example.eventglow.payment.AuthorizationResult
import com.example.eventglow.payment.PayStackPaymentViewModel
import com.example.eventglow.payment.VerificationResult
import com.example.eventglow.ui.theme.Background
import com.example.eventglow.ui.theme.BrandPrimary
import com.example.eventglow.ui.theme.CardGray
import com.example.eventglow.ui.theme.SurfaceLevel2
import com.example.eventglow.ui.theme.TextPrimary
import com.example.eventglow.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedEventScreen(
    eventId: String,
    viewModel: EventsManagementViewModel = viewModel(),
    paymentViewModel: PayStackPaymentViewModel = viewModel(),
    navController: NavController,
    userViewModel: UserViewModel = viewModel()
) {
    val event = viewModel.getEventById(eventId)
    Log.d("DetailedEventScreen", "Event fetched: $event")

    val authorizationResult by paymentViewModel.authorizationResult.collectAsState()
    val verificationResult by paymentViewModel.verificationResult.collectAsState()

    var isLoading by remember { mutableStateOf(false) }

    Log.d("AuthorizationResult", "Authorization result: $authorizationResult")
    Log.d("VerificationResult", "Verification result: $verificationResult")

    // State to hold the chosen ticket type
    var selectedTicketType by remember { mutableStateOf<TicketType?>(null) }

    if (event == null) {
        Log.d("DetailedEventScreen", "Event is null, displaying loading indicator.")
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator() // Display loading indicator
            }
        }
        return
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // to show snackbar
    var showDoneButton by remember { mutableStateOf(false) }

    // Check if the user has already bought a ticket for this event
    val hasBoughtTicket = userViewModel.hasUserBoughtTicketForEvent(event.id)
    Log.d("DetailedEventScreen", "Has user bought ticket for this event? $hasBoughtTicket")

    if (isLoading) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator() // Display loading indicator
            }
        }
    }


    LaunchedEffect(authorizationResult) {
        scope.launch {
            when (authorizationResult) {
                is AuthorizationResult.Error -> {
                    Log.d(
                        "AuthorizationResult",
                        "Payment authorization error: ${(authorizationResult as AuthorizationResult.Error).message}"
                    )
                    Toast.makeText(
                        context,
                        (authorizationResult as AuthorizationResult.Error).message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                AuthorizationResult.Idle -> {
                    Log.d("AuthorizationResult", "Authorization result is idle.")
                }

                is AuthorizationResult.Success -> {
                    Log.d(
                        "AuthorizationResult",
                        "Payment authorized, opening URL: ${(authorizationResult as AuthorizationResult.Success).authorizationUrl}"
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data =
                            android.net.Uri.parse((authorizationResult as AuthorizationResult.Success).authorizationUrl)
                    }
                    context.startActivity(intent)
                    showDoneButton = true
                }
            }
        }
    }

    LaunchedEffect(verificationResult) {
        scope.launch {
            when (verificationResult) {
                is VerificationResult.Error -> {
                    isLoading = false
                    showDoneButton = false
                    Log.d(
                        "VerificationResult",
                        "Payment verification error: ${(verificationResult as VerificationResult.Error).message}"
                    )
                    Toast.makeText(
                        context,
                        (verificationResult as VerificationResult.Error).message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is VerificationResult.Idle -> {
                    Log.d("VerificationResult", "Verification result is idle.")
                }

                is VerificationResult.Success -> {
                    isLoading = false
                    Log.d("VerificationResult", "Payment verified successfully, redirecting to Tickets Screen.")

                    val ticketReference = paymentViewModel.transactionReference
                    selectedTicketType?.let { ticketType ->
                        val boughtTicket = BoughtTicket(
                            ticketReference,
                            event.eventOrganizer,
                            event.id,
                            event.eventName,
                            event.startDate,
                            event.eventStatus,
                            event.endDate,
                            event.imageUri,
                            ticketType.name, // Using selected ticket type
                            ticketType.price.toString()
                        )

                        userViewModel.processBoughtTicket(boughtTicket)
                        Log.d("DetailedEventScreen", "Processed bought ticket: $boughtTicket")

                        // Navigate to Tickets Screen with the selected ticket type
                        navController.navigate(Routes.TICKETS_SCREEN)
                    }
                }

                is VerificationResult.Loading -> {
                    isLoading = true
                    Log.d("DetailedEventScreen", "Payment verification is loading.")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Event Details", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d("DetailedEventScreen", "Navigating back from event details.")
                        // Reset authorization result so the URL is not opened again
                        paymentViewModel.resetAuthorizationResult()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            EventDetailsScreen()
//            DetailedEventContent(
//                event = event,
//                showDoneButton = showDoneButton,
//                hasBoughtTicket = hasBoughtTicket,
//                chosenTicketType = { ticketType -> selectedTicketType = ticketType }
//            )

        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Transparent overlay
                .clickable(enabled = false) {} // Prevent clicks while loading
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen() {

    var quantity by rememberSaveable { mutableIntStateOf(1) }

    Scaffold(
        containerColor = Background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            EventHeaderImage()

            EventActionsRow()

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {

                Text(
                    text = "bjj",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Wednesday Feb 18",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                EventInfoBlock(
                    title = "Time",
                    value = "5:00 PM"
                )

                EventInfoBlock(
                    title = "Duration",
                    value = "2 hr 30 min"
                )

                EventInfoBlock(
                    title = "Venue",
                    value = "dtuj"
                )

                EventInfoBlock(
                    title = "Description",
                    value = "fhkklll"
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Tickets",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                TicketTypeItem(
                    title = "Normal - GHS 0.0"
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Quantity",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                QuantitySelector(
                    value = quantity,
                    onDecrease = {
                        if (quantity > 1) quantity--
                    },
                    onIncrease = {
                        quantity++
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {

                    Text(
                        text = "Proceed to Payment",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EventHeaderImage() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLevel2)
    ) {

        AsyncImage(
            model = "https://picsum.photos/800/600",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EventActionsRow() {

    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Icon(
            imageVector = Icons.Filled.BookmarkBorder,
            contentDescription = null,
            tint = TextPrimary
        )

        Icon(
            imageVector = Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = TextPrimary
        )
    }
}

@Composable
private fun EventInfoBlock(
    title: String,
    value: String
) {

    Column(
        modifier = Modifier.padding(bottom = 18.dp)
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun TicketTypeItem(
    title: String
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardGray)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
    }
}

@Composable
private fun QuantitySelector(
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onDecrease) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = null,
                tint = TextPrimary
            )
        }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        IconButton(onClick = onIncrease) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = TextPrimary
            )
        }
    }
}



@Composable
fun DetailedEventContent(
    event: Event,
    paymentViewModel: PayStackPaymentViewModel = viewModel(),
    sharedPreferencesViewModel: SharedPreferencesViewModel = viewModel(),
    showDoneButton: Boolean,
    hasBoughtTicket: Boolean,
    chosenTicketType: (ticketType: TicketType) -> Unit,
    userViewModel: UserViewModel = viewModel(),
    eventsManagementViewModel: EventsManagementViewModel = viewModel(),
    userPreferences: UserPreferences = viewModel()
) {

    var isBookmarked by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    // Check if the event is already marked as favorite
    LaunchedEffect(Unit) {
        isFavorite = userViewModel.isEventFavorite(event)
    }

    val userData by sharedPreferencesViewModel.userInfo.collectAsState()
    val email = userData["USER_EMAIL"]

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTicketType by remember { mutableStateOf<TicketType?>(null) }

    val formattedStartDate = eventsManagementViewModel.convertToFormattedDate(event.startDate)
    val formattedEndDate = eventsManagementViewModel.convertToFormattedDate(event.endDate)

    val dayOfWeekStart = formattedStartDate.first.first
    val monthStart = formattedStartDate.first.second
    val dayOfMonthStart = formattedStartDate.second

    val dayOfWeekEnd = formattedEndDate.first.first
    val monthEnd = formattedEndDate.first.second
    val dayOfMonthEnd = formattedEndDate.second

    // Main content layout
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Banner Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(event.imageUri),
                    contentDescription = "Event Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        item {
            // Event Details
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = {
                            isBookmarked = !isBookmarked
                            if (isBookmarked) {
                                userViewModel.addBookmarkEventToFireStore(event)
                                userPreferences.addBookmark(event)
                            } else {
                                userViewModel.deleteBookmarkEventFromFirestore(event)
                                userPreferences.removeBookmark(event)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        isFavorite = !isFavorite
                        if (isFavorite) {
                            userViewModel.addFavoriteEventToFireStore(event)
                            userPreferences.addFavoriteEvent(event)
                        } else {
                            userViewModel.deleteFavoriteEventFromFirestore(event)
                            userPreferences.removeFavoriteEvent(event)
                        }
                    }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    text = event.eventName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Text(
                    text = "Event Date",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = if (event.isMultiDayEvent) "$dayOfWeekStart - $dayOfWeekEnd" else dayOfWeekStart,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                )

                Text(
                    text = if (event.isMultiDayEvent) "$dayOfMonthStart - $dayOfMonthEnd $monthStart" else "$dayOfMonthStart $monthStart",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                )
                Text(
                    text = "Venue",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = event.eventVenue,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground)
                )
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = event.eventDescription,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                )
            }
        }
        item {
            // Ticket Types Section
            Text(
                text = "Ticket Types",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(event.ticketTypes) { ticketType ->
                    TicketTypeCard(
                        ticketType = ticketType,
                        isSelected = selectedTicketType == ticketType,
                        onClick = {
                            selectedTicketType = if (selectedTicketType == ticketType) null else ticketType
                            chosenTicketType(ticketType)
                        }
                    )
                }
            }
        }
        item {
            // Buy Ticket Button Section
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectedTicketType != null) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (showDoneButton) {
                                    paymentViewModel.verifyTransaction()
                                } else {
                                    if (hasBoughtTicket) {
                                        snackbarHostState.showSnackbar(
                                            message = "You have already bought a ticket for this event.",
                                            duration = SnackbarDuration.Short
                                        )
                                    } else {
                                        paymentViewModel.initiatePayment(
                                            email = email!!,
                                            amount = selectedTicketType!!.price.toString()
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (showDoneButton) "Done" else "Buy Ticket",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }

    // Snackbar for showing messages
    LaunchedEffect(snackbarHostState) {
        snackbarHostState.currentSnackbarData?.dismiss()
    }
}

@Composable
fun TicketTypeCard(
    ticketType: TicketType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Log.d("TicketTypeCard", "Rendering ticket type card for: $ticketType")
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.2f
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = ticketType.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "GHS ${ticketType.price}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light)
            )
        }
    }
}


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EventDetailsScreenPreview() {
    EventDetailsScreen()
}
