package com.example.eventglow.common.email_verification

import android.app.Application
import com.example.eventglow.common.BaseViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class EmailVerificationViewModel(application: Application) : BaseViewModel(application) {

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _emailSentCount = MutableStateFlow(0)
    val emailSentCount: StateFlow<Int> = _emailSentCount.asStateFlow()

    fun sendVerificationEmail() {
        setLoading()
        launchWhenConnected(tag = "EmailVerificationViewModel") {
            val user = firebaseAuth.currentUser
            if (user == null) {
                setFailure("No authenticated user found.")
                return@launchWhenConnected
            }

            user.sendEmailVerification().await()
            _emailSentCount.value = _emailSentCount.value + 1
            setSuccess()
        }
    }
}
