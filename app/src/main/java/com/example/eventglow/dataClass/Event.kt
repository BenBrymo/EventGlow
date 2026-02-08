package com.example.eventglow.dataClass

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable


private val _events = MutableStateFlow<List<Event>>(emptyList())
val events: StateFlow<List<Event>> = _events


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
    val eventOrganizer: String = "",
    val eventDescription: String = ""
) {

    fun addEvent(
        id: String,
        eventName: String,
        startDate: String,
        endDate: String,
        isMultiDayEvent: Boolean,
        eventTime: String,
        eventVenue: String,
        eventStatus: String,
        eventCategory: String,
        ticketTypes: List<TicketType>,
        imageUri: String?,
        isDraft: Boolean,
        eventOrganizer: String,
        eventDescription: String
    ) {
        val newEvent = Event(
            id,
            eventName,
            startDate,
            endDate,
            isMultiDayEvent,
            eventTime,
            eventVenue,
            eventStatus,
            eventCategory,
            ticketTypes,
            imageUri,
            isDraft,
            eventOrganizer,
            eventDescription
        )

        //add to users list
        _events.value += newEvent
        Log.d("AddEventFunction", "Event created")
    }
}

