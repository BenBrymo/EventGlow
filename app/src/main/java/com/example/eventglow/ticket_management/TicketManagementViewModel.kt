package com.example.eventglow.ticket_management

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.eventglow.dataClass.Transaction
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

}