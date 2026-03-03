package com.example.eventglow.user

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.eventglow.common.BaseViewModel
import com.example.eventglow.dataClass.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionViewModel(application: Application) : BaseViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    private val isoDateFormatNoMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    private val localDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            clearError()
            try {
                val currentUid = auth.currentUser?.uid?.trim().orEmpty()
                val currentEmail = auth.currentUser?.email?.trim().orEmpty()

                val querySnapshot = if (currentUid.isNotBlank()) {
                    val byUserId = firestore.collection("transactions")
                        .whereEqualTo("userId", currentUid)
                        .get()
                        .await()

                    if (!byUserId.isEmpty) {
                        byUserId
                    } else if (currentEmail.isNotBlank()) {
                        firestore.collection("transactions")
                            .whereEqualTo("customerEmail", currentEmail)
                            .get()
                            .await()
                    } else {
                        byUserId
                    }
                } else if (currentEmail.isNotBlank()) {
                    firestore.collection("transactions")
                        .whereEqualTo("customerEmail", currentEmail)
                        .get()
                        .await()
                } else {
                    firestore.collection("transactions")
                        .get()
                        .await()
                }

                val transactionList = querySnapshot.documents
                    .mapNotNull { document ->
                        runCatching {
                            document.toObject(Transaction::class.java)?.copy(
                                id = document.getString("id").orEmpty().ifBlank { document.id }
                            )
                        }.getOrNull()
                    }
                    .sortedByDescending { parseDateForSort(it.paidAt, it.createdAt) }

                _transactions.value = transactionList
                Log.d("TransactionViewModel", "Fetched ${transactionList.size} transactions")
            } catch (e: Exception) {
                _transactions.value = emptyList()
                setFailure(e.localizedMessage ?: "Failed to fetch transactions")
                Log.e("TransactionViewModel", "Error fetching transactions", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseDateForSort(primary: String, fallback: String): Long {
        val candidate = primary.ifBlank { fallback }
        if (candidate.isBlank()) return 0L
        return try {
            isoDateFormat.parse(candidate)?.time
                ?: isoDateFormatNoMillis.parse(candidate)?.time
                ?: localDateFormat.parse(candidate)?.time
                ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
