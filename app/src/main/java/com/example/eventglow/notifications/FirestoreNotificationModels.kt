package com.example.eventglow.notifications

data class FirestoreNotification(
    val title: String = "",
    val body: String = "",
    val route: String = "detailed_event_screen",
    val eventId: String? = null,
    val targetRole: String = "all"
)

