package com.example.eventglow.events_management

import android.app.TimePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.TimePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.BuildConfig
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.navigation.Routes
import com.example.eventglow.ui.theme.EventGlowTheme
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt


private const val CLOUDINARY_DEBUG_TAG = "CreateEventCloudinary"


private fun formatDurationLabel(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours > 0 && remainingMinutes > 0 -> "${hours} hr ${remainingMinutes} min"
        hours > 0 -> "${hours} hr"
        else -> "${minutes} min"
    }
}



@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    navController: NavController,
    viewModel: EventsManagementViewModel = viewModel(),
) {

    // Remember coroutine scope for performing asynchronous actions
    val scope = rememberCoroutineScope()


    // Variables to hold event details
    var eventName by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var isMultiDayEvent by remember { mutableStateOf(false) }
    var eventTime by remember { mutableStateOf("") }
    var durationLabel by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableIntStateOf(0) }
    var eventVenue by remember { mutableStateOf("") }
    var eventStatus by remember { mutableStateOf("Upcoming") }
    var isImportant by remember { mutableStateOf(false) }
    var eventCategory by remember { mutableStateOf("") } // Add eventCategory
    var ticketTypes by remember { mutableStateOf(listOf(TicketType("", 0.0, 0))) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var eventOrganizer by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var validationErrors = remember { mutableListOf<String>() }

    var eventNameAndDateError by remember { mutableStateOf<String?>(null) }
    val eventCategories by viewModel.eventCategories.collectAsState()
    val categoriesList = remember(eventCategories) { eventCategories.map { it.name } }


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
        if (ticketTypes.any { !it.isFree && it.price < 1 }) {
            validationErrors.add("Paid ticket price must be at least 1 GHS")
        }
        if (imageUri == null) validationErrors.add("Event image is required")
        if (eventNameAndDateError != null) validationErrors.add("$eventNameAndDateError")
    }


    // Variables to track the current selected tab
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Event Image picker launcher
    val context = LocalContext.current
    var isUploadingImage by remember { mutableStateOf(false) }
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

    fun markEventsUpdatedAndReturn() {
        navController.previousBackStackEntry?.savedStateHandle?.set("events_updated", true)
        navController.getBackStackEntryOrNull(Routes.ADMIN_MAIN_SCREEN)
            ?.savedStateHandle
            ?.set("events_updated", true)
        navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        viewModel.fetchEventCategories()
    }

    // Real event images organized by categories
    val realEventImages = rememberCloudinarySampleImages(
        categories = categoriesList,
        samplesPerCategory = 3
    )

    EventGlowTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Create Event", fontWeight = FontWeight.Bold) },
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

                    // creates a tab row
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
                                        scope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
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
                                    //and if it exist creates a copy of it with the new Name else keeps the ticket as it is
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
                                                //notificationViewModel.sendNewEventNotification()
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
fun FinishSection(
    eventName: String,
    startDate: String,
    endDate: String,
    isMultiDayEvent: Boolean,
    eventTime: String,
    durationLabel: String,
    eventVenue: String,
    eventCategory: String,
    eventStatus: String,
    isImportant: Boolean,
    ticketTypes: List<TicketType>,
    imageUri: Uri?,
    eventOrganizer: String,
    eventDescription: String,
    onPublishClick: () -> Unit,
    onSaveDraftClick: () -> Unit
) {
    val eventDateLabel = if (isMultiDayEvent) "$startDate - $endDate" else startDate

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review Event Details",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Event Summary",
                        style = MaterialTheme.typography.titleLarge
                    )
                    ReviewDetailRow("Event Name", eventName)
                    ReviewDetailRow("Description", eventDescription)
                    ReviewDetailRow("Date", eventDateLabel)
                    ReviewDetailRow("Time", eventTime)
                    ReviewDetailRow("Duration", durationLabel)
                    ReviewDetailRow("Venue", eventVenue)
                    ReviewDetailRow("Status", eventStatus)
                    ReviewDetailRow("Featured Event", if (isImportant) "Yes" else "No")
                    ReviewDetailRow("Category", eventCategory)
                    ReviewDetailRow("Organizer", eventOrganizer)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            )
            {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ticket Types",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (ticketTypes.isEmpty()) {
                        Text(
                            text = "No ticket types added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(ticketTypes.size) { index ->
                                TicketTypeSummaryCard(ticketType = ticketTypes[index])
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Event Image",
                        style = MaterialTheme.typography.titleLarge
                    )
                    imageUri?.let {
                        Image(
                            painter = rememberAsyncImagePainter(model = it),
                            contentDescription = "Event Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } ?: run {
                        Text(
                            text = "No image selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSaveDraftClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save as Draft")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
                Button(
                    onClick = onPublishClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Publish, contentDescription = "Publish")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish")
                }
            }
        }
    }
}

