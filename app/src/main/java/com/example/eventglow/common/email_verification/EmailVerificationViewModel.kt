package com.example.eventglow.common.email_verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EmailVerificationViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    init {
        viewModelScope.launch {
            sendVerificationEmail(
                onSuccess = { /* Handle success, e.g., update UI */ },
                onError = {/* Handle error, e.g., show a message to the user */ }
            )
        }
    }

    fun sendVerificationEmail(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val user = firebaseAuth.currentUser
                user?.let {
                    // Sends verification email and waits for result
                    it.sendEmailVerification().await()
                    onSuccess()
                } ?: run {
                    onError(Exception("No current user found."))
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}