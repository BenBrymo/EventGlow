package com.example.eventglow.common.create_account

import android.app.Application
import android.util.Log
import com.example.eventglow.common.BaseViewModel
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class CreateAccountViewModel(application: Application) : BaseViewModel(application) {

    //created an object of UserPreferences class
    private val sharedPreferences = UserPreferences(application)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    //creates a mutableStateFlow variable of type CreateAccountState
    private val _createAccountState = MutableStateFlow<CreateAccountState>(CreateAccountState.Idle)

    //public access to _createAccountState property
    val createAccountState: StateFlow<CreateAccountState> = _createAccountState.asStateFlow()

    //role of user set to user by default
    private val role = "admin"

    suspend fun checkIfUsernameIsTaken(username: String, onResult: (Boolean) -> Unit) {
        if (!isNetworkAvailable()) {
            setFailure("No internet connection. Check your network and try again.")
            onResult(false)
            return
        }
        try {
            val documents = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            val isTaken = !documents.isEmpty
            onResult(isTaken)
            Log.d("CreateAccountViewModel", "Is username taken: $isTaken")

        } catch (e: Exception) {
            Log.e("Firestore", "Exception checking username: ${e.message}")
            setFailure(resolveErrorMessage(e))
            onResult(false) // Assume the username is not taken if there's an exception
        }
    }

    fun createAccount(username: String, email: String, password: String) {
        if (!isNetworkAvailable()) {
            val message = "No internet connection. Check your network and try again."
            setFailure(message)
            _createAccountState.value = CreateAccountState.Failure(message)
            return
        }

        _createAccountState.value = CreateAccountState.Loading
        setLoading()

        launchWhenConnected(
            tag = "CreateAccountViewModel",
            onError = { throwable ->
                _createAccountState.value = CreateAccountState.Failure(resolveErrorMessage(throwable))
            }
        ) {
            auth.createUserWithEmailAndPassword(email, password).await()
            val user = auth.currentUser ?: throw IllegalStateException("User was not created")

            val userMap = hashMapOf(
                "username" to username,
                "email" to email,
                "role" to role,
                "isSuspended" to false,
                "profilePictureUrl" to null,
                "headerPictureUrl" to null,
                "boughtTickets" to emptyList<BoughtTicket>(),
                "bookmarks" to emptyList<Event>(),
                "favouriteEvents" to emptyList<Event>()
            )

            user.sendEmailVerification().await()

            firestore.collection("users").document(user.uid)
                .set(userMap)
                .await()

            sharedPreferences.saveUserInfo(
                email = email,
                userName = username,
                profilePictureUrl = null,
                headerPictureUrl = null,
                role = role,
                boughtTickets = emptyList(),
                filteredEvents = emptyList(),
                bookmarks = emptyList(),
                favoriteEvents = emptyList()
            )

            setSuccess()
            _createAccountState.value = CreateAccountState.Success(
                message = "Great! your account was created successfully"
            )
        }
    }
}

sealed class CreateAccountState() {
    data object Idle : CreateAccountState()
    data object Loading : CreateAccountState()
    data class Success(val message: String) : CreateAccountState()
    data class Failure(var errorMessage: String) : CreateAccountState()
}
