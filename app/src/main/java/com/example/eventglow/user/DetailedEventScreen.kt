package com.example.eventglow.user

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.common.formatDisplayDate
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.dataClass.Transaction
import com.example.eventglow.dataClass.UserPreferences
import com.example.eventglow.events_management.EventsManagementViewModel
import com.example.eventglow.events_management.FetchEventsState
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToInt

private val KikuuCard = Color(0xFFFFE9D2)
private val KikuuCardDeep = Color(0xFFFFD9B0)
private val KikuuAccent = Color(0xFFE67E22)
private val KikuuText = Color(0xFF4A2E15)
private val KikuuMuted = Color(0xFF7B5432)

private enum class PurchaseFlowState {
    IDLE,
    AUTHORIZING,
    AWAITING_VERIFICATION,
    VERIFYING,
    COMMITTING,
    FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedEventScreen(
    eventId: String,
    viewModel: EventsManagementViewModel = viewModel(),
    paymentViewModel: PayStackPaymentViewModel = viewModel(),
    navController: NavController,
    userViewModel: UserViewModel = viewModel()
) {
    val events by viewModel.events.collectAsState()
    val draftedEvents by viewModel.draftedEvents.collectAsState()
    val fetchState by viewModel.fetchEventsState.collectAsState()
    val event = remember(eventId, events, draftedEvents) {
        (events + draftedEvents).find { it.id == eventId }
    }
    Log.d("DetailedEventScreen", "Event fetched: $event")

    val authorizationResult by paymentViewModel.authorizationResult.collectAsState()
    val verificationResult by paymentViewModel.verificationResult.collectAsState()
    val paymentErrorMessage by paymentViewModel.errorMessage.collectAsState()
    val verifiedTransaction by paymentViewModel.latestVerifiedTransaction.collectAsState()
    val boughtTickets by userViewModel.boughtTickets.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var isCommittingPurchase by remember { mutableStateOf(false) }
    var purchaseFlowState by remember { mutableStateOf(PurchaseFlowState.IDLE) }
    var pendingTransactionReference by rememberSaveable { mutableStateOf("") }

    Log.d("AuthorizationResult", "Authorization result: $authorizationResult")
    Log.d("VerificationResult", "Verification result: $verificationResult")

    // State to hold the chosen ticket type
    var selectedTicketType by remember { mutableStateOf<TicketType?>(null) }

    LaunchedEffect(eventId) {
        if (eventId.isNotBlank()) {
            viewModel.fetchEvents()
        }
    }

    if (event == null) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (fetchState) {
                    is FetchEventsState.Loading -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading event...", color = KikuuText)
                    }

                    is FetchEventsState.Failure -> {
                        Text("Failed to load event.", color = KikuuText, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            (fetchState as FetchEventsState.Failure).errorMessage,
                            color = KikuuMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.fetchEvents() }) { Text("Retry") }
                    }

