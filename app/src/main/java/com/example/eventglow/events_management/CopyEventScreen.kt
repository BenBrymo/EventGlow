package com.example.eventglow.events_management

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.example.eventglow.BuildConfig
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.navigation.Routes
import com.example.eventglow.notifications.FirestoreNotificationSenderViewModel
import com.example.eventglow.notifications.ROUTE_DETAILED_EVENT_SCREEN
import kotlinx.coroutines.launch


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyEventScreen(
    navController: NavController,
    viewModel: EventsManagementViewModel = viewModel(),
    senderViewModel: FirestoreNotificationSenderViewModel = viewModel(),
    eventId: String?
) {

    // Remember coroutine scope for performing asynchronous actions
    val scope = rememberCoroutineScope()
    val fetchState by viewModel.fetchEventsState.collectAsState()

    LaunchedEffect(eventId) {
        if (!eventId.isNullOrBlank() && viewModel.getEventById(eventId) == null) {
            viewModel.fetchEvents()
        }
    }

    if (eventId.isNullOrBlank()) {
        CopyEventStateScreen(
            title = "Invalid Event",
            message = "This event link is missing a valid event id.",
            onPrimaryAction = { navController.popBackStack() },
            primaryActionLabel = "Go Back"
        )
        return
    }


    //retrives event from eventlist
    val event = viewModel.getEventById(eventId)
    println("Retrived id: $eventId")
    println("Retrived event: ${event?.eventName}")

    // Handle the UI when the event data is null
    if (event == null) {
        when (val state = fetchState) {
            is FetchEventsState.Loading -> {
                CopyEventStateScreen(
                    title = "Loading Event",
                    message = "Please wait while we load event details.",
                    showLoading = true
                )
            }

            is FetchEventsState.Failure -> {
                CopyEventStateScreen(
                    title = "Could Not Load Event",
                    message = state.errorMessage,
                    onPrimaryAction = { viewModel.fetchEvents() },
                    primaryActionLabel = "Retry",
                    onSecondaryAction = { navController.popBackStack() },
                    secondaryActionLabel = "Go Back"
                )
            }

            is FetchEventsState.Success -> {
                CopyEventStateScreen(
                    title = "Event Not Found",
                    message = "This event may have been deleted or is no longer available.",
                    onPrimaryAction = { navController.popBackStack() },
                    primaryActionLabel = "Go Back"
                )
            }
        }

    } else {

        // Variables to hold event detailsl
        var eventName by remember { mutableStateOf(event.eventName) }
        var startDate by remember { mutableStateOf(event.startDate) }
        var endDate by remember { mutableStateOf(event.endDate) }
        var isMultiDayEvent by remember { mutableStateOf(event.isMultiDayEvent) }
        var eventTime by remember { mutableStateOf(event.eventTime) }
        var durationLabel by remember { mutableStateOf(event.durationLabel) }
        var durationMinutes by remember { mutableIntStateOf(event.durationMinutes) }
        var eventVenue by remember { mutableStateOf(event.eventVenue) }
        var eventStatus by remember { mutableStateOf(event.eventStatus) }
        var isImportant by remember { mutableStateOf(event.isImportant) }
        var eventCategory by remember { mutableStateOf(event.eventCategory) }
        var ticketTypes by remember { mutableStateOf(event.ticketTypes) }
        var imageUri by remember { mutableStateOf<Uri?>(event.imageUri?.takeIf { it.isNotBlank() }?.toUri()) }
        var eventOrganizer by remember { mutableStateOf(event.eventOrganizer) }
        var eventDescription by remember { mutableStateOf(event.eventDescription) }
        var isUploadingImage by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val eventCategories by viewModel.eventCategories.collectAsState()
        val categoriesList = remember(eventCategories) { eventCategories.map { it.name } }

        var validationErrors = remember { mutableListOf<String>() }
        var pendingPublishedEventId by remember { mutableStateOf<String?>(null) }
        var useCustomPublishNotification by remember { mutableStateOf(false) }
        var publishNotificationTitle by remember { mutableStateOf("") }
        var publishNotificationBody by remember { mutableStateOf("") }
        var publishNotificationTargetRole by remember { mutableStateOf("all") }

        var eventNameAndDateError by remember { mutableStateOf<String?>(null) }

        fun markEventsUpdatedAndReturn() {
            navController.previousBackStackEntry?.savedStateHandle?.set("events_updated", true)
            navController.getBackStackEntryOrNull(Routes.ADMIN_MAIN_SCREEN)
                ?.savedStateHandle
                ?.set("events_updated", true)
            navController.popBackStack()
        }


        fun validateEventData() {

            //reset validationErrors
            validationErrors.clear()

            if (eventName.isEmpty()) validationErrors.add("Event name is required")
            if (isMultiDayEvent) {
                if (startDate.isEmpty()) validationErrors.add("Start date is required")
                if (endDate.isEmpty()) validationErrors.add("End date is required")
            } else {
                if (startDate.isEmpty()) validationErrors.add("Event date is required")
            }
            if (eventTime.isEmpty()) validationErrors.add("Event time is required")
            if (durationMinutes <= 0) validationErrors.add("Event duration is required")
            if (eventVenue.isEmpty()) validationErrors.add("Event venue is required")
            if (eventStatus.isEmpty()) validationErrors.add("Event status is required")
            if (eventCategory.isEmpty()) validationErrors.add("Event category is required")
            if (eventOrganizer.isEmpty()) validationErrors.add("Event Organizer is required")
            if (eventDescription.isEmpty()) validationErrors.add("Event Description is required")
            if (ticketTypes.isEmpty()) validationErrors.add("At least one ticket Type is required")
            if (ticketTypes.any { !it.isFree && it.price < 1 }) validationErrors.add("Paid ticket price must be at least 1 GHS")
            if (imageUri == null) validationErrors.add("Event image is required")
            if (eventNameAndDateError != null) validationErrors.add("$eventNameAndDateError")
        }


        // Variables to track the current selected tab
        var selectedTabIndex by remember { mutableIntStateOf(0) }

        // Snackbar state
        val snackbarHostState = remember { SnackbarHostState() }
        val pushErrorMessage by senderViewModel.errorMessage.collectAsState()
        LaunchedEffect(Unit) {
            viewModel.fetchEventCategories()
        }
        LaunchedEffect(pushErrorMessage) {
            val message = pushErrorMessage ?: return@LaunchedEffect
            snackbarHostState.showSnackbar(message)
            senderViewModel.clearError()
        }

        // Event Image picker launcher
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                scope.launch {
                    isUploadingImage = true
                    val uploadedUrl = viewModel.uploadEventImageToCloudinary(
                        appContext = context,
                        imageUri = selectedUri,
                        eventName = eventName
                    )
                    isUploadingImage = false
                    if (uploadedUrl.isNullOrBlank()) {
                        snackbarHostState.showSnackbar("Image upload failed. Please try again.")
                    } else {
                        imageUri = Uri.parse(uploadedUrl)
                        snackbarHostState.showSnackbar("Image uploaded successfully.")
                    }
                }
            }
        }

        val realEventImages = rememberCloudinarySampleImagesForCopy(
            categories = categoriesList,
            samplesPerCategory = 3
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Copy Event", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->

            // For all Content
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                //For TabRow Content
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        edgePadding = 8.dp
                    ) {
                        Tab(
                            text = { Text("Event Details") },
                            selected = selectedTabIndex == 0, //set selected to true
                            onClick = { selectedTabIndex = 0 }  // selects Event Details section
                        )
                        Tab(
                            text = { Text("Ticket Details") },
                            selected = selectedTabIndex == 1, //set selected to true
                            onClick = { selectedTabIndex = 1 }  // selects Ticket Details section
                        )
                        Tab(
                            text = { Text("Event Image") },
                            selected = selectedTabIndex == 2, //set selected to true
                            onClick = { selectedTabIndex = 2 }  // selects Event Images section
                        )
                        Tab(
                            text = { Text("Notification") },
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 }
                        )
                        Tab(
                            text = { Text("Finish") },
                            selected = selectedTabIndex == 4,
                            onClick = { selectedTabIndex = 4 }
                        )
                    }

                    when (selectedTabIndex) {
                        0 -> EventDetailsSection(
                            eventName = eventName,
                            startDate = startDate,
                            endDate = endDate,
                            isMultiDayEvent = isMultiDayEvent,
                            eventTime = eventTime,
                            durationLabel = durationLabel,
                            durationMinutes = durationMinutes,
                            eventVenue = eventVenue,
                            eventStatus = eventStatus,
                            isImportant = isImportant,
                            eventCategory = eventCategory,
                            eventOrganizer = eventOrganizer,
                            eventDescription = eventDescription,
                            onEventNameChange = { newEventName -> eventName = newEventName },
                            onStartDateChange = { newStartDate -> startDate = newStartDate },
                            onEndDateChange = { newEndDate -> endDate = newEndDate },
                            onIsMultiDayEventChange = { isMultiDayEvent = it },
                            onEventTimeChange = { newEventTime -> eventTime = newEventTime },
                            onDurationChange = { label, minutes ->
                                durationLabel = label
                                durationMinutes = minutes
                            },
                            onEventVenueChange = { newEventVenue -> eventVenue = newEventVenue },
                            onImportantChange = { isImportant = it },
                            onEventCategoryChange = { newEventCategory -> eventCategory = newEventCategory },
                            eventCategoryOptions = categoriesList,
                            onAddEventCategory = { newCategory ->
                                viewModel.addEventCategory(
                                    categoryName = newCategory,
                                    onSuccess = { addedCategory ->
                                        eventCategory = addedCategory
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Category added: $addedCategory")
                                        }
                                    },
                                    onFailure = { message ->
                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                    }
                                )
                            },
                            onEventOrganizerChange = { newEventOrganizer -> eventOrganizer = newEventOrganizer },
                            onEventDescriptionChange = { newEventDescription ->
                                eventDescription = newEventDescription
                            },
                            eventNameAndDateError = { errorMessage -> eventNameAndDateError = errorMessage }
                        )

                        1 -> TicketDetailsSection(


                            ticketTypes = ticketTypes,  //pass initial ticketTypes list
                            onAddTicketType = {

                                //add a new ticket type object
                                ticketTypes = ticketTypes + TicketType("", 0.0, 0)
                            },
                            onTicketTypeNameChange = { index, newName ->

                                //map tickets in the list of tickets to an index
                                ticketTypes = ticketTypes.mapIndexed { i, ticketType ->

                                    //check if an index in this new list matches index of the ticket item we are dealing with
                                    //and if it exist Copys a copy of it with the new Name else keeps the ticket as it is
                                    if (i == index) ticketType.copy(name = newName) else ticketType
                                }
                            },

                            onTicketTypePriceChange = { index, newPrice ->
                                ticketTypes = ticketTypes.mapIndexed { i, ticketType ->

                                    //same for changing ticket name but converts newPrice to Double
                                    if (i == index) ticketType.copy(
                                        price = newPrice.toDoubleOrNull() ?: 0.0
                                    ) else ticketType
                                }
                            },
                            onTicketTypeAvailableChange = { index, newAvailable ->
                                ticketTypes = ticketTypes.mapIndexed { i, ticketType ->

                                    //same for changing available tickets
                                    if (i == index) ticketType.copy(
                                        availableTickets = newAvailable.toIntOrNull() ?: 0
                                    ) else ticketType
                                }
                            },
                            onTicketTypeFreeChange = { index, isFree ->
                                ticketTypes = ticketTypes.mapIndexed { i, ticketType ->
                                    if (i == index) {
                                        if (isFree) ticketType.copy(isFree = true, price = 0.0)
                                        else ticketType.copy(isFree = false)
                                    } else {
                                        ticketType
                                    }
                                }
                            },
                            onRemoveTicketType = { index ->

                                // filter through the ticket type list using their index and exclude the index of the ticketType we removing
                                ticketTypes = ticketTypes.filterIndexed { i, _ -> i != index }
                            }
                        )

                        2 -> EventImageSection(
                            imageUri = imageUri,
                            isUploadingImage = isUploadingImage,
                            onPickImageClick = { imagePickerLauncher.launch("image/*") },
                            realEventImages = realEventImages,
                            onImageSelected = { selectedImageUri ->
                                imageUri = selectedImageUri
                            }
                        )

                        3 -> EventNotificationSection(
                            useCustomNotification = useCustomPublishNotification,
                            notificationTitle = publishNotificationTitle,
                            notificationBody = publishNotificationBody,
                            notificationTargetRole = publishNotificationTargetRole,
                            onUseCustomNotificationChange = { useCustomPublishNotification = it },
                            onNotificationTitleChange = { publishNotificationTitle = it },
                            onNotificationBodyChange = { publishNotificationBody = it },
                            onNotificationTargetRoleChange = { publishNotificationTargetRole = it }
                        )

                        4 -> FinishSection(
                            eventName = eventName,
                            startDate = startDate,
                            endDate = endDate,
                            isMultiDayEvent = isMultiDayEvent,
                            eventTime = eventTime,
                            durationLabel = durationLabel,
                            eventVenue = eventVenue,
                            eventStatus = eventStatus,
                            isImportant = isImportant,
                            eventCategory = eventCategory,
                            eventOrganizer = eventOrganizer,
                            eventDescription = eventDescription,
                            ticketTypes = ticketTypes,
                            imageUri = imageUri,
                            onSaveDraftClick = {
                                //call to validate event details
                                validateEventData()

                                //if there are errors in event data
                                if (validationErrors.isNotEmpty()) {
                                    scope.launch {
                                        //display error messages
                                        validationErrors.forEach { error ->
                                            snackbarHostState.showSnackbar(error)
                                        }
                                    }
                                } else {

                                    //save to firestore
                                    viewModel.saveEventToFirestore(
                                        eventName = eventName,
                                        startDate = startDate,
                                        endDate = endDate,
                                        isMultiDayEvent = isMultiDayEvent,
                                        eventTime = eventTime,
                                        durationLabel = durationLabel,
                                        durationMinutes = durationMinutes,
                                        eventVenue = eventVenue,
                                        eventStatus = eventStatus,
                                        isImportant = isImportant,
                                        eventCategory = eventCategory,
                                        eventOrganizer = eventOrganizer,
                                        eventDescription = eventDescription,
                                        ticketTypes = ticketTypes,
                                        imageUri = imageUri,
                                        isDraft = true,
                                        //when successful
                                        onSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Event saved to drafts.")
                                                markEventsUpdatedAndReturn()
                                            }
                                        },
                                        //when saving fails
                                        onFailure = { e ->
                                            scope.launch {
                                                //show an error message
                                                snackbarHostState.showSnackbar(
                                                    message = "Failed to save draft: ${e.message}",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    )
                                }
                            },
                            onPublishClick = {

                                //call to validate event details
                                validateEventData()

                                //if there are errors in event data
                                if (validationErrors.isNotEmpty()) {
                                    scope.launch {
                                        validationErrors.forEach { error ->
                                            snackbarHostState.showSnackbar(error)
                                        }
                                    }
                                } else {
                                    //save to firestore
                                    viewModel.saveEventToFirestore(
                                        eventName = eventName,
                                        startDate = startDate,
                                        endDate = endDate,
                                        isMultiDayEvent = isMultiDayEvent,
                                        eventTime = eventTime,
                                        durationLabel = durationLabel,
                                        durationMinutes = durationMinutes,
                                        eventVenue = eventVenue,
                                        eventStatus = eventStatus,
                                        isImportant = isImportant,
                                        eventCategory = eventCategory,
                                        eventOrganizer = eventOrganizer,
                                        eventDescription = eventDescription,
                                        ticketTypes = ticketTypes,
                                        imageUri = imageUri,
                                        isDraft = false,
                                        onSaved = { savedEventId ->
                                            pendingPublishedEventId = savedEventId
                                        },
                                        onSuccess = {
                                            val savedEventId = pendingPublishedEventId
                                            if (savedEventId.isNullOrBlank()) {
                                                scope.launch { markEventsUpdatedAndReturn() }
                                                return@saveEventToFirestore
                                            }
                                            val fallbackTitle =
                                                "✨ Fresh on EventGlow: ${eventName.ifBlank { "Untitled Event" }}"
                                            val fallbackBody = buildString {
                                                when {
                                                    startDate.isNotBlank() && eventTime.isNotBlank() ->
                                                        append("📅 $startDate at $eventTime is set.")

                                                    startDate.isNotBlank() ->
                                                        append("📅 $startDate is set.")

                                                    eventTime.isNotBlank() ->
                                                        append("⏰ $eventTime is set.")

                                                    else ->
                                                        append("It’s officially live.")
                                                }
                                                append(" Tap to check it out 🔎")
                                            }
                                            val notificationTitle = if (useCustomPublishNotification) {
                                                publishNotificationTitle.trim().ifBlank { fallbackTitle }
                                            } else {
                                                "New Event Published"
                                            }
                                            val notificationBody = if (useCustomPublishNotification) {
                                                publishNotificationBody.trim().ifBlank { fallbackBody }
                                            } else {
                                                eventName
                                            }
                                            senderViewModel.sendNotificationToRole(
                                                title = notificationTitle,
                                                body = notificationBody,
                                                targetRole = publishNotificationTargetRole,
                                                route = ROUTE_DETAILED_EVENT_SCREEN,
                                                eventId = savedEventId,
                                                onResult = { _, _ ->
                                                    pendingPublishedEventId = null
                                                    scope.launch { markEventsUpdatedAndReturn() }
                                                }
                                            )
                                        },
                                        onFailure = { e ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Failed to publish event: ${e.message}",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventNotificationSection(
    useCustomNotification: Boolean,
    notificationTitle: String,
    notificationBody: String,
    notificationTargetRole: String,
    onUseCustomNotificationChange: (Boolean) -> Unit,
    onNotificationTitleChange: (String) -> Unit,
    onNotificationBodyChange: (String) -> Unit,
    onNotificationTargetRoleChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Publish Notification",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Configure the push notification to send when this event is published.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Use custom notification")
                Switch(
                    checked = useCustomNotification,
                    onCheckedChange = onUseCustomNotificationChange
                )
            }
        }
        item {
            Text("Target Role", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all", "user", "admin").forEach { role ->
                    FilterChip(
                        selected = notificationTargetRole == role,
                        onClick = { onNotificationTargetRoleChange(role) },
                        label = { Text(role) }
                    )
                }
            }
        }
        if (useCustomNotification) {
            item {
                OutlinedTextField(
                    value = notificationTitle,
                    onValueChange = onNotificationTitleChange,
                    label = { Text("Custom Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = notificationBody,
                    onValueChange = onNotificationBodyChange,
                    label = { Text("Custom Body") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
private fun NavController.getBackStackEntryOrNull(route: String) =
    runCatching { getBackStackEntry(route) }.getOrNull()

@Composable
private fun CopyEventStateScreen(
    title: String,
    message: String,
    showLoading: Boolean = false,
    onPrimaryAction: (() -> Unit)? = null,
    primaryActionLabel: String = "",
    onSecondaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String = ""
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onPrimaryAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onPrimaryAction) {
                    Text(primaryActionLabel)
                }
            }
            if (onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onSecondaryAction) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun rememberCloudinarySampleImagesForCopy(
    categories: List<String>,
    samplesPerCategory: Int,
): Map<String, List<String>> {
    val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
    if (cloudName.isBlank() || cloudName == "YOUR_CLOUD_NAME") return emptyMap()

    val safeCount = samplesPerCategory.coerceAtLeast(1)
    return remember(cloudName, categories, safeCount) {
        categories.associateWith { categoryDisplay ->
            val token = categoryDisplay.replace(" ", "").trim()
            (1..safeCount).map { index ->
                "https://res.cloudinary.com/$cloudName/image/upload/${token}${index}.jpg"
            }
        }
    }
}

