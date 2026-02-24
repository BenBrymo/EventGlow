package com.example.eventglow.events_management

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(
    navController: NavController,
    viewModel: EventsManagementViewModel = viewModel(),
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
        EditEventStateScreen(
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
                EditEventStateScreen(
                    title = "Loading Event",
                    message = "Please wait while we load event details.",
                    showLoading = true
                )
            }

            is FetchEventsState.Failure -> {
                EditEventStateScreen(
                    title = "Could Not Load Event",
                    message = state.errorMessage,
                    onPrimaryAction = { viewModel.fetchEvents() },
                    primaryActionLabel = "Retry",
                    onSecondaryAction = { navController.popBackStack() },
                    secondaryActionLabel = "Go Back"
                )
            }

            is FetchEventsState.Success -> {
                EditEventStateScreen(
                    title = "Event Not Found",
                    message = "This event may have been deleted or is no longer available.",
                    onPrimaryAction = { navController.popBackStack() },
                    primaryActionLabel = "Go Back"
                )
            }
        }
    } else {

        // Variables to hold event details
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
        var imageUri by remember { mutableStateOf(event.imageUri?.takeIf { it.isNotBlank() }?.toUri()) }
        var eventOrganizer by remember { mutableStateOf(event.eventOrganizer) }
        var eventDescription by remember { mutableStateOf(event.eventDescription) }
        var isUploadingImage by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val eventCategories by viewModel.eventCategories.collectAsState()
        val categoriesList = remember(eventCategories) { eventCategories.map { it.name } }

        var validationErrors = remember { mutableListOf<String>() }

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

        LaunchedEffect(Unit) {
            viewModel.fetchEventCategories()
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

        val realEventImages = rememberCloudinarySampleImagesForEdit(
            categories = categoriesList,
            samplesPerCategory = 3
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Event", fontWeight = FontWeight.Bold) },
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
                            text = { Text("Finish") },
                            selected = selectedTabIndex == 3, //set selected to true
                            onClick = { selectedTabIndex = 3 }  // selects Finish section
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
                                    //and if it exist Edits a copy of it with the new Name else keeps the ticket as it is
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

                        3 -> FinishSection(
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
                            showSaveDraftButton = false,
                            publishButtonText = "Update & Publish",
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
                                        onSuccess = {
                                            scope.launch {
                                                markEventsUpdatedAndReturn()
                                            }
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

@Composable
private fun EditEventStateScreen(
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
private fun NavController.getBackStackEntryOrNull(route: String) =
    runCatching { getBackStackEntry(route) }.getOrNull()

@Composable
private fun rememberCloudinarySampleImagesForEdit(
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

