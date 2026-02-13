package com.example.eventglow.common.create_account

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.Event
import com.example.eventglow.dataClass.User
import com.example.eventglow.dataClass.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class CreateAccountViewModel(application: Application) : AndroidViewModel(application) {

    //instance of User data class
    private val userClassInstance = User()

    //created an object of UserPreferences class
    private val sharedPreferences = UserPreferences(application)

    //creates a mutableStateFlow variable of type CreateAccountState
    private val _createAccountState = MutableStateFlow<CreateAccountState>(CreateAccountState.Idle)

    //public access to _createAccountState property
    val createAccountState: StateFlow<CreateAccountState> = _createAccountState

    //role of user set to user by default
    private val role = "admin"

    suspend fun checkIfUsernameIsTaken(username: String, onResult: (Boolean) -> Unit) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            //retrives users collection
            firestore.collection("users")

                //only get documents that match the username entered by user
                .whereEqualTo("username", username)
                .get()

                //listen for completion response
                .addOnCompleteListener { task ->

                    //if documents are fetched succesfully
                    if (task.isSuccessful) {

                        //get returned results
                        val documents = task.result
                        val isTaken = documents != null && !documents.isEmpty // sets is Taken to true
                        onResult(isTaken)
                        Log.e("Firestore", "Is username take : $isTaken")
                    } else {
                        Log.e("Firestore", "Error checking username: ${task.exception?.message}")
                        onResult(false) // Assume the username is not taken if there's an error
                    }
                }

        } catch (e: Exception) {
            Log.e("Firestore", "Exception checking username: ${e.message}")
            onResult(false) // Assume the username is not taken if there's an exception
        }
    }

    fun createAccount(username: String, email: String, password: String) {
        // Get an instance of FirebaseAuth
        val auth = FirebaseAuth.getInstance()

        try {
            //createAccountState initially set to loading
            _createAccountState.value = CreateAccountState.Loading

            //start creating account
            auth.createUserWithEmailAndPassword(email, password)

                //listen for task completion response
                .addOnCompleteListener { task ->

                    // Check if the task was successful
                    if (task.isSuccessful) {

                        //get the nearly created user object
                        val user = auth.currentUser

                        //creates instance of firestore
                        val db = FirebaseFirestore.getInstance()

                        //creates hashMap of user data
                        val userMap = hashMapOf(
                            "username" to username,
                            "email" to email,
                            "role" to role, // sets role to user for every new account
                            "isSuspended" to false,
                            "profilePictureUrl" to null,
                            "headerPictureUrl" to null,
                            "boughtTickets" to emptyList<BoughtTicket>(),
                            "bookmarks" to emptyList<Event>(),
                            "favouriteEvents" to emptyList<Event>()
                        )

                        // Store user information in Firestore
                        user?.let { userObject ->

                            //send verification email and listen for completion response
                            user.sendEmailVerification().addOnCompleteListener { emailSendTask ->
                                if (emailSendTask.isSuccessful) {
                                    Log.d("Auth", "Verification email sent.")
                                } else {
                                    Log.e("Auth", "Failed to send verification email.", emailSendTask.exception)
                                    _createAccountState.value =
                                        CreateAccountState.Failure(getAuthErrorMessage(emailSendTask.exception))
                                }
                            }

                            // creates a new document with user's uid as id
                            db.collection("users").document(userObject.uid)

                                //add the data
                                .set(userMap)
                                .addOnSuccessListener {

                                    //save user info to shared preferences
                                    sharedPreferences.saveUserInfo(
                                        email, username, null, null, role, boughtTickets = emptyList(), emptyList(),
                                        emptyList(), emptyList()
                                    )
                                    Log.e(
                                        "Create Account ViewModel",
                                        "Data to be saved to shared Preference Data: $email, $username,$role, null,null, emptyList()"
                                    )

                                    // When the user logs in or registers
                                    _createAccountState.value =
                                        CreateAccountState.Success(message = "Great! your account was created successfully")
                                }
                                .addOnFailureListener { e ->
                                    _createAccountState.value = CreateAccountState.Failure(getAuthErrorMessage(e))
                                }
                        }

                    } else {
                        _createAccountState.value = CreateAccountState.Failure(getAuthErrorMessage(task.exception))
                    }
                }
        } catch (e: Exception) {
            Log.e("CreateAccountViewModel", "Error creating account", e)
            val errorMessage = getAuthErrorMessage(e)
            _createAccountState.value = CreateAccountState.Failure(errorMessage)
        }
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

sealed class CreateAccountState() {
    data object Idle : CreateAccountState()
    data object Loading : CreateAccountState()
    data class Success(val message: String) : CreateAccountState()
    data class Failure(var errorMessage: String) : CreateAccountState()
}
