package com.example.eventglow.user

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.dataClass.User
import com.example.eventglow.dataClass.UserPreferences
import com.example.eventts.dataClass.FilterCriteria
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Create an object of UserPreferences class
    private val sharedPreferences = UserPreferences(application)

    // StateFlow to hold and emit the list of bought tickets
    private val _boughtTickets = MutableStateFlow<List<BoughtTicket>>(emptyList())
    val boughtTickets: StateFlow<List<BoughtTicket>> = _boughtTickets

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _featuredEvents = MutableStateFlow<List<Event>>(emptyList())
    val featuredEvents: StateFlow<List<Event>> = _featuredEvents.asStateFlow()

    // StateFlow to hold and emit the list of favorite events
    private val _favoriteEvents = MutableStateFlow<List<Event>>(emptyList())
    val favoriteEvents: StateFlow<List<Event>> = _favoriteEvents.asStateFlow()

    // StateFlow to hold and emit the list of bookmarked events
    private val _bookmarkEvents = MutableStateFlow<List<Event>>(emptyList())
    val bookmarkEvents: StateFlow<List<Event>> = _bookmarkEvents.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredEvents = MutableStateFlow<List<Event>>(emptyList())
    val filteredEvents: StateFlow<List<Event>> = _filteredEvents.asStateFlow()

    // StateFlow to hold and emit the list of bought tickets
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        fetchEvents()
    }

    fun fetchFeaturedEvents() {
        Log.d("UserViewModel", "Fetching events from Firestore.")
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("events")
            .whereEqualTo("isDraft", false) // Filter out drafted events
            .get()
            .addOnSuccessListener { result ->
                val nonDraftedList = mutableListOf<Event>()

                for (document in result) {
                    val data = document.data
                    Log.d("FirestoreData", "Document ID: ${document.id}, Data: $data")

                    val event = Event(
                        id = document.id,
                        eventName = data["eventName"] as? String ?: "",
                        startDate = data["startDate"] as? String ?: "",
                        endDate = data["endDate"] as? String ?: "",
                        isMultiDayEvent = data["isMultiDayEvent"] as? Boolean ?: false,
                        eventTime = data["eventTime"] as? String ?: "",
                        eventVenue = data["eventVenue"] as? String ?: "",
                        eventStatus = data["eventStatus"] as? String ?: "",
                        eventCategory = data["eventCategory"] as? String ?: "",
                        ticketTypes = (data["ticketTypes"] as? List<Map<String, Any>>)?.map {
                            TicketType(
                                name = it["name"] as? String ?: "",
                                price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                                availableTickets = (it["availableTickets"] as? Number)?.toInt() ?: 0
                            )
                        } ?: listOf(),
                        imageUri = data["imageUri"] as? String ?: "",
                        isDraft = data["isDraft"] as? Boolean ?: false,
                        eventOrganizer = data["eventOrganizer"] as? String ?: "",
                        eventDescription = data["eventDescription"] as? String ?: ""
                    )

                    nonDraftedList.add(event)
                }

                // Shuffle the list and select up to 4 random events
                val randomEvents = nonDraftedList.shuffled().take(4)

                _featuredEvents.value = randomEvents
                Log.d("EventCount", "Total random events selected: ${randomEvents.size}")
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Failed to fetch events", e)
            }
    }

    fun getBoughtTicketByReference(ticketReference: String): BoughtTicket? {
        val boughtTicketList = _boughtTickets.value

        Log.d("TicketSearch", "Searching for ticket with reference: $ticketReference")

        val ticket = boughtTicketList.find { it.transactionReference == ticketReference }

        return if (ticket != null) {
            Log.d("TicketSearch", "Ticket found: $ticket")
            ticket
        } else {
            Log.w("TicketSearch", "Ticket with reference $ticketReference not found")
            null
        }
    }


    fun fetchEvents() {
        Log.d("UserViewModel", "Fetching events from Firestore.")
        firestore.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val nonDraftedList = mutableListOf<Event>()
                for (document in result) {
                    val data = document.data
                    Log.d("FirestoreData", "Document ID: ${document.id}, Data: $data")

                    val event = Event(
                        id = document.id,
                        eventName = data["eventName"] as? String ?: "",
                        startDate = data["startDate"] as? String ?: "",
                        endDate = data["endDate"] as? String ?: "",
                        isMultiDayEvent = data["isMultiDayEvent"] as? Boolean ?: false,
                        eventTime = data["eventTime"] as? String ?: "",
                        eventVenue = data["eventVenue"] as? String ?: "",
                        eventStatus = data["eventStatus"] as? String ?: "",
                        eventCategory = data["eventCategory"] as? String ?: "",
                        ticketTypes = (data["ticketTypes"] as? List<Map<String, Any>>)?.map {
                            TicketType(
                                name = it["name"] as? String ?: "",
                                price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                                availableTickets = (it["availableTickets"] as? Number)?.toInt() ?: 0
                            )
                        } ?: listOf(),
                        imageUri = data["imageUri"] as? String ?: "",
                        isDraft = data["isDraft"] as? Boolean ?: false,
                        eventOrganizer = data["eventOrganizer"] as? String ?: "",
                        eventDescription = data["eventDescription"] as? String ?: "",
                    )

                    Log.d("FirestoreEvent", "Event ID: ${event.id}, Is Draft: ${event.isDraft}")

                    if (!event.isDraft) {
                        nonDraftedList.add(event)
                    }
                }

                _events.value = nonDraftedList
                Log.d("EventCount", "Total non-drafted events: ${nonDraftedList.size}")
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Failed to fetch events", e)
            }
    }


    fun fetchFavoriteEvents() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDocRef = firestore.collection("users").document(userId)

                // Fetch user's document from Firestore
                val userSnapshot = userDocRef.get().await()
                val favoriteEventsData = userSnapshot.get("favoriteEvents") as? List<Map<String, Any>> ?: emptyList()

                // Convert data to Event objects
                val favoriteEvents = favoriteEventsData.map { eventMap ->
                    Event(
                        id = eventMap["id"] as? String ?: "",
                        eventName = eventMap["eventName"] as? String ?: "",
                        startDate = eventMap["startDate"] as? String ?: "",
                        endDate = eventMap["endDate"] as? String ?: "",
                        isMultiDayEvent = eventMap["isMultiDayEvent"] as? Boolean ?: false,
                        eventTime = eventMap["eventTime"] as? String ?: "",
                        eventVenue = eventMap["eventVenue"] as? String ?: "",
                        eventStatus = eventMap["eventStatus"] as? String ?: "",
                        eventCategory = eventMap["eventCategory"] as? String ?: "",
                        ticketTypes = (eventMap["ticketTypes"] as? List<Map<String, Any>>)?.map { ticketMap ->
                            TicketType(
                                name = ticketMap["name"] as? String ?: "",
                                price = ticketMap["price"] as? Double ?: 0.0,
                                availableTickets = ticketMap["availableTickets"] as? Int ?: 0
                            )
                        } ?: emptyList(),
                        imageUri = eventMap["imageUri"] as? String,
                        isDraft = eventMap["isDraft"] as? Boolean ?: false,
                        eventOrganizer = eventMap["eventOrganizer"] as? String ?: "",
                        eventDescription = eventMap["eventDescription"] as? String ?: ""
                    )
                }

                _favoriteEvents.value = favoriteEvents
                Log.d("UserViewModel", "Successfully fetched favorite events.")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error fetching favorite events", e)
            }
        }
    }

    fun fetchBookmarkEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDocRef = firestore.collection("users").document(userId)

                // Fetch user's document from Firestore
                val userSnapshot = userDocRef.get().await()
                val bookmarkEventsData = userSnapshot.get("bookmarkEvents") as? List<Map<String, Any>> ?: emptyList()

                // Convert data to Event objects
                val bookmarkEvents = bookmarkEventsData.map { eventMap ->
                    Event(
                        id = eventMap["id"] as? String ?: "",
                        eventName = eventMap["eventName"] as? String ?: "",
                        startDate = eventMap["startDate"] as? String ?: "",
                        endDate = eventMap["endDate"] as? String ?: "",
                        isMultiDayEvent = eventMap["isMultiDayEvent"] as? Boolean ?: false,
                        eventTime = eventMap["eventTime"] as? String ?: "",
                        eventVenue = eventMap["eventVenue"] as? String ?: "",
                        eventStatus = eventMap["eventStatus"] as? String ?: "",
                        eventCategory = eventMap["eventCategory"] as? String ?: "",
                        ticketTypes = (eventMap["ticketTypes"] as? List<Map<String, Any>>)?.map { ticketMap ->
                            TicketType(
                                name = ticketMap["name"] as? String ?: "",
                                price = ticketMap["price"] as? Double ?: 0.0,
                                availableTickets = ticketMap["availableTickets"] as? Int ?: 0
                            )
                        } ?: emptyList(),
                        imageUri = eventMap["imageUri"] as? String ?: "",
                        isDraft = eventMap["isDraft"] as? Boolean ?: false,
                        eventOrganizer = eventMap["eventOrganizer"] as? String ?: "",
                        eventDescription = eventMap["eventDescription"] as? String ?: ""
                    )
                }

                _bookmarkEvents.value = bookmarkEvents
                _isLoading.value = false
                Log.d("UserViewModel", "Successfully fetched bookmark events.")
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("UserViewModel", "Error fetching bookmark events", e)
            }
        }
    }

    fun hasUserBoughtTicketForEvent(eventId: String): Boolean {
        val hasTicket = _boughtTickets.value.any { it.eventId == eventId }
        Log.d("UserViewModel", "Has user bought ticket for event $eventId: $hasTicket")
        return hasTicket
    }

    fun filterEventsAdvancedMatchAllCriteria(criteria: FilterCriteria) {
        val filteredList = _events.value.filter { event ->
            (criteria.status.isEmpty() || event.eventStatus == criteria.status) &&
                    (criteria.categories.isEmpty() || criteria.categories.contains(event.eventCategory)) &&
                    (criteria.startDate.isEmpty() || event.startDate >= criteria.startDate) &&
                    (criteria.endDate.isEmpty() || event.endDate <= criteria.endDate)
        }

        sharedPreferences.keepFilteredEventsInSharedPref(filteredList)
        Log.d(
            "UserViewModel",
            "Advanced match all criteria filter applied. ${filteredList.size} events found and saved to SharedPreferences."
        )
    }

    fun filterEventsAdvancedDontMatchAllCriteria(criteria: FilterCriteria) {
        val filteredList = _events.value.filter { event ->
            (criteria.status.isNotEmpty() && event.eventStatus == criteria.status) ||
                    (criteria.categories.isNotEmpty() && criteria.categories.contains(event.eventCategory)) ||
                    (criteria.startDate.isNotEmpty() && event.startDate >= criteria.startDate) ||
                    (criteria.endDate.isNotEmpty() && event.endDate <= criteria.endDate)
        }

        sharedPreferences.keepFilteredEventsInSharedPref(filteredList)
        Log.d(
            "UserViewModel",
            "Advanced don't match all criteria filter applied. ${filteredList.size} events found and saved to SharedPreferences."
        )
    }

    fun addFavoriteEventToFireStore(event: Event) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDocRef = firestore.collection("users").document(userId)

                val userSnapshot = userDocRef.get().await()
                val favoriteEventsData = userSnapshot.get("favoriteEvents") as? List<Map<String, Any>> ?: emptyList()
                val favoriteEvents = favoriteEventsData.toMutableList()

                val eventExists = favoriteEvents.any { it["id"] == event.id }

                if (!eventExists) {
                    val eventMap = mapOf(
                        "id" to (event.id ?: ""),
                        "eventName" to (event.eventName ?: ""),
                        "startDate" to (event.startDate ?: ""),
                        "endDate" to (event.endDate ?: ""),
                        "isMultiDayEvent" to event.isMultiDayEvent,
                        "eventTime" to (event.eventTime ?: ""),
                        "eventVenue" to (event.eventVenue ?: ""),
                        "eventStatus" to (event.eventStatus ?: ""),
                        "eventCategory" to (event.eventCategory ?: ""),
                        "ticketTypes" to event.ticketTypes.map { ticket ->
                            mapOf(
                                "name" to (ticket.name ?: ""),
                                "price" to (ticket.price ?: 0.0),
                                "availableTickets" to (ticket.availableTickets ?: 0)
                            )
                        },
                        "imageUri" to (event.imageUri ?: ""),
                        "isDraft" to event.isDraft,
                        "eventOrganizer" to (event.eventOrganizer ?: ""),
                        "eventDescription" to (event.eventDescription ?: "")
                    )

                    favoriteEvents.add(eventMap)
                    userDocRef.update("favoriteEvents", favoriteEvents).await()
                    Log.d("UserViewModel", "Event ${event.eventName} added to favorites.")
                } else {
                    Log.d("UserViewModel", "Event ${event.eventName} already in favorites.")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error adding event ${event.eventName} to favorites", e)
            }
        }
    }

    fun addBookmarkEventToFireStore(event: Event) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDocRef = firestore.collection("users").document(userId)

                Log.d("UserViewModel", "Fetching bookmark events for user ID: $userId")
                // Get the current bookmark events list
                val userSnapshot = userDocRef.get().await()
                val bookmarkEventsData = userSnapshot.get("bookmarkEvents") as? List<Map<String, Any>> ?: emptyList()

                Log.d("UserViewModel", "Current bookmark events: $bookmarkEventsData")
                // Convert the immutable list to a mutable list
                val bookmarkEvents = bookmarkEventsData.toMutableList()

                // Check if the event is already in the list
                val eventExists = bookmarkEvents.any { it["id"] == event.id }

                if (!eventExists) {
                    // Convert event object to a map
                    val eventMap = mapOf(
                        "id" to (event.id ?: ""),
                        "eventName" to (event.eventName ?: ""),
                        "startDate" to (event.startDate ?: ""),
                        "endDate" to (event.endDate ?: ""),
                        "isMultiDayEvent" to event.isMultiDayEvent,
                        "eventTime" to (event.eventTime ?: ""),
                        "eventVenue" to (event.eventVenue ?: ""),
                        "eventStatus" to (event.eventStatus ?: ""),
                        "eventCategory" to (event.eventCategory ?: ""),
                        "ticketTypes" to event.ticketTypes.map { ticket ->
                            mapOf(
                                "name" to (ticket.name ?: ""),
                                "price" to (ticket.price ?: 0.0),
                                "availableTickets" to (ticket.availableTickets ?: 0)
                            )
                        },
                        "imageUri" to (event.imageUri ?: ""),
                        "isDraft" to event.isDraft,
                        "eventOrganizer" to (event.eventOrganizer ?: ""),
                        "eventDescription" to (event.eventDescription ?: "")
                    )

                    Log.d("UserViewModel", "Adding event to bookmarks: $eventMap")
                    // Add the new event
                    bookmarkEvents.add(eventMap)
                    userDocRef.update("bookmarkEvents", bookmarkEvents).await()
                    Log.d("UserViewModel", "Successfully added event ${event.eventName} to bookmarks.")
                } else {
                    Log.d("UserViewModel", "Event ${event.eventName} is already in bookmarks.")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error adding event ${event.eventName} to bookmarks", e)
            }
        }
    }

    fun deleteFavoriteEventFromFirestore(event: Event) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDocRef = firestore.collection("users").document(userId)

                Log.d("UserViewModel", "Fetching favorite events for user ID: $userId")
                val userSnapshot = userDocRef.get().await()
                val favoriteEventsData = userSnapshot.get("favoriteEvents") as? List<Map<String, Any>> ?: emptyList()
                val favoriteEvents = favoriteEventsData.toMutableList()

                Log.d("UserViewModel", "Current favorite events: $favoriteEventsData")
                val eventToRemove = favoriteEvents.find { it["id"] == event.id }
                if (eventToRemove != null) {
                    favoriteEvents.remove(eventToRemove)
                    Log.d("UserViewModel", "Removing event from favorites: $eventToRemove")
                    userDocRef.update("favoriteEvents", favoriteEvents).await()
                    Log.d("UserViewModel", "Successfully deleted event ${event.eventName} from favorites.")
                } else {
                    Log.d("UserViewModel", "Event ${event.eventName} not found in favorites.")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error deleting event ${event.eventName} from favorites", e)
            }
        }
    }

    fun deleteBookmarkEventFromFirestore(event: Event) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDocRef = firestore.collection("users").document(userId)

                Log.d("UserViewModel", "Fetching bookmark events for user ID: $userId")
                val userSnapshot = userDocRef.get().await()
                val bookmarkEventsData = userSnapshot.get("bookmarkEvents") as? List<Map<String, Any>> ?: emptyList()
                val bookmarkEvents = bookmarkEventsData.toMutableList()

                Log.d("UserViewModel", "Current bookmark events: $bookmarkEventsData")
                val eventToRemove = bookmarkEvents.find { it["id"] == event.id }
                if (eventToRemove != null) {
                    bookmarkEvents.remove(eventToRemove)
                    Log.d("UserViewModel", "Removing event from bookmarks: $eventToRemove")
                    userDocRef.update("bookmarkEvents", bookmarkEvents).await()
                    Log.d("UserViewModel", "Successfully deleted event ${event.eventName} from bookmarks.")
                } else {
                    Log.d("UserViewModel", "Event ${event.eventName} not found in bookmarks.")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error deleting event ${event.eventName} from bookmarks", e)
            }
        }
    }

    fun isEventFavorite(event: Event): Boolean {
        // Retrieve the list of favorite events from SharedPreferences
        val favoriteEvents = sharedPreferences.getFavoriteEvents() ?: return false

        Log.d("UserViewModel", "Checking if event is favorite: ${event.id}")
        // Check if the eventId is in the list of favorite events
        val isFavorite = favoriteEvents.contains(event)
        Log.d("UserViewModel", "Event ${event.id} is ${if (isFavorite) "a favorite" else "not a favorite"}.")
        return isFavorite
    }

    fun fetchBoughtTickets() {
        viewModelScope.launch {
            _isLoading.value = true
            _isRefreshing.value = true
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                Log.d("UserViewModel", "Fetching tickets for user ID: $userId")

                val userDocRef = firestore.collection("users").document(userId)
                val userSnapshot = userDocRef.get().await()
                val user = userSnapshot.toObject(User::class.java)

                if (user != null) {
                    Log.d("UserViewModel", "Successfully fetched tickets: ${user.boughtTickets}")
                    _boughtTickets.value = user.boughtTickets
                } else {
                    Log.d("UserViewModel", "No user data found in Firestore.")
                    _boughtTickets.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error fetching tickets from Firestore", e)
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun processBoughtTicket(boughtTicket: BoughtTicket) {
        viewModelScope.launch {
            try {
                Log.d("UserViewModel", "Processing bought ticket: $boughtTicket")
                // Save bought ticket to SharedPreferences
                sharedPreferences.addTicketToBoughtTickets(boughtTicket)
                Log.d("UserViewModel", "Successfully saved ticket to SharedPreferences.")

                // Update the user's information in Firestore
                updateUserInformationInFirestore(boughtTicket)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error processing bought ticket", e)
            }
        }
    }

    private fun updateUserInformationInFirestore(boughtTicket: BoughtTicket) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                Log.d("UserViewModel", "Updating Firestore for user ID: $userId")

                val userDocRef = firestore.collection("users").document(userId)
                val userSnapshot = userDocRef.get().await()
                val userData = userSnapshot.data ?: mutableMapOf<String, Any>()
                Log.d("UserViewModel", "Fetched user data: $userData")

                val existingTickets = (userData["boughtTickets"] as? List<Map<String, Any>>)?.map {
                    BoughtTicket(
                        transactionReference = it["transactionReference"] as? String ?: "",
                        eventOrganizer = it["eventOrganizer"] as? String ?: "",
                        eventId = it["eventId"] as? String ?: "",
                        eventName = it["eventName"] as? String ?: "",
                        startDate = it["startDate"] as? String ?: "",
                        eventStatus = it["eventStatus"] as? String ?: "",
                        endDate = it["endDate"] as? String ?: "",
                        imageUrl = it["imageUrl"] as? String ?: "",
                        ticketName = it["ticketName"] as? String ?: "",
                        ticketPrice = it["ticketPrice"] as? String ?: ""
                    )
                }?.toMutableList() ?: mutableListOf()

                Log.d("UserViewModel", "Adding new ticket: $boughtTicket")
                existingTickets.add(boughtTicket)

                userDocRef.update("boughtTickets", existingTickets.map {
                    mapOf(
                        "transactionReference" to it.transactionReference,
                        "eventOrganizer" to it.eventOrganizer,
                        "eventId" to it.eventId,
                        "eventName" to it.eventName,
                        "startDate" to it.startDate,
                        "eventStatus" to it.eventStatus,
                        "endDate" to it.endDate,
                        "imageUrl" to it.imageUrl,
                        "ticketName" to it.ticketName,
                        "ticketPrice" to it.ticketPrice
                    )
                }).await()
                Log.d("UserViewModel", "Successfully updated Firestore with bought tickets.")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error updating Firestore with bought ticket", e)
            }
        }
    }

    fun convertToFormattedDate(dateString: String): Pair<Pair<String, String>, String> {
        val inputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())

        return try {
            Log.d("DateUtils", "Parsing date: $dateString")
            val date = inputDateFormat.parse(dateString)
            if (date != null) {
                val dayOfWeek = dayFormat.format(date)
                val month = monthFormat.format(date)
                val dayOfMonth = dayOfMonthFormat.format(date)
                Log.d("DateUtils", "Formatted date: $dayOfWeek, $month, $dayOfMonth")
                Pair(dayOfWeek, month) to dayOfMonth
            } else {
                Log.w("DateUtils", "Date parsing returned null for date string: $dateString")
                Pair("Invalid Date", "Invalid Date") to "Invalid Date"
            }
        } catch (e: Exception) {
            Log.e("DateUtils", "Error parsing date string: $dateString", e)
            Pair("Invalid Date", "Invalid Date") to "Invalid Date"
        }
    }

    private fun filterEvents(query: String) {
        Log.d("EventFilter", "Filtering events with query: $query")
        val filteredList = _events.value.filter { event ->
            event.eventName.contains(query, ignoreCase = true) ||
                    event.eventVenue.contains(query, ignoreCase = true)
        }
        Log.d("EventFilter", "Filtered events count: ${filteredList.size}")
        _filteredEvents.value = filteredList
    }

    fun onSearchQueryChange(query: String) {
        Log.d("SearchQuery", "Search query changed: $query")
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            filterEvents(query)
        } else {
            Log.d("SearchQuery", "Search query is empty, clearing filtered events")
            _filteredEvents.value = emptyList()
        }
    }


}
