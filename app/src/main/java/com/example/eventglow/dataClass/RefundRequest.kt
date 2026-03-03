package com.example.eventglow.dataClass

data class RefundRequest(
    val id: String = "",
    val transactionReference: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val eventId: String = "",
    val eventName: String = "",
    val ticketName: String = "",
    val amount: String = "",
    val currency: String = "GHS",
    val reason: String = "",
    val status: String = "pending",
    val createdAt: String = "",
    val reviewedAt: String = "",
    val reviewedByAdminId: String = "",
    val adminNote: String = ""
)
