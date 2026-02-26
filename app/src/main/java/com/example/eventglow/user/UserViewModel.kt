package com.example.eventglow.user

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.EventCategory
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.dataClass.Transaction
import com.example.eventglow.dataClass.User
import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

    private val _eventCategories = MutableStateFlow<List<EventCategory>>(emptyList())
    val eventCategories: StateFlow<List<EventCategory>> = _eventCategories.asStateFlow()

    // StateFlow to hold and emit the list of bought tickets
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _ticketErrorMessage = MutableStateFlow<String?>(null)
    val ticketErrorMessage: StateFlow<String?> = _ticketErrorMessage.asStateFlow()

    private var hasObservedBoughtTickets = false

    init {
        fetchEvents()
        fetchEventCategories()
        observeBoughtTickets()
    }

    private fun observeBoughtTickets() {
        if (hasObservedBoughtTickets) return
        hasObservedBoughtTickets = true
        fetchBoughtTickets()
    }

    fun fetchEventCategories() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("eventCategories").get().await()
                val categories = snapshot.documents.mapNotNull { document ->
                    val name = document.getString("name")?.trim().orEmpty()
                    if (name.isBlank()) {
                        null
                    } else {
                        EventCategory(
                            id = document.id,
                            name = name
                        )
                    }
                }

                _eventCategories.value = if (categories.isNotEmpty()) {
                    categories.sortedBy { it.name.lowercase(Locale.getDefault()) }
                } else {
                    _events.value.mapNotNull { event ->
                        val category = event.eventCategory.trim()
                        if (category.isBlank()) null else EventCategory(name = category)
                    }.distinctBy { it.name.lowercase(Locale.getDefault()) }
                        .sortedBy { it.name.lowercase(Locale.getDefault()) }
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Failed to fetch event categories", e)
                _eventCategories.value = _events.value.mapNotNull { event ->
                    val category = event.eventCategory.trim()
                    if (category.isBlank()) null else EventCategory(name = category)
                }.distinctBy { it.name.lowercase(Locale.getDefault()) }
                    .sortedBy { it.name.lowercase(Locale.getDefault()) }
            }
        }
    }

    fun fetchFeaturedEvents() {
        Log.d("UserViewModel", "Fetching events from Firestore.")
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("events")
            .whereEqualTo("isDraft", false) // Filter out drafted events
            .whereEqualTo("isImportant", true) // Only fetch important events for featured section
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
                        isImportant = data["isImportant"] as? Boolean ?: false,
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
//
//    fun filterEventsAdvancedMatchAllCriteria(criteria: FilterCriteria) {
//        val filteredList = _events.value.filter { event ->
//            (criteria.status.isEmpty() || event.eventStatus == criteria.status) &&
//                    (criteria.categories.isEmpty() || criteria.categories.contains(event.eventCategory)) &&
//                    (criteria.startDate.isEmpty() || event.startDate >= criteria.startDate) &&
//                    (criteria.endDate.isEmpty() || event.endDate <= criteria.endDate)
//        }
//
//        sharedPreferences.keepFilteredEventsInSharedPref(filteredList)
//        Log.d(
//            "UserViewModel",
//            "Advanced match all criteria filter applied. ${filteredList.size} events found and saved to SharedPreferences."
//        )
//    }
//
//    fun filterEventsAdvancedDontMatchAllCriteria(criteria: FilterCriteria) {
//        val filteredList = _events.value.filter { event ->
//            (criteria.status.isNotEmpty() && event.eventStatus == criteria.status) ||
//                    (criteria.categories.isNotEmpty() && criteria.categories.contains(event.eventCategory)) ||
//                    (criteria.startDate.isNotEmpty() && event.startDate >= criteria.startDate) ||
//                    (criteria.endDate.isNotEmpty() && event.endDate <= criteria.endDate)
//        }
//
//        sharedPreferences.keepFilteredEventsInSharedPref(filteredList)
//        Log.d(
//            "UserViewModel",
//            "Advanced don't match all criteria filter applied. ${filteredList.size} events found and saved to SharedPreferences."
//        )
//    }

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
            _ticketErrorMessage.value = null
            try {
                val userId = auth.currentUser?.uid
                if (userId.isNullOrBlank()) {
                    _boughtTickets.value = emptyList()
                    _ticketErrorMessage.value = "No signed-in user found."
                    return@launch
                }
                Log.d("UserViewModel", "Fetching tickets for user ID: $userId")

                val userDocRef = firestore.collection("users").document(userId)
                val userSnapshot = userDocRef.get().await()
                val user = userSnapshot.toObject(User::class.java)

                if (user != null) {
                    Log.d("UserViewModel", "Successfully fetched tickets: ${user.boughtTickets}")
                    _boughtTickets.value = user.boughtTickets.sortedByDescending { parseTicketSortKey(it) }
                } else {
                    Log.d("UserViewModel", "No user data found in Firestore.")
                    _boughtTickets.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error fetching tickets from Firestore", e)
                _boughtTickets.value = emptyList()
                _ticketErrorMessage.value = e.localizedMessage ?: "Failed to fetch tickets."
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun clearTicketError() {
        _ticketErrorMessage.value = null
    }

    fun refreshBoughtTickets() {
        fetchBoughtTickets()
    }

    private fun parseTicketSortKey(ticket: BoughtTicket): Long {
        val candidates = listOf(
            ticket.paymentPaidAt,
            ticket.paymentCreatedAt,
            ticket.startDate
        ).filter { it.isNotBlank() }

        val parsers = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        for (value in candidates) {
            for (parser in parsers) {
                try {
                    val parsed = parser.parse(value)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {
                    // continue
                }
            }
        }
        return 0L
    }

    fun processBoughtTicket(boughtTicket: BoughtTicket) {
        viewModelScope.launch {
            val fallbackTransaction = transactionFromTicket(boughtTicket)
            commitTicketPurchase(boughtTicket, fallbackTransaction)
        }
    }

    suspend fun commitTicketPurchase(
        boughtTicket: BoughtTicket,
        transaction: Transaction
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid
            if (userId.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("No signed-in user found."))
            }

            val reference = transaction.reference.ifBlank { boughtTicket.transactionReference.orEmpty() }
            if (reference.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Missing transaction reference."))
            }

            val normalizedTicket = if (boughtTicket.transactionReference.isNullOrBlank()) {
                boughtTicket.copy(transactionReference = reference)
            } else {
                boughtTicket
            }

            val userDocRef = firestore.collection("users").document(userId)
            val transactionDocRef = firestore.collection("transactions").document(reference)

            val userSnapshot = userDocRef.get().await()
            val existingTickets = (userSnapshot.get("boughtTickets") as? List<Map<String, Any>>)
                ?.map { mapToBoughtTicket(it) }
                ?.toMutableList()
                ?: mutableListOf()

            val alreadyExists = existingTickets.any { it.transactionReference == normalizedTicket.transactionReference }
            if (!alreadyExists) {
                existingTickets.add(normalizedTicket)
            }

            val normalizedTransaction = transaction.copy(
                id = transaction.id.ifBlank { reference },
                reference = reference
            )

            firestore.runBatch { batch ->
                batch.set(transactionDocRef, transactionToMap(normalizedTransaction), SetOptions.merge())
                batch.set(
                    userDocRef,
                    mapOf("boughtTickets" to existingTickets.map { boughtTicketToMap(it) }),
                    SetOptions.merge()
                )
            }.await()

            sharedPreferences.addTicketToBoughtTickets(normalizedTicket)
            _boughtTickets.value = existingTickets.sortedByDescending { parseTicketSortKey(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error committing ticket purchase", e)
            Result.failure(e)
        }
    }

    private fun transactionFromTicket(ticket: BoughtTicket): Transaction {
        val reference = ticket.transactionReference.orEmpty()
        return Transaction(
            id = reference,
            status = ticket.paymentStatus.ifBlank { if (ticket.isFreeTicket) "success" else "" },
            reference = reference,
            amount = ticket.paymentAmount.ifBlank { if (ticket.isFreeTicket) "0" else ticket.ticketPrice },
            gatewayResponse = ticket.paymentGatewayResponse,
            paidAt = ticket.paymentPaidAt,
            createdAt = ticket.paymentCreatedAt,
            channel = ticket.paymentChannel.ifBlank { if (ticket.isFreeTicket) "free" else "" },
            currency = ticket.paymentCurrency.ifBlank { "GHS" },
            authorizationCode = ticket.paymentAuthorizationCode,
            cardType = ticket.paymentCardType,
            bank = ticket.paymentBank,
            customerEmail = ticket.paymentCustomerEmail
        )
    }

    private fun transactionToMap(transaction: Transaction): Map<String, Any?> {
        return mapOf(
            "id" to transaction.id,
            "status" to transaction.status,
            "reference" to transaction.reference,
            "amount" to transaction.amount,
            "gatewayResponse" to transaction.gatewayResponse,
            "paidAt" to transaction.paidAt,
            "createdAt" to transaction.createdAt,
            "channel" to transaction.channel,
            "currency" to transaction.currency,
            "authorizationCode" to transaction.authorizationCode,
            "cardType" to transaction.cardType,
            "bank" to transaction.bank,
            "customerEmail" to transaction.customerEmail,
            "customerCode" to transaction.customerCode,
            "customerPhone" to transaction.customerPhoneNumber
        )
    }

    private fun boughtTicketToMap(ticket: BoughtTicket): Map<String, Any?> {
        return mapOf(
            "transactionReference" to ticket.transactionReference,
            "paymentProvider" to ticket.paymentProvider,
            "paymentStatus" to ticket.paymentStatus,
            "paymentGatewayResponse" to ticket.paymentGatewayResponse,
            "paymentAmount" to ticket.paymentAmount,
            "paymentCurrency" to ticket.paymentCurrency,
            "paymentChannel" to ticket.paymentChannel,
            "paymentAuthorizationCode" to ticket.paymentAuthorizationCode,
            "paymentCardType" to ticket.paymentCardType,
            "paymentBank" to ticket.paymentBank,
            "paymentCustomerEmail" to ticket.paymentCustomerEmail,
            "paymentPaidAt" to ticket.paymentPaidAt,
            "paymentCreatedAt" to ticket.paymentCreatedAt,
            "isFreeTicket" to ticket.isFreeTicket,
            "eventOrganizer" to ticket.eventOrganizer,
            "eventId" to ticket.eventId,
            "eventName" to ticket.eventName,
            "startDate" to ticket.startDate,
            "eventStatus" to ticket.eventStatus,
            "endDate" to ticket.endDate,
            "imageUrl" to ticket.imageUrl,
            "ticketName" to ticket.ticketName,
            "ticketPrice" to ticket.ticketPrice,
            "qrCodeData" to ticket.qrCodeData,
            "isScanned" to ticket.isScanned,
            "scannedAt" to ticket.scannedAt,
            "scannedByAdminId" to ticket.scannedByAdminId,
            "scannedByAdminName" to ticket.scannedByAdminName
        )
    }

    private fun mapToBoughtTicket(map: Map<String, Any>): BoughtTicket {
        return BoughtTicket(
            transactionReference = map["transactionReference"] as? String ?: "",
            paymentProvider = map["paymentProvider"] as? String ?: "",
            paymentStatus = map["paymentStatus"] as? String ?: "",
            paymentGatewayResponse = map["paymentGatewayResponse"] as? String ?: "",
            paymentAmount = map["paymentAmount"] as? String ?: "",
            paymentCurrency = map["paymentCurrency"] as? String ?: "",
            paymentChannel = map["paymentChannel"] as? String ?: "",
            paymentAuthorizationCode = map["paymentAuthorizationCode"] as? String ?: "",
            paymentCardType = map["paymentCardType"] as? String ?: "",
            paymentBank = map["paymentBank"] as? String ?: "",
            paymentCustomerEmail = map["paymentCustomerEmail"] as? String ?: "",
            paymentPaidAt = map["paymentPaidAt"] as? String ?: "",
            paymentCreatedAt = map["paymentCreatedAt"] as? String ?: "",
            isFreeTicket = map["isFreeTicket"] as? Boolean ?: false,
            eventOrganizer = map["eventOrganizer"] as? String ?: "",
            eventId = map["eventId"] as? String ?: "",
            eventName = map["eventName"] as? String ?: "",
            startDate = map["startDate"] as? String ?: "",
            eventStatus = map["eventStatus"] as? String ?: "",
            endDate = map["endDate"] as? String ?: "",
            imageUrl = map["imageUrl"] as? String ?: "",
            ticketName = map["ticketName"] as? String ?: "",
            ticketPrice = map["ticketPrice"] as? String ?: "",
            qrCodeData = map["qrCodeData"] as? String ?: "",
            isScanned = map["isScanned"] as? Boolean ?: false,
            scannedAt = map["scannedAt"] as? String ?: "",
            scannedByAdminId = map["scannedByAdminId"] as? String ?: "",
            scannedByAdminName = map["scannedByAdminName"] as? String ?: ""
        )
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
