package com.example.eventglow.dataClass

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


private val _users = MutableStateFlow<List<User>>(emptyList())
val users: StateFlow<List<User>> = _users


data class User(
    val id: String = "",
    val userName: String = "",
    val email: String = "",
    val role: String = "",
    val isSuspended: Boolean = false,
    val profilePictureUrl: String? = null,
    val headerPictureUrl: String? = null,
    val boughtTickets: List<BoughtTicket> = emptyList(),
    val bookmarkEvents: List<Event> = emptyList(),
    val favoriteEvents: List<Event> = emptyList()
) {
    fun addUser(
        id: String,
        userName: String,
        email: String,
        role: String,
        isSuspended: Boolean,
        profilePictureUrl: String?,
        headerPictureUrl: String?,
        boughtTickets: List<BoughtTicket>
    ) {
        val newUser = User(id, userName, email, role, isSuspended, profilePictureUrl, headerPictureUrl, boughtTickets)
        //checks if user already exists
        if (_users.value.contains(newUser)) {
            //logs a message
            Log.d("AddUserFunction", "User already exists")
        } else {
            //add to users list
            _users.value += newUser
            Log.d("AddUserFunction", "User created")
        }
    }

}

