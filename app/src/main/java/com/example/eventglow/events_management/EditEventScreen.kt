package com.example.eventglow.events_management

import android.app.TimePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.TimePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eventglow.dataClass.TicketType
import kotlinx.coroutines.launch
import java.util.*


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

    //retrives event from eventlist
    val event = viewModel.getEventById(eventId)
    println("Retrived id: $eventId")
    println("Retrived event: ${event?.eventName}")

    // Handle the UI when the event data is null
    if (event == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator() // Display loading indicator
            }
        }

    } else {

        // Variables to hold event details
        var eventName by remember { mutableStateOf(event.eventName) }
        var startDate by remember { mutableStateOf(event.startDate) }
        var endDate by remember { mutableStateOf(event.endDate) }
        var isMultiDayEvent by remember { mutableStateOf(event.isMultiDayEvent) }
        var eventTime by remember { mutableStateOf(event.eventTime) }
        var eventVenue by remember { mutableStateOf(event.eventVenue) }
        var eventStatus by remember { mutableStateOf(event.eventStatus) }
        var eventCategory by remember { mutableStateOf(event.eventCategory) }
        var ticketTypes by remember { mutableStateOf(event.ticketTypes) }
        var imageUri by remember { mutableStateOf<Uri?>(event.imageUri!!.toUri()) }
        var eventOrganizer by remember { mutableStateOf(event.eventOrganizer) }
        var eventDescription by remember { mutableStateOf(event.eventDescription) }

        var validationErrors = remember { mutableListOf<String>() }

        var eventNameAndDateError by remember { mutableStateOf<String?>(null) }


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
            if (eventVenue.isEmpty()) validationErrors.add("Event venue is required")
            if (eventStatus.isEmpty()) validationErrors.add("Event status is required")
            if (eventCategory.isEmpty()) validationErrors.add("Event category is required")
            if (eventOrganizer.isEmpty()) validationErrors.add("Event Organizer is required")
            if (eventDescription.isEmpty()) validationErrors.add("Event Description is required")
            if (ticketTypes.isEmpty()) validationErrors.add("At least one ticket Type is required")
            if (ticketTypes.any { it.price < 1 }) validationErrors.add("Ticket price must be at least 1")
            if (imageUri == null) validationErrors.add("Event image is required")
            if (eventNameAndDateError != null) validationErrors.add("$eventNameAndDateError")
        }


        // Variables to track the current selected tab
        var selectedTabIndex by remember { mutableIntStateOf(0) }

        // Snackbar state
        val snackbarHostState = remember { SnackbarHostState() }

        // Event Image picker launcher
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { imageUri = it }
        }

        // Real event images organized by categories
        val realEventImages = mapOf(
            "Concert" to listOf(
                "https://plus.unsplash.com/premium_photo-1681830630610-9f26c9729b75?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8MXx8Y29uY2VydHxlbnwwfHwwfHx8MA%3D%3D",
                "https://images.unsplash.com/photo-1429514513361-8fa32282fd5f?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8Y29uY2VydHxlbnwwfHwwfHx8MA%3D%3D",
                "https://plus.unsplash.com/premium_photo-1661306437817-8ab34be91e0c?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8OXx8Y29uY2VydHxlbnwwfHwwfHx8MA%3D%3D"
            ),
            "Conference" to listOf(
                "https://plus.unsplash.com/premium_photo-1664302656889-e0ff44331843?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8MXx8Q29uZmVyZW5jZXxlbnwwfHwwfHx8MA%3D%3D",
                "https://images.unsplash.com/photo-1477281765962-ef34e8bb0967?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Nnx8Q29uZmVyZW5jZXxlbnwwfHwwfHx8MA%3D%3D",
                "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8MTR8fENvbmZlcmVuY2V8ZW58MHx8MHx8fDA%3D"
            ),
            "Sports" to listOf(
                "https://images.unsplash.com/photo-1452626038306-9aae5e071dd3?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8MTh8fHNwb3J0JTIwZXZlbnRzfGVufDB8fDB8fHww",
                "https://images.unsplash.com/photo-1569863959165-56dae551d4fc?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8MTR8fHNwb3J0JTIwZXZlbnRzfGVufDB8fDB8fHww",
                "https://media.istockphoto.com/id/952397298/photo/friends-football-supporter-fans-cheering-with-confetti-watching-soccer-match-event-at-stadium.jpg?s=612x612&w=0&k=20&c=nVAQqWLkUW62OuHxj-d3PTHsljnekRGhutCjZIoASNs="
            )
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

                    // Edits a tab row
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                        0 -> EventDetailsSectionEdit(
                            eventName = eventName,
                            startDate = startDate,
                            endDate = endDate,
                            isMultiDayEvent = isMultiDayEvent,
                            eventTime = eventTime,
                            eventVenue = eventVenue,
                            eventStatus = eventStatus,
                            eventCategory = eventCategory,
                            eventOrganizer = eventOrganizer,
                            eventDescription = eventDescription,
                            onEventNameChange = { newEventName -> eventName = newEventName },
                            onStartDateChange = { newStartDate -> startDate = newStartDate },
                            onEndDateChange = { newEndDate -> endDate = newEndDate },
                            onIsMultiDayEventChange = { isMultiDayEvent = it },
                            onEventTimeChange = { newEventTime -> eventTime = newEventTime },
                            onEventVenueChange = { newEventVenue -> eventVenue = newEventVenue },
                            onEventCategoryChange = { newEventCategory -> eventCategory = newEventCategory },
                            onEventOrganizerChange = { newEventOrganizer -> eventOrganizer = newEventOrganizer },
                            onEventDescriptionChange = { newEventDescription ->
                                eventDescription = newEventDescription
                            },
                            eventNameAndDateError = { errorMessage -> eventNameAndDateError = errorMessage }
                        )

                        1 -> TicketDetailsSectionEdit(


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
                            onRemoveTicketType = { index ->

                                // filter through the ticket type list using their index and exclude the index of the ticketType we removing
                                ticketTypes = ticketTypes.filterIndexed { i, _ -> i != index }
                            }
                        )

                        2 -> EventImageSectionEdit(
                            imageUri = imageUri,
                            onPickImageClick = { imagePickerLauncher.launch("image/*") },
                            realEventImages = realEventImages,
                            onImageSelected = { selectedImageUri ->
                                imageUri = selectedImageUri
                            }
                        )

                        3 -> FinishSectionEdit(
                            eventName = eventName,
                            startDate = startDate,
                            endDate = endDate,
                            isMultiDayEvent = isMultiDayEvent,
                            eventTime = eventTime,
                            eventVenue = eventVenue,
                            eventStatus = eventStatus,
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
                                        eventVenue = eventVenue,
                                        eventStatus = eventStatus,
                                        eventCategory = eventCategory,
                                        eventOrganizer = eventOrganizer,
                                        eventDescription = eventDescription,
                                        ticketTypes = ticketTypes,
                                        imageUri = imageUri,
                                        isDraft = true,
                                        //when successful
                                        onSuccess = {
                                            scope.launch {
                                                //go back to Event Management Screen
                                                navController.popBackStack()
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
                                        eventVenue = eventVenue,
                                        eventStatus = eventStatus,
                                        eventCategory = eventCategory,
                                        eventOrganizer = eventOrganizer,
                                        eventDescription = eventDescription,
                                        ticketTypes = ticketTypes,
                                        imageUri = imageUri,
                                        isDraft = false,
                                        onSuccess = {
                                            scope.launch {
                                                //notificationViewModel.sendNewEventNotification()
                                                navController.popBackStack()
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
fun FinishSectionEdit(
    eventName: String,
    startDate: String,
    endDate: String,
    isMultiDayEvent: Boolean,
    eventTime: String,
    eventVenue: String,
    eventCategory: String,
    eventStatus: String,
    ticketTypes: List<TicketType>,
    imageUri: Uri?,
    eventOrganizer: String,
    eventDescription: String,
    onPublishClick: () -> Unit,
    onSaveDraftClick: () -> Unit
) {

    //For all content
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        //Title
        item {
            Text(
                "Review Event Details",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        //Events details Summary
        item {

            //Card for details
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {

                //column for content
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Event Name: $eventName", style = MaterialTheme.typography.bodyMedium)
                    Text("Event Description: $eventDescription", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Event Date: ${if (isMultiDayEvent) "$startDate - $endDate" else startDate}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Event Time: $eventTime", style = MaterialTheme.typography.bodyMedium)
                    Text("Event Venue: $eventVenue", style = MaterialTheme.typography.bodyMedium)
                    Text("Event Status: $eventStatus", style = MaterialTheme.typography.bodyMedium)
                    Text("Event Category: $eventCategory", style = MaterialTheme.typography.bodyMedium)
                    Text("Event Organizer: $eventOrganizer", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

        }

        // Display ticket Types summary
        item {

            //Card for details
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {

                //Column for content
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ticket Types:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)

                    //Display all ticket types
                    ticketTypes.forEach { Type ->
                        Text(
                            "Type Name: ${Type.name}, Price: ${Type.price}, Available Tickets: ${Type.availableTickets}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Display event image summary
        item {

            //Card for Image Data
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {

                //For content
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Event Image:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)

                    //Diplay Image
                    imageUri?.let {
                        Image(
                            painter = rememberAsyncImagePainter(model = it),
                            contentDescription = "Event Image",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } ?: Text("No image selected", style = MaterialTheme.typography.bodyMedium) //If no image is chosen
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Publish and Save Draft buttons
        item {

            //row for all content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = onSaveDraftClick) {
                    Icon(Icons.Filled.Save, contentDescription = "Save as Draft")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save as Draft")
                }
                Button(onClick = onPublishClick) {
                    Icon(Icons.Filled.Publish, contentDescription = "Publish")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsSectionEdit(
    eventName: String,
    startDate: String,
    endDate: String,
    isMultiDayEvent: Boolean,
    eventTime: String,
    eventVenue: String,
    eventStatus: String,
    onEventNameChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onIsMultiDayEventChange: (Boolean) -> Unit,
    onEventTimeChange: (String) -> Unit,
    onEventVenueChange: (String) -> Unit,
    onEventCategoryChange: (String) -> Unit,
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
    Log.e("Edit Event Screen", "Event Date error message initially set to :  $eventNameDateErrorLocal")


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
                            Log.e("Edit Event Screen", "Retrived boolean is Event Name  Taken: $isEventNameTaken")

                            //if event name is taken set error message or null
                            isEventNameExisting = isEventNameTaken
                            Log.e(
                                "Edit Event Screen",
                                "is Event Name Taken in name field set to:  $isEventNameExisting"
                            )
                        }
                    )

                    //call to check if event date is taken
                    viewModel.checkIfEventDateIsTaken(
                        eventDateLocal,
                        onResult = { isEventDateTaken ->
                            Log.e("Edit Event Screen", "Retrived boolean is Event Name  Taken: $isEventDateTaken")

                            //if event name is taken set error message or null
                            isEventDateExisting = isEventDateTaken
                            Log.e("Edit Event Screen", "Is Event Date Taken: $isEventDateExisting")

                            // Both name and date check return true
                            if (isEventNameExisting && isEventDateExisting) {
                                Log.e(
                                    "Edit Event Screen",
                                    "Both is name and date existing: $isEventNameExisting $isEventDateExisting"
                                )

                                // Set error message
                                eventNameDateErrorLocal = "An event with the same name and date already exists"

                                //set error message for global error variable
                                eventNameAndDateError(eventNameDateErrorLocal)
                            } else {

                                // Set error to null
                                eventNameDateErrorLocal = null
                                Log.e("Edit Event Screen", "Event Date error message set to: $eventNameDateErrorLocal")
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
                            Log.e("Edit Event Screen", "Retrived boolean is Event Name  Taken: $isEventNameTaken")

                            //if event name is taken set error message or null
                            isEventNameExisting = isEventNameTaken
                            Log.e(
                                "Edit Event Screen",
                                "is Event Name Taken in name field set to:  $isEventNameExisting"
                            )
                        }
                    )


                    //call to check if event dates are taken
                    viewModel.checkIfDateRangeExists(
                        eventStartDateLocal,
                        eventEndDateLocal,
                        onResult = { areEventDatesTaken ->
                            Log.e("Edit Event Screen", "Retrived boolean is Event Name Taken: $areEventDatesTaken")

                            //if event name is taken set error message or null
                            areEventDatesExisting = areEventDatesTaken
                            Log.e("Edit Event Screen", "are Event Dates Taken: $areEventDatesExisting")

                            // Both name and date check return true
                            if (isEventNameExisting && areEventDatesExisting) {
                                Log.e(
                                    "Edit Event Screen",
                                    "Both is name and are dates existing: $isEventNameExisting $areEventDatesExisting"
                                )

                                // Set error message
                                eventNameDateErrorLocal = "An event with the same name and dates already exists"

                                //set error message for global error variable
                                eventNameAndDateError(eventNameDateErrorLocal)
                            } else {

                                // Set error to null
                                eventNameDateErrorLocal = null
                                Log.e("Edit Event Screen", "Event Date error message set to: $eventNameDateErrorLocal")
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
                enabled = false,
                label = { Text("Event Status") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        //event category
        item {
            // List of event category options
            val eventCategoryOptions = listOf("Music", "Sports", "Tech", "Arts", "Health", "Other")

            var expanded by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = eventCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Event Category") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Drop Down")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenu(
                expanded = expanded, // Use the tracked state variable
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                eventCategoryOptions.forEach { category ->
                    DropdownMenuItem(
                        onClick = {
                            onEventCategoryChange(category) // Update event category
                            expanded = false // Close menu
                        },
                        text = { Text(text = category) } // Display the category text
                    )
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
fun TicketDetailsSectionEdit(
    ticketTypes: List<TicketType>,
    onAddTicketType: () -> Unit,
    onTicketTypeNameChange: (index: Int, newName: String) -> Unit,
    onTicketTypePriceChange: (index: Int, newPrice: String) -> Unit,
    onTicketTypeAvailableChange: (index: Int, newAvailable: String) -> Unit,
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
        LazyColumn {

            //diaplay only one ticket initialized at top
            items(ticketTypes.size) { index ->

                //Edits object of initialized ticket
                val ticketType = ticketTypes[index]

                TicketTypeItemEdit(
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
                    onRemoveTicketType = { onRemoveTicketType(index) } //call to remove ticket
                )

                Spacer(modifier = Modifier.height(16.dp))

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        //Add Ticket Button
        Button(
            onClick = onAddTicketType,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Ticket Type")
            Text("Add Ticket Type", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun TicketTypeItemEdit(
    ticketType: TicketType,
    onTicketTypeNameChange: (newName: String) -> Unit,
    onTicketTypePriceChange: (newPrice: String) -> Unit,
    onTicketTypeAvailableChange: (newAvailable: String) -> Unit,
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

                //Show only number keyboard type
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

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
fun EventImageSectionEdit(
    imageUri: Uri?,
    onPickImageClick: () -> Unit,
    realEventImages: Map<String, List<String>>, // Map of category to image
    onImageSelected: (Uri) -> Unit // Callback to handle image selection
) {

    //for all content
    LazyColumn(modifier = Modifier.padding(16.dp)) {

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
                Button(onClick = onPickImageClick) {
                    Text("Pick Image")
                }
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
                        ChosenImageCardEdit(imageUri)
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
                        RealEventImageCardEdit(images[index], onImageSelected)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ChosenImageCardEdit(uri: Uri) {
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
fun RealEventImageCardEdit(
    imageUrl: String,
    onImageSelected: (Uri) -> Unit // Callback to notify parent about image selection
) {

    Card(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(120.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
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
fun EventImageSectionEditPreview() {
    EventImageSectionEdit(
        imageUri = null,
        onPickImageClick = {},
        realEventImages = mapOf(
            "Concerts" to listOf(""),
            "Conferences" to listOf("")
        ),
        onImageSelected = {}
    )
}

