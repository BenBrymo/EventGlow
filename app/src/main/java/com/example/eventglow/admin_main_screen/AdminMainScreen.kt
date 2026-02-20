package com.example.eventglow.admin_main_screen


import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eventglow.R
import com.example.eventglow.common.LoadState
import com.example.eventglow.common.SharedPreferencesViewModel
import com.example.eventglow.dataClass.Event
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.EventGlowTheme
import kotlinx.coroutines.launch


@Composable
fun AdminHomeScreen(
    navController: NavController,
    sharedPreferencesViewModel: SharedPreferencesViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userData by sharedPreferencesViewModel.userInfo.collectAsState()
    val userName = userData["USERNAME"] ?: "Admin"
    val profileImageUrl = userData["PROFILE_PICTURE_URL"]
    val uiState by adminViewModel.adminHomeUiState.collectAsState()
    val loadState by adminViewModel.loadState.collectAsState()
    val isRefreshing by adminViewModel.isRefreshing.collectAsState()
    val errorMessage by adminViewModel.errorMessage.collectAsState()
    val isOnline by rememberInternetConnectionState(context)
    val snackbarHostState = remember { SnackbarHostState() }
    var hasInitializedConnectivityObserver by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                sharedPreferencesViewModel.refreshUserInfo()
                adminViewModel.refreshAdminHomeData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        adminViewModel.clearError()
    }
    LaunchedEffect(isOnline) {
        if (!hasInitializedConnectivityObserver) {
            hasInitializedConnectivityObserver = true
            return@LaunchedEffect
        }
        if (isOnline) {
            snackbarHostState.showSnackbar("Internet connection restored")
            adminViewModel.refreshAdminHomeData()
        } else {
            snackbarHostState.showSnackbar("No internet connection")
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
        drawerContent = {
            AdminModernDrawerContent(
                onNavigate = { route ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route)
                    }
                }
            )
        }
    ) {
        AdminHomeContent(
            userName = userName,
            profileImageUrl = profileImageUrl,
            upcomingEvents = uiState.upcomingEvents,
            todayEvents = uiState.todayEvents,
            totalEvents = uiState.totalEvents,
            totalTickets = uiState.totalTickets,
            loadState = loadState,
            isRefreshing = isRefreshing,
            snackbarHostState = snackbarHostState,
            onRefresh = { adminViewModel.refreshAdminHomeData() },
            onMenuClick = {
                scope.launch {
                    drawerState.open()
                }
            },
            onProfileClick = { navController.navigate(Routes.ADMIN_PROFILE_SCREEN) },
            onEventClick = { event -> navController.navigate("detailed_event_screen_admin/${event.id}") },
            onEventsCardClick = { navController.navigate(Routes.EVENTS_MANAGEMENT_SCREEN) },
            onTicketsCardClick = { navController.navigate(Routes.TICKET_MANAGEMENT_SCREEN) }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AdminHomeContent(
    userName: String = "BenBrymo",
    profileImageUrl: String? = null,
    upcomingEvents: List<Event> = emptyList(),
    todayEvents: List<Event> = emptyList(),
    totalEvents: Int = 0,
    totalTickets: Int = 0,
    loadState: LoadState = LoadState.SUCCESS,
    isRefreshing: Boolean = false,
    snackbarHostState: SnackbarHostState? = null,
    onRefresh: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onEventClick: (Event) -> Unit = {},
    onEventsCardClick: () -> Unit = {},
    onTicketsCardClick: () -> Unit = {}
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            snackbarHostState?.let { SnackbarHost(hostState = it) }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            when (loadState) {
                LoadState.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                LoadState.FAILURE -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Failed to load dashboard data",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                LoadState.SUCCESS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {

                        HomeHeader(
                            userName = userName,
                            profileImageUrl = profileImageUrl,
                            onMenuClick = onMenuClick,
                            onProfileClick = onProfileClick
                        )

                        Spacer(Modifier.height(12.dp))

                        DashboardCardsRow(
                            totalEvents = totalEvents,
                            totalTickets = totalTickets,
                            onEventsCardClick = onEventsCardClick,
                            onTicketsCardClick = onTicketsCardClick
                        )

                        Spacer(Modifier.height(24.dp))

                        EventsSection(
                            title = "Upcoming Events",
                            events = upcomingEvents,
                            onEventClick = onEventClick
                        )

                        Spacer(Modifier.height(24.dp))

                        EventsSection(
                            title = "Events Today",
                            events = todayEvents,
                            onEventClick = onEventClick
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}


@Composable
fun HomeHeader(
    userName: String,
    profileImageUrl: String?,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val fallbackPainter = painterResource(id = R.drawable.applogo)
    val safeProfileModel = profileImageUrl
        ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(
            "Welcome $userName",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onProfileClick) {
            AsyncImage(
                model = safeProfileModel ?: R.drawable.applogo,
                contentDescription = "Profile",
                fallback = fallbackPainter,
                error = fallbackPainter,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50)),
            )
        }
    }
}


@Composable
fun DashboardCardsRow(
    totalEvents: Int,
    totalTickets: Int,
    onEventsCardClick: () -> Unit,
    onTicketsCardClick: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(2) {
            DashboardCard(
                title = if (it == 0) "Events" else "Tickets",
                count = if (it == 0) totalEvents.toString() else totalTickets.toString(),
                onClick = if (it == 0) onEventsCardClick else onTicketsCardClick
            )
        }
    }
}