                    is FetchEventsState.Success -> {
                        Text("Event not found.", color = KikuuText, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("Back") }
                    }
                }
            }
        }
        return
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun navigateToPurchaseResult(status: String, reference: String? = null, message: String? = null) {
        val route = "${RoutesUser.PURCHASE_RESULT}/$status?reference=${Uri.encode(reference.orEmpty())}&message=${
            Uri.encode(message.orEmpty())
        }"
        navController.navigate(route)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Check if the user has already bought a ticket for this event
    val hasBoughtTicket = userViewModel.hasUserBoughtTicketForEvent(event.id)
    val ownedTicketReference = remember(boughtTickets, event.id) {
        boughtTickets.firstOrNull { it.eventId == event.id }?.transactionReference
    }
    val eventStatusNormalized = event.eventStatus.trim().lowercase()
    val isEventOngoing = eventStatusNormalized == "ongoing"
    val isEventEnded = eventStatusNormalized == "ended"
    val isPurchaseBlocked = isEventOngoing || isEventEnded
    val purchaseBlockedMessage = when {
        isEventEnded -> "Ticket purchase is disabled because this event has ended."
        isEventOngoing -> "Ticket purchase is disabled while event is ongoing."
        else -> ""
    }
    Log.d("DetailedEventScreen", "Has user bought ticket for this event? $hasBoughtTicket")

    if (isLoading) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(0.45f),
            ) {
                CircularProgressIndicator() // Display loading indicator
            }
        }
    }


    LaunchedEffect(authorizationResult) {
        scope.launch {
            when (authorizationResult) {
                is AuthorizationResult.Error -> {
                    purchaseFlowState = PurchaseFlowState.FAILED
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
                    if (
                        purchaseFlowState != PurchaseFlowState.COMMITTING &&
                        purchaseFlowState != PurchaseFlowState.AWAITING_VERIFICATION
                    ) {
                        purchaseFlowState = PurchaseFlowState.IDLE
                    }
                }

                is AuthorizationResult.Success -> {
                    Log.d(
                        "AuthorizationResult",
                        "Payment authorized, opening URL: ${(authorizationResult as AuthorizationResult.Success).authorizationUrl}"
                    )
                    pendingTransactionReference = paymentViewModel.transactionReference.orEmpty()
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data =
                            android.net.Uri.parse((authorizationResult as AuthorizationResult.Success).authorizationUrl)
                    }
                    context.startActivity(intent)
                    purchaseFlowState = PurchaseFlowState.AWAITING_VERIFICATION
                }
            }
        }
    }

    LaunchedEffect(paymentErrorMessage) {
        paymentErrorMessage?.let { error ->
            purchaseFlowState = PurchaseFlowState.FAILED
            snackbarHostState.showSnackbar(message = error)
            paymentViewModel.clearError()
        }
    }

    LaunchedEffect(verificationResult) {
        scope.launch {
            when (verificationResult) {
                is VerificationResult.Error -> {
                    isLoading = false
                    purchaseFlowState = PurchaseFlowState.FAILED
                    Log.d(
                        "VerificationResult",
                        "Payment verification error: ${(verificationResult as VerificationResult.Error).message}"
                    )
                    navigateToPurchaseResult(
                        status = "failure",
                        reference = pendingTransactionReference.ifBlank { null },
                        message = (verificationResult as VerificationResult.Error).message
                    )
                }

                is VerificationResult.Idle -> {
                    if (
                        purchaseFlowState == PurchaseFlowState.VERIFYING ||
                        purchaseFlowState == PurchaseFlowState.FAILED
                    ) {
                        purchaseFlowState = PurchaseFlowState.IDLE
                    }
                }

                is VerificationResult.Success -> {
                    isLoading = false
                    Log.d("VerificationResult", "Payment verified successfully, redirecting to Tickets Screen.")
                    val verifiedReference = verifiedTransaction?.reference.orEmpty()
                    val ticketReference = verifiedReference.ifBlank {
                        paymentViewModel.transactionReference.orEmpty().ifBlank { pendingTransactionReference }
                    }
                    selectedTicketType?.let { ticketType ->
                        val normalizedTransaction = verifiedTransaction?.copy(reference = ticketReference)
                        if (normalizedTransaction == null || ticketReference.isBlank()) {
                            navigateToPurchaseResult(
                                status = "failure",
                                message = "Missing verified transaction reference."
                            )
                            return@let
                        }

                        val boughtTicket = buildBoughtTicket(
                            event = event,
                            ticketType = ticketType,
                            ticketReference = ticketReference,
                            paymentTransaction = normalizedTransaction,
                            isFreeTicket = false
                        )
                        scope.launch {
                            val isStillAvailable = isTicketAvailable(event.id, ticketType)
                            if (!isStillAvailable) {
                                purchaseFlowState = PurchaseFlowState.FAILED
                                navigateToPurchaseResult(
                                    status = "failure",
                                    reference = ticketReference,
                                    message = "Ticket is sold out. Verification succeeded but purchase was not completed."
                                )
                                return@launch
                            }

                            purchaseFlowState = PurchaseFlowState.COMMITTING
                            isCommittingPurchase = true
                            val result = userViewModel.commitTicketPurchase(
                                boughtTicket = boughtTicket,
                                transaction = normalizedTransaction
                            )
                            isCommittingPurchase = false
                            result.onSuccess {
                                Log.d("DetailedEventScreen", "Processed bought ticket: $boughtTicket")
                                pendingTransactionReference = ""
                                purchaseFlowState = PurchaseFlowState.IDLE
                                navigateToPurchaseResult(
                                    status = "success",
                                    reference = ticketReference,
                                    message = "Your ticket purchase is complete."
                                )
                            }.onFailure { error ->
                                purchaseFlowState = PurchaseFlowState.FAILED
                                navigateToPurchaseResult(
                                    status = "failure",
                                    reference = ticketReference,
                                    message = error.message ?: "Failed to complete purchase."
                                )
                            }
                        }
                    }
                }

                is VerificationResult.Loading -> {
                    isLoading = true
                    purchaseFlowState = PurchaseFlowState.VERIFYING
                    Log.d("DetailedEventScreen", "Payment verification is loading.")
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
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

                )
        }
    ) { paddingValues ->
        DetailedEventContent(
            event = event,
            contentPadding = paddingValues,
            showDoneButton = purchaseFlowState == PurchaseFlowState.AWAITING_VERIFICATION,
            hasBoughtTicket = hasBoughtTicket,
            chosenTicketType = { ticketType -> selectedTicketType = ticketType },
            isPurchaseBlocked = isPurchaseBlocked,
            purchaseBlockedMessage = purchaseBlockedMessage,
            isActionInProgress = purchaseFlowState == PurchaseFlowState.AUTHORIZING ||
                    purchaseFlowState == PurchaseFlowState.VERIFYING ||
                    purchaseFlowState == PurchaseFlowState.COMMITTING,
            ownedTicketReference = ownedTicketReference,
            onViewMyTicketClicked = { reference ->
                navController.navigate("detailed_ticket_screen/$reference")
            },
            onOpenTicketsClicked = {
                navController.navigate(Routes.TICKETS_SCREEN)
            },
            onBuyTicketClicked = { ticketType, email ->
                scope.launch {
                    Log.d("DetailedEventScreen", "Buy clicked. flow=$purchaseFlowState ticket=${ticketType.name}")
                    if (isCommittingPurchase) {
                        snackbarHostState.showSnackbar("Please wait, processing your ticket.")
                        return@launch
                    }
                    if (isPurchaseBlocked) {
                        Log.d("DetailedEventScreen", "Buy blocked by event status: ${event.eventStatus}")
                        Toast.makeText(
                            context,
                            purchaseBlockedMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    if (hasBoughtTicket) {
                        Log.d("DetailedEventScreen", "Buy blocked: user already bought ticket for event=${event.id}")
                        Toast.makeText(
                            context,
                            "You have already bought a ticket for this event.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    val availableNow = isTicketAvailable(event.id, ticketType)
                    if (!availableNow) {
                        purchaseFlowState = PurchaseFlowState.FAILED
                        Log.d(
                            "DetailedEventScreen",
                            "Buy blocked: ticket unavailable. event=${event.id}, ticket=${ticketType.name}"
                        )
                        snackbarHostState.showSnackbar(message = "Ticket is sold out.")
                        return@launch
                    }

                    if (ticketType.price <= 0.0) {
                        val freeReference = "FREE-${UUID.randomUUID()}"
                        val freeTicket = buildBoughtTicket(
                            event = event,
                            ticketType = ticketType,
                            ticketReference = freeReference,
                            paymentTransaction = null,
                            isFreeTicket = true
                        )
                        val freeTransaction = buildFreeTransaction(
                            ticketReference = freeReference,
                            boughtTicket = freeTicket
                        )
                        purchaseFlowState = PurchaseFlowState.COMMITTING
                        isCommittingPurchase = true
                        val result = userViewModel.commitTicketPurchase(
                            boughtTicket = freeTicket,
                            transaction = freeTransaction
                        )
                        isCommittingPurchase = false
                        result.onSuccess {
                            purchaseFlowState = PurchaseFlowState.IDLE
                            navigateToPurchaseResult(
                                status = "success",
                                reference = freeReference,
                                message = "Free ticket issued successfully."
                            )
                        }.onFailure { error ->
                            purchaseFlowState = PurchaseFlowState.FAILED
                            navigateToPurchaseResult(
                                status = "failure",
                                reference = freeReference,
                                message = error.message ?: "Failed to issue free ticket."
                            )
                        }
                        return@launch
                    }

                    if (purchaseFlowState == PurchaseFlowState.AWAITING_VERIFICATION) {
                        if (pendingTransactionReference.isNotBlank()) {
                            paymentViewModel.transactionReference = pendingTransactionReference
                        }
                        purchaseFlowState = PurchaseFlowState.VERIFYING
                        paymentViewModel.verifyTransaction()
                    } else if (purchaseFlowState == PurchaseFlowState.IDLE || purchaseFlowState == PurchaseFlowState.FAILED) {
                        val safeEmail = email?.trim().orEmpty()
                        if (safeEmail.isBlank()) {
                            Log.d("DetailedEventScreen", "Buy blocked: missing user email.")
                            Toast.makeText(
                                context,
                                "No account email available for payment.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                        purchaseFlowState = PurchaseFlowState.AUTHORIZING
                        paymentViewModel.initiatePayment(
                            email = safeEmail,
                            amount = (ticketType.price * 100).roundToInt().toString()
                        )
                        pendingTransactionReference = paymentViewModel.transactionReference.orEmpty()
                    } else {
                        Log.d("DetailedEventScreen", "Buy ignored due to flow state=$purchaseFlowState")
                        snackbarHostState.showSnackbar("Please complete current payment step first.")
                    }
                }
            }
        )
    }

    if (isLoading || isCommittingPurchase) {
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

private suspend fun isTicketAvailable(eventId: String, selectedTicketType: TicketType): Boolean {
    return try {
        val snapshot = FirebaseFirestore.getInstance().collection("events").document(eventId).get().await()
        val ticketMaps = snapshot.get("ticketTypes") as? List<Map<String, Any?>>
        val currentTicket = ticketMaps
            ?.map { map ->
                TicketType(
                    name = map["name"] as? String ?: "",
                    price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                    availableTickets = when (val raw = map["availableTickets"]) {
                        is Number -> raw.toInt()
                        is String -> raw.toIntOrNull() ?: selectedTicketType.availableTickets
                        else -> selectedTicketType.availableTickets
                    },
                    isFree = (map["isFree"] as? Boolean) ?: (map["free"] as? Boolean) ?: false
                )
            }
            ?.firstOrNull { ticket ->
                val sameName = ticket.name.equals(selectedTicketType.name, ignoreCase = true)
                val samePrice = kotlin.math.abs(ticket.price - selectedTicketType.price) < 0.0001
                sameName || samePrice
            }

        when {
            currentTicket == null -> selectedTicketType.availableTickets > 0
            currentTicket.availableTickets <= 0 -> false
            else -> true
        }
    } catch (e: Exception) {
        Log.e("DetailedEventScreen", "Ticket availability check failed", e)
        selectedTicketType.availableTickets > 0
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
    sharedPreferencesViewModel: SharedPreferencesViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showDoneButton: Boolean,
    hasBoughtTicket: Boolean,
    chosenTicketType: (ticketType: TicketType) -> Unit,
    isPurchaseBlocked: Boolean,
    purchaseBlockedMessage: String,
    isActionInProgress: Boolean,
    ownedTicketReference: String?,
    onViewMyTicketClicked: (String) -> Unit,
    onOpenTicketsClicked: () -> Unit,
    onBuyTicketClicked: (ticketType: TicketType, email: String?) -> Unit,
    userViewModel: UserViewModel = viewModel(),
    eventsManagementViewModel: EventsManagementViewModel = viewModel(),
    userPreferences: UserPreferences = viewModel()
) {
    val layoutDirection = LocalLayoutDirection.current

    var isBookmarked by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    // Check if the event is already marked as favorite
    LaunchedEffect(Unit) {
        isFavorite = userViewModel.isEventFavorite(event)
    }

    val userData by sharedPreferencesViewModel.userInfo.collectAsState()
    val email = userData["USER_EMAIL"]

    var selectedTicketType by remember { mutableStateOf<TicketType?>(null) }

    val displayStartDate = formatDisplayDate(event.startDate.ifBlank { "01/01/1970" })
    val displayEndDate = formatDisplayDate(event.endDate.ifBlank { event.startDate.ifBlank { "01/01/1970" } })

    val displayName = event.eventName.ifBlank { "Unnamed Event" }
    val displayVenue = event.eventVenue.ifBlank { "Venue not set" }
    val displayStartTime = event.eventTime.ifBlank { "Time not set" }
    val displayDescription = event.eventDescription.ifBlank { "No description provided." }
    val displayStatus = event.eventStatus.ifBlank { "Unknown" }
    val safeImageUri = event.imageUri?.takeIf { it.isNotBlank() }
    val canBuyTicket = !hasBoughtTicket && !isPurchaseBlocked && !isActionInProgress
    val disabledReason = when {
        hasBoughtTicket -> "You already bought a ticket for this event."
        isPurchaseBlocked -> purchaseBlockedMessage
        isActionInProgress -> "Please complete the current payment step."
        else -> ""
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp + contentPadding.calculateStartPadding(layoutDirection),
            end = 16.dp + contentPadding.calculateEndPadding(layoutDirection),
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(KikuuCardDeep)
            ) {
                if (safeImageUri != null) {
                    AsyncImage(
                        model = safeImageUri,
                        contentDescription = "Event Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f))
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(50),
                    color = if (isPurchaseBlocked) Color(0xFFD35400) else KikuuAccent
                ) {
                    Text(
                        text = displayStatus.uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = KikuuCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isBookmarked = !isBookmarked
                                if (isBookmarked) {
                                    userViewModel.addBookmarkEventToFireStore(event)
                                    userPreferences.addBookmarkEvent(event)
                                } else {
                                    userViewModel.deleteBookmarkEventFromFirestore(event)
                                    userPreferences.removeBookmarkEvent(event)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = KikuuAccent
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
                                tint = KikuuAccent
                            )
                        }
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = KikuuText
                        )
                    )

                    Text(
                        text = if (event.isMultiDayEvent) "$displayStartDate - $displayEndDate" else displayStartDate,
                        style = MaterialTheme.typography.bodySmall.copy(color = KikuuMuted)
                    )
                    Text(
                        text = "Time: $displayStartTime",
                        style = MaterialTheme.typography.bodyMedium.copy(color = KikuuText)
                    )
                    Text(
                        text = "Venue: $displayVenue",
                        style = MaterialTheme.typography.bodyMedium.copy(color = KikuuText)
                    )
                    Text(
                        text = displayDescription,
                        style = MaterialTheme.typography.bodySmall.copy(color = KikuuMuted)
                    )
                    if (isPurchaseBlocked) {
                        Text(
                            text = purchaseBlockedMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFB03A2E),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = "Ticket Types",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (event.ticketTypes.isEmpty()) {
                Text(
                    text = "No ticket types available.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                    )
                )
            } else {
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
                                selectedTicketType?.let { chosenTicketType(it) }
                            }
                        )
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasBoughtTicket) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFE6F6EA),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(
                            text = "Already Purchased",
                            color = Color(0xFF1B5E20),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = "You already have a ticket for this event.",
                        style = MaterialTheme.typography.bodySmall.copy(color = KikuuMuted)
                    )
                    Button(
                        onClick = {
                            val reference = ownedTicketReference
                            if (!reference.isNullOrBlank()) {
                                onViewMyTicketClicked(reference)
                            } else {
                                onOpenTicketsClicked()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KikuuAccent)
                    ) {
                        Text(
                            text = "View My Ticket",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenTicketsClicked,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Tickets")
                    }
                } else if (selectedTicketType != null) {
                    val buttonText = when {
                        isPurchaseBlocked -> "Purchase Disabled"
                        showDoneButton -> "Done"
                        selectedTicketType?.isFree == true || selectedTicketType?.price == 0.0 -> "Claim Free Ticket"
                        else -> "Buy Ticket"
                    }
                    Button(
                        onClick = {
                            onBuyTicketClicked(selectedTicketType!!, email)
                        },
                        enabled = canBuyTicket,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KikuuAccent,
                            disabledContainerColor = KikuuMuted
                        )
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    if (!canBuyTicket && disabledReason.isNotBlank()) {
                        Text(
                            text = disabledReason,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB03A2E)),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun buildBoughtTicket(
    event: Event,
    ticketType: TicketType,
    ticketReference: String,
    paymentTransaction: Transaction?,
    isFreeTicket: Boolean
): BoughtTicket {
    val authUser = FirebaseAuth.getInstance().currentUser
    val qrData = "eventglow_ticket|$ticketReference|${event.id}|${authUser?.uid.orEmpty()}"
    return BoughtTicket(
        transactionReference = ticketReference,
        paymentProvider = if (isFreeTicket) "free" else "paystack",
        paymentStatus = if (isFreeTicket) "success" else (paymentTransaction?.status ?: ""),
        paymentGatewayResponse = if (isFreeTicket) "Free ticket issued" else (paymentTransaction?.gatewayResponse
            ?: ""),
        paymentAmount = if (isFreeTicket) "0" else (paymentTransaction?.amount ?: ticketType.price.toString()),
        paymentCurrency = if (isFreeTicket) "GHS" else (paymentTransaction?.currency ?: "GHS"),
        paymentChannel = if (isFreeTicket) "free" else (paymentTransaction?.channel ?: ""),
        paymentAuthorizationCode = paymentTransaction?.authorizationCode ?: "",
        paymentCardType = paymentTransaction?.cardType ?: "",
        paymentBank = paymentTransaction?.bank ?: "",
        paymentCustomerEmail = paymentTransaction?.customerEmail ?: "",
        paymentPaidAt = paymentTransaction?.paidAt ?: "",
        paymentCreatedAt = paymentTransaction?.createdAt ?: "",
        isFreeTicket = isFreeTicket,
        eventOrganizer = event.eventOrganizer,
        eventId = event.id,
        eventName = event.eventName,
        startDate = event.startDate,
        eventStatus = event.eventStatus,
        endDate = event.endDate,
        imageUrl = event.imageUri,
        ticketName = ticketType.name,
        ticketPrice = ticketType.price.toString(),
        qrCodeData = qrData,
        isScanned = false,
        scannedAt = "",
        scannedByAdminId = "",
        scannedByAdminName = ""
    )
}

private fun buildFreeTransaction(
    ticketReference: String,
    boughtTicket: BoughtTicket
): Transaction {
    val authUser = FirebaseAuth.getInstance().currentUser
    return Transaction(
        id = ticketReference,
        userId = authUser?.uid.orEmpty(),
        status = "success",
        reference = ticketReference,
        amount = "0",
        gatewayResponse = "Free ticket issued",
        paidAt = boughtTicket.paymentPaidAt,
        createdAt = boughtTicket.paymentCreatedAt,
        channel = "free",
        currency = "GHS",
        customerEmail = boughtTicket.paymentCustomerEmail.ifBlank { authUser?.email.orEmpty() }
    )
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
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = if (isSelected) KikuuAccent else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.2f
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KikuuCard),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = ticketType.name.ifBlank { "Ticket" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = KikuuText)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (ticketType.isFree || ticketType.price <= 0.0) "FREE" else "GHS ${ticketType.price}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (ticketType.isFree || ticketType.price <= 0.0) Color(0xFF1E8449) else KikuuMuted
                )
            )
        }
    }
}


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EventDetailsScreenPreview() {
    EventDetailsScreen()
}
