package com.example.eventglow.dataClass

import kotlinx.serialization.Serializable


@Serializable
data class TicketType(
    val name: String = "",
    val price: Double = 0.0,
    val availableTickets: Int = 1,
    val isFree: Boolean = false
)
