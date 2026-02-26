package com.example.eventglow.ticket_management

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventglow.dataClass.TicketType
import com.example.eventglow.dataClass.Transaction
import com.example.eventglow.reportingandanalytics.TicketAnalyticsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TicketManagementViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val repository = TicketAnalyticsRepository(firestore)

    private val _events = MutableStateFlow<List<ManagedTicketEvent>>(emptyList())
    val events: StateFlow<List<ManagedTicketEvent>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _attendees = MutableStateFlow<List<ManagedTicketAttendee>>(emptyList())
    val attendees: StateFlow<List<ManagedTicketAttendee>> = _attendees.asStateFlow()

    private val _salesSummary = MutableStateFlow(ManagedSalesSummary())
    val salesSummary: StateFlow<ManagedSalesSummary> = _salesSummary.asStateFlow()

    private val _recentScanActivity = MutableStateFlow<List<RecentScanActivityItem>>(emptyList())
    val recentScanActivity: StateFlow<List<RecentScanActivityItem>> = _recentScanActivity.asStateFlow()

    init {
        fetchManagedEvents()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun fetchManagedEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val aggregate = repository.fetchAggregate()
                val mappedEvents = aggregate.events.map { snapshot ->
                    ManagedTicketEvent(
                        id = snapshot.id,
                        title = snapshot.title,
                        date = snapshot.date,
                        imageUrl = snapshot.imageUrl,
                        soldCount = aggregate.soldByEvent[snapshot.id] ?: 0,
                        availableCount = snapshot.ticketTypes.sumOf { it.availableTickets },
                        ticketTypes = snapshot.ticketTypes
                    )
                }

                _events.value = mappedEvents
                _salesSummary.value = ManagedSalesSummary(
                    totalSold = mappedEvents.sumOf { it.soldCount },
                    totalRevenue = aggregate.totalRevenue,
                    totalFreeTickets = aggregate.totalFreeTickets
                )
                _recentScanActivity.value = aggregate.recentScans.map {
                    RecentScanActivityItem(
                        eventName = it.eventName,
                        ticketReference = it.ticketReference,
                        scannedAt = it.scannedAt,
                        scannedByAdminName = it.scannedByAdminName
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load ticket management data."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAttendeesForEvent(eventId: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                _attendees.value = repository.loadAttendeesForEvent(eventId).map {
                    ManagedTicketAttendee(
                        userId = it.userId,
                        userName = it.userName,
                        userEmail = it.userEmail,
                        ticketReference = it.ticketReference,
                        ticketName = it.ticketName,
                        ticketPrice = it.ticketPrice,
                        isScanned = it.isScanned,
                        scannedAt = it.scannedAt
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load attendees."
            }
        }
    }

    fun updateEventTicketTypes(eventId: String, ticketTypes: List<TicketType>, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                if (ticketTypes.isEmpty()) {
                    _errorMessage.value = "At least one ticket type is required."
                    onDone(false)
                    return@launch
                }
                repository.updateEventTicketTypes(eventId, ticketTypes)
                fetchManagedEvents()
                onDone(true)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update ticket types."
                onDone(false)
            }
        }
    }

    suspend fun getTransactionByReference(ticketReference: String): Transaction? {
        val transactionsRef = firestore.collection("transactions")
        Log.d("TransactionSearch", "Starting search for transaction with reference: $ticketReference")

        return try {
            val querySnapshot = transactionsRef
                .whereEqualTo("reference", ticketReference)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.w("TransactionSearch", "No transaction found with reference: $ticketReference")
                return null
            }

            val document = querySnapshot.documents.firstOrNull()
            val transaction = document?.toObject(Transaction::class.java)?.apply {
                id = document.id
            }
            transaction
        } catch (e: Exception) {
            Log.e("TransactionSearch", "Failed to retrieve transaction: ${e.message}")
            null
        }
    }

    suspend fun markTicketAsScanned(rawQrValue: String): TicketScanResult {
        val rawValue = rawQrValue.trim()
        if (rawValue.isBlank()) return TicketScanResult.Error("Scanned QR is empty.")

        val reference = extractReference(rawValue)
        if (reference.isBlank()) return TicketScanResult.Error("Invalid QR payload.")

        val usersSnapshot = firestore.collection("users").get().await()
        val currentAdmin = FirebaseAuth.getInstance().currentUser
        val adminId = currentAdmin?.uid.orEmpty()
        val adminName = currentAdmin?.email.orEmpty()
        val scannedAt = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        for (userDoc in usersSnapshot.documents) {
            val tickets = (userDoc.get("boughtTickets") as? List<Map<String, Any?>>)
                ?.map { it.toMutableMap() }
                ?.toMutableList()
                ?: mutableListOf()

            val index = tickets.indexOfFirst { ticket ->
                val ticketReference = ticket["transactionReference"] as? String ?: ""
                val qrCodeData = ticket["qrCodeData"] as? String ?: ""
                ticketReference == reference || qrCodeData == rawValue
            }
            if (index == -1) continue

            val ticket = tickets[index]
            val isScanned = ticket["isScanned"] as? Boolean ?: false
            if (isScanned) {
                val previousAt = ticket["scannedAt"] as? String ?: ""
                val previousBy = ticket["scannedByAdminName"] as? String ?: ""
                return TicketScanResult.AlreadyScanned(previousAt, previousBy)
            }

            ticket["isScanned"] = true
            ticket["scannedAt"] = scannedAt
            ticket["scannedByAdminId"] = adminId
            ticket["scannedByAdminName"] = adminName
            tickets[index] = ticket

            userDoc.reference.update("boughtTickets", tickets).await()
            val eventName = ticket["eventName"] as? String ?: ""
            return TicketScanResult.Success(reference = reference, eventName = eventName)
        }

        return TicketScanResult.NotFound(reference)
    }

    private fun extractReference(rawValue: String): String {
        if (rawValue.startsWith("eventglow_ticket|")) {
            val parts = rawValue.split("|")
            if (parts.size >= 2) return parts[1].trim()
        }
        return rawValue
    }
}

data class ManagedTicketEvent(
    val id: String,
    val title: String,
    val date: String,
    val imageUrl: String,
    val soldCount: Int,
    val availableCount: Int,
    val ticketTypes: List<TicketType>
)

data class ManagedTicketAttendee(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val ticketReference: String,
    val ticketName: String,
    val ticketPrice: String,
    val isScanned: Boolean,
    val scannedAt: String
)

data class ManagedSalesSummary(
    val totalSold: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalFreeTickets: Int = 0
)

data class RecentScanActivityItem(
    val eventName: String,
    val ticketReference: String,
    val scannedAt: String,
    val scannedByAdminName: String
)

sealed class TicketScanResult {
    data class Success(val reference: String, val eventName: String) : TicketScanResult()
    data class NotFound(val reference: String) : TicketScanResult()
    data class AlreadyScanned(val scannedAt: String, val scannedBy: String) : TicketScanResult()
    data class Error(val message: String) : TicketScanResult()
}
