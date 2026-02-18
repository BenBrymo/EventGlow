package com.example.eventglow.common.login

import android.app.Application
import android.util.Log
import com.example.eventglow.common.BaseViewModel
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class LoginViewModel(application: Application) : BaseViewModel(application) {

    //created an object of UserPreferences class
    private val sharedPreferences = UserPreferences(application)

    // Initialize FirebaseAuth instance
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Initialize FirebaseFirestore instance
    private val firestore = FirebaseFirestore.getInstance()

    // MutableStateFlow for login state
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)

    // Public access to the login state
    val loginState: StateFlow<LoginState> get() = _loginState


    // Function to handle login logic
    fun login(email: String, password: String) {
        if (!isNetworkAvailable()) {
            val message = "No internet connection. Check your network and try again."
            setFailure(message)
            _loginState.value = LoginState.Error(message)
            return
        }

        _loginState.value = LoginState.Loading
        setLoading()

        launchSafely(
            tag = "LoginViewModel",
            onError = { throwable ->
                _loginState.value = LoginState.Error(resolveErrorMessage(throwable))
            }
        ) {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()

            val userUid = firebaseAuth.currentUser?.uid
                ?: throw IllegalStateException("Unable to find authenticated user.")

            Log.d("LoginViewModel", "Retrieved user uid: $userUid")
            val document = firestore.collection("users").document(userUid).get().await()

            if (!document.exists()) {
                val message = "There is no user associated with login provided."
                setFailure(message)
                _loginState.value = LoginState.Error(message)
                return@launchSafely
            }

            val emailPref = document.getString("email").orEmpty().ifBlank { email }
            val userName = document.getString("username").orEmpty()
            val profilePictureUrl = document.getString("profilePictureUrl")
            val headerPictureUrl = document.getString("headerPictureUrl")
            val role = document.getString("role").orEmpty()

            val boughtTickets = mapListToBoughtTickets(document.get("boughtTickets"))
            val bookmarks = mapListToEvents(document.get("bookmarks"))
            val favoriteEvents =
                mapListToEvents(document.get("favoriteEvents")).ifEmpty {
                    mapListToEvents(document.get("favouriteEvents"))
                }

            sharedPreferences.saveUserInfo(
                email = emailPref,
                userName = userName,
                profilePictureUrl = profilePictureUrl,
                headerPictureUrl = headerPictureUrl,
                role = role,
                boughtTickets = boughtTickets,
                filteredEvents = emptyList(),
                bookmarks = bookmarks,
                favoriteEvents = favoriteEvents
            )

            Log.d("LoginViewModel", "User data stored to shared preferences")
            setSuccess()
            _loginState.value = LoginState.Success(role)
        }
    }
}


private fun mapListToBoughtTickets(value: Any?): List<BoughtTicket> {
    val list = value as? List<*> ?: return emptyList()
    return list.mapNotNull { (it as? Map<*, *>)?.toBoughtTicket() }
}

private fun mapListToEvents(value: Any?): List<Event> {
    val list = value as? List<*> ?: return emptyList()
    return list.mapNotNull { (it as? Map<*, *>)?.toEvent() }
}

private fun Map<*, *>.toBoughtTicket(): BoughtTicket {
    return BoughtTicket(
        transactionReference = this["transactionReference"] as? String,
        eventOrganizer = this["eventOrganizer"] as? String ?: "",
        eventId = this["eventId"] as? String ?: "",
        eventName = this["eventName"] as? String ?: "",
        startDate = this["startDate"] as? String ?: "",
        eventStatus = this["eventStatus"] as? String ?: "",
        endDate = this["endDate"] as? String ?: "",
        imageUrl = this["imageUrl"] as? String,
        ticketName = this["ticketName"] as? String ?: "",
        ticketPrice = this["ticketPrice"] as? String ?: ""
    )
}

private fun Map<*, *>.toEvent(): Event {
    val ticketTypesRaw = this["ticketTypes"] as? List<*> ?: emptyList<Any>()
    val ticketTypes = ticketTypesRaw.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        TicketType(
            name = map["name"] as? String ?: "",
            price = (map["price"] as? Number)?.toDouble() ?: 0.0,
            availableTickets = (map["availableTickets"] as? Number)?.toInt() ?: 0
        )
    }

    return Event(
        id = this["id"] as? String ?: "",
        eventName = this["eventName"] as? String ?: "",
        startDate = this["startDate"] as? String ?: "",
        endDate = this["endDate"] as? String ?: "",
        isMultiDayEvent = this["isMultiDayEvent"] as? Boolean ?: false,
        eventTime = this["eventTime"] as? String ?: "",
        eventVenue = this["eventVenue"] as? String ?: "",
        eventStatus = this["eventStatus"] as? String ?: "",
        eventCategory = this["eventCategory"] as? String ?: "",
        ticketTypes = ticketTypes,
        imageUri = this["imageUri"] as? String,
        isDraft = this["isDraft"] as? Boolean ?: false,
        isImportant = this["isImportant"] as? Boolean ?: false,
        eventOrganizer = this["eventOrganizer"] as? String ?: "",
        eventDescription = this["eventDescription"] as? String ?: ""
    )
}

// Sealed class representing the possible states of the login process
sealed class LoginState {
    data object Idle : LoginState() // Initial state
    data object Loading : LoginState() // Loading state
    data class Success(val role: String?) : LoginState() // Success state with user role
    data class Error(val message: String) : LoginState() // Error state with message
}