@Composable
private fun ReviewDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun TicketTypeSummaryCard(ticketType: TicketType) {
    val isFreeTicket = ticketType.isFree || ticketType.price <= 0.0
    val priceLabel = if (isFreeTicket) "Free" else "GHS ${"%.2f".format(ticketType.price)}"

    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.width(180.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = ticketType.name.ifBlank { "Unnamed Ticket" },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Available: ${ticketType.availableTickets}",
                style = MaterialTheme.typography.bodyMedium
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isFreeTicket) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Text(
                    text = priceLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isFreeTicket) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsSection(
    eventName: String,
    startDate: String,
    endDate: String,
    isMultiDayEvent: Boolean,
    eventTime: String,
    durationLabel: String,
    durationMinutes: Int,
    eventVenue: String,
    eventStatus: String,
    isImportant: Boolean,
    onEventNameChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onIsMultiDayEventChange: (Boolean) -> Unit,
    onEventTimeChange: (String) -> Unit,
    onDurationChange: (String, Int) -> Unit,
    onEventVenueChange: (String) -> Unit,
    onImportantChange: (Boolean) -> Unit,
    onEventCategoryChange: (String) -> Unit,
    eventCategoryOptions: List<String>,
    onAddEventCategory: (String) -> Unit,
    onEventOrganizerChange: (String) -> Unit,
    onEventDescriptionChange: (String) -> Unit,
    eventCategory: String,
    eventOrganizer: String,
    eventDescription: String,
    viewModel: EventsManagementViewModel = viewModel(),
    eventNameAndDateError: (String?) -> Unit
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    //to show or not Date Picker
    val showDatePicker = remember { mutableStateOf(false) }
    // to set whether date picking is for a start or end date
    val datePickerType = remember { mutableStateOf("start") }


    // to help trim name to get right result in checking if name already exists
    var eventNameLocal by remember { mutableStateOf("") }

    // to help trim name to get right result in checking if name already exists
    var eventDateLocal by remember { mutableStateOf("") }

    // to help trim name to get right result in checking if name already exists
    var eventStartDateLocal by remember { mutableStateOf("") }

    // to help trim name to get right result in checking if name already exists
    var eventEndDateLocal by remember { mutableStateOf("") }

    //set is event name existing
    var isEventNameExisting by remember { mutableStateOf(false) }
    //set is event date existing
    var isEventDateExisting by remember { mutableStateOf(false) }
    //set is event start date existing
    var areEventDatesExisting by remember { mutableStateOf(false) }
    //set is event end date existing
    var isEventMutiDayEventLocal by remember { mutableStateOf(false) }

    //to display error where an event has the same name and date
    var eventNameDateErrorLocal by remember { mutableStateOf<String?>(null) }
    Log.e("Create Event Screen", "Event Date error message initially set to :  $eventNameDateErrorLocal")


    fun checkNameIfAndDateAlreadyExists() {

        //is event is a single event
        if (!isEventMutiDayEventLocal) {

            //only check name and date are not empty
            if (eventNameLocal != "" && eventDateLocal != "") {
                scope.launch {

                    //call to check if event name is taken
                    viewModel.checkIfEventNameIsTaken(
                        eventNameLocal,
                        onResult = { isEventNameTaken ->
                            Log.e("Create Event Screen", "Retrived boolean is Event Name  Taken: $isEventNameTaken")

                            //if event name is taken set error message or null
                            isEventNameExisting = isEventNameTaken
                            Log.e(
                                "Create Event Screen",
                                "is Event Name Taken in name field set to:  $isEventNameExisting"
                            )
                        }
                    )

                    //call to check if event date is taken
                    viewModel.checkIfEventDateIsTaken(
                        eventDateLocal,
                        onResult = { isEventDateTaken ->
                            Log.e("Create Event Screen", "Retrived boolean is Event Name  Taken: $isEventDateTaken")

                            //if event name is taken set error message or null
                            isEventDateExisting = isEventDateTaken
                            Log.e("Create Event Screen", "Is Event Date Taken: $isEventDateExisting")

                            // Both name and date check return true
                            if (isEventNameExisting && isEventDateExisting) {
                                Log.e(
                                    "Create Event Screen",
                                    "Both is name and date existing: $isEventNameExisting $isEventDateExisting"
                                )

                                // Set error message
                                eventNameDateErrorLocal = "An event with the same name and date already exists"

                                //set error message for global error variable
                                eventNameAndDateError(eventNameDateErrorLocal)
                            } else {

                                // Set error to null
                                eventNameDateErrorLocal = null
                                Log.e(
                                    "Create Event Screen",
                                    "Event Date error message set to: $eventNameDateErrorLocal"
                                )
                            }
                        }
                    )
                }
            }
            //when event is a multi day event
        } else {

            //only check name and start date are end date not empty
            if (eventNameLocal != "" && eventStartDateLocal != "" && eventEndDateLocal != "") {
                scope.launch {

                    //call to check if event name is taken
                    viewModel.checkIfEventNameIsTaken(
                        eventNameLocal,
                        onResult = { isEventNameTaken ->
                            Log.e("Create Event Screen", "Retrived boolean is Event Name  Taken: $isEventNameTaken")

                            //if event name is taken set error message or null
                            isEventNameExisting = isEventNameTaken
                            Log.e(
                                "Create Event Screen",
                                "is Event Name Taken in name field set to:  $isEventNameExisting"
                            )
                        }
                    )


                    //call to check if event dates are taken
                    viewModel.checkIfDateRangeExists(
                        eventStartDateLocal,
                        eventEndDateLocal,
                        onResult = { areEventDatesTaken ->
                            Log.e("Create Event Screen", "Retrived boolean is Event Name Taken: $areEventDatesTaken")

                            //if event name is taken set error message or null
                            areEventDatesExisting = areEventDatesTaken
                            Log.e("Create Event Screen", "are Event Dates Taken: $areEventDatesExisting")

                            // Both name and date check return true
                            if (isEventNameExisting && areEventDatesExisting) {
                                Log.e(
                                    "Create Event Screen",
                                    "Both is name and are dates existing: $isEventNameExisting $areEventDatesExisting"
                                )

                                // Set error message
                                eventNameDateErrorLocal = "An event with the same name and dates already exists"

                                //set error message for global error variable
                                eventNameAndDateError(eventNameDateErrorLocal)
                            } else {

                                // Set error to null
                                eventNameDateErrorLocal = null
                                Log.e(
                                    "Create Event Screen",
                                    "Event Date error message set to: $eventNameDateErrorLocal"
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // Function to show time picker
    val showTimePicker = {

        //instance of Calendar Class to display current hour and minute
        val calendar = Calendar.getInstance()

        //setup a TimerPickerDialog
        val timePickerDialog = TimePickerDialog(
            context,

            //returns the selected hour and minute chosen by user
            { _: TimePicker, hour: Int, minute: Int ->

                //call back to check if event name and date are already existing and set error message
                checkNameIfAndDateAlreadyExists()

                // Convert the selected time to AM/PM format
                val amPm = if (hour < 12) "AM" else "PM"

                //convert to 12-hour format because it is in 24-hour format
                val hourIn12Format = if (hour % 12 == 0) 12 else hour % 12

                //pass hour and minute to callback
                onEventTimeChange("$hourIn12Format:$minute $amPm")
            },

            //set initial curent hour
            calendar.get(Calendar.HOUR_OF_DAY),

            //set the initial current minute
            calendar.get(Calendar.MINUTE),

            // use 12-hour system (AM/PM)
            false
        )

        //Show TimePicker dialog
        timePickerDialog.show()
    }

    val minDuration = 30
    val maxDuration = 720
    val durationStep = 15
    val sliderSteps = ((maxDuration - minDuration) / durationStep) - 1
    var durationSliderValue by remember {
        mutableFloatStateOf((if (durationMinutes > 0) durationMinutes else 60).toFloat())
    }


    //For all content
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //Title
        item {
            Text("Event Details", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        //event name
        item {


            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName ->
                    eventNameLocal = eventName.trim()
                    checkNameIfAndDateAlreadyExists()
                    onEventNameChange(eventName)
                },
                label = { Text("Event Name") },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        //event Description
        item {
            OutlinedTextField(
                value = eventDescription,
                onValueChange = onEventDescriptionChange,
                label = { Text("Event Description") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Radio button to indicate multi-day event
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isMultiDayEvent,
                    onCheckedChange = { isChecked ->
                        // Toggle the multi-day event state
                        onIsMultiDayEventChange(isChecked)
                        isEventMutiDayEventLocal = isChecked

                        // Clear any errors when toggling
                        eventNameDateErrorLocal = null
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Multiple event dates")
            }
        }

// Conditional rendering of date fields based on multi-day event selection
        if (isMultiDayEvent) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = onStartDateChange,
                            readOnly = true,
                            label = { Text("Start Date") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    datePickerType.value = "start"
                                    showDatePicker.value = true
                                }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick Start Date")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = endDate,
                            onValueChange = onEndDateChange,
                            readOnly = true,
                            label = { Text("End Date") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    datePickerType.value = "end"
                                    showDatePicker.value = true
                                }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick End Date")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    if (eventNameDateErrorLocal != null) {
                        Text(
                            text = eventNameDateErrorLocal!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            // Single date text field when event is not multi-day
            item {
                Column {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = onStartDateChange,
                        readOnly = true,
                        label = { Text("Event Date") },
                        trailingIcon = {
                            IconButton(onClick = {
                                datePickerType.value = "start"
                                showDatePicker.value = true
                            }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))

                    // Displays name and date error message
                    if (eventNameDateErrorLocal != null) {
                        Text(
                            text = eventNameDateErrorLocal!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // event time
        item {
            OutlinedTextField(
                value = eventTime,
                onValueChange = {},
                readOnly = true,
                label = { Text("Event Time") },
                trailingIcon = {
                    IconButton(onClick = showTimePicker) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Pick Time")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        // event duration
        item {
            Text(
                text = "Event Duration",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = durationSliderValue,
                onValueChange = { rawValue ->
                    val snapped = (rawValue / durationStep).roundToInt() * durationStep
                    val bounded = snapped.coerceIn(minDuration, maxDuration)
                    durationSliderValue = bounded.toFloat()
                    onDurationChange(formatDurationLabel(bounded), bounded)
                },
                valueRange = minDuration.toFloat()..maxDuration.toFloat(),
                steps = sliderSteps,
                modifier = Modifier.fillMaxWidth()
            )
            if (durationMinutes > 0) {
                Text(
                    text = "Selected: $durationLabel ($durationMinutes min)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        //event venue
        item {
            OutlinedTextField(
                value = eventVenue,
                onValueChange = onEventVenueChange,
                label = { Text("Event Venue") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        //event status
        item {
            OutlinedTextField(
                value = eventStatus,
                onValueChange = {},
                readOnly = true,
                label = { Text("Event Status") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        text = "Status is managed automatically.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
        // featured event toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mark as Featured", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Featured events can be shown on user home feeds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isImportant,
                    onCheckedChange = onImportantChange
                )
            }
        }
        //event category
        item {
            var expanded by remember { mutableStateOf(false) }
            var newCategoryName by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                OutlinedTextField(
                    value = eventCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Event Category") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Category List"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            if (eventCategoryOptions.isEmpty()) {
                                Text(
                                    text = "No categories available yet. Add one below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            } else {
                                eventCategoryOptions.forEach { category ->
                                    val isSelected = eventCategory == category
                                    Text(
                                        text = category,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onEventCategoryChange(category)
                                                expanded = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCategoryName,
                                    onValueChange = { newCategoryName = it },
                                    label = { Text("Add new category") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val category = newCategoryName.trim()
                                        if (category.isNotBlank()) {
                                            onAddEventCategory(category)
                                            newCategoryName = ""
                                        }
                                    },
                                    enabled = newCategoryName.trim().isNotBlank()
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            }
        }
        // event organizer
        item {
            OutlinedTextField(
                value = eventOrganizer,
                onValueChange = onEventOrganizerChange,
                label = { Text("Event Organizer") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Shows date picker is showDatePicker is true
    if (showDatePicker.value) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val newDate = "$day/${month + 1}/$year"
                if (datePickerType.value == "start") {
                    // Assign formatted date to local variable
                    eventDateLocal = newDate

                    // Assign formatted date to local start date variable
                    eventStartDateLocal = newDate

                    checkNameIfAndDateAlreadyExists()

                    // Assign to global callback
                    onStartDateChange(newDate)
                } else {
                    // Assign formatted date to local end date variable
                    eventEndDateLocal = newDate

                    checkNameIfAndDateAlreadyExists()
                    onEndDateChange(newDate)
                }
                showDatePicker.value = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.setOnCancelListener {
            // Reset the date picker type when canceled
            datePickerType.value = "start"
        }
        // Set the minimum date to the current date
        datePickerDialog.datePicker.minDate = calendar.timeInMillis // Sets minimum date to current date
        datePickerDialog.show()
        showDatePicker.value = false // Reset the state after showing the date picker
    }

}


@Composable
fun TicketDetailsSection(
    ticketTypes: List<TicketType>,
    onAddTicketType: () -> Unit,
    onTicketTypeNameChange: (index: Int, newName: String) -> Unit,
    onTicketTypePriceChange: (index: Int, newPrice: String) -> Unit,
    onTicketTypeAvailableChange: (index: Int, newAvailable: String) -> Unit,
    onTicketTypeFreeChange: (index: Int, isFree: Boolean) -> Unit,
    onRemoveTicketType: (index: Int) -> Unit
) {

    //for all content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        //Title
        Text(
            text = "Ticket Details",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        //For tickets
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {

            //diaplay only one ticket initialized at top
            items(ticketTypes.size) { index ->

                //creates object of initialized ticket
                val ticketType = ticketTypes[index]

                TicketTypeItem(
                    ticketType = ticketType,
                    onTicketTypeNameChange = { newName ->
                        onTicketTypeNameChange(
                            index,
                            newName
                        )
                    }, //call to name in a particular index in ticket type list
                    onTicketTypePriceChange = { newPrice ->
                        onTicketTypePriceChange(
                            index,
                            newPrice
                        )
                    }, //call to price in a particular index in ticket type list
                    onTicketTypeAvailableChange = { newAvailable ->
                        onTicketTypeAvailableChange(
                            index,
                            newAvailable
                        )
                    }, //call to available in a particular index in ticket type list
                    onTicketTypeFreeChange = { isFree ->
                        onTicketTypeFreeChange(index, isFree)
                    },
                    onRemoveTicketType = { onRemoveTicketType(index) } //call to remove ticket
                )

                Spacer(modifier = Modifier.height(16.dp))

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        //Add Ticket Button
        Button(
            onClick = onAddTicketType,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .navigationBarsPadding()
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Ticket Type")
            Text("Add Ticket Type", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun TicketTypeItem(
    ticketType: TicketType,
    onTicketTypeNameChange: (newName: String) -> Unit,
    onTicketTypePriceChange: (newPrice: String) -> Unit,
    onTicketTypeAvailableChange: (newAvailable: String) -> Unit,
    onTicketTypeFreeChange: (isFree: Boolean) -> Unit,
    onRemoveTicketType: () -> Unit
) {

    //tickPrice retrived as string
    var ticketPrice by remember { mutableStateOf(ticketType.price.toString()) }

    //available tickets retrived as String
    var availableTickets by remember { mutableStateOf(ticketType.availableTickets.toString()) }

    //For all content
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {

        //for Ticket details
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            TextField(
                value = ticketType.name,
                onValueChange = onTicketTypeNameChange,
                label = { Text("Ticket Type Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = ticketPrice,
                onValueChange = { newPrice ->
                    ticketPrice = newPrice

                    // Only update price when newPrice is not empty and contains a number
                    if (newPrice.isEmpty() || newPrice.toDoubleOrNull() != null) {
                        onTicketTypePriceChange(newPrice) //call to update ticket price
                    }
                },
                label = { Text("Price") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        //when field is focussed
                        if (it.isFocused && ticketPrice == "0.0") {

                            // Clear default value
                            ticketPrice = ""
                        }
                    },
                enabled = !ticketType.isFree,

                //Show only number keyboard type
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Free Ticket", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = ticketType.isFree,
                    onCheckedChange = { isFree ->
                        onTicketTypeFreeChange(isFree)
                        if (isFree) {
                            ticketPrice = "0"
                            onTicketTypePriceChange("0")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = availableTickets,
                onValueChange = { newAvailable ->
                    availableTickets = newAvailable


                    // Only update price when availableTickets is not empty and contains a number
                    if (newAvailable.isEmpty() || newAvailable.toIntOrNull() != null) {
                        onTicketTypeAvailableChange(newAvailable)
                    }
                },
                label = { Text("Available Tickets") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        //when field is focussed
                        if (it.isFocused && availableTickets == "0") {

                            // Clear default value
                            availableTickets = ""
                        }
                    },

                //Show only number keyboard type
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            //Remove Ticket Button
            Button(
                onClick = onRemoveTicketType,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Ticket Type")
                Text("Remove", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventImageSection(
    imageUri: Uri?,
    isUploadingImage: Boolean,
    onPickImageClick: () -> Unit,
    realEventImages: Map<String, List<String>>, // Map of category to image
    onImageSelected: (Uri) -> Unit // Callback to handle image selection
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        //for all content
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .alpha(if (isUploadingImage) 0.45f else 1f),
            state = listState
        ) {

        //Adding Image section
        item {

            // for all content
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Add Images",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                //Add Image Button
                Button(onClick = onPickImageClick, enabled = !isUploadingImage) {
                    Text("Pick Image")
                }
            }
            if (isUploadingImage) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Uploading image to cloud...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Display chosen images
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {

                //chosen image is not null
                if (imageUri != null) {

                    //Chosen image
                    item(imageUri) {
                        ChosenImageCard(imageUri)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        //Sample Images Title
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Sample Images",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

            }
            Spacer(modifier = Modifier.height(5.dp))
        }

        //Real event images
        item {
            // Real event images organized by categories
            if (realEventImages.isEmpty()) {
                Text(
                    text = "No Cloudinary sample images configured yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                realEventImages.forEach { (category, images) ->
                    Text(
                        text = category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {

                        //display real event images
                        items(images.size) { index ->
                            RealEventImageCard(images[index]) { uri ->
                                onImageSelected(uri)
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        }

        AnimatedVisibility(
            visible = isUploadingImage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Uploading image...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Please wait",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun NavController.getBackStackEntryOrNull(route: String): NavBackStackEntry? {
    return runCatching { getBackStackEntry(route) }.getOrNull()
}


@Composable
private fun rememberCloudinarySampleImages(
    categories: List<String>,
    samplesPerCategory: Int,
): Map<String, List<String>> {
    val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
    if (cloudName.isBlank() || cloudName == "YOUR_CLOUD_NAME") return emptyMap()

    val safeCount = samplesPerCategory.coerceAtLeast(1)
    Log.d(
        CLOUDINARY_DEBUG_TAG,
        "Building sample urls with cloudName='$cloudName', categories=$categories, count=$safeCount"
    )

    return remember(cloudName, categories, safeCount) {
        categories.associateWith { categoryDisplay ->
            val token = categoryDisplay.replace(" ", "").trim()

            // Deterministic list: exactly N samples per category.
            val distinctUrls = (1..safeCount).map { index ->
                "https://res.cloudinary.com/$cloudName/image/upload/${token}${index}.jpg"
            }
            Log.d(
                CLOUDINARY_DEBUG_TAG,
                "Category '$categoryDisplay' generated ${distinctUrls.size} url candidates. First candidates=${
                    distinctUrls.take(
                        8
                    )
                }"
            )
            distinctUrls
        }
    }
}

@Composable
fun ChosenImageCard(uri: Uri) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(120.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {

            //Image
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Chosen Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


@Composable
fun RealEventImageCard(
    imageUrl: String,
    onImageSelected: (Uri) -> Unit // Callback to notify parent about image selection
) {
    val painter = rememberAsyncImagePainter(imageUrl)

    LaunchedEffect(imageUrl, painter.state) {
        when (val state = painter.state) {
            is AsyncImagePainter.State.Success -> {
                Log.d(CLOUDINARY_DEBUG_TAG, "Image load success: $imageUrl")
            }

            is AsyncImagePainter.State.Error -> {
                Log.e(
                    CLOUDINARY_DEBUG_TAG,
                    "Image load failed: $imageUrl, throwable=${state.result.throwable.message}",
                    state.result.throwable
                )
            }

            is AsyncImagePainter.State.Loading -> {
                Log.d(CLOUDINARY_DEBUG_TAG, "Image loading: $imageUrl")
            }

            is AsyncImagePainter.State.Empty -> Unit
        }
    }

    Card(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(120.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Image(
            painter = painter,
            contentDescription = "Real Event Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()

                //makes card clickable
                .clickable {

                    // Convert imageUrl to Uri
                    val uri = Uri.parse(imageUrl)

                    // Notify parent about image selection
                    onImageSelected(uri)
                }
        )
    }
}


@Preview(showBackground = true, apiLevel = 34)
@Composable
fun CreateEventScreenPreview() {
    EventImageSection(
        imageUri = null,
        isUploadingImage = false,
        onPickImageClick = {},
        realEventImages = mapOf(
            "Concerts" to listOf(""),
            "Conferences" to listOf("")
        ),
        onImageSelected = {}
    )
}

