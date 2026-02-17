package com.example.eventglow.admin_main_screen

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.eventglow.common.BaseViewModel
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminViewModel(application: Application) : BaseViewModel(application) {

    private val sharedPreferences = UserPreferences(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _adminHomeUiState = MutableStateFlow(AdminHomeUiState())
    val adminHomeUiState: StateFlow<AdminHomeUiState> = _adminHomeUiState.asStateFlow()

    init {
        fetchAdminHomeData()
    }


    fun fetchAdminHomeData() {
        setLoading()
        launchWhenConnected(tag = "AdminViewModel") {
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

    // Function to update profilePictureUrl in Firestore
    fun updateProfilePictureUrlInFirestore(newProfileImageUrl: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val userDocRef = firestore.collection("users").document(userId)
            userDocRef.update("profilePictureUrl", newProfileImageUrl)
                .addOnSuccessListener {
                    // Update successful
                    Log.d("AdminViewModel", "Saved new profile image to firestore")
                }
                .addOnFailureListener { exception ->
                    // Handle the error
                    Log.d("AdminViewModel", "Failed to saved new profile image to firestore $exception.message")
                }
        }
    }

    // Function to fetch profilePictureUrl from Firestore
    suspend fun fetchProfilePictureUrlFromFirestore(): String? {
        val userId = auth.currentUser?.uid ?: return null

        return try {
            val userDocRef = firestore.collection("users").document(userId)
            val documentSnapshot = userDocRef.get().await()
            if (documentSnapshot.exists()) {
                documentSnapshot.getString("profilePictureUrl")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d("AdminViewModel", "Failed to fetch profile image from Firestore: ${e.message}")
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
