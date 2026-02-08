package com.example.eventglow.dataClass

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
)


