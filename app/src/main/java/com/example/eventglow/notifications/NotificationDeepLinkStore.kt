package com.example.eventglow.notifications

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

data class NotificationDeepLink(
    val route: String,
    val eventId: String? = null
)

object NotificationDeepLinkStore {
    const val EXTRA_ROUTE = "deep_link_route"
    const val EXTRA_EVENT_ID = "deep_link_event_id"

    private val _pendingDeepLink = MutableStateFlow<NotificationDeepLink?>(null)

    fun setFromIntent(intent: Intent?) {
        val route = intent?.getStringExtra(EXTRA_ROUTE)?.trim().orEmpty()
        if (route.isBlank()) return
        _pendingDeepLink.value = NotificationDeepLink(
            route = route,
            eventId = intent?.getStringExtra(EXTRA_EVENT_ID)
        )
    }

    fun consume(): NotificationDeepLink? {
        val current = _pendingDeepLink.value
        _pendingDeepLink.value = null
        return current
    }
}
