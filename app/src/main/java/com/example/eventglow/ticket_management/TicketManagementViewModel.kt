package com.example.eventglow.ticket_management

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.eventglow.dataClass.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TicketManagementViewModel : ViewModel() {

    suspend fun getTransactionByReference(ticketReference: String): Transaction? {
        val db = FirebaseFirestore.getInstance()
        val transactionsRef = db.collection("transactions")

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

            if (transaction != null) {
                Log.d("TransactionSearch", "Transaction found: $transaction")
            } else {
                Log.w("TransactionSearch", "Transaction document is null after conversion")
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

        val usersSnapshot = FirebaseFirestore.getInstance().collection("users").get().await()
        val currentAdmin = FirebaseAuth.getInstance().currentUser
        val adminId = currentAdmin?.uid.orEmpty()
        val adminName = currentAdmin?.email.orEmpty()
        val scannedAt = java.time.Instant.now().toString()

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

sealed class TicketScanResult {
    data class Success(val reference: String, val eventName: String) : TicketScanResult()
    data class NotFound(val reference: String) : TicketScanResult()
    data class AlreadyScanned(val scannedAt: String, val scannedBy: String) : TicketScanResult()
    data class Error(val message: String) : TicketScanResult()
}
