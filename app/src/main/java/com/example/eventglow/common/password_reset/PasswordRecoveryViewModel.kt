package com.example.eventglow.common.password_reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PasswordRecoveryViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                //sends recovery link to email account and waits for result
                firebaseAuth.sendPasswordResetEmail(email).await()
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}