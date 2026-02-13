package com.example.eventglow.user


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventglow.dataClass.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class TransactionViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            firestore.collection("transactions")
                .get()
                .addOnSuccessListener { result ->
                    val transactionList = mutableListOf<Transaction>()
                    for (document in result) {
                        val transaction = document.toObject(Transaction::class.java)
                        transactionList.add(transaction)
                    }
                    _transactions.value = transactionList
                    _isLoading.value = false
                    Log.d("TransactionViewModel", "Fetched ${transactionList.size} transactions.")
                }
                .addOnFailureListener { e ->
                    Log.e("TransactionViewModel", "Error fetching transactions: ${e.message}", e)
                    _isLoading.value = false
                }
        }
    }
}
