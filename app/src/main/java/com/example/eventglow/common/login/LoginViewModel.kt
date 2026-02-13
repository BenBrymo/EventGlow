package com.example.eventglow.common.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

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

        // Launch coroutine in ViewModel scope
        viewModelScope.launch {

            // Set state to loading
            _loginState.value = LoginState.Loading
            try {

                // Attempt to sign in with email and password
                firebaseAuth.signInWithEmailAndPassword(email, password)

                    //to listen if sign in is complete
                    .addOnCompleteListener { task ->

                        // If login is successful
                        if (task.isSuccessful) {

                            // Retrieve user role from Firestore
                            val user = firebaseAuth.currentUser?.uid // Get current user's UID
                            Log.d("LoginVIewModel", "Retrived User uid: $user")

                            //Perform operations on user object
                            user?.let { userUID ->

                                // Fetch user document from Firestore
                                firestore.collection("users").document(userUID).get()

                                    //to listen if user document is fetch successfully
                                    .addOnSuccessListener { document ->

                                        //check if retrived document is not null or empty
                                        if (document != null && document.exists()) {

                                            //get user data to store in shared preference
                                            val emailPref = document.getString("email") ?: ""
                                            val userName = document.getString("username") ?: ""
                                            val profilePictureUrl = document.getString("profilePictureUrl")
                                            val headerPictureUrl = document.getString("headerPictureUrl")
                                            val role = document.getString("role") ?: ""
                                            // Fetch the boughtTickets field, which is an array
                                            val boughtTickets =
                                                document.get("boughtTickets") as? List<*> ?: emptyList<BoughtTicket>()

                                            //get tickets as map list
                                            val tickets = boughtTickets.mapNotNull {

                                                // Convert each item to BoughtTicket if needed
                                                (it as? Map<*, *>)?.let { ticketMap ->

                                                    convertMapToBoughtTicket(ticketMap)
                                                }
                                            }

                                            Log.d(
                                                "LoginViewModel",
                                                "Retrived user info: $emailPref,$userName, $profilePictureUrl,$headerPictureUrl,$role,$tickets"
                                            )
                                            // Save the retrieved user info to SharedPreferences
                                            sharedPreferences.saveUserInfo(
                                                emailPref,
                                                userName,
                                                profilePictureUrl,
                                                headerPictureUrl,
                                                role,
                                                boughtTickets = tickets,
                                                emptyList(),
                                                emptyList(),
                                                emptyList()
                                            )
                                            Log.d("LoginViewModel", "User data stored to shared preferences")

                                            _loginState.value = LoginState.Success(role)
                                            Log.d("LoginViewModel", "Login State set to success")
                                        } else {
                                            // Update the login state to Error with error message
                                            _loginState.value =
                                                LoginState.Error("There is no user associated with logins provided")
                                        }
                                    }

                                    //to listen if user document fetching fails
                                    .addOnFailureListener { e ->

                                        // Update the login state to Error with error message
                                        Log.d("LoginViewModel", getAuthErrorMessage(e))
                                        _loginState.value = LoginState.Error(getAuthErrorMessage(e))
                                    }

                            }
                            //if login task is unsuccessful
                        } else {
                            // Update the login state to Error with error message
                            Log.d("LoginViewModel", getAuthErrorMessage((task.exception)))
                            _loginState.value = LoginState.Error(getAuthErrorMessage(task.exception))
                        }
                    }
            } catch (e: Exception) {
                // Update the login state to Error with error message
                val errorMessage = getAuthErrorMessage(e)
                _loginState.value = LoginState.Error(errorMessage)
                Log.e("LoginViewModel", "Error creating account", e)
            }
        }
    }
}


fun convertMapToBoughtTicket(ticketMap: Map<*, *>): BoughtTicket? {
    return try {
        BoughtTicket(
            // Replace these with actual field names and conversion logic
            (ticketMap["transactionReference"] as? String)!!,
            (ticketMap["eventOrganizer"] as? String)!!,
            (ticketMap["eventId"] as? String)!!,
            (ticketMap["eventName"] as? String)!!,
            (ticketMap["startDate"] as? String)!!,
            (ticketMap["eventStatus"] as? String)!!,
            (ticketMap["endDate"] as? String)!!,
            (ticketMap["imageUrl"] as? String)!!,
            (ticketMap["ticketName"] as? String)!!,
            (ticketMap["ticketPrice"] as? String)!!,
            // Add other fields as needed
        )
    } catch (e: Exception) {
        null
    }
}

private fun getAuthErrorMessage(exception: Exception?): String {
    return when (exception) {
        is FirebaseAuthException -> {
            when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> "The email address is invalid."
                "ERROR_WRONG_PASSWORD" -> "Wrong username or password."
                "ERROR_USER_NOT_FOUND" -> "No user found with this email."
                "ERROR_USER_DISABLED" -> "User account has been disabled."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many requests. Please try again later."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Operation not allowed. Please enable sign-in method."
                "The supplied auth credential is incorrect, malformed or has expired." -> "Wrong username or password"
                else -> "Authentication error: ${exception.localizedMessage}"
            }
        }

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


// Sealed class representing the possible states of the login process
sealed class LoginState {
    data object Idle : LoginState() // Initial state
    data object Loading : LoginState() // Loading state
    data class Success(val role: String?) : LoginState() // Success state with user role
    data class Error(val message: String) : LoginState() // Error state with message
}
