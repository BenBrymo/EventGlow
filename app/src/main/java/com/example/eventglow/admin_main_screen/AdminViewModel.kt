package com.example.eventglow.admin_main_screen

import android.app.Application
import android.net.Uri
import android.util.Log
import com.example.eventglow.BuildConfig
import com.example.eventglow.common.BaseViewModel
import com.example.eventglow.common.EventDateTimeUtils
import com.example.eventglow.common.EventTimelineBucket
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AdminViewModel(application: Application) : BaseViewModel(application) {

    private val sharedPreferences = UserPreferences(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appContext = application.applicationContext
    private val httpClient = OkHttpClient()

    private val _adminHomeUiState = MutableStateFlow(AdminHomeUiState())
    val adminHomeUiState: StateFlow<AdminHomeUiState> = _adminHomeUiState.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchAdminHomeData()
    }

    fun refreshAdminHomeData() {
        fetchAdminHomeData(isRefresh = true)
    }

    fun fetchAdminHomeData(isRefresh: Boolean = false) {
        if (isRefresh) {
            _isRefreshing.value = true
            clearError()
        } else {
            setLoading()
        }

        if (!isNetworkAvailable()) {
            _isRefreshing.value = false
            setFailure("No internet connection. Check your network and try again.")
            return
        }

        launchSafely(tag = "AdminViewModel") {
            try {
                val result = firestore.collection("events")
                    .whereEqualTo("isDraft", false)
                    .get()
                .await()

            val events = result.documents.map { document ->
                val data = document.data ?: emptyMap<String, Any>()
                Event(
                    id = document.id,
                    eventName = data["eventName"] as? String ?: "",
                    startDate = data["startDate"] as? String ?: "",
                    endDate = data["endDate"] as? String ?: "",
                    isMultiDayEvent = data["isMultiDayEvent"] as? Boolean ?: false,
                    eventTime = data["eventTime"] as? String ?: "",
                    durationMinutes = (data["durationMinutes"] as? Number)?.toInt() ?: 0,
                    eventVenue = data["eventVenue"] as? String ?: "",
                    eventStatus = data["eventStatus"] as? String ?: "",
                    eventCategory = data["eventCategory"] as? String ?: "",
                    ticketTypes = (data["ticketTypes"] as? List<Map<String, Any>>)?.map {
                        TicketType(
                            name = it["name"] as? String ?: "",
                            price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                            availableTickets = (it["availableTickets"] as? Number)?.toInt() ?: 0,
                            isFree = (it["isFree"] as? Boolean) ?: false
                        )
                    } ?: emptyList(),
                    imageUri = data["imageUri"] as? String ?: "",
                    isDraft = data["isDraft"] as? Boolean ?: false,
                    isImportant = data["isImportant"] as? Boolean ?: false,
                    eventOrganizer = data["eventOrganizer"] as? String ?: "",
                    eventDescription = data["eventDescription"] as? String ?: "",
                    createdAtMs = (data["createdAtMs"] as? Number)?.toLong() ?: 0L
                )
            }
            val boughtTicketsResult = firestore.collection("boughtTickets")
                .get()
                .await()

            val boughtTickets = boughtTicketsResult.toObjects(BoughtTicket::class.java)
            val boughtTicketsCount = boughtTickets.size

                val endedEvents = mutableListOf<Event>()
                val liveEvents = mutableListOf<Event>()
                val todayEvents = mutableListOf<Event>()
                val upcomingEvents = mutableListOf<Event>()

                events.forEach { event ->
                    when (EventDateTimeUtils.classifyEventBucket(event)) {
                        EventTimelineBucket.ENDED -> endedEvents.add(event)
                        EventTimelineBucket.LIVE -> liveEvents.add(event)
                        EventTimelineBucket.TODAY -> todayEvents.add(event)
                        EventTimelineBucket.UPCOMING -> upcomingEvents.add(event)
                        EventTimelineBucket.UNKNOWN -> Unit
                }
                }

                _adminHomeUiState.value = AdminHomeUiState(
                    totalEvents = events.size,
                    totalTickets = boughtTicketsCount,
                    upcomingEvents = upcomingEvents.distinctBy { it.id }.sortedByDescending { it.createdAtMs }.take(6),
                    todayEvents = todayEvents.distinctBy { it.id }.sortedByDescending { it.createdAtMs },
                    liveEvents = liveEvents.distinctBy { it.id }.sortedByDescending { it.createdAtMs },
                    endedEvents = endedEvents.distinctBy { it.id }.sortedByDescending { it.createdAtMs }
                )
                setSuccess()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun updateUsernameInSharedPreferences(username: String) {
        sharedPreferences.updateUsername(username)
    }

    fun updateProfileImageUrlInSharedPreferences(newProfileImageUrl: String?) {
        sharedPreferences.updateProfileImageUrl(newProfileImageUrl)
    }

    fun updateHeaderInSharedPreferences(newHeaderImageUrl: String?) {
        sharedPreferences.updateHeaderImageUrl(newHeaderImageUrl)
    }

    suspend fun uploadImageToSupabaseStorage(imageUri: Uri, folder: String): String? {
        if (!isNetworkAvailable()) {
            setFailure("No internet connection. Check your network and try again.")
            return null
        }

        return try {
            val functionsBase = BuildConfig.SUPABASE_FUNCTIONS_BASE_URL.trim().trimEnd('/')
            val anonKey = BuildConfig.SUPABASE_FUNCTIONS_ANON_KEY.trim()
            if (functionsBase.isBlank() || anonKey.isBlank()) {
                setFailure("Supabase configuration is missing in BuildConfig.")
                return null
            }

            val projectBase = when {
                functionsBase.endsWith("/functions/v1") -> functionsBase.removeSuffix("/functions/v1")
                functionsBase.contains("/functions/") -> functionsBase.substringBefore("/functions/")
                else -> functionsBase
            }.trimEnd('/')

            if (projectBase.isBlank()) {
                setFailure("Invalid Supabase base URL configuration.")
                return null
            }

            val mimeType = appContext.contentResolver.getType(imageUri) ?: "image/jpeg"
            val allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp")
            if (mimeType !in allowedMimeTypes) {
                setFailure("Unsupported image type. Use JPG, PNG, or WEBP.")
                return null
            }
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }

            val bytes = appContext.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Could not read selected image.")
            val maxBytes = 5 * 1024 * 1024
            if (bytes.size > maxBytes) {
                setFailure("Image is too large. Maximum size is 5MB.")
                return null
            }

            val cleanFolder = folder.trim().trim('/')
            val userId = auth.currentUser?.uid?.trim().orEmpty()
            val bucket = "admin-images"
            val safeFolder = if (cleanFolder.isBlank()) "misc" else cleanFolder
            val timestamp = System.currentTimeMillis()
            val fileName = "${safeFolder.replace("/", "_")}_${timestamp}.${extension}"
            val objectPath = if (userId.isNotBlank()) {
                "$userId/$safeFolder/$fileName"
            } else {
                "anonymous/$safeFolder/$fileName"
            }

            val uploadUrl = "$projectBase/storage/v1/object/$bucket/$objectPath"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    setFailure("Supabase storage upload failed (${response.code}): $body")
                    return null
                }
            }

            "$projectBase/storage/v1/object/public/$bucket/$objectPath"
        } catch (e: Exception) {
            setFailure(resolveErrorMessage(e))
            null
        }
    }


    // Function to update profilePictureUrl in Firestore
    fun updateProfilePictureUrlInFirestore(newProfileImageUrl: String) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            setFailure("User is not signed in.")
            return
        }

        launchWhenConnected(tag = "AdminViewModel") {
            firestore.collection("users")
                .document(userId)
                .update("profilePictureUrl", newProfileImageUrl)
                .await()
            Log.d("AdminViewModel", "Saved new profile image to firestore")
        }
    }

    // Function to update headerPictureUrl in Firestore
    fun updateHeaderPictureUrlInFirestore(newHeaderImageUrl: String) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            setFailure("User is not signed in.")
            return
        }

        launchWhenConnected(tag = "AdminViewModel") {
            firestore.collection("users")
                .document(userId)
                .update("headerPictureUrl", newHeaderImageUrl)
                .await()
            Log.d("AdminViewModel", "Saved new header image to firestore")
        }
    }


    // Fetch only the missing profile fields from Firestore.
    suspend fun fetchMissingProfileFieldsFromFirestore(
        fetchUsername: Boolean,
        fetchProfilePictureUrl: Boolean,
        fetchHeaderPictureUrl: Boolean
    ): AdminProfileFallbackData? {
        if (!isNetworkAvailable()) {
            setFailure("No internet connection. Check your network and try again.")
            return null
        }

        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            setFailure("User is not signed in.")
            return null
        }

        if (!fetchUsername && !fetchProfilePictureUrl && !fetchHeaderPictureUrl) {
            return null
        }

        return try {
            val documentSnapshot = firestore.collection("users").document(userId).get().await()
            if (!documentSnapshot.exists()) {
                setFailure("User profile record was not found.")
                return null
            }

            AdminProfileFallbackData(
                username = if (fetchUsername) documentSnapshot.getString("username") else null,
                profilePictureUrl = if (fetchProfilePictureUrl) documentSnapshot.getString("profilePictureUrl") else null,
                headerPictureUrl = if (fetchHeaderPictureUrl) documentSnapshot.getString("headerPictureUrl") else null
            )
        } catch (e: Exception) {
            val message = resolveErrorMessage(e)
            setFailure(message)
            Log.d("AdminViewModel", "Failed to fetch missing profile fields: $message")
            null
        }
    }
}

data class AdminHomeUiState(
    val totalEvents: Int = 0,
    val totalTickets: Int = 0,
    val upcomingEvents: List<Event> = emptyList(),
    val todayEvents: List<Event> = emptyList(),
    val liveEvents: List<Event> = emptyList(),
    val endedEvents: List<Event> = emptyList()
)

data class AdminProfileFallbackData(
    val username: String? = null,
    val profilePictureUrl: String? = null,
    val headerPictureUrl: String? = null
)
