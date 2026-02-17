package com.example.eventglow.events_management

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.TicketType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*


class EventsManagementViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _draftedEvents = MutableStateFlow<List<Event>>(emptyList())
    val draftedEvents: StateFlow<List<Event>> = _draftedEvents.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredEvents = MutableStateFlow<List<Event>>(emptyList())
    val filteredEvents: StateFlow<List<Event>> = _filteredEvents.asStateFlow()

    private val _filteredEventsAdvanced = MutableStateFlow<List<Event>>(emptyList())
    val filteredEventsAdvanced: StateFlow<List<Event>> = _filteredEventsAdvanced.asStateFlow()

    private val _fetchEventsState = MutableStateFlow<FetchEventsState>(FetchEventsState.Loading)
    val fetchEventsState: StateFlow<FetchEventsState> = _fetchEventsState

    val db = FirebaseFirestore.getInstance()

    init {
        fetchEvents()
    }

    private fun filterEvents(query: String) {
        val filteredList = _events.value.filter { event ->
            event.eventName.contains(query, ignoreCase = true) ||
                    event.eventVenue.contains(query, ignoreCase = true)
        }
        _filteredEvents.value = filteredList
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            filterEvents(query)
        } else {
            _filteredEvents.value = emptyList()
        }
    }

    fun filterEventsAdvanced(
        eventStatus: String,
        eventCategories: List<String>,
        eventStartDate: String,
        eventEndDate: String,
        matchAllCriteria: Boolean
    ) {
        // Log the filtering criteria
        Log.d(
            "FilterEvents",
            "Filter Criteria - Status: $eventStatus, Categories: $eventCategories, Start Date: $eventStartDate, End Date: $eventEndDate, Match All: $matchAllCriteria"
        )

        val filteredListAdvanced = _events.value.filter { event ->
            val matchesStatus = eventStatus.isEmpty() || event.eventStatus == eventStatus
            val matchesCategory = eventCategories.isEmpty() || eventCategories.contains(event.eventCategory)
            val matchesStartDate = eventStartDate.isEmpty() || event.startDate == eventStartDate
            val matchesEndDate = eventEndDate.isEmpty() || event.endDate == eventEndDate

            val matchesAll = matchesStatus && matchesCategory && matchesStartDate && matchesEndDate
            val matchesAny = matchesStatus || matchesCategory || matchesStartDate || matchesEndDate

            if (matchAllCriteria) matchesAll else matchesAny
        }

        // Log the filtered results
        Log.d("FilterEvents", "Filtered Events Count: ${filteredListAdvanced.size}")
        _filteredEventsAdvanced.value = filteredListAdvanced
        Log.d("FilterEvents", "Filtered Events in count in filteredEvent state: ${_filteredEventsAdvanced.value.size}")
    }

    fun fetchEvents() {

        //fetchEventsState initially set to loading
        _fetchEventsState.value = FetchEventsState.Loading

        //selects event collection in database
        db.collection("events")

            //gets all events
            .get()

            //listens for a completion response
            .addOnSuccessListener { result ->

                //to keep drafted events list
                val draftedList = mutableListOf<Event>()

                //to keep published events list
                val nonDraftedList = mutableListOf<Event>()

                //iterate through result data
                for (document in result) {

                    //hold map of document data
                    val data = document.data
                    Log.d("FirestoreData", "Document ID: ${document.id}, Data: $data")

                    // Manually map Firestore data to Event class
                    val event = Event(
                        id = document.id,
                        //cast eventName Data as string or if its null give default value
                        eventName = data["eventName"] as? String ?: "",
                        startDate = data["startDate"] as? String ?: "",
                        endDate = data["endDate"] as? String ?: "",
                        isMultiDayEvent = data["isMultiDayEvent"] as? Boolean ?: false,
                        eventTime = data["eventTime"] as? String ?: "",
                        eventVenue = data["eventVenue"] as? String ?: "",
                        eventStatus = data["eventStatus"] as? String ?: "",
                        eventCategory = data["eventCategory"] as? String ?: "",
                        //cast ticketTypes data as a list of types maps and create a new list from retrived data
                        ticketTypes = (data["ticketTypes"] as? List<Map<String, Any>>)?.map {
                            TicketType(
                                name = it["name"] as? String ?: "",
                                price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                                availableTickets = (it["availableTickets"] as? Number)?.toInt() ?: 0
                            )
                        } ?: listOf(),  // if returned data is null give default value of empty list
                        imageUri = data["imageUri"] as? String ?: "",
                        isDraft = data["isDraft"] as? Boolean ?: false,
                        eventOrganizer = data["eventOrganizer"] as? String ?: "",
                        eventDescription = data["eventDescription"] as? String ?: "",
                    )

                    Log.d("FirestoreEvent", "Event ID: ${event.id}, Is Draft: ${event.isDraft}")


                    //if event is a drafted event add to drafted list or nonDrafted list
                    if (event.isDraft) {
                        draftedList.add(event)
                    } else {
                        nonDraftedList.add(event)
                    }

                    // Determine the new status of the event
                    val newStatus = when {
                        eventShouldBeEnded(event) -> "ended"
                        eventShouldBeOngoing(event) -> "ongoing"
                        else -> event.eventStatus
                    }

                    // Update the event status if necessary
                    if (newStatus != event.eventStatus) {
                        updateEventStatus(event.id, newStatus)

                        // Update local lists
                        val updatedEvent = event.copy(eventStatus = newStatus)
                        if (event.isDraft) {
                            draftedList.remove(event)
                            draftedList.add(updatedEvent)
                        } else {
                            nonDraftedList.remove(event)
                            nonDraftedList.add(updatedEvent)
                        }
                    }
                }

                // Sort non-drafted events by start date
                nonDraftedList.sortBy { event ->
                    parseDate(event.startDate)
                }

                //expose private variable drafted list to public variable drafted list
                _draftedEvents.value = draftedList

                //expose private variable non drafted list to public variable non drafted list
                _events.value = nonDraftedList

                Log.d("EventCount", "Total drafted events: ${draftedList.size}")
                Log.d("EventCount", "Total non-drafted events: ${nonDraftedList.size}")

                //set state to Success
                _fetchEventsState.value = FetchEventsState.Success
            }

            .addOnFailureListener { exception ->
                _fetchEventsState.value = FetchEventsState.Failure(getErrorMessage(exception))
                Log.d("Could not fetch events", "${exception.message}")
            }
    }


    // Determine if an event should be ended
    private fun eventShouldBeEnded(event: Event): Boolean {
        val currentDate = Date()

        val eventEndDate: Date? = if (event.isMultiDayEvent) {
            parseDate(event.endDate)
        } else {
            parseDate(event.startDate)
        }

        return eventEndDate?.before(currentDate) == true
    }

    // Determine if an event should be ongoing
    private fun eventShouldBeOngoing(event: Event): Boolean {
        val currentDate = Date()

        if (event.isMultiDayEvent) {
            val startDate = parseDate(event.startDate)
            val endDate = parseDate(event.endDate)
            startDate?.before(currentDate) == true && endDate?.after(currentDate) == true
        }
        return false
    }

    // Update the status of an event in the database
    private fun updateEventStatus(eventId: String, newStatus: String) {
        db.collection("events").document(eventId)
            .update("eventStatus", newStatus)
            .addOnSuccessListener {
                Log.d("EventStatusUpdate", "Event ID: $eventId status updated to $newStatus")
            }
            .addOnFailureListener { e ->
                Log.d("EventStatusUpdate", "Error updating event status for Event ID: $eventId", e)
            }
    }


    // Function to parse the date string to a Date object
    fun parseDate(dateString: String): Date? {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun convertToFormattedDate(dateString: String): Pair<Pair<String, String>, String> {
        val inputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())

        return try {
            val date = inputDateFormat.parse(dateString)
            if (date != null) {
                val dayOfWeek = dayFormat.format(date)
                val month = monthFormat.format(date)
                val dayOfMonth = dayOfMonthFormat.format(date)
                Pair(dayOfWeek, month) to dayOfMonth
            } else {
                Pair("Invalid Date", "Invalid Date") to "Invalid Date"
            }
        } catch (e: Exception) {
            Pair("Invalid Date", "Invalid Date") to "Invalid Date"
        }
    }

    fun getEventById(eventId: String?): Event? {
        val eventList = _events.value
        val event = eventList.find { it.id == eventId }
        event?.let {
            return event
        } ?: run {
            val draftedEventList = _draftedEvents.value
            val draftedEvent = draftedEventList.find { it.id == eventId }
            draftedEvent?.let {
                return draftedEvent
            } ?: return null
        }
    }

    suspend fun checkIfEventNameIsTaken(eventName: String, onResult: (Boolean) -> Unit) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            //retrives users collection
            firestore.collection("events")

                //only get documents that match the username entered by user
                .whereEqualTo("eventName", eventName)
                .get()

                //listen for completion response
                .addOnCompleteListener { task ->

                    //if documents are fetched succesfully
                    if (task.isSuccessful) {

                        //get returned results
                        val documents = task.result
                        val isTaken = documents != null && !documents.isEmpty // sets is Taken to true
                        onResult(isTaken)
                        Log.e("Firestore", "Is Event name taken : $isTaken")
                    } else {
                        Log.e("Firestore", "Error checking event name: ${task.exception?.message}")
                        onResult(false) // Assume the username is not taken if there's an error
                    }
                }

        } catch (e: Exception) {
            Log.e("Firestore", "Exception checking username: ${e.message}")
            onResult(false) // Assume the username is not taken if there's an exception
        }
    }

    suspend fun checkIfEventDateIsTaken(eventDate: String, onResult: (Boolean) -> Unit) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            //retrives users collection
            firestore.collection("events")

                //only get documents that match the username entered by user
                .whereEqualTo("startDate", eventDate)
                .get()

                //listen for completion response
                .addOnCompleteListener { task ->

                    //if documents are fetched succesfully
                    if (task.isSuccessful) {

                        //get returned results
                        val documents = task.result
                        val isTaken = documents != null && !documents.isEmpty // sets is Taken to true
                        onResult(isTaken)
                        Log.e("Firestore", "Is Event name taken : $isTaken")
                    } else {
                        Log.e("Firestore", "Error checking event name: ${task.exception?.message}")
                        onResult(false) // Assume the username is not taken if there's an error
                    }
                }

        } catch (e: Exception) {
            Log.e("Firestore", "Exception checking username: ${e.message}")
            onResult(false) // Assume the username is not taken if there's an exception
        }
    }

    suspend fun checkIfTimeIsTaken(eventName: String, onResult: (Boolean) -> Unit) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            //retrives users collection
            firestore.collection("events")

                //only get documents that match the username entered by user
                .whereEqualTo("eventName", eventName)
                .get()

                //listen for completion response
                .addOnCompleteListener { task ->

                    //if documents are fetched succesfully
                    if (task.isSuccessful) {

                        //get returned results
                        val documents = task.result
                        val isTaken = documents != null && !documents.isEmpty // sets is Taken to true
                        onResult(isTaken)
                        Log.e("Firestore", "Is Event name taken : $isTaken")
                    } else {
                        Log.e("Firestore", "Error checking event name: ${task.exception?.message}")
                        onResult(false) // Assume the username is not taken if there's an error
                    }
                }

        } catch (e: Exception) {
            Log.e("Firestore", "Exception checking username: ${e.message}")
            onResult(false) // Assume the username is not taken if there's an exception
        }
    }

    suspend fun checkIfDateRangeExists(startDate: String, endDate: String, onResult: (Boolean) -> Unit) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Retrieve the events collection
            firestore.collection("events")
                // Query for documents where the startDate and endDate match the provided values
                .whereEqualTo("startDate", startDate)
                .whereEqualTo("endDate", endDate)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Check if any documents are returned
                        val documents = task.result
                        val isExists = documents != null && !documents.isEmpty
                        onResult(isExists)
                        Log.e("Firestore", "Do dates exist: $isExists")
                    } else {
                        Log.e("Firestore", "Error checking date range: ${task.exception?.message}")
                        onResult(false) // Assume the dates do not exist if there's an error
                    }
                }
        } catch (e: Exception) {
            Log.e("Firestore", "Exception checking date range: ${e.message}")
            onResult(false) // Assume the dates do not exist if there's an exception
        }
    }

    fun saveEventToFirestore(
        eventName: String,
        startDate: String,
        endDate: String,
        isMultiDayEvent: Boolean,
        eventTime: String,
        eventVenue: String,
        eventStatus: String,
        eventCategory: String,
        ticketTypes: List<TicketType>,
        imageUri: Uri?,
        isDraft: Boolean,
        eventOrganizer: String,
        eventDescription: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val eventData = hashMapOf(
            "eventName" to eventName.trim(),
            "startDate" to startDate,
            "endDate" to endDate,
            "isMultiDayEvent" to isMultiDayEvent,
            "eventTime" to eventTime,
            "eventVenue" to eventVenue,
            "eventStatus" to eventStatus,
            "eventCategory" to eventCategory,
            "ticketTypes" to ticketTypes.map {
                mapOf(
                    "name" to it.name,
                    "price" to it.price,
                    "availableTickets" to it.availableTickets
                )
            }, //create a list containing a map of ticketType objects
            "imageUri" to imageUri?.toString(),
            "isDraft" to isDraft,
            "eventOrganizer" to eventOrganizer,
            "eventDescription" to eventDescription
        )

        try {
            db.collection("events")
                .add(eventData)

                .addOnSuccessListener { documentReference ->
                    val newEvent = Event(
                        id = documentReference.id,  //assigns id of created document to event id
                        eventName = eventName,
                        startDate = startDate,
                        endDate = endDate,
                        isMultiDayEvent = isMultiDayEvent,
                        eventTime = eventTime,
                        eventVenue = eventVenue,
                        eventStatus = eventStatus,
                        eventCategory = eventCategory,
                        ticketTypes = ticketTypes,
                        imageUri = imageUri?.toString() ?: "",
                        isDraft = isDraft,
                        eventOrganizer = eventOrganizer,
                        eventDescription = eventDescription,

                        )
                    println("Saving event. ID: ${documentReference.id}, isDraft: ${isDraft}")
                    addEvent(newEvent)
                    println("Is event draft? ${newEvent.isDraft}")

                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } catch (e: Exception) {
            Log.d("LoginViewModel", "Failed to save event: ${e.message}")
        }
    }

    fun addEvent(newEvent: Event) {
        if (newEvent.isDraft) {
            _draftedEvents.value += newEvent
            println("Added to drafted events: ${newEvent.eventName}")
        } else {
            _events.value += newEvent
            println("Added to non-drafted events: ${newEvent.eventName}")
        }
        println("Added event. IsDraft: ${newEvent.isDraft}, Total Drafted: ${_draftedEvents.value.size}, Total Non-Drafted: ${_events.value.size}")
    }

    fun deleteEvent(event: Event, onFailure: (String) -> Unit) {

        //delete event from database collection
        db.collection("events").document(event.id).delete()
            .addOnSuccessListener {
                //remove event from local list
                _events.value -= event
            }
            .addOnFailureListener { exception ->
                //set callback with error message
                onFailure(getErrorMessage(exception))
                Log.d("Could not delete document", "$exception")
            }
    }

    fun copyEvent(event: Event) {
        val newEvent = event.copy(id = "")
        addEvent(newEvent)
    }
}

private fun getErrorMessage(exception: Exception?): String {
    return when (exception) {
        is FirebaseFirestoreException -> {
            when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Permission denied. Check your Firestore rules."
                FirebaseFirestoreException.Code.UNAVAILABLE -> "Firestore service is currently unavailable."
                FirebaseFirestoreException.Code.NOT_FOUND -> "Document or collection not found."
                else -> "Firestore error: ${exception.localizedMessage}"
            }
        }

        else -> "Error: ${exception?.localizedMessage}"
    }
}

sealed class FetchEventsState() {
    data object Loading : FetchEventsState()
    data object Success : FetchEventsState()
    data class Failure(var errorMessage: String) : FetchEventsState()

}