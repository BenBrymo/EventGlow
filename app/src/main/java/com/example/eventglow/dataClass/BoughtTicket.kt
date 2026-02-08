package com.example.eventglow.dataClass

data class BoughtTicket(
    val transactionReference: String? = "",
    val eventOrganizer: String = "",
    val eventId: String = "",
    val eventName: String = "",
    val startDate: String = "",
    val eventStatus: String = "",
    val endDate: String = "",
    val imageUrl: String? = null,
    val ticketName: String = "",
    val ticketPrice: String = ""
)