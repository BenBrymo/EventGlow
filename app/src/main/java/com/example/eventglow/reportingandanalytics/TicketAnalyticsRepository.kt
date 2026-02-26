package com.example.eventglow.reportingandanalytics

import com.example.eventglow.dataClass.TicketType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TicketAnalyticsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun fetchAggregate(): TicketAnalyticsAggregate {
        val eventsSnapshot = firestore.collection("events")
            .whereEqualTo("isDraft", false)
            .get()
            .await()

        val usersSnapshot = firestore.collection("users").get().await()
        val soldByEvent = mutableMapOf<String, Int>()
        val revenueByEvent = mutableMapOf<String, Double>()
        var totalRevenue = 0.0
        var totalFree = 0
        val recentScans = mutableListOf<TicketScanSnapshot>()

        usersSnapshot.documents.forEach { userDoc ->
            val tickets = (userDoc.get("boughtTickets") as? List<Map<String, Any?>>).orEmpty()
            tickets.forEach { ticket ->
                val eventId = ticket["eventId"] as? String ?: return@forEach
                soldByEvent[eventId] = (soldByEvent[eventId] ?: 0) + 1

                val isFree = ticket["isFreeTicket"] as? Boolean ?: false
                if (isFree) {
                    totalFree += 1
                } else {
                    totalRevenue += (ticket["ticketPrice"] as? String)?.toDoubleOrNull() ?: 0.0
                    val eventRevenue = (ticket["ticketPrice"] as? String)?.toDoubleOrNull() ?: 0.0
                    revenueByEvent[eventId] = (revenueByEvent[eventId] ?: 0.0) + eventRevenue
                }

                val isScanned = ticket["isScanned"] as? Boolean ?: false
                if (isScanned) {
                    recentScans.add(
                        TicketScanSnapshot(
                            eventName = ticket["eventName"] as? String ?: "Unknown event",
                            ticketReference = ticket["transactionReference"] as? String ?: "",
                            scannedAt = ticket["scannedAt"] as? String ?: "",
                            scannedByAdminName = ticket["scannedByAdminName"] as? String ?: ""
                        )
                    )
                }
            }
        }

        val eventSnapshots = eventsSnapshot.documents.map { doc ->
            val ticketTypes = (doc.get("ticketTypes") as? List<Map<String, Any?>>).orEmpty().map { map ->
                TicketType(
                    name = map["name"] as? String ?: "",
                    price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                    availableTickets = (map["availableTickets"] as? Number)?.toInt() ?: 0,
                    isFree = map["isFree"] as? Boolean ?: false
                )
            }
            AnalyticsEventSnapshot(
                id = doc.id,
                title = doc.getString("eventName").orEmpty(),
                date = doc.getString("startDate").orEmpty(),
                imageUrl = doc.getString("imageUri").orEmpty(),
                ticketTypes = ticketTypes
            )
        }

        return TicketAnalyticsAggregate(
            events = eventSnapshots,
            soldByEvent = soldByEvent,
            revenueByEvent = revenueByEvent,
            totalRevenue = totalRevenue,
            totalFreeTickets = totalFree,
            recentScans = recentScans.sortedByDescending { it.scannedAt }.take(20)
        )
    }

    suspend fun loadAttendeesForEvent(eventId: String): List<AnalyticsAttendeeSnapshot> {
        val usersSnapshot = firestore.collection("users").get().await()
        val result = mutableListOf<AnalyticsAttendeeSnapshot>()

        usersSnapshot.documents.forEach { userDoc ->
            val userName = userDoc.getString("username").orEmpty()
            val userEmail = userDoc.getString("email").orEmpty()
            val tickets = (userDoc.get("boughtTickets") as? List<Map<String, Any?>>).orEmpty()
            tickets.forEach { ticket ->
                val ticketEventId = ticket["eventId"] as? String ?: return@forEach
                if (ticketEventId != eventId) return@forEach

                result.add(
                    AnalyticsAttendeeSnapshot(
                        userId = userDoc.id,
                        userName = userName,
                        userEmail = userEmail,
                        ticketReference = ticket["transactionReference"] as? String ?: "",
                        ticketName = ticket["ticketName"] as? String ?: "",
                        ticketPrice = ticket["ticketPrice"] as? String ?: "",
                        isScanned = ticket["isScanned"] as? Boolean ?: false,
                        scannedAt = ticket["scannedAt"] as? String ?: ""
                    )
                )
            }
        }

        return result
    }

    suspend fun updateEventTicketTypes(eventId: String, ticketTypes: List<TicketType>) {
        val mapped = ticketTypes.map {
            mapOf(
                "name" to it.name.trim(),
                "price" to it.price,
                "availableTickets" to it.availableTickets,
                "isFree" to it.isFree
            )
        }
        firestore.collection("events").document(eventId).update("ticketTypes", mapped).await()
    }
}

data class TicketAnalyticsAggregate(
    val events: List<AnalyticsEventSnapshot>,
    val soldByEvent: Map<String, Int>,
    val revenueByEvent: Map<String, Double>,
    val totalRevenue: Double,
    val totalFreeTickets: Int,
    val recentScans: List<TicketScanSnapshot>
)

data class AnalyticsEventSnapshot(
    val id: String,
    val title: String,
    val date: String,
    val imageUrl: String,
    val ticketTypes: List<TicketType>
)

data class TicketScanSnapshot(
    val eventName: String,
    val ticketReference: String,
    val scannedAt: String,
    val scannedByAdminName: String
)

data class AnalyticsAttendeeSnapshot(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val ticketReference: String,
    val ticketName: String,
    val ticketPrice: String,
    val isScanned: Boolean,
    val scannedAt: String
)
