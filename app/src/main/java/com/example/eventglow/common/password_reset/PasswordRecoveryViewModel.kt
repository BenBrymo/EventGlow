package com.example.eventglow.common.password_reset

import android.app.Application
import com.example.eventglow.common.BaseViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class PasswordRecoveryViewModel(application: Application) : BaseViewModel(application) {

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _passwordResetEmailSentCount = MutableStateFlow(0)
    val passwordResetEmailSentCount: StateFlow<Int> = _passwordResetEmailSentCount.asStateFlow()

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            setFailure("Please enter email address.")
            return
        }

        setLoading()
        launchWhenConnected(tag = "PasswordRecoveryViewModel") {
            firebaseAuth.sendPasswordResetEmail(email).await()
            _passwordResetEmailSentCount.value = _passwordResetEmailSentCount.value + 1
            setSuccess()
        }
    }
}
