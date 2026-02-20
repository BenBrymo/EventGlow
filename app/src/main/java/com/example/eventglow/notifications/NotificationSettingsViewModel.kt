package com.example.eventglow.notifications

import android.app.Application
import com.example.eventglow.common.BaseViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class NotificationSettingsViewModel(application: Application) : BaseViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    init {
        fetchNotificationPreference()
    }

    fun fetchNotificationPreference() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) return

        setLoading()
        launchWhenConnected(tag = "NotificationSettingsViewModel") {
            val snapshot = firestore.collection("users").document(userId).get().await()
            val enabled = snapshot.getBoolean("notificationsEnabled") ?: true
            _notificationsEnabled.value = enabled
            syncTopicSubscription(enabled)
            setSuccess()
        }
    }

    fun updateNotificationPreference(enabled: Boolean) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            setFailure("User not signed in.")
            return
        }

        val previousValue = _notificationsEnabled.value
        _notificationsEnabled.value = enabled
        _isUpdating.value = true
        clearError()

        launchWhenConnected(
            tag = "NotificationSettingsViewModel",
            onError = {
                _notificationsEnabled.value = previousValue
                _isUpdating.value = false
            }
        ) {
            firestore.collection("users")
                .document(userId)
                .set(mapOf("notificationsEnabled" to enabled), SetOptions.merge())
                .await()

            syncTopicSubscription(enabled)
            _isUpdating.value = false
            setSuccess()
        }
    }

    private fun syncTopicSubscription(enabled: Boolean) {
        val topic = "eventglow_general"
        if (enabled) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
        }
    }
}
