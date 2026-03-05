package com.example.eventglow.events_management

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.eventglow.BuildConfig
import com.example.eventglow.common.BaseViewModel
import com.example.eventglow.common.EventDateTimeUtils
import com.example.eventglow.common.EventTimelineBucket
import com.example.eventglow.common.cloudinary.CloudinaryApi
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.EventCategory
import com.example.eventglow.dataClass.EventFilterCriteria
import com.example.eventglow.dataClass.TicketType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class EventsManagementViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        private const val AUTO_ARCHIVE_ENDED_EVENTS = true
        private const val ARCHIVE_AFTER_DAYS = 30L
    }

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

    private val _eventCategories = MutableStateFlow<List<EventCategory>>(emptyList())
    val eventCategories: StateFlow<List<EventCategory>> = _eventCategories.asStateFlow()

    val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val httpClient = OkHttpClient()
    private val cloudinaryApi: CloudinaryApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryApi::class.java)
    }

    init {
        fetchEvents()
        fetchEventCategories()
    }

    fun fetchEventCategories() {
        db.collection("eventCategories")
            .get()
            .addOnSuccessListener { result ->
                val categories = result.documents
                    .mapNotNull { document ->
                        val rawName = document.getString("name")?.trim().orEmpty()
                        if (rawName.isBlank()) {
                            null
                        } else {
                            EventCategory(
                                id = document.id,
                                name = rawName
                            )
                        }
                    }
                    .distinctBy { it.name.lowercase(Locale.getDefault()) }
                    .sortedBy { it.name.lowercase(Locale.getDefault()) }
                _eventCategories.value = categories
            }
            .addOnFailureListener { exception ->
                Log.d("EventCategories", "Failed to fetch categories: ${exception.message}")
            }
    }


    fun addEventCategory(
        categoryName: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val trimmed = categoryName.trim()
        if (trimmed.isBlank()) {
            onFailure("Category name cannot be empty.")
            return
        }

        val exists = _eventCategories.value.any { it.name.equals(trimmed, ignoreCase = true) }
        if (exists) {
            onFailure("Category already exists.")
            return
        }

        val payload = mapOf(
            "name" to trimmed,
            "createdAtMs" to System.currentTimeMillis()
        )

        db.collection("eventCategories")
            .add(payload)
            .addOnSuccessListener { documentReference ->
                _eventCategories.value =
                    (_eventCategories.value + EventCategory(
                        id = documentReference.id,
                        name = trimmed
                    )).distinctBy { it.name.lowercase(Locale.getDefault()) }
                        .sortedBy { it.name.lowercase(Locale.getDefault()) }
                onSuccess(trimmed)
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to add category.")
            }
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
        criteria: EventFilterCriteria
    ) {
        Log.d("FilterEvents", "Criteria: $criteria")
        val filteredListAdvanced = _events.value.filter { event ->
            matchesCriteria(event, criteria)
        }
        Log.d("FilterEvents", "Filtered Events Count: ${filteredListAdvanced.size}")
        _filteredEventsAdvanced.value = filteredListAdvanced
        Log.d("FilterEvents", "Filtered Events in state: ${_filteredEventsAdvanced.value.size}")
    }

    fun filterEventsAdvanced(
        eventStatus: String,
        eventCategories: List<String>,
        eventStartDate: String,
        eventEndDate: String,
        matchAllCriteria: Boolean
    ) {
        filterEventsAdvanced(
            EventFilterCriteria(
                eventStatus = eventStatus,
                eventCategories = eventCategories,
                eventStartDate = eventStartDate,
                eventEndDate = eventEndDate,
                matchAllCriteria = matchAllCriteria
            )
        )
    }

    private fun matchesCriteria(event: Event, criteria: EventFilterCriteria): Boolean {
        val normalizedStatus = criteria.eventStatus.trim()
        val normalizedCategories = criteria.eventCategories
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .toSet()
        val normalizedStartDate = criteria.eventStartDate.trim()
        val normalizedEndDate = criteria.eventEndDate.trim()

        val hasStatusFilter = normalizedStatus.isNotBlank()
        val hasCategoryFilter = normalizedCategories.isNotEmpty()
        val hasStartDateFilter = normalizedStartDate.isNotBlank()
        val hasEndDateFilter = normalizedEndDate.isNotBlank()

        val activeFiltersCount = listOf(
            hasStatusFilter,
            hasCategoryFilter,
            hasStartDateFilter,
            hasEndDateFilter
        ).count { it }
        if (activeFiltersCount == 0) return true

        val eventStatusMatch = !hasStatusFilter ||
                event.eventStatus.trim().equals(normalizedStatus, ignoreCase = true)
        val eventCategoryMatch = !hasCategoryFilter ||
                normalizedCategories.contains(event.eventCategory.trim().lowercase(Locale.getDefault()))
        val eventStartDateMatch = !hasStartDateFilter ||
                event.startDate.trim() == normalizedStartDate
        val eventEndDateMatch = !hasEndDateFilter ||
                event.endDate.trim() == normalizedEndDate

        return if (criteria.matchAllCriteria) {
            eventStatusMatch && eventCategoryMatch && eventStartDateMatch && eventEndDateMatch
        } else {
            val matches = mutableListOf<Boolean>()
            if (hasStatusFilter) matches += eventStatusMatch
            if (hasCategoryFilter) matches += eventCategoryMatch
            if (hasStartDateFilter) matches += eventStartDateMatch
            if (hasEndDateFilter) matches += eventEndDateMatch
            matches.any { it }
        }
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
                val userId = auth.currentUser?.uid
                if (userId.isNullOrBlank()) {
                    processFetchedEvents(result, canWriteStatus = false)
                } else {
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val role = userDoc.getString("role").orEmpty()
                            val isAdminUser = role.equals("admin", ignoreCase = true)
                            processFetchedEvents(result, canWriteStatus = isAdminUser)
                        }
                        .addOnFailureListener {
                            processFetchedEvents(result, canWriteStatus = false)
                        }
                }
            }

            .addOnFailureListener { exception ->
                _fetchEventsState.value = FetchEventsState.Failure(getErrorMessage(exception))
                Log.d("Could not fetch events", "${exception.message}")
            }
    }

    private fun processFetchedEvents(
        result: com.google.firebase.firestore.QuerySnapshot,
        canWriteStatus: Boolean
    ) {
        val draftedList = mutableListOf<Event>()
        val nonDraftedList = mutableListOf<Event>()
        val archiveThresholdMs = ARCHIVE_AFTER_DAYS * 24L * 60L * 60L * 1000L
        val nowMs = System.currentTimeMillis()

        for (document in result) {
            val data = document.data
            Log.d("FirestoreData", "Document ID: ${document.id}, Data: $data")
            val isArchived = data["isArchived"] as? Boolean ?: false

            val event = Event(
                id = document.id,
                eventName = data["eventName"] as? String ?: "",
                startDate = data["startDate"] as? String ?: "",
                endDate = data["endDate"] as? String ?: "",
                isMultiDayEvent = data["isMultiDayEvent"] as? Boolean ?: false,
                eventTime = data["eventTime"] as? String ?: "",
                eventVenue = data["eventVenue"] as? String ?: "",
                eventStatus = data["eventStatus"] as? String ?: "",
                isImportant = (data["important"] as? Boolean) ?: (data["isImportant"] as? Boolean) ?: false,
                eventCategory = data["eventCategory"] as? String ?: "",
                ticketTypes = (data["ticketTypes"] as? List<Map<String, Any>>)?.map {
                    TicketType(
                        name = it["name"] as? String ?: "",
                        price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                        availableTickets = (it["availableTickets"] as? Number)?.toInt() ?: 0,
                        isFree = (it["isFree"] as? Boolean) ?: (it["free"] as? Boolean) ?: false
                    )
                } ?: listOf(),
                imageUri = data["imageUri"] as? String ?: "",
                imagePublicId = data["imagePublicId"] as? String ?: "",
                isDraft = data["isDraft"] as? Boolean ?: false,
                eventOrganizer = data["eventOrganizer"] as? String ?: "",
                eventDescription = data["eventDescription"] as? String ?: "",
                durationLabel = data["durationLabel"] as? String ?: "",
                durationMinutes = (data["durationMinutes"] as? Number)?.toInt() ?: 0,
                createdAtMs = (data["createdAtMs"] as? Number)?.toLong() ?: 0L,
            )

            if (isArchived || event.eventStatus.equals("archived", ignoreCase = true)) {
                continue
            }

            val computedStatus = computeEventStatus(event)
            val statusAlignedEvent = if (computedStatus != event.eventStatus) {
                if (canWriteStatus) {
                    updateEventStatus(event.id, computedStatus)
                }
                event.copy(eventStatus = computedStatus)
            } else {
                event
            }

            if (AUTO_ARCHIVE_ENDED_EVENTS && statusAlignedEvent.eventStatus.equals("ended", ignoreCase = true)) {
                val endDate = EventDateTimeUtils.parseDateFlexible(
                    statusAlignedEvent.endDate.ifBlank { statusAlignedEvent.startDate }
                )
                val endMs = endDate?.time ?: 0L
                if (endMs > 0L && (nowMs - endMs) >= archiveThresholdMs) {
                    if (canWriteStatus) {
                        archiveEvent(statusAlignedEvent.id)
                    }
                    Log.d(
                        "AutoArchive",
                        "Archived ended event id=${statusAlignedEvent.id}, name=${statusAlignedEvent.eventName}"
                    )
                    continue
                }
            }

            val updatedEvent = statusAlignedEvent
            if (updatedEvent.isDraft) draftedList.add(updatedEvent) else nonDraftedList.add(updatedEvent)
        }

        nonDraftedList.sortByDescending { event -> event.createdAtMs }
        _draftedEvents.value = draftedList
        _events.value = nonDraftedList
        _fetchEventsState.value = FetchEventsState.Success
    }

    private fun computeEventStatus(event: Event): String {
        return when (EventDateTimeUtils.classifyEventBucket(event)) {
            EventTimelineBucket.ENDED -> "ended"
            EventTimelineBucket.LIVE -> "ongoing"
            EventTimelineBucket.TODAY,
            EventTimelineBucket.UPCOMING,
            EventTimelineBucket.UNKNOWN -> "upcoming"
        }
    }

    // Update the status of an event in the database
    fun updateEventStatus(eventId: String, newStatus: String) {
        val normalizedStatus = newStatus.trim().lowercase(Locale.getDefault())
        if (eventId.isBlank() || normalizedStatus.isBlank()) return
        db.collection("events").document(eventId)
            .update("eventStatus", normalizedStatus)
            .addOnSuccessListener {
                Log.d("EventStatusUpdate", "Event ID: $eventId status updated to $normalizedStatus")
            }
            .addOnFailureListener { e ->
                Log.d("EventStatusUpdate", "Error updating event status for Event ID: $eventId", e)
            }
    }

    fun markEventOngoing(eventId: String) {
        updateEventStatus(eventId, "ongoing")
    }

    fun markEventEnded(eventId: String) {
        updateEventStatus(eventId, "ended")
    }

    private fun archiveEvent(eventId: String) {
        db.collection("events").document(eventId)
            .update(
                mapOf(
                    "eventStatus" to "archived",
                    "isArchived" to true,
                    "archivedAtMs" to System.currentTimeMillis()
                )
            )
            .addOnFailureListener { e ->
                Log.d("AutoArchive", "Failed to archive event $eventId", e)
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

    fun checkIfEventNameIsTaken(eventName: String, onResult: (Boolean) -> Unit) {
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

    fun checkIfEventDateIsTaken(eventDate: String, onResult: (Boolean) -> Unit) {
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

    fun checkIfDateRangeExists(startDate: String, endDate: String, onResult: (Boolean) -> Unit) {
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
        durationLabel: String,
        durationMinutes: Int,
        eventVenue: String,
        eventStatus: String,
        isImportant: Boolean,
        eventCategory: String,
        ticketTypes: List<TicketType>,
        imageUri: Uri?,
        isDraft: Boolean,
        eventOrganizer: String,
        eventDescription: String,
        onSaved: (String) -> Unit = {},
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val finalImageUrl = imageUri?.toString().orEmpty()
        if (finalImageUrl.isBlank() || (!finalImageUrl.startsWith("http://") && !finalImageUrl.startsWith("https://"))) {
            onFailure(IllegalArgumentException("Please upload the event image to Cloudinary first."))
            return
        }
        val finalImagePublicId = extractCloudinaryPublicId(finalImageUrl).orEmpty()

        val createdAtMs = System.currentTimeMillis()
        val eventData = hashMapOf(
            "eventName" to eventName.trim(),
            "startDate" to startDate,
            "endDate" to endDate,
            "isMultiDayEvent" to isMultiDayEvent,
            "eventTime" to eventTime,
            "durationLabel" to durationLabel,
            "durationMinutes" to durationMinutes,
            "eventVenue" to eventVenue,
            "eventStatus" to eventStatus,
            "important" to isImportant,
            "isImportant" to isImportant,
            "eventCategory" to eventCategory,
            "ticketTypes" to ticketTypes.map {
                mapOf(
                    "name" to it.name,
                    "price" to it.price,
                    "availableTickets" to it.availableTickets,
                    "isFree" to it.isFree,
                )
            }, //create a list containing a map of ticketType objects
            "imageUri" to finalImageUrl,
            "imagePublicId" to finalImagePublicId,
            "isDraft" to isDraft,
            "eventOrganizer" to eventOrganizer,
            "eventDescription" to eventDescription,
            "createdAtMs" to createdAtMs
        )

        enforceCreateEventRateLimit(
            onAllowed = {
                try {
            db.collection("events")
                .add(eventData)

                .addOnSuccessListener { documentReference ->
                    onSaved(documentReference.id)
                    val newEvent = Event(
                        id = documentReference.id,  //assigns id of created document to event id
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
                        ticketTypes = ticketTypes,
                        imageUri = finalImageUrl,
                        imagePublicId = finalImagePublicId,
                        isDraft = isDraft,
                        eventOrganizer = eventOrganizer,
                        eventDescription = eventDescription,
                        createdAtMs = createdAtMs,

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
            },
            onRejected = { exception ->
                onFailure(exception)
            }
        )
    }

    private fun enforceCreateEventRateLimit(
        onAllowed: () -> Unit,
        onRejected: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            onAllowed()
            return
        }

        val limiterRef = db.collection("rate_limits").document(userId)
        val cooldownMs = 15_000L

        db.runTransaction { transaction ->
            val snapshot = transaction.get(limiterRef)
            val lastCreateAtMs = snapshot.getLong("lastEventCreateAtMs") ?: 0L
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastCreateAtMs < cooldownMs) {
                val secondsLeft = ((cooldownMs - (nowMs - lastCreateAtMs)) / 1000L).coerceAtLeast(1L)
                throw IllegalStateException("Please wait ${secondsLeft}s before creating another event.")
            }

            transaction.set(
                limiterRef,
                mapOf(
                    "lastEventCreateAtMs" to nowMs,
                    "updatedAtMs" to nowMs
                ),
                SetOptions.merge()
            )
            null
        }.addOnSuccessListener {
            onAllowed()
        }.addOnFailureListener { throwable ->
            onRejected((throwable as? Exception) ?: Exception(throwable.message ?: "Rate limit check failed."))
        }
    }

    suspend fun uploadEventImageToCloudinary(
        appContext: Context,
        imageUri: Uri,
        eventName: String
    ): String? {
        return try {
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
            val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()
            if (cloudName.isBlank() || uploadPreset.isBlank()) {
                throw IllegalStateException("Cloudinary credentials are missing in BuildConfig.")
            }

            val mimeType = appContext.contentResolver.getType(imageUri) ?: "image/jpeg"
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }

            val input = appContext.contentResolver.openInputStream(imageUri)
                ?: throw IllegalArgumentException("Could not read selected image.")

            val folderValue = "eventGlow/events"
            val publicIdValue = buildEventImagePublicId(eventName)
            val tempFile = File(appContext.cacheDir, "$publicIdValue.$extension")
            input.use { source ->
                tempFile.outputStream().use { target ->
                    source.copyTo(target)
                }
            }

            val fileBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = tempFile.name,
                body = fileBody
            )

            val uploadPresetBody = uploadPreset.toRequestBody("text/plain".toMediaTypeOrNull())
            val folderBody = folderValue.toRequestBody("text/plain".toMediaTypeOrNull())
            val assetFolderBody = folderValue.toRequestBody("text/plain".toMediaTypeOrNull())
            val publicIdBody = publicIdValue.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = cloudinaryApi.uploadImage(
                cloudName = cloudName,
                file = filePart,
                uploadPreset = uploadPresetBody,
                folder = folderBody,
                assetFolder = assetFolderBody,
                publicId = publicIdBody
            )

            tempFile.delete()
            response.secureUrl?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e("EventsManagementVM", "Cloudinary upload failed: ${e.message}", e)
            null
        }
    }

    private fun buildEventImagePublicId(eventName: String): String {
        // Naming format:
        // evg_{eventName}_{yyyyMMdd_HHmmss}_{shortRandom}
        val eventToken = sanitizeToken(eventName).ifBlank { "event" }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val shortRandom = UUID.randomUUID().toString().take(6)
        return "evg_${eventToken}_${timestamp}_${shortRandom}"
    }

    private fun sanitizeToken(input: String): String {
        return input.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(40)
    }

    private fun extractCloudinaryPublicId(imageUrl: String?): String? {
        val url = imageUrl?.trim().orEmpty()
        if (url.isBlank()) return null
        val marker = "/image/upload/"
        val markerIndex = url.indexOf(marker)
        if (markerIndex == -1) return null

        var path = url.substring(markerIndex + marker.length)
        val queryIndex = path.indexOf('?')
        if (queryIndex >= 0) path = path.substring(0, queryIndex)

        if (path.startsWith("v") && path.length > 2 && path[1].isDigit()) {
            val slash = path.indexOf('/')
            if (slash > 0) path = path.substring(slash + 1)
        }

        return path.substringBeforeLast('.').takeIf { it.isNotBlank() }
    }

    private suspend fun deleteImageViaSupabaseFunction(publicId: String): Result<Unit> {
        return try {
            val functionsBaseUrl = BuildConfig.SUPABASE_FUNCTIONS_BASE_URL.trim().trimEnd('/')
            val anonKey = BuildConfig.SUPABASE_FUNCTIONS_ANON_KEY.trim()
            val deletePath = BuildConfig.SUPABASE_FUNCTIONS_CLOUDINARY_DELETE_PATH.trim().trimStart('/')
            if (functionsBaseUrl.isBlank() || anonKey.isBlank() || deletePath.isBlank()) {
                return Result.failure(
                    IllegalStateException(
                        "Cloudinary delete function config missing. Set SUPABASE_FUNCTIONS_BASE_URL, SUPABASE_FUNCTIONS_ANON_KEY and SUPABASE_FUNCTIONS_CLOUDINARY_DELETE_PATH."
                    )
                )
            }

            val idToken = auth.currentUser?.getIdToken(false)?.await()?.token?.trim().orEmpty()
            val url = "$functionsBaseUrl/$deletePath"
            val requestBody = """{"publicId":"$publicId"}"""
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", "application/json")
            if (idToken.isNotBlank()) {
                requestBuilder.addHeader("X-Firebase-Id-Token", idToken)
            }
            val request = requestBuilder.build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(
                        IllegalStateException(
                            "Cloudinary delete function failed (${response.code}): $responseBody"
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addEvent(newEvent: Event) {
        if (newEvent.isDraft) {
            _draftedEvents.value = listOf(newEvent) + _draftedEvents.value
            println("Added to drafted events: ${newEvent.eventName}")
        } else {
            _events.value = listOf(newEvent) + _events.value
            println("Added to non-drafted events: ${newEvent.eventName}")
        }
        println("Added event. IsDraft: ${newEvent.isDraft}, Total Drafted: ${_draftedEvents.value.size}, Total Non-Drafted: ${_events.value.size}")
    }

    fun deleteEvent(event: Event, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val publicId = event.imagePublicId.ifBlank { extractCloudinaryPublicId(event.imageUri) ?: "" }
            var cleanupWarning: String? = null
            if (publicId.isNotBlank()) {
                deleteImageViaSupabaseFunction(publicId)
                    .onFailure { cleanupWarning = it.localizedMessage ?: "Cloudinary image cleanup failed." }
            }

            try {
                db.collection("events").document(event.id).delete().await()
                _events.value = _events.value.filterNot { it.id == event.id }
                _draftedEvents.value = _draftedEvents.value.filterNot { it.id == event.id }
                cleanupWarning?.let { warning -> onFailure("Event deleted, but image cleanup failed: $warning") }
            } catch (exception: Exception) {
                onFailure(getErrorMessage(exception))
                Log.d("Could not delete document", "$exception")
            }
        }
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
