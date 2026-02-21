package com.example.eventglow.dataClass

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    var id: String = "",
    val eventName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isMultiDayEvent: Boolean = false,
    val eventTime: String = "",
    val eventVenue: String = "",
    val eventStatus: String = "",
    val eventCategory: String = "",
    val ticketTypes: List<TicketType> = emptyList(),
    val imageUri: String? = null,
    val isDraft: Boolean = false,
    val isImportant: Boolean = false,
    val eventOrganizer: String = "",
    val eventDescription: String = "",
    val durationLabel: String = "",
    val durationMinutes: Int = 0,
    val createdAtMs: Long = 0L
)