@Composable
fun DashboardCard(
    title: String,
    count: String,
    onClick: () -> Unit
) {
    val dividerColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
    ) {

        Column {

            // Top section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary)
            }

            // Bottom gradient section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, color = MaterialTheme.colorScheme.onPrimary)
                    Text(count, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // Curvy divider
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.Center)
        ) {

            val path = Path().apply {
                moveTo(0f, size.height / 2)

                cubicTo(
                    size.width * 0.25f, 0f,
                    size.width * 0.75f, size.height,
                    size.width, size.height / 2
                )
            }

            drawPath(
                path = path,
                color = dividerColor,
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}


@Composable
fun EventsSection(
    title: String,
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        if (events.isEmpty()) {

            Text(
                "No events available",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

        } else {

            events.forEach { event ->
                EventCard(event = event, onClick = { onEventClick(event) })
            }
        }
    }
}


@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {

        AsyncImage(
            model = event.imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val freeLabel = event.ticketTypes.any { it.price <= 0.0 }
            Text(if (freeLabel) "Free" else "Paid", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}


@Preview(name = "Admin Home Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 34)
@Composable
fun AdminHomeScreenLightPreview() {
    EventGlowTheme(darkTheme = false) {
        AdminHomeContent(
            userName = "Admin User",
            upcomingEvents = listOf(
                Event(id = "1", eventName = "Music Night", imageUri = "", ticketTypes = emptyList()),
                Event(id = "2", eventName = "Tech Expo", imageUri = "", ticketTypes = emptyList())
            ),
            todayEvents = emptyList(),
            totalEvents = 12,
            totalTickets = 420,
            loadState = LoadState.SUCCESS,
            isRefreshing = false
        )
    }
}

@Preview(name = "Admin Home Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 34)
@Composable
fun AdminHomeScreenDarkPreview() {
    EventGlowTheme(darkTheme = true) {
        AdminHomeContent(
            userName = "Admin User",
            upcomingEvents = listOf(
                Event(id = "1", eventName = "Music Night", imageUri = "", ticketTypes = emptyList()),
                Event(id = "2", eventName = "Tech Expo", imageUri = "", ticketTypes = emptyList())
            ),
            todayEvents = emptyList(),
            totalEvents = 12,
            totalTickets = 420,
            loadState = LoadState.SUCCESS,
            isRefreshing = false
        )
    }
}


@Composable
private fun rememberInternetConnectionState(context: Context): androidx.compose.runtime.State<Boolean> {
    val isConnected = remember {
        mutableStateOf(checkInternetConnection(context))
    }

    DisposableEffect(context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected.value = true
            }

            override fun onLost(network: Network) {
                isConnected.value = checkInternetConnection(context)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                isConnected.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    return isConnected
}

private fun checkInternetConnection(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
private fun AdminModernDrawerContent(
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        DrawerNavItem("Event Management", Icons.Default.Event, Routes.EVENTS_MANAGEMENT_SCREEN),
        DrawerNavItem("User Management", Icons.Default.AccountBox, Routes.USER_MANAGEMENT_SCREEN),
        DrawerNavItem("Ticket Management", Icons.AutoMirrored.Filled.AirplaneTicket, Routes.TICKET_MANAGEMENT_SCREEN),
        DrawerNavItem("Reporting & Analytics", Icons.Default.Analytics, Routes.REPORTING_AND_ANALYTICS),
        DrawerNavItem("Settings", Icons.Default.Settings, Routes.SETTINGS)
    )
    var selectedIndex by remember { mutableStateOf(0) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Admin",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable {
                            selectedIndex = index
                            onNavigate(item.route)
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private data class DrawerNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun HomeHeaderPreview() {
    HomeHeader(
        userName = "Admin User",
        profileImageUrl = null,
        onMenuClick = {},
        onProfileClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun DashboardCardPreview() {
    DashboardCard(
        title = "Events",
        count = "12",
        onClick = {}
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EventsSectionEmptyPreview() {
    EventsSection(
        title = "Upcoming Events",
        events = emptyList(),
        onEventClick = { }
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EventsSectionWithDataPreview() {
    EventsSection(
        title = "Upcoming Events",
        events = listOf(
            Event(id = "1", eventName = "Open Air", imageUri = ""),
            Event(id = "2", eventName = "Sport Day", imageUri = "")
        ),
        onEventClick = { }
    )
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EventCardPreview() {
    EventCard(
        event = Event(id = "1", eventName = "Open Air", imageUri = ""),
        onClick = {}
    )
}
