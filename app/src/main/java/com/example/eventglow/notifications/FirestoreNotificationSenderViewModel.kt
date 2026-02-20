package com.example.eventglow.notifications

import android.app.Application
import com.example.eventglow.common.BaseViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirestoreNotificationSenderViewModel(application: Application) : BaseViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun sendNotificationToRole(
        title: String,
        body: String,
        targetRole: String,
        route: String = "detailed_event_screen",
        eventId: String? = null
    ) {
        if (title.isBlank() || body.isBlank()) {
            setFailure("Title and body are required.")
            return
        }
        if (targetRole.isBlank()) {
            setFailure("Target role is required.")
            return
        }

        _isSending.value = true
        clearError()
        launchWhenConnected(tag = "FirestoreNotificationSenderViewModel", onError = {
            _isSending.value = false
        }) {
            val payload = hashMapOf(
                "title" to title.trim(),
                "body" to body.trim(),
                "route" to route,
                "eventId" to (eventId?.trim().orEmpty()),
                "targetRole" to targetRole.trim(),
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("notifications")
                .add(payload)
                .await()

            _isSending.value = false
            setSuccess()
        }
    }
}

