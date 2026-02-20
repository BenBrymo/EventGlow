package com.example.eventglow.admin_main_screen

import android.app.Application
import android.net.Uri
import android.util.Log
import com.example.eventglow.BuildConfig
import com.example.eventglow.common.BaseViewModel
import com.example.eventglow.common.cloudinary.CloudinaryApi
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
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminViewModel(application: Application) : BaseViewModel(application) {

    private val sharedPreferences = UserPreferences(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appContext = application.applicationContext

    private val cloudinaryApi: CloudinaryApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryApi::class.java)
    }

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
                    eventVenue = data["eventVenue"] as? String ?: "",
                    eventStatus = data["eventStatus"] as? String ?: "",
                    eventCategory = data["eventCategory"] as? String ?: "",
                    ticketTypes = (data["ticketTypes"] as? List<Map<String, Any>>)?.map {
                        TicketType(
                            name = it["name"] as? String ?: "",
                            price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                            availableTickets = (it["availableTickets"] as? Number)?.toInt() ?: 0
                        )
                    } ?: emptyList(),
                    imageUri = data["imageUri"] as? String ?: "",
                    isDraft = data["isDraft"] as? Boolean ?: false,
                    isImportant = data["isImportant"] as? Boolean ?: false,
                    eventOrganizer = data["eventOrganizer"] as? String ?: "",
                    eventDescription = data["eventDescription"] as? String ?: ""
                )
            }
            val boughtTicketsResult = firestore.collection("boughtTickets")
                .get()
                .await()

            val boughtTickets = boughtTicketsResult.toObjects(BoughtTicket::class.java)
            val boughtTicketsCount = boughtTickets.size

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }

            fun parseDateOrNull(value: String): Date? = try {
                dateFormat.parse(value)
            } catch (_: Exception) {
                null
            }

            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val today = todayCal.time

            val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrow = tomorrowCal.time

            val nextSixDaysEndCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
            val nextSixDaysEnd = nextSixDaysEndCal.time

            val todayEvents = events.filter { event ->
                val start = parseDateOrNull(event.startDate) ?: return@filter false
                val end = parseDateOrNull(event.endDate) ?: start
                !today.before(start) && !today.after(end)
            }

            val upcomingEvents = events
                .filter { event ->
                    val start = parseDateOrNull(event.startDate) ?: return@filter false
                    val end = parseDateOrNull(event.endDate) ?: start
                    // Event happens within [tomorrow, next 6 days]
                    !end.before(tomorrow) && !start.after(nextSixDaysEnd)
                }
                .filterNot { event -> todayEvents.any { it.id == event.id } }
                .sortedBy { parseDateOrNull(it.startDate) ?: Date(Long.MAX_VALUE) }
                .take(6)

                _adminHomeUiState.value = AdminHomeUiState(
                    totalEvents = events.size,
                    totalTickets = boughtTicketsCount,
                    upcomingEvents = upcomingEvents,
                    todayEvents = todayEvents
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


    suspend fun uploadImageToCloudinary(imageUri: Uri, folder: String): String? {
        if (!isNetworkAvailable()) {
            setFailure("No internet connection. Check your network and try again.")
            return null
        }

        return try {
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
            val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()
            if (cloudName.isBlank() || uploadPreset.isBlank()) {
                setFailure("Cloudinary credentials are missing in BuildConfig.")
                return null
            }

            val mimeType = appContext.contentResolver.getType(imageUri) ?: "image/jpeg"
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }

            val input = appContext.contentResolver.openInputStream(imageUri)
                ?: throw IllegalArgumentException("Could not read selected image.")

            val cleanFolder = folder.trim().trim('/')
            val folderToken = cleanFolder.replace("/", "_")
            val timestamp = System.currentTimeMillis()
            val publicIdValue = "${folderToken}_$timestamp"
            val tempFile = File(appContext.cacheDir, "$publicIdValue.$extension")
            input.use { source ->
                tempFile.outputStream().use { target ->
                    source.copyTo(target)
                }
            }

            val fileRequestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = tempFile.name,
                body = fileRequestBody
            )

            val uploadPresetBody = uploadPreset.toRequestBody("text/plain".toMediaTypeOrNull())
            val folderBody = cleanFolder.toRequestBody("text/plain".toMediaTypeOrNull())
            val assetFolderBody = cleanFolder.toRequestBody("text/plain".toMediaTypeOrNull())
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

            val secureUrl = response.secureUrl
            if (secureUrl.isNullOrBlank()) {
                setFailure("Cloudinary upload failed.")
                null
            } else {
                secureUrl
            }
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
    val todayEvents: List<Event> = emptyList()
)

data class AdminProfileFallbackData(
    val username: String? = null,
    val profilePictureUrl: String? = null,
    val headerPictureUrl: String? = null
)
