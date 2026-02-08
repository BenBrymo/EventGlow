package com.example.eventglow.user_management

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventglow.dataClass.BoughtTicket
import com.example.eventglow.dataClass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserManagementViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> get() = _users

    private val _searchQueryUser = MutableStateFlow("")
    val searchQueryUser: StateFlow<String> get() = _searchQueryUser

    private val _filteredUsers = MutableStateFlow<List<User>>(emptyList())
    val filteredUsers: StateFlow<List<User>> = _filteredUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    init {
        fetchUsers()
    }

    fun fetchUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                usersCollection.get()
                    .addOnSuccessListener { result ->
                        val userList = result.documents.mapNotNull { document ->
                            val data = document.data
                            Log.d("FirestoreData", "Document ID: ${document.id}, Data: $data")

                            // Manually map Firestore data to User class
                            try {
                                val user = User(
                                    id = document.id,
                                    userName = data?.get("username") as? String ?: "",
                                    email = data?.get("email") as? String ?: "",
                                    role = data?.get("role") as? String ?: "user", // Default role is "user"
                                    isSuspended = data?.get("isSuspended") as? Boolean ?: false,
                                    boughtTickets = (data?.get("boughtTickets") as? List<Map<String, Any>>)?.map {
                                        BoughtTicket(
                                            transactionReference = it["transactionReference"] as? String ?: "",
                                            eventOrganizer = it["eventOrganizer"] as? String ?: "",
                                            eventId = it["eventId"] as? String ?: "",
                                            eventName = it["eventName"] as? String ?: "",
                                            startDate = it["startDate"] as? String ?: "",
                                            eventStatus = it["eventStatus"] as? String ?: "",
                                            endDate = it["endDate"] as? String ?: "",
                                            imageUrl = it["imageUrl"] as? String,
                                            ticketName = it["ticketName"] as? String ?: "",
                                            ticketPrice = it["ticketPrice"] as? String ?: ""
                                        )
                                    } ?: listOf(),
                                    profilePictureUrl = data?.get("profilePictureUrl") as? String ?: ""
                                )

                                // Log the mapped user data
                                Log.d("MappedUser", "Mapped User: $user")
                                user // Return the user object

                            } catch (e: Exception) {
                                Log.e("MappingError", "Error mapping user data for document ID: ${document.id}", e)
                                null // Exclude faulty document
                            }
                        }

                        _users.value = userList
                        Log.d("FetchUsersSuccess", "Successfully fetched ${userList.size} users. User list: $userList")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FetchUsersFailure", "Failed to fetch users", exception)
                        handleFirestoreError(exception)
                    }
                    .addOnCompleteListener {
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("FetchUsersError", "Unexpected error during user fetching", e)
                _isLoading.value = false
            }
        }
    }


    private fun handleFirestoreError(exception: Exception) {
        when (exception) {
            is FirebaseFirestoreException -> {
                Log.e("FirestoreError", "Firestore exception occurred: ${exception.message}", exception)
                // Handle specific Firestore errors here, such as network issues
            }

            else -> {
                Log.e("GenericError", "An unexpected error occurred: ${exception.message}", exception)
            }
        }
    }


    fun addUser(email: String, password: String, username: String, role: String, context: Context) {
        // Get an instance of FirebaseAuth
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // Check if the task was successful
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val db = FirebaseFirestore.getInstance()
                    val userMap = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "role" to role, // sets role to user for every new account
                        "password" to password,
                        "isSuspended" to false
                    )

                    // Store user information in Firestore
                    user?.let {
                        db.collection("users").document(it.uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(context, "Account creation failed: ${task.exception?.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun filterUsers(query: String) {
        val filteredListUser = _filteredUsers.value.filter { user ->
            user.userName.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true)
        }
        _filteredUsers.value = filteredListUser
    }

    fun onSearchQueryChangeUser(query: String) {
        _searchQueryUser.value = query
        filterUsers(query)
    }

    fun addUser(user: User) {
        viewModelScope.launch {
            usersCollection.add(user).addOnSuccessListener {
                fetchUsers()
            }
        }
    }

    fun suspendUser(userId: String, suspend: Boolean) {
        viewModelScope.launch {
            usersCollection.document(userId).update("isSuspended", suspend).addOnSuccessListener {
                fetchUsers()
            }
        }
    }
}
