package com.example.eventglow.dataClass

data class Transaction(
    var id: String = "",
    val status: String = "", // e.g., "success"
    val reference: String = "", // e.g., "rd0bz6z2wu"
    val amount: String = "", // Amount in kobo (e.g., 20000 represents 200.00 NGN)
    val gatewayResponse: String = "", // e.g., "Successful"
    val paidAt: String = "", // e.g., "2022-08-09T14:21:32.000Z"
    val createdAt: String = "", // e.g., "2022-08-09T14:20:57.000Z"
    val channel: String = "", // e.g., "card"
    val currency: String = "", // e.g., "NGN"
    val authorizationCode: String? = "", // e.g., "AUTH_ahisucjkru"
    val cardType: String? = "", // e.g., "visa"
    val bank: String? = "", // e.g., "TEST BANK"
    val customerEmail: String = "", // e.g., "hello@email.com"
    val customerCode: String = "", // e.g., "CUS_i5yosncbl8h2kvc"
    val customerPhoneNumber: String = "" // e.g., "+2348000000000"
)